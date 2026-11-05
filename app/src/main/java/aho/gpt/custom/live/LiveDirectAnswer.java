package aho.gpt.custom.live;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

/** A spoken conversational answer cannot stand in for pending delegated work. */
final class LiveDirectAnswer {
    private final StringBuilder spoken = new StringBuilder();
    private boolean delegated;
    void consume(JSONObject event) throws IOException {
        switch (event.optString("type")) {
            case "session.delegation.created", "response.event": delegated = true; break;
            case "session.output_transcript.delta": append(event.optString("delta")); break;
            default: break;
        }
    }
    private void append(String text) throws IOException {
        if (spoken.length() + text.length() > 80_000) { throw new IOException("Live answer too large"); }
        spoken.append(text);
    }
    boolean ready(LiveAudio audio) { return !delegated && spoken.length() > 0 && audio.settled(); }
    JSONObject response() throws Exception {
        JSONObject part = new JSONObject().put("type", "output_text").put("text", spoken.toString().trim());
        JSONObject message = new JSONObject().put("type", "message").put("role", "assistant").put("content", new JSONArray().put(part));
        return new JSONObject().put("output", new JSONArray().put(message));
    }
}
