package dev.lightassistant.openai;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ResponseText {
    private ResponseText() { }

    public static String extract(JSONArray output) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.optJSONObject(i);
            if (item != null && "message".equals(item.optString("type"))) {
                appendContent(text, item.optJSONArray("content"));
            }
        }
        return text.toString().trim();
    }

    private static void appendContent(StringBuilder text, JSONArray content) {
        if (content == null) { return; }
        for (int i = 0; i < content.length(); i++) {
            JSONObject part = content.optJSONObject(i);
            if (part != null) { text.append(part.optString("text", part.optString("refusal"))).append('\n'); }
        }
    }
}
