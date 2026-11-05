package aho.gpt.custom.live;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

/** Validate the entire completed batch before any call reaches the executor. */
final class LiveOutput {
    private LiveOutput() { }
    static void validate(JSONObject item) throws Exception {
        String type = item.getString("type");
        if ("reasoning".equals(type)) { return; }
        if (!"completed".equals(item.getString("status"))) { throw new IOException("Incomplete Live item"); }
        switch (type) {
            case "function_call": item.getString("name"); item.getString("call_id"); item.getString("arguments"); break;
            case "message": content(item); break;
            default: throw new IOException("Unsupported Live output");
        }
    }
    private static void content(JSONObject item) throws Exception {
        if (!"assistant".equals(item.getString("role"))) { throw new IOException("Invalid Live message"); }
        JSONArray content = item.getJSONArray("content");
        for (int index = 0; index < content.length(); index++) {
            JSONObject part = content.getJSONObject(index);
            switch (part.getString("type")) {
                case "output_text": part.getString("text"); break;
                case "refusal": part.getString("refusal"); break;
                default: throw new IOException("Invalid Live content");
            }
        }
    }
}
