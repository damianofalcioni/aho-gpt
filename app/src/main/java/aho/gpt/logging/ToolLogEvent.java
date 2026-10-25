package aho.gpt.logging;

import java.util.Arrays;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/** Allowlisted metadata only, even when a model or tool supplies unexpected strings. */
public final class ToolLogEvent {
    private static final List<String> FUNCTIONS = Arrays.asList("find_intents", "invoke_intent", "read_screen", "ui_action");
    private static final List<String> STATES = Arrays.asList("accepted", "rejected", "refused", "unknown");
    private static final List<String> CODES = Arrays.asList("INVALID_ARGUMENTS", "CALL_ID_REUSED", "INVALID_CALL_ID",
            "UNKNOWN_NAMESPACE", "TOOL_DISABLED", "TOOL_EXECUTION_UNCONFIRMED", "INVALID_INTENT_ARGUMENTS",
            "ANDROID_DISPATCH_REJECTED", "INTENT_DISPATCH_UNCONFIRMED", "SERVICE_NOT_STARTED",
            "INTENT_DISPATCHED", "UNKNOWN_TOOL", "STALE_CAPABILITY", "UI_ACTION_ACCEPTED", "UI_ACTION_REJECTED");

    private ToolLogEvent() { }
    public static String format(String name, String receipt) {
        String function = FUNCTIONS.contains(name) ? name : "unknown_function";
        try {
            JSONObject result = new JSONObject(receipt);
            String status = allowed(STATES, result.optString("status"), "completed");
            return function + " status=" + status + " code=" + allowed(CODES, result.optString("code"), "none");
        } catch (JSONException exception) { return function + " status=unreadable code=none"; }
    }
    private static String allowed(List<String> choices, String value, String fallback) {
        return choices.contains(value) ? value : fallback;
    }
}
