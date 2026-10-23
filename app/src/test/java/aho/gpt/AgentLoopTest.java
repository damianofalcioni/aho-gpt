package aho.gpt;

import aho.gpt.core.Conversation;
import aho.gpt.openai.AgentLoop;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentLoopTest {
    private static final String TYPE = "type";
    private static final String MODEL = "test-model";
    @Test public void roundTripsToolResultsAndUsesOAuthRequestContract() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Find maps", 1);
        CountingTools tools = new CountingTools();
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> {
            assertFalse(request.getBoolean("store")); assertTrue(request.getBoolean("stream"));
            assertFalse(request.has("previous_response_id")); assertFalse(request.has("max_output_tokens"));
            assertEquals("namespace", request.getJSONArray("tools").getJSONObject(0).getString(TYPE));
            requests[0]++;
            if (requests[0] == 1) { return output(call("find_intents", "device")); }
            assertEquals("function_call_output", request.getJSONArray("input").getJSONObject(2).getString(TYPE));
            return output(new JSONObject().put(TYPE, "message").put("content", new JSONArray()
                    .put(new JSONObject().put(TYPE, "output_text").put("text", "Found maps."))));
        }, tools);
        assertEquals("Found maps.", loop.run(conversation, MODEL)); assertEquals(1, tools.calls);
    }
    @Test public void refusesUnknownNamespaces() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Find", 1);
        CountingTools tools = new CountingTools();
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> ++requests[0] == 1
                ? output(call("find_intents", "remote")) : output(new JSONObject().put(TYPE, "message")), tools);
        loop.run(conversation, MODEL); assertEquals(0, tools.calls);
    }
    @Test public void boundsToolLoop() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Find", 1);
        CountingTools tools = new CountingTools();
        AgentLoop loop = new AgentLoop(request -> output(call("find_intents", "device")), tools);
        assertThrows(java.io.IOException.class, () -> loop.run(conversation, MODEL));
        assertEquals(8, tools.calls);
    }
    @Test public void returnsLaunchRejectionsToModel() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Open browser", 1);
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> {
            if (++requests[0] == 1) { return output(call("invoke_intent", "device")); }
            JSONObject result = request.getJSONArray("input").getJSONObject(2);
            assertEquals("call1", result.getString("call_id"));
            assertEquals("Launch rejected", new JSONObject(result.getString("output")).getString("error"));
            return output(new JSONObject().put(TYPE, "message").put("content", new JSONArray()
                    .put(new JSONObject().put(TYPE, "output_text").put("text", "Android blocked the launch."))));
        }, new RejectedTools());
        assertEquals("Android blocked the launch.", loop.run(conversation, MODEL));
        assertEquals(2, requests[0]);
    }
    private static JSONObject output(JSONObject item) throws Exception { return new JSONObject().put("output", new JSONArray().put(item)); }
    private static JSONObject call(String name, String namespace) throws Exception {
        return new JSONObject().put(TYPE, "function_call").put("call_id", "call1").put("name", name)
                .put("namespace", namespace).put("arguments", "{}");
    }
    private static final class CountingTools implements AgentLoop.Tools {
        int calls;
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public String execute(String name, JSONObject args) { calls++; return "[]"; }
    }
    private static final class RejectedTools implements AgentLoop.Tools {
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public String execute(String name, JSONObject args) { throw new SecurityException("Launch rejected"); }
    }
}
