package aho.gpt;

import aho.gpt.core.RequestScope;
import aho.gpt.openai.AgentLoop;
import aho.gpt.openai.ToolCalls;
import aho.gpt.openai.ToolOutcome;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolCallsTest {
    private static final String CALL_ID = "one";
    private static final String VALID = "{\"capability_id\":\"known\"}";
    private static final String STATUS = "status";
    private static final String REFUSED = "refused";
    @Test public void replaysReceiptWithoutRepeatingMutation() throws Exception {
        RecordingTools tools = new RecordingTools();
        ToolCalls calls = new ToolCalls(tools, new RequestScope());
        JSONObject item = call(CALL_ID, VALID);
        String receipt = calls.execute(item);
        assertEquals(receipt, calls.execute(item));
        assertEquals(1, tools.dispatches);
        assertEquals(2, tools.receipts.size());
        assertEquals(receipt, tools.receipts.get(1));
        assertEquals(Boolean.FALSE, tools.replayed.get(0));
        assertEquals(Boolean.TRUE, tools.replayed.get(1));
        assertFalse(new JSONObject(receipt).getBoolean("completion_verified"));
    }
    @Test public void reusedIdWithDifferentArgumentsIsRefused() throws Exception {
        RecordingTools tools = new RecordingTools();
        ToolCalls calls = new ToolCalls(tools, new RequestScope());
        calls.execute(call(CALL_ID, VALID));
        JSONObject changed = new JSONObject(calls.execute(call(CALL_ID, "{\"capability_id\":\"other\"}")));
        assertEquals("CALL_ID_REUSED", changed.getString("code"));
        assertEquals(changed.toString(), new JSONObject(tools.receipts.get(1)).toString());
        assertEquals(Boolean.FALSE, tools.replayed.get(1));
        assertEquals(1, tools.dispatches);
    }
    @Test public void uncertainMutationIsCachedWithoutLeakingExceptionText() throws Exception {
        RecordingTools tools = new RecordingTools(); tools.fail = true;
        ToolCalls calls = new ToolCalls(tools, new RequestScope());
        String receipt = calls.execute(call(CALL_ID, VALID));
        assertEquals("unknown", new JSONObject(receipt).getString(STATUS));
        assertFalse(receipt.contains("private"));
        assertEquals(receipt, calls.execute(call(CALL_ID, VALID)));
        assertEquals(1, tools.dispatches);
    }
    @Test public void reusedIdCannotChangeFunctionOrNamespace() throws Exception {
        RecordingTools tools = new RecordingTools();
        ToolCalls calls = new ToolCalls(tools, new RequestScope());
        calls.execute(call(CALL_ID, VALID));
        JSONObject renamed = call(CALL_ID, VALID).put("name", "find_intents");
        assertEquals("CALL_ID_REUSED", new JSONObject(calls.execute(renamed)).getString("code"));
        JSONObject moved = call(CALL_ID, VALID).put("namespace", "other");
        assertEquals("CALL_ID_REUSED", new JSONObject(calls.execute(moved)).getString("code"));
        assertEquals(1, tools.dispatches);
    }
    @Test public void rejectsMalformedJsonAndAmbiguousDuplicateKeysBeforeDispatch() throws Exception {
        RecordingTools tools = new RecordingTools();
        ToolCalls calls = new ToolCalls(tools, new RequestScope());
        String[] malformed = {"{capability_id:'known'}", VALID + " trailing", "{\"capability_id\":\"known\",}",
                "{\"capability_id\":\"known\",\"capability_id\":\"other\"}",
                "{\"capability_id\":\"known\",\"string_extras\":{\"a\":\"x\",\"a\":\"y\"}}", "{\"capability_id\":null}",
                "{\"capability_id\":\"bad\ntext\"}", "{\"capability_id\":\"bad\\x\"}",
                "{\"capability_id\":\"known\",\"string_extras\":{\"a\":{}}}", "{\"offset\":01}"};
        for (int i = 0; i < malformed.length; i++) {
            assertEquals(REFUSED, new JSONObject(calls.execute(call("bad" + i, malformed[i]))).getString(STATUS));
        }
        assertEquals(0, tools.dispatches);
        assertEquals(malformed.length, tools.receipts.size());
    }
    @Test public void rejectsOversizedAndIncorrectlyTypedArguments() throws Exception {
        RecordingTools tools = new RecordingTools();
        ToolCalls calls = new ToolCalls(tools, new RequestScope());
        assertEquals(REFUSED, new JSONObject(calls.execute(call("large", "x".repeat(80_001)))).getString(STATUS));
        assertEquals(REFUSED, new JSONObject(calls.execute(call("type", "{\"capability_id\":7}"))).getString(STATUS));
        assertEquals(0, tools.dispatches);
        assertEquals(2, tools.receipts.size());
    }
    @Test public void validEscapesAndWhitespacePreserveUserText() throws Exception {
        RecordingTools tools = new RecordingTools();
        ToolCalls calls = new ToolCalls(tools, new RequestScope());
        JSONObject args = new JSONObject().put("capability_id", "known")
                .put("string_extras", new JSONObject().put("text", "a\n\"b\\c\tè"));
        String receipt = calls.execute(call(CALL_ID, " \n" + args + "\t"));
        assertEquals("accepted", new JSONObject(receipt).getString(STATUS));
        assertEquals("a\n\"b\\c\tè", tools.last.getJSONObject("string_extras").getString("text"));
    }
    @Test public void requestCancellationBlocksValidCalls() throws Exception {
        RecordingTools tools = new RecordingTools();
        RequestScope cancelled = new RequestScope(); cancelled.cancel();
        ToolCalls calls = new ToolCalls(tools, cancelled);
        assertThrows(InterruptedException.class, () -> calls.execute(call(CALL_ID, VALID)));
        assertEquals(0, tools.dispatches);
    }
    @Test public void longBatchRetainsReceiptsWithoutACallCountCap() throws Exception {
        RecordingTools tools = new RecordingTools();
        ToolCalls calls = new ToolCalls(tools, new RequestScope());
        String first = calls.execute(call("call0", VALID));
        for (int i = 1; i < 96; i++) { calls.execute(call("call" + i, VALID)); }
        assertEquals(first, calls.execute(call("call0", VALID)));
        assertEquals(96, tools.dispatches);
        new ToolCalls(tools, new RequestScope()).execute(call("call0", VALID));
        assertEquals(97, tools.dispatches);
    }
    private static JSONObject call(String id, String raw) throws Exception {
        return new JSONObject().put("call_id", id).put("name", "invoke_intent").put("arguments", raw);
    }
    private static final class RecordingTools implements AgentLoop.Tools {
        final List<String> receipts = new ArrayList<>();
        final List<Boolean> replayed = new ArrayList<>();
        int dispatches;
        boolean fail;
        JSONObject last;
        @Override public boolean accessibilityAvailable() { return false; }
        @Override public void receipt(String name, String result, boolean cached) { receipts.add(result); replayed.add(cached); }
        @Override public String execute(String name, JSONObject args) throws Exception {
            dispatches++; last = args;
            if (fail) { throw new IllegalStateException("private content must not escape"); }
            return ToolOutcome.accepted("INTENT_DISPATCHED");
        }
    }
}
