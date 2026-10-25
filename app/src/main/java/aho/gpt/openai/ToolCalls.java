package aho.gpt.openai;

import aho.gpt.core.RequestScope;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** One run's receipt cache. A repeated call ID cannot dispatch twice. */
public final class ToolCalls {
    private final AgentLoop.Tools tools;
    private final RequestScope scope;
    private final Map<String, String> signatures = new HashMap<>();
    private final Map<String, String> receipts = new HashMap<>();

    public ToolCalls(AgentLoop.Tools tools, RequestScope scope) { this.tools = tools; this.scope = scope; }

    public String execute(JSONObject item) throws Exception {
        scope.check();
        String id = item.getString("call_id");
        if (item.getString("arguments").length() > 80_000) { return receipt(item, ToolOutcome.refused("INVALID_ARGUMENTS"), false); }
        String signature = signature(item);
        if (signatures.containsKey(id)) {
            if (!signature.equals(signatures.get(id))) { return receipt(item, ToolOutcome.refused("CALL_ID_REUSED"), false); }
            return receipt(item, receipts.get(id), true);
        }
        if (id.isEmpty() || id.length() > 512) { return receipt(item, ToolOutcome.refused("INVALID_CALL_ID"), false); }
        signatures.put(id, signature);
        String result = call(item);
        receipts.put(id, result);
        return receipt(item, result, false);
    }
    private String receipt(JSONObject item, String result, boolean replayed) {
        tools.receipt(item.optString("name"), result, replayed);
        return result;
    }
    private String signature(JSONObject item) throws Exception {
        return new JSONArray().put(item.optString("namespace", "device"))
                .put(item.getString("name")).put(item.getString("arguments")).toString();
    }
    private String call(JSONObject item) throws Exception {
        if (!"device".equals(item.optString("namespace", "device"))) { return ToolOutcome.refused("UNKNOWN_NAMESPACE"); }
        String name = item.getString("name");
        if (!tools.enabled(name)) { return ToolOutcome.refused("TOOL_DISABLED"); }
        JSONObject arguments;
        try {
            arguments = ToolArguments.validate(name, ToolJson.parse(item.getString("arguments")), tools.accessibilityAvailable());
        } catch (Exception exception) { return ToolOutcome.refused("INVALID_ARGUMENTS"); }
        return dispatch(name, arguments);
    }
    private String dispatch(String name, JSONObject arguments) throws Exception {
        scope.check();
        try {
            String result = tools.execute(name, arguments);
            scope.check();
            return result;
        } catch (InterruptedException exception) { throw exception; }
        catch (Exception exception) { return ToolOutcome.unknown("TOOL_EXECUTION_UNCONFIRMED"); }
    }
}
