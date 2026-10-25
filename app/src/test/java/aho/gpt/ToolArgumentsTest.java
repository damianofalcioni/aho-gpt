package aho.gpt;

import aho.gpt.openai.ToolArguments;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolArgumentsTest {
    private static final String OFFSET = "offset";
    private static final String FIND = "find_intents";
    private static final String INVOKE = "invoke_intent";
    private static final String UI = "ui_action";
    private static final String CAPABILITY = "capability_id";
    private static final String ACTION = "action";
    private static final String TEXT = "text";
    @Test public void acceptsOptionalSearchAndDocumentedInvocation() throws Exception {
        ToolArguments.validate(FIND, new JSONObject(), false);
        ToolArguments.validate(FIND, new JSONObject().put(OFFSET, Integer.MAX_VALUE).put("mode", "overview"), false);
        ToolArguments.validate(INVOKE, new JSONObject().put(CAPABILITY, "known"), false);
    }
    @Test public void rejectsCoercionAndInvalidSearchBounds() throws Exception {
        invalid(FIND, new JSONObject().put(OFFSET, "0"));
        invalid(FIND, new JSONObject().put(OFFSET, -1));
        invalid(FIND, new JSONObject().put(OFFSET, 0.5));
        invalid(FIND, new JSONObject().put(OFFSET, 2147483648L));
        invalid(FIND, new JSONObject().put("app", JSONObject.NULL));
    }
    @Test public void rejectsUnknownFieldsEnumsAndDisabledTools() throws Exception {
        invalid(FIND, new JSONObject().put("shell", "anything"));
        invalid(FIND, new JSONObject().put("category", "invented"));
        invalid("read_screen", new JSONObject());
        invalid("invented", new JSONObject());
    }
    @Test public void validatesNestedExtrasAndTextBounds() throws Exception {
        JSONObject args = new JSONObject().put(CAPABILITY, "known");
        invalid(INVOKE, args.put("string_extras", new JSONObject().put(TEXT, 42)));
        invalid(INVOKE, args.put("string_extras", new JSONObject().put(TEXT, "x".repeat(4097))));
        JSONObject extras = new JSONObject();
        for (int i = 0; i < 17; i++) { extras.put("extra" + i, "value"); }
        invalid(INVOKE, args.put("string_extras", extras));
        invalid(INVOKE, new JSONObject().put(CAPABILITY, ""));
    }
    @Test public void acceptsLongUrisAndTextOnlyWithinTheirAdvertisedLimits() throws Exception {
        JSONObject intent = new JSONObject().put(CAPABILITY, "known").put("data_uri", "x".repeat(4096));
        ToolArguments.validate(INVOKE, intent, false);
        invalid(INVOKE, intent.put("data_uri", "x".repeat(4097)));
        JSONObject edit = new JSONObject().put(ACTION, "set_text").put("node_id", "1:0").put(TEXT, "x".repeat(4096));
        ToolArguments.validate(UI, edit, true);
        edit.put(TEXT, "x".repeat(4097));
        assertThrows(IllegalArgumentException.class, () -> ToolArguments.validate(UI, edit, true));
    }
    @Test public void requiresVisibleNodeIdsForTargetedActions() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ToolArguments.validate(UI, new JSONObject().put(ACTION, "click"), true));
        JSONObject edit = new JSONObject().put(ACTION, "set_text").put("node_id", "1:0");
        assertThrows(IllegalArgumentException.class, () -> ToolArguments.validate(UI, edit, true));
        ToolArguments.validate(UI, edit.put(TEXT, ""), true);
        ToolArguments.validate(UI, new JSONObject().put(ACTION, "home"), true);
        assertThrows(IllegalArgumentException.class, () -> ToolArguments.validate("read_screen", new JSONObject().put("extra", "x"), true));
    }
    private void invalid(String name, JSONObject arguments) {
        assertThrows(IllegalArgumentException.class, () -> ToolArguments.validate(name, arguments, false));
    }
}
