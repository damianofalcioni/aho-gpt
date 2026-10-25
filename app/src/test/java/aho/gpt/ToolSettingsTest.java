package aho.gpt;

import aho.gpt.core.Conversation;
import aho.gpt.openai.AgentLoop;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolSettingsTest {
    private static final String REQUEST = "Open app";
    private static final String TYPE = "type";
    private static final String MODEL = "model";
    @Test public void unadvertisedCallsAreRefusedEvenIfTheModelInventsThem() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin(REQUEST, 1);
        ConfigurableTools tools = new ConfigurableTools();
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> {
            assertEquals(0, request.getJSONArray("tools").length());
            String instructions = request.getString("instructions");
            assertFalse(instructions.contains("invoke_intent")); assertFalse(instructions.contains("find_intents"));
            assertFalse(instructions.contains("read_screen")); assertFalse(instructions.contains("ui_action"));
            if (++requests[0] == 1) { return response(call()); }
            JSONObject receipt = new JSONObject(request.getJSONArray("input").getJSONObject(2).getString("output"));
            assertEquals("refused", receipt.getString("status")); assertEquals("TOOL_DISABLED", receipt.getString("code"));
            return response(new JSONObject().put(TYPE, "message"));
        }, tools);
        loop.run(conversation, MODEL);
        assertEquals(0, tools.calls); assertEquals(2, requests[0]);
    }
    @Test public void guidanceDescribesOnlyTheAdvertisedTools() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Read this", 1);
        ConfigurableTools tools = new ConfigurableTools(); tools.allowed = "read_screen";
        AgentLoop loop = new AgentLoop(request -> {
            String instructions = request.getString("instructions");
            assertTrue(instructions.contains(tools.allowed));
            assertFalse(instructions.contains("invoke_intent")); assertFalse(instructions.contains("find_intents"));
            assertFalse(instructions.contains("ui_action"));
            assertEquals(1, request.getJSONArray("tools").getJSONObject(0).getJSONArray("tools").length());
            return response(new JSONObject().put(TYPE, "message"));
        }, tools);
        loop.run(conversation, MODEL);
    }
    @Test public void changedChoicesRemovePreviouslyDiscoveredDataFromOutgoingContext() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Find app", 1);
        conversation.toolResult("old", "private disabled capability"); conversation.answered(2);
        conversation.begin(REQUEST, 3);
        ConfigurableTools tools = new ConfigurableTools(); tools.version = 1;
        AgentLoop loop = new AgentLoop(request -> {
            JSONArray input = request.getJSONArray("input");
            assertEquals(1, input.length()); assertEquals(REQUEST, input.getJSONObject(0).getString("content"));
            assertFalse(request.toString().contains("private disabled capability"));
            return response(new JSONObject().put(TYPE, "message"));
        }, tools);
        loop.run(conversation, MODEL);
    }
    @Test public void settingChangesDuringInferenceStopOldResponseBeforeDispatchOrAnotherRequest() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin(REQUEST, 1);
        ConfigurableTools tools = new ConfigurableTools();
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> { requests[0]++; tools.version++; return response(call()); }, tools);
        assertThrows(InterruptedException.class, () -> loop.run(conversation, MODEL));
        assertEquals(0, tools.calls); assertEquals(1, requests[0]);
        assertEquals(1, conversation.input().length());
    }
    private static JSONObject call() throws Exception {
        return new JSONObject().put(TYPE, "function_call").put("call_id", "disabled-call").put("name", "invoke_intent")
                .put("arguments", "{\"capability_id\":\"previous-id\"}");
    }
    private static JSONObject response(JSONObject output) throws Exception {
        return new JSONObject().put("output", new JSONArray().put(output));
    }
    private static final class ConfigurableTools implements AgentLoop.Tools {
        int calls;
        long version;
        String allowed;
        @Override public boolean accessibilityAvailable() { return true; }
        @Override public boolean enabled(String name) { return name.equals(allowed); }
        @Override public long configurationVersion() { return version; }
        @Override public String execute(String name, JSONObject arguments) { calls++; return "{}"; }
    }
}
