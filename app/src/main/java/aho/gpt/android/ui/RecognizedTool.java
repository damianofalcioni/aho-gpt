package aho.gpt.android.ui;

import android.content.Context;
import aho.gpt.R;
import aho.gpt.android.access.AssistantAccessibilityService;
import aho.gpt.android.tools.IntentCapability;
import aho.gpt.android.tools.IntentCatalog;
import aho.gpt.openai.ToolDefinitions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local inventory includes disabled entries; model discovery uses the filtered catalog. */
record RecognizedTool(String key, String name, String summary, JSONObject details, String searchable) {
    RecognizedTool(String key, String name, String summary, JSONObject details) {
        this(key, name, summary, details, (name + " " + summary + " " + details).toLowerCase(Locale.ROOT));
    }
    static List<RecognizedTool> load(Context context, IntentCatalog catalog) throws Exception {
        List<RecognizedTool> result = new ArrayList<>();
        JSONArray functions = ToolDefinitions.functions(true);
        for (int i = 0; i < functions.length(); i++) { result.add(function(context, functions.getJSONObject(i))); }
        List<IntentCapability> entries = catalog.entries();
        Collections.sort(entries, RecognizedTool::compare);
        for (IntentCapability cap : entries) {
            result.add(new RecognizedTool(cap.preferenceKey(), cap.label,
                    context.getString(R.string.tool_app_action, cap.action), cap.describe()));
        }
        return result;
    }

    private static int compare(IntentCapability first, IntentCapability second) {
        int result = first.label.compareToIgnoreCase(second.label);
        if (result != 0) { return result; }
        result = first.action.compareTo(second.action);
        return result == 0 ? first.component.flattenToString().compareTo(second.component.flattenToString()) : result;
    }

    private static RecognizedTool function(Context context, JSONObject details) throws Exception {
        String key = details.getString("name");
        int title;
        switch (key) {
            case "find_intents": title = R.string.tool_find_intents; break;
            case "invoke_intent": title = R.string.tool_invoke_intent; break;
            case "read_screen": title = R.string.tool_read_screen; break;
            default: title = R.string.tool_ui_action; break;
        }
        boolean available = !key.equals("read_screen") && !key.equals("ui_action")
                || AssistantAccessibilityService.connected() != null;
        details.put("availability", context.getString(available ? R.string.tool_available : R.string.tool_needs_accessibility));
        return new RecognizedTool(key, context.getString(title), context.getString(R.string.tool_device_function, key), details);
    }
}
