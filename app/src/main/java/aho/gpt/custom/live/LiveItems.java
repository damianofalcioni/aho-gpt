package aho.gpt.custom.live;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounds indexed output and checks announced identities before batch validation. */
final class LiveItems {
    private final Map<Integer, JSONObject> items = new TreeMap<>();
    private final Map<Integer, String> added = new TreeMap<>();
    private int size;
    void announce(JSONObject event) throws Exception {
        int index = event.getInt("output_index"); String id = event.getJSONObject("item").getString("id");
        size += id.length(); bounds(index);
        if (added.put(index, id) != null) { throw new IOException("Duplicate Live output announcement"); }
    }
    void complete(JSONObject event) throws Exception {
        int index = event.getInt("output_index"); JSONObject item = event.getJSONObject("item");
        size += item.toString().length(); bounds(index);
        if (!item.getString("id").equals(added.get(index))) { throw new IOException("Invalid Live output item"); }
        if (items.put(index, item) != null) { throw new IOException("Duplicate Live output item"); }
    }
    private void bounds(int index) throws IOException { if (index < 0 || size > 2_000_000) { throw new IOException("Invalid Live output"); } }
    JSONArray output() throws Exception {
        if (added.size() != items.size() || items.isEmpty()) { throw new IOException("Missing Live output item"); }
        JSONArray output = new JSONArray();
        for (var entry : items.entrySet()) {
            if (entry.getKey() != output.length()) { throw new IOException("Missing Live output item"); }
            LiveOutput.validate(entry.getValue()); output.put(entry.getValue());
        }
        return output;
    }
}
