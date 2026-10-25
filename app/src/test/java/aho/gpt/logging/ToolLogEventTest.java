package aho.gpt.logging;

import aho.gpt.openai.ToolOutcome;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolLogEventTest {
    @Test public void recordsDispatchAndRefusalCodes() throws Exception {
        assertEquals("invoke_intent status=accepted code=INTENT_DISPATCHED",
                ToolLogEvent.format("invoke_intent", ToolOutcome.accepted("INTENT_DISPATCHED")));
        assertEquals("ui_action status=refused code=TOOL_DISABLED",
                ToolLogEvent.format("ui_action", ToolOutcome.refused("TOOL_DISABLED")));
        assertEquals("invoke_intent status=unknown code=INTENT_DISPATCH_UNCONFIRMED",
                ToolLogEvent.format("invoke_intent", ToolOutcome.unknown("INTENT_DISPATCH_UNCONFIRMED")));
    }
    @Test public void neverCopiesToolPayloadOrUnknownMetadata() throws Exception {
        JSONObject result = new JSONObject().put("status", "private screen text")
                .put("code", "Bearer secret\nforged entry").put("text", "private answer")
                .put("url", "https://auth.openai.com/?code=private");
        assertEquals("unknown_function status=completed code=none", ToolLogEvent.format("private request", result.toString()));
        result.put("status", "accepted").put("code", "INTENT_DISPATCHED");
        assertEquals("read_screen status=accepted code=INTENT_DISPATCHED", ToolLogEvent.format("read_screen", result.toString()));
    }
    @Test public void discoveryDoesNotExposeCatalogOrScreenData() throws Exception {
        String result = new JSONObject().put("capabilities", "private app metadata").put("nodes", "screen content").toString();
        assertEquals("find_intents status=completed code=none", ToolLogEvent.format("find_intents", result));
        assertEquals("read_screen status=completed code=none", ToolLogEvent.format("read_screen", result));
    }
    @Test public void malformedReceiptsNeverEscape() {
        assertEquals("unknown_function status=unreadable code=none", ToolLogEvent.format("secret", "private invalid data"));
    }
}
