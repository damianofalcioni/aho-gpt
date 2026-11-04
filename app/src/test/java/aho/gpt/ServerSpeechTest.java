package aho.gpt;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.ServerSpeech;
import aho.gpt.custom.SpeechAudio;
import aho.gpt.custom.SpeechSilence;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServerSpeechTest {
    private static final String BASE = "http://localhost:8080/v1";
    private static final String TOKEN = "test-token";
    @Test public void supportRequiresBothWorkingEndpoints() throws Exception {
        int[] calls = {0};
        ServerSpeech speech = new ServerSpeech((url, token, body, type, limit, scope) -> {
            assertEquals(TOKEN, token);
            if (++calls[0] == 1) {
                assertEquals(BASE + "/audio/speech", url);
                JSONObject request = new JSONObject(new String(body, StandardCharsets.UTF_8));
                assertEquals("tts-1", request.getString("model"));
                assertEquals("alloy", request.getString("voice")); assertEquals("pcm", request.getString("response_format"));
                return new byte[48_000];
            }
            assertEquals(BASE + "/audio/transcriptions", url);
            assertTrue(type.startsWith("multipart/form-data; boundary="));
            String upload = new String(body, StandardCharsets.ISO_8859_1);
            assertTrue(upload.contains("whisper-1")); assertTrue(upload.contains("RIFF"));
            return "{\"text\":\"Hello.\"}".getBytes(StandardCharsets.UTF_8);
        });
        speech.check(BASE, TOKEN, new RequestScope()); assertEquals(2, calls[0]);
    }
    @Test public void absentOrInvalidSpeechCannotPassSupportCheck() {
        ServerSpeech missing = new ServerSpeech((url, token, body, type, limit, scope) -> { throw new IOException("HTTP 404"); });
        assertThrows(IOException.class, () -> missing.check(BASE, TOKEN, new RequestScope()));
        ServerSpeech invalid = new ServerSpeech((url, token, body, type, limit, scope) -> "{}".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> invalid.check(BASE, TOKEN, new RequestScope()));
    }
    @Test public void speechOnlyAndEmptyTranscriptionAreUnsupported() {
        ServerSpeech speechOnly = new ServerSpeech((url, token, body, type, limit, scope) -> {
            if (url.endsWith("/audio/speech")) { return new byte[4800]; }
            throw new IOException("HTTP 501");
        });
        assertThrows(IOException.class, () -> speechOnly.check(BASE, TOKEN, new RequestScope()));
        ServerSpeech empty = new ServerSpeech((url, token, body, type, limit, scope) ->
                url.endsWith("/audio/speech") ? new byte[4800] : "{\"text\":\" \"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> empty.check(BASE, TOKEN, new RequestScope()));
    }
    @Test public void cancellationBetweenEndpointsPreventsTranscription() {
        int[] calls = {0}; RequestScope scope = new RequestScope();
        ServerSpeech speech = new ServerSpeech((url, token, body, type, limit, request) -> {
            calls[0]++; request.cancel(); return new byte[4800];
        });
        assertThrows(InterruptedException.class, () -> speech.check(BASE, TOKEN, scope)); assertEquals(1, calls[0]);
    }
    @Test public void wavHeaderAndLanguagePreserveAudioContract() throws Exception {
        byte[] pcm = {1, 2, 3, 4}; byte[] wav = SpeechAudio.wav(pcm, 16_000);
        ByteBuffer header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(16_000, header.getInt(24)); assertEquals(4, header.getInt(40));
        assertArrayEquals(pcm, Arrays.copyOfRange(wav, 44, wav.length));
        String body = new String(SpeechAudio.transcription(wav, "it").body(), StandardCharsets.ISO_8859_1);
        assertTrue(body.contains("name=\"language\"\r\n\r\nit\r\n"));
        assertThrows(IOException.class, () -> SpeechAudio.transcript(new JSONObject().put("text", 7)));
    }
    @Test public void silenceDoesNotLoopOrCreateASpokenTurn() {
        SpeechSilence gate = new SpeechSilence(); short[] silence = new short[1600];
        for (int index = 0; index < 150; index++) { assertFalse(gate.accept(silence, silence.length)); }
        assertTrue(gate.finished()); assertFalse(gate.began());
    }
    @Test public void speechEndsAfterTrailingSilenceAndHasAMaximumLength() {
        SpeechSilence gate = new SpeechSilence(); short[] voice = new short[1600]; Arrays.fill(voice, (short) 1000);
        assertTrue(gate.accept(voice, voice.length)); assertFalse(gate.accept(voice, voice.length));
        short[] silence = new short[1600];
        for (int index = 0; index < 11; index++) { gate.accept(silence, silence.length); assertFalse(gate.finished()); }
        gate.accept(silence, silence.length); assertTrue(gate.finished());
        SpeechSilence continuous = new SpeechSilence();
        for (int index = 0; index < 202; index++) { continuous.accept(voice, voice.length); }
        assertTrue(continuous.finished());
    }
}
