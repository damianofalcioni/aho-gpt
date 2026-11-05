package aho.gpt.custom.live;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/** Fixed diagnostic vocabulary; no provider-controlled data is returned. */
public enum LiveStage {
    STARTED, INPUT_TRANSCRIBED, VOICE_AUDIO, VOICE_TRANSCRIPT, DELEGATED, BACKEND_STARTED, BACKEND_COMPLETED, FAILED, CLOSED, OTHER;
    private static final Map<String, LiveStage> EVENTS = events();
    private static Map<String, LiveStage> events() {
        Map<String, LiveStage> events = new HashMap<>();
        events.put("session.started", STARTED); events.put("session.input_transcript.delta", INPUT_TRANSCRIBED);
        events.put("session.output_transcript.delta", VOICE_TRANSCRIPT); events.put("session.output_audio.delta", VOICE_AUDIO);
        events.put("session.delegation.created", DELEGATED); events.put("session.closed", CLOSED); events.put("error", FAILED);
        return events;
    }
    public static LiveStage of(JSONObject event) {
        String type = event.optString("type");
        if ("response.event".equals(type)) { return backend(event.optJSONObject("event")); }
        LiveStage stage = EVENTS.get(type); return stage == null ? OTHER : stage;
    }
    private static LiveStage backend(JSONObject event) {
        if (event == null) { return OTHER; }
        return switch (event.optString("type")) {
            case "response.created" -> BACKEND_STARTED;
            case "response.completed" -> BACKEND_COMPLETED;
            case "response.failed", "response.incomplete" -> FAILED;
            default -> OTHER;
        };
    }
}
