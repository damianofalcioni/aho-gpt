package aho.gpt.custom.live;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/** Fixed diagnostic vocabulary; no provider-controlled data is returned. */
public enum LiveStage {
    STARTED, INPUT_TRANSCRIBED, VOICE_AUDIO, VOICE_TRANSCRIPT, DELEGATED, BACKEND_STARTED, BACKEND_COMPLETED, FAILED, CLOSED, OTHER;
    private static final Map<String, LiveStage> EVENTS = events();
    private static final Map<String, LiveStage> BACKEND = backendEvents();
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
        return lookup(EVENTS, type);
    }
    private static LiveStage backend(JSONObject event) {
        if (event == null) { return OTHER; }
        return lookup(BACKEND, event.optString("type"));
    }
    private static Map<String, LiveStage> backendEvents() {
        Map<String, LiveStage> events = new HashMap<>();
        events.put("response.created", BACKEND_STARTED); events.put("response.completed", BACKEND_COMPLETED);
        events.put("response.failed", FAILED); events.put("response.incomplete", FAILED);
        events.put("response.cancelled", FAILED); events.put("error", FAILED);
        return events;
    }
    private static LiveStage lookup(Map<String, LiveStage> events, String type) {
        LiveStage stage = events.get(type); return stage == null ? OTHER : stage;
    }
}
