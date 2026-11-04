package aho.gpt.custom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** OpenAI-compatible model IDs in server order, without duplicate or blank choices. */
public final class ServerModels {
    private ServerModels() { }
    public static List<String> read(JSONObject response) throws JSONException {
        JSONArray data = response.getJSONArray("data");
        Set<String> ids = new LinkedHashSet<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item != null) { add(ids, item.opt("id")); }
        }
        return new ArrayList<>(ids);
    }
    private static void add(Set<String> ids, Object id) {
        if (id instanceof String value && !value.trim().isEmpty()) { ids.add(value); }
    }
    public static String first(JSONObject response) throws JSONException, IOException {
        List<String> ids = read(response);
        if (ids.isEmpty()) { throw new IOException("No custom model"); }
        return ids.get(0);
    }
}
