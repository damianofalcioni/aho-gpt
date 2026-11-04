package aho.gpt.custom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Converts transient Responses history into Chat Completions messages. */
final class ChatMessages {
    private static final String CONTENT = "content";
    private static final String ROLE = "role";
    private ChatMessages() { }
    static JSONArray create(String instructions, JSONArray input) throws JSONException {
        JSONArray messages = new JSONArray().put(new JSONObject().put(ROLE, "system").put(CONTENT, instructions));
        for (int i = 0; i < input.length(); i++) { append(messages, input.getJSONObject(i)); }
        return messages;
    }
    private static void append(JSONArray messages, JSONObject item) throws JSONException {
        switch (item.optString("type")) {
            case "function_call" -> call(messages, item);
            case "function_call_output" -> messages.put(new JSONObject().put(ROLE, "tool")
                    .put("tool_call_id", item.getString("call_id")).put(CONTENT, item.getString("output")));
            default -> messages.put(new JSONObject().put(ROLE, item.getString(ROLE)).put(CONTENT, text(item)));
        }
    }
    private static void call(JSONArray messages, JSONObject item) throws JSONException {
        JSONObject last = messages.getJSONObject(messages.length() - 1);
        if (!"assistant".equals(last.optString(ROLE))) {
            last = new JSONObject().put(ROLE, "assistant"); messages.put(last);
        }
        JSONArray calls = last.optJSONArray("tool_calls");
        if (calls == null) { calls = new JSONArray(); last.put("tool_calls", calls); }
        calls.put(new JSONObject().put("id", item.getString("call_id")).put("type", "function")
                .put("function", new JSONObject().put("name", item.getString("name")).put("arguments", item.getString("arguments"))));
    }
    private static String text(JSONObject item) throws JSONException {
        Object content = item.get(CONTENT);
        if (content instanceof String value) { return value; }
        JSONArray parts = item.getJSONArray(CONTENT);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) { text.append(parts.getJSONObject(i).optString("text")); }
        return text.toString();
    }
}
