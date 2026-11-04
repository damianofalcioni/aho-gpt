package aho.gpt;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.ServerSpeech;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.SpeechAudio;
import aho.gpt.custom.SpeechCatalog;
import aho.gpt.custom.realtime.RealtimeProtocol;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpeechConfigurationTest {
    private static final String BASE = "https://example.org/v1";
    private static final String VOICE_A = "speaker-a";
    private static final String CUSTOM_STT = "provider/stt";
    private static final String CUSTOM_TTS = "provider/tts";
    private static final String CUSTOM_VOICE = "speaker-7";
    private static final String REALTIME_VOICE = "cedar";
    private static final SpeechApiConfig CONFIG = new SpeechApiConfig(CUSTOM_STT, CUSTOM_TTS, CUSTOM_VOICE, REALTIME_VOICE);
    @Test public void supportProbeAndActualRequestsUseTheConfiguredIdentifiers() throws Exception {
        int[] requests = {0};
        ServerSpeech speech = new ServerSpeech((url, token, body, type, limit, scope) -> {
            requests[0]++;
            if (url.endsWith("/audio/speech")) {
                JSONObject request = new JSONObject(new String(body, StandardCharsets.UTF_8));
                assertEquals(CUSTOM_TTS, request.getString("model")); assertEquals(CUSTOM_VOICE, request.getString("voice"));
                return new byte[4800];
            }
            String request = new String(body, StandardCharsets.UTF_8);
            assertTrue(request.contains(CUSTOM_STT)); assertFalse(request.contains("whisper-1"));
            return "{\"text\":\"Hello\"}".getBytes(StandardCharsets.UTF_8);
        }, CONFIG);
        RequestScope scope = new RequestScope();
        speech.check(BASE, "fixture", scope);
        speech.speech(BASE, "fixture", "Answer", scope);
        speech.transcribe(BASE, "fixture", SpeechAudio.wav(new byte[3200], 16_000), "it", scope);
        assertEquals(4, requests[0]);
    }
    @Test public void realtimeVoiceIsIndependentOfSpeechVoice() throws Exception {
        JSONObject session = RealtimeProtocol.session(true, CONFIG.realtimeVoice()).getJSONObject("session");
        assertEquals(REALTIME_VOICE, session.getJSONObject("audio").getJSONObject("output").getString("voice"));
        assertEquals(CUSTOM_VOICE, SpeechAudio.speech("Hi", CONFIG).getString("voice"));
    }
    @Test public void changedChoicesRequireCheckingOnlyTheAffectedMode() {
        SpeechApiConfig voice = new SpeechApiConfig(CUSTOM_STT, CUSTOM_TTS, "speaker-8", REALTIME_VOICE);
        SpeechApiConfig realtime = new SpeechApiConfig(CUSTOM_STT, CUSTOM_TTS, CUSTOM_VOICE, "marin");
        assertTrue(AudioMode.SPEECH.requiresRecheck(CONFIG, voice));
        assertFalse(AudioMode.REALTIME.requiresRecheck(CONFIG, voice));
        assertTrue(AudioMode.REALTIME.requiresRecheck(CONFIG, realtime));
        assertFalse(AudioMode.SPEECH.requiresRecheck(CONFIG, realtime));
        assertFalse(AudioMode.SPEECH.requiresRecheck(CONFIG, CONFIG));
        assertFalse(AudioMode.PLATFORM.requiresRecheck(CONFIG, voice));
    }
    @Test public void identifiersCannotInjectMultipartFields() {
        assertEquals("provider/stt", SpeechApiConfig.identifier(" provider/stt "));
        for (String invalid : new String[]{"", " ", "bad\r\nfield", "bad\u0000field", "x".repeat(257)}) {
            assertThrows(IllegalArgumentException.class, () -> new SpeechApiConfig(invalid, CUSTOM_TTS, CUSTOM_VOICE, REALTIME_VOICE));
        }
        assertThrows(IllegalArgumentException.class, () -> SpeechApiConfig.identifier(null));
    }
    @Test public void optionalVoiceInventoryRetainsServerOrderAndSkipsMalformedIds() throws Exception {
        JSONObject response = new JSONObject().put("voices", new JSONArray().put(VOICE_A).put(VOICE_A)
                .put(new JSONObject().put("id", "speaker-b")).put(new JSONObject().put("name", "Not an ID"))
                .put(" ").put("bad\r\nvoice").put(7));
        assertEquals(Arrays.asList(VOICE_A, "speaker-b"), SpeechCatalog.voices(response));
        assertEquals(Arrays.asList(VOICE_A), SpeechCatalog.voices(new JSONObject().put("data", new JSONArray()
                .put(new JSONObject().put("id", VOICE_A)))));
        assertThrows(IOException.class, () -> SpeechCatalog.voices(new JSONObject()));
    }
    @Test public void suggestionsAreScopedToOpenAiAndTheSpeechModel() {
        assertTrue(SpeechCatalog.openAi("https://api.openai.com/v1/"));
        assertFalse(SpeechCatalog.openAi(BASE));
        assertFalse(SpeechCatalog.openAi("https://api.openai.com.evil.test/v1"));
        assertFalse(SpeechCatalog.openAi("http://api.openai.com/v1"));
        assertFalse(SpeechCatalog.speechVoices("tts-1").contains("verse"));
        assertTrue(SpeechCatalog.speechVoices("gpt-4o-mini-tts").contains("verse"));
        assertFalse(SpeechCatalog.realtimeVoices().contains("fable"));
        assertEquals(Arrays.asList("good"), SpeechCatalog.identifiers(Arrays.asList("good", "good", "", "bad\nname")));
    }
}
