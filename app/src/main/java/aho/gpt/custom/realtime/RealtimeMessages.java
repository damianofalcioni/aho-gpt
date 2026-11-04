package aho.gpt.custom.realtime;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

/** Adapts the shared agent history while preserving complete tool calls and receipts. */
public final class RealtimeMessages {
    private static final String TYPE = "type";
    private static final String CALL_ID = "call_id";
    private static final String CONTENT = "content";
    private static final String TEXT = "text";
    private RealtimeMessages() { }
    public static JSONArray input(JSONArray history) throws Exception {
        JSONArray result = new JSONArray();
        for (int index = 0; index < history.length(); index++) { result.put(item(history.getJSONObject(index))); }
        return result;
    }
    private static JSONObject item(JSONObject source) throws Exception {
        String type = source.optString(TYPE);
        if ("function_call".equals(type)) {
            return new JSONObject().put(TYPE, type).put(CALL_ID, source.getString(CALL_ID))
                    .put("name", source.getString("name")).put("arguments", source.getString("arguments"));
        }
        if ("function_call_output".equals(type)) {
            return new JSONObject().put(TYPE, type).put(CALL_ID, source.getString(CALL_ID)).put("output", source.getString("output"));
        }
        String role = source.getString("role");
        Object content = source.get(CONTENT);
        JSONArray parts = content instanceof JSONArray ? (JSONArray) content : new JSONArray().put(new JSONObject()
                .put(TYPE, "assistant".equals(role) ? "output_text" : "input_text").put(TEXT, content));
        return new JSONObject().put(TYPE, "message").put("role", role).put(CONTENT, parts);
    }
    public static JSONArray completed(JSONObject response) throws Exception {
        if (!"completed".equals(response.optString("status"))) { throw new IOException("Incomplete Realtime response"); }
        JSONArray output = response.getJSONArray("output"); JSONArray result = new JSONArray();
        for (int index = 0; index < output.length(); index++) { result.put(output(output.getJSONObject(index))); }
        return result;
    }
    private static JSONObject output(JSONObject item) throws Exception {
        if (!"completed".equals(item.optString("status", "completed"))) { throw new IOException("Incomplete Realtime item"); }
        if ("function_call".equals(item.getString(TYPE))) { return item(item); }
        if (!"message".equals(item.getString(TYPE))) { throw new IOException("Unknown Realtime output"); }
        JSONArray content = item.getJSONArray(CONTENT); JSONArray text = new JSONArray();
        for (int index = 0; index < content.length(); index++) {
            JSONObject part = content.getJSONObject(index);
            text.put(new JSONObject().put(TYPE, "output_text").put(TEXT, text(part)));
        }
        return new JSONObject().put(TYPE, "message").put("role", "assistant").put(CONTENT, text);
    }
    private static String text(JSONObject part) throws Exception {
        return switch (part.getString(TYPE)) {
            case "audio", "output_audio" -> part.getString("transcript");
            case TEXT, "output_text" -> part.getString(TEXT);
            default -> throw new IOException("Unknown Realtime content");
        };
    }
}
