package aho.gpt.android.net;

import aho.gpt.openai.AgentLoop;
import aho.gpt.openai.ToolDefinitions;
import org.json.JSONArray;
import org.json.JSONObject;

/** An isolated discovery result; no Android tools or app actions can execute. */
final class LiveToolFixture implements AgentLoop.Tools {
    private int calls;
    JSONObject request() throws Exception {
        return new JSONObject().put("instructions", "Use find_intents with mode overview exactly once before answering. "
                + "Report the returned category count briefly. Do not perform other actions.")
                .put("input", new JSONArray()).put("tools", ToolDefinitions.create(false, this::enabled));
    }
    @Override public boolean accessibilityAvailable() { return false; }
    @Override public boolean enabled(String name) { return "find_intents".equals(name); }
    @Override public String execute(String name, JSONObject arguments) {
        if (!enabled(name)) { throw new IllegalArgumentException("Unexpected fixture tool"); }
        calls++;
        return "{\"categories\":[],\"category_count\":0}";
    }
    void verify() { if (calls != 1) { throw new IllegalStateException("Missing fixture tool exchange"); } }
}
