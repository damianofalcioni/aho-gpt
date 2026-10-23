package aho.gpt;

import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.openai.AgentLoop;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentCancellationTest {
    private static final String OUTPUT = "output";
    @Test public void completedResponseCannotExecuteAfterCancellation() throws Exception {
        RequestScope scope = new RequestScope();
        Conversation conversation = new Conversation(); conversation.begin("Open app", 1);
        CountingTools tools = new CountingTools();
        AgentLoop agent = new AgentLoop(request -> { scope.cancel(); return response(); }, tools);
        assertThrows(InterruptedException.class, () -> agent.run(conversation, "model", scope));
        assertEquals(0, tools.calls);
        assertEquals(1, conversation.input().length());
    }
    @Test public void duplicateCallAcrossRoundsReturnsReceiptOnly() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin("Open app", 1);
        CountingTools tools = new CountingTools();
        int[] rounds = {0};
        AgentLoop agent = new AgentLoop(request -> ++rounds[0] <= 2 ? response()
                : new JSONObject().put(OUTPUT, new JSONArray()), tools);
        agent.run(conversation, "model");
        assertEquals(1, tools.calls);
        assertEquals(3, rounds[0]);
    }
    @Test public void cancellationBetweenToolsStopsRemainingBatch() throws Exception {
        RequestScope scope = new RequestScope();
        Conversation conversation = new Conversation(); conversation.begin("Find apps", 1);
        int[] calls = {0};
        AgentLoop.Tools tools = new AgentLoop.Tools() {
            @Override public boolean accessibilityAvailable() { return false; }
            @Override public String execute(String name, JSONObject args) {
                calls[0]++; scope.cancel(); return "{}";
            }
        };
        JSONObject batch = response();
        batch.getJSONArray(OUTPUT).put(response().getJSONArray(OUTPUT).getJSONObject(0).put("call_id", "second"));
        AgentLoop agent = new AgentLoop(request -> batch, tools);
        assertThrows(InterruptedException.class, () -> agent.run(conversation, "model", scope));
        assertEquals(1, calls[0]);
    }
    private static JSONObject response() throws Exception {
        return new JSONObject().put(OUTPUT, new JSONArray().put(new JSONObject().put("type", "function_call")
                .put("call_id", "one").put("name", "find_intents").put("arguments", "{}")));
    }
    private static final class CountingTools implements AgentLoop.Tools {
        int calls;
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public String execute(String name, JSONObject args) { calls++; return "{}"; }
    }
}
