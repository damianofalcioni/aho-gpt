package aho.gpt.custom.live;

import java.io.IOException;
import org.json.JSONObject;

/** Rebuilds stripped Live backend snapshots only from complete, correlated items. */
public final class LiveResponses {
    private LiveItems items = new LiveItems();
    private String response = "";
    private String delegation = "";
    public JSONObject consume(JSONObject envelope) throws Exception {
        JSONObject event = envelope.getJSONObject("event"); String type = event.getString("type");
        if (failed(type)) { throw new IOException("Incomplete Live backend response"); }
        if ("response.created".equals(type)) { start(event, envelope.getString("delegation_id")); return null; }
        if (!relevant(type)) { return null; }
        requireOwner(envelope);
        switch (type) {
            case "response.output_item.added": requireResponse(event); items.announce(event); return null;
            case "response.output_item.done": requireResponse(event); items.complete(event); return null;
            default: return completed(event.getJSONObject("response"));
        }
    }
    private void start(JSONObject event, String id) throws Exception {
        if (!response.isEmpty() || id.isEmpty()) { throw new IOException("Overlapping Live responses"); }
        response = event.getJSONObject("response").getString("id");
        delegation = id; items = new LiveItems();
    }
    private boolean relevant(String type) {
        return "response.output_item.added".equals(type) || "response.output_item.done".equals(type) || "response.completed".equals(type);
    }
    private void requireOwner(JSONObject envelope) throws Exception {
        if (response.isEmpty() || !delegation.equals(envelope.getString("delegation_id"))) { throw new IOException("Foreign Live response"); }
    }
    private void requireResponse(JSONObject event) throws Exception {
        if (!response.equals(event.getString("response_id"))) { throw new IOException("Foreign Live item"); }
    }
    private JSONObject completed(JSONObject value) throws Exception {
        requireCompleted(value);
        JSONObject result = new JSONObject().put("output", items.output());
        response = ""; return result;
    }
    private void requireCompleted(JSONObject value) throws Exception {
        if (!response.equals(value.getString("id"))) { throw new IOException("Foreign Live response"); }
        if (!"completed".equals(value.getString("status"))) { throw new IOException("Incomplete Live response"); }
    }
    private boolean failed(String type) {
        return "response.failed".equals(type) || "response.incomplete".equals(type) || "response.cancelled".equals(type) || "error".equals(type);
    }
}
