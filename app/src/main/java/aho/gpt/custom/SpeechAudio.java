package aho.gpt.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.json.JSONObject;

/** OpenAI-compatible audio wire formats. Audio remains in memory. */
public final class SpeechAudio {
    public record Upload(byte[] body, String type) { }
    private SpeechAudio() { }
    public static JSONObject speech(String text) throws Exception {
        return speech(text, SpeechApiConfig.DEFAULT);
    }
    public static JSONObject speech(String text, SpeechApiConfig config) throws Exception {
        return new JSONObject().put("model", config.speechModel()).put("input", text).put("voice", config.voice()).put("response_format", "pcm");
    }
    public static byte[] wav(byte[] pcm, int rate) {
        ByteBuffer buffer = ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + pcm.length);
        buffer.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) 1);
        buffer.putInt(rate).putInt(rate * 2).putShort((short) 2).putShort((short) 16);
        buffer.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(pcm.length).put(pcm);
        return buffer.array();
    }
    public static Upload transcription(byte[] audio, String language) throws IOException {
        return transcription(audio, language, SpeechApiConfig.DEFAULT);
    }
    public static Upload transcription(byte[] audio, String language, SpeechApiConfig config) throws IOException {
        String boundary = "aho-" + UUID.randomUUID();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        field(output, boundary, "model", config.transcriptionModel());
        field(output, boundary, "response_format", "json");
        if (!language.isEmpty()) { field(output, boundary, "language", language); }
        output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(audio);
        output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return new Upload(output.toByteArray(), "multipart/form-data; boundary=" + boundary);
    }
    private static void field(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
    public static String transcript(JSONObject response) throws IOException {
        Object text = response.opt("text");
        if (!(text instanceof String)) { throw new IOException("Invalid transcription"); }
        return ((String) text).trim();
    }
    public static void validatePcm(byte[] audio) throws IOException {
        if (audio.length < 2 || audio.length % 2 != 0) { throw new IOException("Invalid speech audio"); }
        String prefix = new String(audio, 0, Math.min(16, audio.length), StandardCharsets.US_ASCII).trim();
        if (encodedResponse(prefix)) {
            throw new IOException("Expected raw PCM audio");
        }
    }
    private static boolean encodedResponse(String prefix) {
        return prefix.startsWith("{") || prefix.startsWith("<") || prefix.startsWith("RIFF");
    }
}
