package aho.gpt.custom.live;

import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.realtime.WireBase64;
import aho.gpt.openai.AgentLoop;
import java.io.IOException;
import org.json.JSONObject;
import org.junit.Test;
import static aho.gpt.custom.live.LiveFixture.*;
import static org.junit.Assert.*;

public class LiveProtocolTest {
    private static final String COMPLETED = "completed";
    private static final String SESSION = "session";
    private static final String MODEL_KEY = "model";
    private static final String AUDIO = "audio";
    @Test public void checkStartsAndClosesWithoutAudioOrInference() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.ready();
        fixture.queue.add(event("session.closed").put("reason", "close_requested"));
        LiveProtocol protocol = new LiveProtocol(fixture); RequestScope scope = new RequestScope();
        protocol.initialize(MODEL, SpeechApiConfig.DEFAULT, request(new Conversation()), scope); protocol.close(scope);
        assertEquals(2, fixture.sent.size());
        JSONObject session = fixture.sent.get(0).getJSONObject(SESSION);
        assertEquals("gpt-live-1", session.getString(MODEL_KEY)); assertFalse(session.getBoolean("store"));
        assertEquals(MODEL, session.getJSONObject("delegation").getJSONObject("responses").getString(MODEL_KEY));
        assertEquals("marin", session.getJSONObject(AUDIO).getJSONObject("output").getString("voice"));
        assertEquals("session.close", fixture.sent.get(1).getString(TYPE));
    }
    @Test public void startupRejectsSubstitutedModelsVoiceAndAudio() throws Exception {
        JSONObject session = LiveSession.start(MODEL, SpeechApiConfig.DEFAULT, request(new Conversation())).getJSONObject(SESSION);
        session.put(MODEL_KEY, "another-model");
        assertThrows(IOException.class, () -> initialize(session));
        session.put(MODEL_KEY, SpeechApiConfig.DEFAULT.liveModel());
        session.getJSONObject(AUDIO).getJSONObject("output").put("voice", "unknown");
        assertThrows(IOException.class, () -> initialize(session));
        session.getJSONObject(AUDIO).getJSONObject("output").put("voice", SpeechApiConfig.DEFAULT.liveVoice());
        session.getJSONObject(AUDIO).getJSONObject("format").put("rate", 8000);
        assertThrows(IOException.class, () -> initialize(session));
    }
    @Test public void completedBackendCallsUseSharedValidationAndReceipts() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.response(COMPLETED, call()); fixture.response(COMPLETED, message());
        Conversation history = new Conversation(); history.begin("Find an app", 1); Tools tools = new Tools();
        RequestScope scope = new RequestScope(); LiveProtocol protocol = new LiveProtocol(fixture);
        assertEquals("Done", new AgentLoop(body -> protocol.respond(body, scope), tools).run(history, MODEL, scope));
        assertEquals(1, tools.calls); assertEquals(2, fixture.sent.size());
        assertEquals("response.item.create", fixture.sent.get(0).getString(TYPE));
        assertEquals("c1", fixture.sent.get(0).getJSONObject("item").getString("call_id"));
        assertEquals("response.create", fixture.sent.get(1).getString(TYPE));
    }
    @Test public void historicalReceiptsAreNotSubmittedToANewSession() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.ready(); fixture.response(COMPLETED, message());
        Conversation history = new Conversation(); history.begin("Earlier request", 1); history.toolResult("old", "[]");
        JSONObject request = request(history); RequestScope scope = new RequestScope(); LiveProtocol protocol = new LiveProtocol(fixture);
        protocol.initialize(MODEL, SpeechApiConfig.DEFAULT, request, scope); protocol.respond(request, scope);
        assertEquals(1, fixture.sent.size());
        assertEquals(1, fixture.sent.get(0).getJSONObject(SESSION).getJSONArray("input").length());
    }
    @Test public void incompleteBackendCannotDispatchACompleteLookingCall() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.response("incomplete", call()); Tools tools = new Tools();
        LiveProtocol protocol = new LiveProtocol(fixture); RequestScope scope = new RequestScope();
        assertThrows(IOException.class, () -> new AgentLoop(body -> protocol.respond(body, scope), tools).run(new Conversation(), MODEL, scope));
        assertEquals(0, tools.calls);
    }
    @Test public void malformedCompletedBatchCannotPartlyDispatch() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.response(COMPLETED, call(), malformed());
        Tools tools = new Tools(); LiveProtocol protocol = new LiveProtocol(fixture); RequestScope scope = new RequestScope();
        assertThrows(Exception.class, () -> new AgentLoop(body -> protocol.respond(body, scope), tools).run(new Conversation(), MODEL, scope));
        assertEquals(0, tools.calls);
    }
    @Test public void textOnlyRepliesSkipTheAudioGateAndKeepTheTranscript() throws Exception {
        LiveFixture fixture = new LiveFixture();
        fixture.queue.add(event("session.input_transcript.delta").put("delta", "Hello"));
        fixture.response(COMPLETED, message());
        LiveProtocol protocol = new LiveProtocol(fixture); RequestScope scope = new RequestScope();
        protocol.respond(request(new Conversation()), scope); assertArrayEquals(new byte[0], protocol.finish(false, scope));
        assertEquals("Hello", protocol.transcript());
    }
    @Test public void playbackRequiresSpeechThenSilenceAfterBackendCompletion() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.response(COMPLETED, message());
        fixture.queue.add(event("session.output_audio.delta").put("delta", WireBase64.encode(new byte[]{0, 20})));
        fixture.queue.add(event("session.output_audio.delta").put("delta", WireBase64.encode(new byte[96_000])));
        LiveProtocol protocol = new LiveProtocol(fixture); RequestScope scope = new RequestScope();
        protocol.respond(request(new Conversation()), scope); assertEquals(96_002, protocol.finish(true, scope).length);
    }
    @Test public void cancelledScopesAndServerErrorsNeverDispatch() throws Exception {
        RequestScope scope = new RequestScope(); scope.cancel(); LiveFixture fixture = new LiveFixture();
        assertThrows(InterruptedException.class, () -> new LiveProtocol(fixture).initialize(MODEL, SpeechApiConfig.DEFAULT, request(new Conversation()), scope));
        assertTrue(fixture.sent.isEmpty());
        fixture.queue.add(event("error").put("message", "private server detail"));
        IOException failure = assertThrows(IOException.class, () -> new LiveProtocol(fixture).respond(request(new Conversation()), new RequestScope()));
        assertFalse(failure.getMessage().contains("private"));
    }
    @Test public void liveSettingsAreIndependentAndRestoredModesAreExclusive() {
        SpeechApiConfig defaults = SpeechApiConfig.DEFAULT;
        SpeechApiConfig changed = new SpeechApiConfig(defaults.transcriptionModel(), defaults.speechModel(), defaults.voice(), defaults.realtimeVoice(), "custom-live", "quartz");
        assertTrue(AudioMode.LIVE.requiresRecheck(defaults, changed));
        assertFalse(AudioMode.REALTIME.requiresRecheck(defaults, changed)); assertFalse(AudioMode.SPEECH.requiresRecheck(defaults, changed));
        assertEquals(AudioMode.LIVE, AudioMode.saved(true, true, true)); assertEquals(AudioMode.REALTIME, AudioMode.saved(true, true, false));
    }
    @Test public void rejectedSetupHasAFixedActionableFailureWithoutServerText() throws Exception {
        LiveFixture fixture = new LiveFixture();
        fixture.queue.add(event("error").put("error", new JSONObject().put(TYPE, "invalid_request_error").put("message", "private model detail")));
        LiveSetupException failure = assertThrows(LiveSetupException.class,
                () -> new LiveProtocol(fixture).respond(request(new Conversation()), new RequestScope()));
        assertEquals("Live setup rejected", failure.getMessage());
    }
    private void initialize(JSONObject session) throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.queue.add(event("session.started").put(SESSION, session));
        new LiveProtocol(fixture).initialize(MODEL, SpeechApiConfig.DEFAULT, request(new Conversation()), new RequestScope());
    }
    private JSONObject malformed() throws Exception { JSONObject item = call().put("id", "fc2"); item.remove("arguments"); return item; }
    private static final class Tools implements AgentLoop.Tools {
        int calls;
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public String execute(String name, JSONObject arguments) { calls++; return "[]"; }
    }
}
