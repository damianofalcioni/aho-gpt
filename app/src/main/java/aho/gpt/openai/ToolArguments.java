package aho.gpt.openai;

import org.json.JSONArray;
import org.json.JSONObject;

/** Enforces the advertised tool schema locally, without JSON coercion or extra fields. */
public final class ToolArguments {
    private ToolArguments() { }
    public static JSONObject validate(String name, JSONObject arguments, boolean accessibility) throws Exception {
        JSONArray tools = ToolDefinitions.functions(accessibility);
        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            if (name.equals(tool.getString("name"))) {
                ArgumentSchema.object(arguments, tool.getJSONObject("parameters"));
                target(name, arguments);
                return arguments;
            }
        }
        throw new IllegalArgumentException("Unknown or unavailable tool");
    }
    private static void target(String name, JSONObject args) throws Exception {
        if ("invoke_intent".equals(name)) { requireText(args, "capability_id"); }
        if ("ui_action".equals(name)) { uiTarget(args); }
    }
    private static void uiTarget(JSONObject args) throws Exception {
        String action = args.getString("action");
        if (!java.util.Arrays.asList("back", "home", "recents").contains(action)) { requireText(args, "node_id"); }
        if ("set_text".equals(action) && !args.has("text")) { invalid(); }
    }
    private static void requireText(JSONObject args, String key) {
        if (args.optString(key).isEmpty()) { invalid(); }
    }
    private static void invalid() { throw new IllegalArgumentException("Invalid tool arguments"); }
}
