package aho.gpt.logging;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;

/** Only fixed protocol metadata may leave a Live event for diagnostics. */
public final class LiveLogEvent {
    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final List<String> TYPES = Arrays.asList("invalid_request_error", "server_error",
            "authentication_error", "permission_error", "rate_limit_error", "insufficient_quota");
    private static final List<String> CODES = Arrays.asList("invalid_api_key", "model_not_found", "invalid_value",
            "invalid_request_error", "unknown_parameter", "missing_required_parameter", "unsupported_parameter",
            "unsupported_value", "unsupported_model", "insufficient_quota", "rate_limit_exceeded",
            "server_error", "internal_server_error", "invalid_audio", "invalid_audio_format", "invalid_base64",
            "context_length_exceeded", "content_filter", "permission_denied");
    private static final List<String> PARAMS = Arrays.asList("type", "event_id", "model", "session", "session.model",
            "session.store", "session.instructions", "session.input", "session.voice", "session.audio",
            "session.audio.format", "session.audio.format.type", "session.audio.format.rate", "session.audio.output",
            "session.audio.output.voice", "session.delegation", "session.delegation.type", "session.delegation.responses",
            "session.delegation.responses.model", "session.delegation.responses.instructions",
            "session.delegation.responses.tools", "session.delegation.responses.parallel_tool_calls",
            "delegation.responses.model", "audio", "item", "item.type", "item.call_id", "item.output", "response");
    private static final List<String> STATUSES = Arrays.asList("completed", "failed", "incomplete", "cancelled");
    private static final List<String> INCOMPLETE = Arrays.asList("max_output_tokens", "content_filter");
    private static final List<String> CLOSE = Arrays.asList("close_requested", "expired", "content", "remote_hangup", "connection_lost");

    private LiveLogEvent() { }
    public static String failure(JSONObject event) {
        if (ERROR.equals(event.optString(TYPE))) { return "source=session" + error(event.optJSONObject(ERROR)); }
        if (!"response.event".equals(event.optString(TYPE))) { return ""; }
        JSONObject nested = event.optJSONObject("event");
        if (nested == null) { return ""; }
        if (ERROR.equals(nested.optString(TYPE))) { return "source=backend" + error(nested.optJSONObject(ERROR)); }
        return backend(nested);
    }
    private static String backend(JSONObject event) {
        String type = event.optString(TYPE);
        if (!Arrays.asList("response.failed", "response.incomplete", "response.cancelled", "response.completed").contains(type)) { return ""; }
        JSONObject response = event.optJSONObject("response");
        String status = field(response, "status", STATUSES);
        if ("response.completed".equals(type) && "completed".equals(status)) { return ""; }
        JSONObject details = response == null ? null : response.optJSONObject("incomplete_details");
        return "source=backend status=" + status + error(response == null ? null : response.optJSONObject(ERROR))
                + " incompleteReason=" + field(details, "reason", INCOMPLETE);
    }
    private static String error(JSONObject error) {
        return " errorType=" + field(error, TYPE, TYPES) + " errorCode=" + field(error, "code", CODES)
                + " errorParam=" + field(error, "param", PARAMS);
    }
    public static String closeReason(JSONObject event) { return field(event, "reason", CLOSE); }
    private static String field(JSONObject object, String key, List<String> allowed) {
        if (object == null || object.isNull(key)) { return "none"; }
        Object value = object.opt(key);
        return allowed.contains(value) ? (String) value : "other";
    }
    public static String command(JSONObject event) {
        return switch (event.optString(TYPE)) {
            case "session.start" -> "START";
            case "session.input_audio.append" -> "INPUT_AUDIO";
            case "response.item.create" -> "TOOL_RESULT";
            case "response.create" -> "CONTINUE";
            case "session.close" -> "CLOSE";
            default -> "OTHER";
        };
    }
}
