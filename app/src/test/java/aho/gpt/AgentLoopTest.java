package aho.gpt;

import aho.gpt.core.Conversation;
import aho.gpt.openai.AgentLoop;
import aho.gpt.openai.ToolOutcome;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentLoopTest {
    private static final String TYPE = "type";
    private static final String MODEL = "test-model";
    private static final String FIND = "find_intents";
    private static final String DEVICE = "device";
    private static final String MESSAGE = "message";
    private static final String CALL_ID = "call_id";
    @Test public void roundTripsToolResultsAndUsesOAuthRequestContract() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Find maps", 1);
        CountingTools tools = new CountingTools();
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> {
            assertFalse(request.getBoolean("store")); assertTrue(request.getBoolean("stream"));
            assertFalse(request.has("previous_response_id")); assertFalse(request.has("max_output_tokens"));
            assertEquals("namespace", request.getJSONArray("tools").getJSONObject(0).getString(TYPE));
            requests[0]++;
            if (requests[0] == 1) { return output(call(FIND, DEVICE)); }
            assertEquals("function_call_output", request.getJSONArray("input").getJSONObject(2).getString(TYPE));
            return output(new JSONObject().put(TYPE, MESSAGE).put("content", new JSONArray()
                    .put(new JSONObject().put(TYPE, "output_text").put("text", "Found maps."))));
        }, tools);
        assertEquals("Found maps.", loop.run(conversation, MODEL)); assertEquals(1, tools.calls);
    }
    @Test public void refusesUnknownNamespaces() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Find", 1);
        CountingTools tools = new CountingTools();
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> ++requests[0] == 1
                ? output(call("find_intents", "remote")) : output(new JSONObject().put(TYPE, MESSAGE)), tools);
        loop.run(conversation, MODEL); assertEquals(0, tools.calls);
    }
    @Test public void longToolSequenceReturnsEveryReceiptToModelBeforeContinuing() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Find", 1);
        CountingTools tools = new CountingTools();
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> {
            JSONArray history = request.getJSONArray("input");
            if (requests[0] > 0) {
                JSONObject receipt = history.getJSONObject(history.length() - 1);
                assertEquals("call" + requests[0], receipt.getString(CALL_ID));
                assertEquals("[]", receipt.getString("output"));
            }
            return ++requests[0] <= 70 ? output(call(FIND, DEVICE).put(CALL_ID, "call" + requests[0]))
                    : output(new JSONObject().put(TYPE, MESSAGE));
        }, tools);
        loop.run(conversation, MODEL);
        assertEquals(70, tools.calls);
        assertEquals(71, requests[0]);
    }
    @Test public void progressReturnsToThinkingAfterToolExecutionBeforeModelRequest() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Find", 1);
        boolean[] executing = {false};
        int[] requests = {0};
        AgentLoop.Tools tools = new CountingTools() {
            @Override public String execute(String name, JSONObject args) {
                assertTrue(executing[0]); return "[]";
            }
        };
        AgentLoop loop = new AgentLoop(request -> {
            assertFalse(executing[0]);
            return ++requests[0] == 1 ? output(call(FIND, DEVICE))
                    : output(new JSONObject().put(TYPE, MESSAGE));
        }, tools, running -> executing[0] = running);
        loop.run(conversation, MODEL);
        assertEquals(2, requests[0]);
        assertFalse(executing[0]);
    }
    @Test public void returnsLaunchRejectionsToModel() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Open browser", 1);
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> {
            if (++requests[0] == 1) { return output(call("invoke_intent", "device")); }
            JSONObject result = request.getJSONArray("input").getJSONObject(2);
            assertEquals("call1", result.getString(CALL_ID));
            assertEquals("rejected", new JSONObject(result.getString("output")).getString("status"));
            return output(new JSONObject().put(TYPE, MESSAGE).put("content", new JSONArray()
                    .put(new JSONObject().put(TYPE, "output_text").put("text", "Android blocked the launch."))));
        }, new RejectedTools());
        assertEquals("Android blocked the launch.", loop.run(conversation, MODEL));
        assertEquals(2, requests[0]);
    }
    private static JSONObject output(JSONObject item) throws Exception { return new JSONObject().put("output", new JSONArray().put(item)); }
    private static JSONObject call(String name, String namespace) throws Exception {
        return new JSONObject().put(TYPE, "function_call").put(CALL_ID, "call1").put("name", name)
                .put("namespace", namespace).put("arguments", "invoke_intent".equals(name) ? "{\"capability_id\":\"test\"}" : "{}");
    }
    private static class CountingTools implements AgentLoop.Tools {
        int calls;
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public String execute(String name, JSONObject args) { calls++; return "[]"; }
    }
    private static final class RejectedTools implements AgentLoop.Tools {
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public String execute(String name, JSONObject args) throws Exception { return ToolOutcome.rejected("ANDROID_DISPATCH_REJECTED"); }
    }
}
