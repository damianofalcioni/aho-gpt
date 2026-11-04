package aho.gpt;

import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.ChatProtocol;
import aho.gpt.custom.ServerEndpoint;
import aho.gpt.openai.AgentLoop;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class CustomProviderTest {
    private static final String ROLE = "role";
    private static final String FIND_APPS = "Find apps";
    private static final String LOCAL_MODEL = "local-model";
    private static final String MESSAGES = "messages";
    private static final String CONTENT = "content";
    private static final String TOOL_CALLS = "tool_calls";
    @Test public void normalizesLocalAndRemoteApiBases() {
        assertEquals("https://api.openai.com/v1", ServerEndpoint.DEFAULT);
        assertEquals(ServerEndpoint.DEFAULT, ServerEndpoint.normalize(" https://api.openai.com/v1/ "));
        assertEquals("http://127.0.0.1:8080/v1", ServerEndpoint.normalize(" http://127.0.0.1:8080/v1/ "));
        assertEquals("http://192.168.1.10:8080/v1", ServerEndpoint.normalize("http://192.168.1.10:8080/v1"));
        assertEquals("https://example.org/api/v1", ServerEndpoint.normalize("https://example.org/api/v1/"));
    }
    @Test public void refusesCredentialsAndNonHttpEndpoints() {
        String[] invalid = {"file:///tmp/server", "ftp://example.org", "http://user:secret@example.org/v1",
                "https://example.org/v1?token=secret", "https://example.org/v1#part", "relative", "http:///v1",
                "http://example.org:0/v1", "http://example.org:99999/v1"};
        for (String endpoint : invalid) { assertThrows(IllegalArgumentException.class, () -> ServerEndpoint.normalize(endpoint)); }
    }
    @Test public void translatesToolsAndReturnsReceiptsBeforeAnswering() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin(FIND_APPS, 1);
        CountingTools tools = new CountingTools();
        int[] rounds = {0};
        AgentLoop agent = new AgentLoop(request -> {
            JSONObject chat = ChatProtocol.request(request, LOCAL_MODEL);
            assertFalse(chat.getBoolean("stream")); assertEquals(LOCAL_MODEL, chat.getString("model"));
            assertFalse(chat.has("include")); assertFalse(chat.has("input"));
            assertEquals("find_intents", chat.getJSONArray("tools").getJSONObject(0).getJSONObject("function").getString("name"));
            JSONArray messages = chat.getJSONArray(MESSAGES);
            assertEquals("system", messages.getJSONObject(0).getString(ROLE));
            assertFalse(messages.getJSONObject(0).getString(CONTENT).isEmpty());
            assertEquals(FIND_APPS, messages.getJSONObject(1).getString(CONTENT));
            if (++rounds[0] == 1) { return ChatProtocol.response(reply(TOOL_CALLS, calls())); }
            assertEquals(2, messages.getJSONObject(2).getJSONArray(TOOL_CALLS).length());
            assertEquals("tool", messages.getJSONObject(3).getString(ROLE));
            assertEquals("one", messages.getJSONObject(3).getString("tool_call_id"));
            assertEquals("[]", messages.getJSONObject(4).getString(CONTENT));
            return ChatProtocol.response(reply("stop", new JSONObject().put(ROLE, "assistant").put(CONTENT, "Found apps.")));
        }, tools);
        assertEquals("Found apps.", agent.run(conversation, "unused-chatgpt-model"));
        assertEquals(2, tools.calls);
        conversation.answered(2); conversation.begin("More", 3);
        assertEquals("Found apps.", ChatProtocol.request(request(conversation), LOCAL_MODEL)
                .getJSONArray(MESSAGES).getJSONObject(5).getString(CONTENT));
    }
    @Test public void truncatedResponseCannotExecuteTools() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin(FIND_APPS, 1);
        CountingTools tools = new CountingTools();
        AgentLoop agent = new AgentLoop(request -> ChatProtocol.response(reply("length", calls())), tools);
        assertThrows(IOException.class, () -> agent.run(conversation, LOCAL_MODEL));
        assertEquals(0, tools.calls); assertEquals(1, conversation.input().length());
    }
    @Test public void cancellationRejectsCompleteCustomToolCalls() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin(FIND_APPS, 1);
        CountingTools tools = new CountingTools(); RequestScope scope = new RequestScope();
        AgentLoop agent = new AgentLoop(request -> { scope.cancel(); return ChatProtocol.response(reply(TOOL_CALLS, calls())); }, tools);
        assertThrows(InterruptedException.class, () -> agent.run(conversation, LOCAL_MODEL, scope));
        assertEquals(0, tools.calls);
    }
    @Test public void malformedCallBatchCannotPartlyExecute() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin(FIND_APPS, 1);
        CountingTools tools = new CountingTools();
        JSONObject message = calls(); message.getJSONArray(TOOL_CALLS).getJSONObject(1).put("type", "unsupported");
        AgentLoop agent = new AgentLoop(request -> ChatProtocol.response(reply(TOOL_CALLS, message)), tools);
        assertThrows(IOException.class, () -> agent.run(conversation, LOCAL_MODEL));
        assertEquals(0, tools.calls);
    }
    @Test public void disabledToolsStayAbsentAndCallsAreRefused() throws Exception {
        Conversation conversation = new Conversation(); conversation.begin(FIND_APPS, 1);
        CountingTools tools = new CountingTools() { @Override public boolean enabled(String name) { return false; } };
        int[] rounds = {0};
        AgentLoop agent = new AgentLoop(request -> {
            assertFalse(ChatProtocol.request(request, LOCAL_MODEL).has("tools"));
            return ++rounds[0] == 1 ? ChatProtocol.response(reply(TOOL_CALLS, calls()))
                    : ChatProtocol.response(reply("stop", new JSONObject().put(CONTENT, "Unavailable.")));
        }, tools);
        assertEquals("Unavailable.", agent.run(conversation, LOCAL_MODEL)); assertEquals(0, tools.calls);
        assertTrue(conversation.input().toString().contains("TOOL_DISABLED"));
    }
    private static JSONObject request(Conversation conversation) throws Exception {
        return new JSONObject().put("instructions", "Help").put("tools", new JSONArray()).put("input", conversation.input());
    }
    private static JSONObject reply(String reason, JSONObject message) throws Exception {
        return new JSONObject().put("choices", new JSONArray().put(new JSONObject().put("finish_reason", reason).put("message", message)));
    }
    private static JSONObject calls() throws Exception {
        JSONArray calls = new JSONArray();
        for (String id : new String[]{"one", "two"}) {
            calls.put(new JSONObject().put("id", id).put("type", "function").put("function",
                    new JSONObject().put("name", "find_intents").put("arguments", "{}")));
        }
        return new JSONObject().put(ROLE, "assistant").put(CONTENT, JSONObject.NULL).put(TOOL_CALLS, calls);
    }
    private static class CountingTools implements AgentLoop.Tools {
        int calls;
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public String execute(String name, JSONObject args) { calls++; return "[]"; }
    }
}
