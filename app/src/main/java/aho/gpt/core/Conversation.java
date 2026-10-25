package aho.gpt.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Memory only; complete tool exchanges are kept together, never arbitrarily truncated. */
public final class Conversation {
    private JSONArray history = new JSONArray();
    private final FollowUpWindow window = new FollowUpWindow();
    private long toolVersion;

    public void begin(String text, long now) throws JSONException {
        if (!window.isOpen(now) || history.length() > 80 || history.toString().length() > 80_000) {
            clear();
        }
        window.close();
        history.put(new JSONObject().put("role", "user").put("content", text));
    }

    public void append(JSONArray items) {
        for (int i = 0; i < items.length(); i++) { history.put(items.opt(i)); }
    }

    public void toolResult(String callId, String result) throws JSONException {
        history.put(new JSONObject().put("type", "function_call_output")
                .put("call_id", callId).put("output", result));
    }

    public JSONArray input() throws JSONException { return new JSONArray(history.toString()); }
    public void configureTools(long version) {
        if (toolVersion == version) { return; }
        toolVersion = version;
        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject item = history.optJSONObject(i);
            if (item != null && "user".equals(item.optString("role"))) {
                history = new JSONArray().put(item); window.close(); return;
            }
        }
        clear();
    }
    public void answered(long now) { window.open(now); }
    public boolean continues(long now) { return window.isOpen(now); }
    public long remaining(long now) { return window.remaining(now); }
    public void clear() { history = new JSONArray(); window.close(); }
}
