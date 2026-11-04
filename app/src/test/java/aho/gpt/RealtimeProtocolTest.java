package aho.gpt;

import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.SpeechAudio;
import aho.gpt.custom.realtime.RealtimeAudio;
import aho.gpt.custom.realtime.RealtimeMessages;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.realtime.WireBase64;
import aho.gpt.openai.AgentLoop;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class RealtimeProtocolTest {
    private static final String SESSION = "session";
    private static final String AUDIO = "audio";
    private static final String INPUT = "input";
    private static final String TOOLS = "tools";
    private static final String TEXT = "text";
    private static final String COMPLETED = "completed";
    private static final String ONE = "one";
    private static final String RESPONSE = "response";
    private static final String TYPE = "type";
    private static final String OUTPUT = "output";
    private static final String CALL_ID = "call_id";
    @Test public void restoredSpeechModesAreAlwaysExclusive() {
        assertEquals(AudioMode.PLATFORM, AudioMode.saved(false, false));
        assertEquals(AudioMode.SPEECH, AudioMode.saved(true, false));
        assertEquals(AudioMode.REALTIME, AudioMode.saved(false, true));
        assertEquals(AudioMode.REALTIME, AudioMode.saved(true, true));
    }
    @Test public void capabilityCheckUsesOnlySessionConfiguration() throws Exception {
        Events events = new Events();
        events.queue.add(event("session.created"));
        events.queue.add(event("session.updated").put(SESSION, RealtimeProtocol.session(true).getJSONObject(SESSION)));
        new RealtimeProtocol(events).initialize(true, new RequestScope());
        assertEquals(1, events.sent.size()); JSONObject session = events.sent.get(0).getJSONObject(SESSION);
        assertEquals(AUDIO, session.getJSONArray("output_modalities").getString(0));
        assertTrue(session.getJSONObject(AUDIO).getJSONObject(INPUT).isNull("turn_detection"));
        assertFalse(session.has(TOOLS));
        assertEquals(TEXT, RealtimeProtocol.session(false).getJSONObject(SESSION).getJSONArray("output_modalities").getString(0));
    }
    @Test public void unsupportedSessionFormatsRefuseEnabling() throws Exception {
        Events events = new Events(); events.queue.add(event("session.created"));
        JSONObject session = RealtimeProtocol.session(true).getJSONObject(SESSION);
        session.getJSONObject(AUDIO).getJSONObject("output").getJSONObject("format").put("rate", 8000);
        events.queue.add(event("session.updated").put(SESSION, session));
        assertThrows(IOException.class, () -> new RealtimeProtocol(events).initialize(true, new RequestScope()));
    }
    @Test public void audioResponsesKeepTranscriptAndRawPcmSeparate() throws Exception {
        Events events = new Events(); byte[] pcm = {1, 0, 2, 0};
        events.queue.add(created()); events.queue.add(event("response.output_audio.delta")
                .put("response_id", "r1").put("delta", WireBase64.encode(pcm)));
        events.queue.add(done(COMPLETED, new JSONArray().put(message(AUDIO, "Hello"))));
        RealtimeProtocol protocol = new RealtimeProtocol(events);
        JSONObject result = protocol.respond(request(new Conversation()), new RequestScope());
        assertArrayEquals(pcm, protocol.audio());
        assertEquals("Hello", result.getJSONArray(OUTPUT).getJSONObject(0).getJSONArray("content").getJSONObject(0).getString(TEXT));
        JSONObject response = events.sent.get(0).getJSONObject(RESPONSE);
        assertEquals("none", response.getString("conversation")); assertFalse(response.has("store"));
        assertEquals(0, response.getJSONArray(TOOLS).length());
    }
    @Test public void completeCallsUseSharedToolValidationAndReturnReceipts() throws Exception {
        Events events = new Events();
        events.queue.add(created());
        events.queue.add(event("response.function_call_arguments.delta").put("delta", "{\"query\":"));
        events.queue.add(done(COMPLETED, new JSONArray().put(call(ONE))));
        events.queue.add(created()); events.queue.add(done(COMPLETED, new JSONArray().put(message(TEXT, "Done"))));
        Tools tools = new Tools(); Conversation history = new Conversation(); history.beginAudio(audio(), 1);
        RealtimeProtocol protocol = new RealtimeProtocol(events);
        RequestScope scope = new RequestScope();
        assertEquals("Done", new AgentLoop(request -> protocol.respond(request, scope), tools).run(history, "unused", scope));
        assertEquals(1, tools.calls);
        JSONObject first = events.sent.get(0).getJSONObject(RESPONSE);
        assertEquals("input_audio", first.getJSONArray(INPUT).getJSONObject(0).getJSONArray("content").getJSONObject(0).getString(TYPE));
        assertEquals("find_intents", first.getJSONArray(TOOLS).getJSONObject(0).getString("name"));
        JSONArray second = events.sent.get(1).getJSONObject(RESPONSE).getJSONArray(INPUT);
        assertEquals("function_call_output", second.getJSONObject(2).getString(TYPE));
        assertEquals(ONE, second.getJSONObject(2).getString(CALL_ID));
    }
    @Test public void incompleteStreamCannotDispatchTools() throws Exception {
        Events events = new Events(); events.queue.add(created()); events.queue.add(done("incomplete", new JSONArray().put(call(ONE))));
        Tools tools = new Tools(); Conversation history = new Conversation(); history.beginAudio(audio(), 1);
        RequestScope scope = new RequestScope(); RealtimeProtocol protocol = new RealtimeProtocol(events);
        assertThrows(IOException.class, () -> new AgentLoop(request -> protocol.respond(request, scope), tools).run(history, "unused", scope));
        assertEquals(0, tools.calls);
    }
    @Test public void malformedCompletedBatchCannotPartlyDispatch() throws Exception {
        JSONArray batch = new JSONArray().put(call(ONE)).put(new JSONObject().put(TYPE, "function_call"));
        Events events = new Events(); events.queue.add(created()); events.queue.add(done(COMPLETED, batch));
        Tools tools = new Tools(); Conversation history = new Conversation(); history.beginAudio(audio(), 1);
        RequestScope scope = new RequestScope(); RealtimeProtocol protocol = new RealtimeProtocol(events);
        assertThrows(Exception.class, () -> new AgentLoop(request -> protocol.respond(request, scope), tools).run(history, "unused", scope));
        assertEquals(0, tools.calls);
    }
    @Test public void foreignAudioAndServerErrorsFailVisibly() throws Exception {
        Events events = new Events(); events.queue.add(created());
        events.queue.add(event("response.output_audio.delta").put("response_id", "wrong").put("delta", "AAA="));
        assertThrows(IOException.class, () -> new RealtimeProtocol(events).respond(request(new Conversation()), new RequestScope()));
        events.queue.clear(); events.queue.add(event("error").put("error", new JSONObject().put("message", "sensitive")));
        IOException failure = assertThrows(IOException.class, () -> new RealtimeProtocol(events).initialize(true, new RequestScope()));
        assertFalse(failure.getMessage().contains("sensitive"));
    }
    @Test public void audioHistoryExpiresAndToolChangesKeepOnlyLatestSpeech() throws Exception {
        Conversation history = new Conversation(); history.beginAudio(audio(), 1);
        history.append(new JSONArray().put(message(TEXT, "Reply"))); history.answered(10);
        history.beginAudio(audio(), 11); assertEquals(3, history.input().length());
        history.configureTools(7); assertEquals(1, history.input().length());
        history.answered(20); history.beginAudio(audio(), 8020); assertEquals(1, history.input().length());
        history.clear(); assertEquals(0, history.input().length());
    }
    @Test public void captureIsResampledWithoutAWavHeaderInRealtime() throws Exception {
        byte[] wav = SpeechAudio.wav(new byte[]{0, 0, 30, 0, 60, 0, 90, 0}, 16_000);
        assertArrayEquals(new byte[]{0, 0, 20, 0, 40, 0, 60, 0, 80, 0, 90, 0}, RealtimeAudio.pcm(wav));
        assertThrows(IOException.class, () -> RealtimeAudio.pcm(new byte[0]));
        assertThrows(IOException.class, () -> RealtimeAudio.pcm(SpeechAudio.wav(new byte[8], 24_000)));
    }
    @Test public void cancelledRequestsSendNoAudio() throws Exception {
        RequestScope scope = new RequestScope(); scope.cancel(); Events events = new Events();
        assertThrows(InterruptedException.class, () -> new RealtimeProtocol(events).respond(request(new Conversation()), scope));
        assertEquals(0, events.sent.size());
    }
    private static JSONObject request(Conversation conversation) throws Exception {
        return new JSONObject().put(INPUT, conversation.input()).put("instructions", "Help").put(TOOLS, new JSONArray());
    }
    private static JSONObject audio() throws Exception { return RealtimeAudio.message(SpeechAudio.wav(new byte[3200], 16_000)); }
    private static JSONObject event(String type) throws Exception { return new JSONObject().put(TYPE, type); }
    private static JSONObject created() throws Exception { return event("response.created").put(RESPONSE, new JSONObject().put("id", "r1")); }
    private static JSONObject done(String status, JSONArray output) throws Exception {
        return event("response.done").put(RESPONSE, new JSONObject().put("id", "r1").put("status", status).put(OUTPUT, output));
    }
    private static JSONObject call(String id) throws Exception {
        return new JSONObject().put(TYPE, "function_call").put(CALL_ID, id).put("name", "find_intents").put("arguments", "{}");
    }
    private static JSONObject message(String type, String text) throws Exception {
        return new JSONObject().put(TYPE, "message").put("role", "assistant").put("content", new JSONArray().put(new JSONObject()
                .put(TYPE, type).put(AUDIO.equals(type) ? "transcript" : TEXT, text)));
    }
    private static final class Events implements RealtimeProtocol.Events {
        final ArrayDeque<JSONObject> queue = new ArrayDeque<>(); final List<JSONObject> sent = new ArrayList<>();
        @Override public void send(JSONObject event, RequestScope scope) throws Exception { scope.check(); sent.add(event); }
        @Override public JSONObject receive(RequestScope scope) throws Exception { scope.check(); return queue.remove(); }
    }
    private static final class Tools implements AgentLoop.Tools {
        int calls;
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public String execute(String name, JSONObject arguments) { calls++; return "[]"; }
    }
}
