package aho.gpt.custom.live;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.RealtimeProtocol;
import java.io.IOException;
import org.json.JSONObject;

/** Tracks bounded voice data, transport inactivity and terminal session events. */
final class LiveEvents {
    private static final String TYPE = "type";
    private final RealtimeProtocol.Events events;
    final LiveAudio audio = new LiveAudio();
    private final StringBuilder transcript = new StringBuilder();
    private long progressed = System.nanoTime();
    private boolean closing;
    LiveEvents(RealtimeProtocol.Events events) { this.events = events; }
    void progress() { progressed = System.nanoTime(); }
    String transcript() { return transcript.toString(); }
    void send(JSONObject event, RequestScope scope) throws Exception { scope.check(); events.send(event, scope); }
    void send(String type, RequestScope scope) throws Exception { send(new JSONObject().put(TYPE, type), scope); }
    JSONObject receive(RequestScope scope) throws Exception {
        scope.check(); JSONObject event = events.receive(scope); scope.check();
        if (System.nanoTime() - progressed > 60_000_000_000L) { throw new IOException("Live response stalled"); }
        String type = event.getString(TYPE); terminal(type, event); consume(type, event); return event;
    }
    private void terminal(String type, JSONObject event) throws IOException {
        if ("error".equals(type)) { serverError(event.optJSONObject("error")); }
        if (!closing && "session.closed".equals(type)) { throw new IOException("Live session ended"); }
    }
    private void serverError(JSONObject error) throws IOException {
        if (error != null && "invalid_request_error".equals(error.optString(TYPE))) { throw new LiveSetupException(); }
        throw new IOException("Live server error");
    }
    private void consume(String type, JSONObject event) throws Exception {
        switch (type) {
            case "session.output_audio.delta": appendAudio(event.getString("delta")); break;
            case "session.input_transcript.delta": appendTranscript(event.getString("delta")); progress(); break;
            case "session.output_transcript.delta", "response.event": progress(); break;
            default: break;
        }
    }
    private void appendAudio(String data) throws IOException { audio.append(data); if (audio.speaking()) { progress(); } }
    private void appendTranscript(String text) throws IOException {
        if (transcript.length() + text.length() > 80_000) { throw new IOException("Live transcript too large"); }
        transcript.append(text);
    }
    JSONObject await(String type, RequestScope scope) throws Exception {
        while (true) { JSONObject event = receive(scope); if (type.equals(event.getString(TYPE))) { return event; } }
    }
    void close(RequestScope scope) throws Exception {
        closing = true; progress(); send("session.close", scope);
        JSONObject closed = await("session.closed", scope);
        if (!"close_requested".equals(closed.optString("reason"))) { throw new IOException("Live session closed unexpectedly"); }
    }
}
