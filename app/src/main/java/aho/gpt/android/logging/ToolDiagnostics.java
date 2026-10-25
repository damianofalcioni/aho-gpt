package aho.gpt.android.logging;

import org.json.JSONObject;
import org.json.JSONException;

/** Only known function names and receipt states enter diagnostics. */
public final class ToolDiagnostics {
    private ToolDiagnostics() { }
    public static void completed(String name, String result) {
        if (!AppLogger.isLoggingEnabled()) { return; }
        AppLogger.i("Tools", function(name) + " " + status(result));
    }
    private static String function(String name) {
        return switch (name) {
            case "find_intents", "invoke_intent", "read_screen", "ui_action" -> name;
            default -> "unknown_function";
        };
    }
    private static String status(String result) {
        try {
            return switch (new JSONObject(result).optString("status")) {
                case "accepted" -> "accepted";
                case "rejected" -> "rejected";
                case "refused" -> "refused";
                case "unknown" -> "unknown";
                default -> "completed";
            };
        } catch (JSONException exception) { return "completed"; }
    }
}
