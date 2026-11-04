package aho.gpt.custom;

import aho.gpt.core.RequestScope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Compatible speech protocol and functional support check, independent of Android. */
public final class ServerSpeech {
    public interface Transport {
        byte[] exchange(String url, String token, byte[] body, String type, int limit, RequestScope scope) throws Exception;
    }
    private final Transport transport;
    private final SpeechApiConfig config;
    public ServerSpeech(Transport transport) { this(transport, SpeechApiConfig.DEFAULT); }
    public ServerSpeech(Transport transport, SpeechApiConfig config) { this.transport = transport; this.config = config; }
    public byte[] speech(String base, String token, String text, RequestScope scope) throws Exception {
        scope.check();
        byte[] body = SpeechAudio.speech(text, config).toString().getBytes(StandardCharsets.UTF_8);
        byte[] audio = transport.exchange(base + "/audio/speech", token, body, "application/json", 24_000_000, scope);
        scope.check(); SpeechAudio.validatePcm(audio);
        return audio;
    }
    public String transcribe(String base, String token, byte[] wav, String language, RequestScope scope) throws Exception {
        scope.check();
        SpeechAudio.Upload upload = SpeechAudio.transcription(wav, language, config);
        byte[] response = transport.exchange(base + "/audio/transcriptions", token, upload.body(), upload.type(), 1_000_000, scope);
        scope.check(); return SpeechAudio.transcript(new JSONObject(new String(response, StandardCharsets.UTF_8)));
    }
    public void check(String base, String token, RequestScope scope) throws Exception {
        byte[] audio = speech(base, token, "Hello.", scope);
        String text = transcribe(base, token, SpeechAudio.wav(audio, 24_000), "en", scope);
        if (text.isEmpty()) { throw new IOException("Speech APIs unavailable"); }
    }
}
