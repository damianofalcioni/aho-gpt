package aho.gpt;

import aho.gpt.core.Conversation;
import aho.gpt.openai.AgentLoop;
import aho.gpt.openai.ToolOutcome;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolHandoffTest {
    private static final String TYPE = "type";
    private static final String OUTPUT = "output";
    private static final String CALL_ID = "call_id";
    @Test public void everyDispatchStatusReturnsToModelWithItsCallId() throws Exception {
        String[] receipts = {ToolOutcome.accepted("SENT"), ToolOutcome.rejected("BLOCKED"),
                ToolOutcome.refused("DISABLED"), ToolOutcome.unknown("UNCONFIRMED")};
        for (String receipt : receipts) { verifyHandoff(receipt, false); }
    }
    @Test public void unexpectedToolFailureReturnsUnknownStatusToModel() throws Exception {
        verifyHandoff(ToolOutcome.unknown("TOOL_EXECUTION_UNCONFIRMED"), true);
    }
    private void verifyHandoff(String receipt, boolean fail) throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Open app", 1);
        AgentLoop.Tools tools = new AgentLoop.Tools() {
            @Override public boolean accessibilityAvailable() { return false; }
            @Override public String execute(String name, JSONObject arguments) {
                if (fail) { throw new IllegalStateException("private failure"); }
                return receipt;
            }
        };
        int[] requests = {0};
        AgentLoop loop = new AgentLoop(request -> {
            if (++requests[0] == 1) { return toolResponse(); }
            JSONArray input = request.getJSONArray("input");
            JSONObject result = input.getJSONObject(input.length() - 1);
            assertEquals("function_call_output", result.getString(TYPE));
            assertEquals("launch", result.getString(CALL_ID));
            assertEquals(receipt, result.getString(OUTPUT));
            return new JSONObject().put(OUTPUT, new JSONArray().put(new JSONObject().put(TYPE, "message")
                    .put("content", new JSONArray().put(new JSONObject().put(TYPE, "output_text").put("text", "Result.")))));
        }, tools);
        assertEquals("Result.", loop.run(conversation, "model"));
        assertEquals(2, requests[0]);
    }
    private JSONObject toolResponse() throws Exception {
        return new JSONObject().put(OUTPUT, new JSONArray().put(new JSONObject().put(TYPE, "function_call")
                .put(CALL_ID, "launch").put("name", "invoke_intent").put("arguments", "{\"capability_id\":\"app\"}")));
    }
}
