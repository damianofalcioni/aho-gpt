package dev.lightassistant.android.tools;

import android.content.ComponentName;
import android.content.IntentFilter;
import org.json.JSONException;
import org.json.JSONObject;

public final class IntentCapability {
    public final String id;
    public final String label;
    public final String kind;
    public final ComponentName component;
    public final String action;
    public final IntentFilter filter;

    public IntentCapability(String label, String kind, ComponentName component, String action, IntentFilter filter) {
        this.label = label; this.kind = kind; this.component = component; this.action = action; this.filter = filter;
        id = kind + ":" + component.flattenToString() + ":" + action + ":" + filter.toString().hashCode();
    }

    public JSONObject describe() throws JSONException {
        return new JSONObject().put("id", id).put("app", label).put("kind", kind)
                .put("component", component.flattenToString()).put("action", action)
                .put("schemes", strings(filter.schemesIterator())).put("mime_types", strings(filter.typesIterator()))
                .put("authorities", authorities()).put("paths", paths()).put("categories", strings(filter.categoriesIterator()))
                .put("note", "Exported intent. Extra parameter semantics are not discoverable; do not invent them.");
    }

    private org.json.JSONArray authorities() throws JSONException {
        org.json.JSONArray values = new org.json.JSONArray();
        for (int i = 0; i < filter.countDataAuthorities(); i++) {
            IntentFilter.AuthorityEntry entry = filter.getDataAuthority(i);
            values.put(new JSONObject().put("host", entry.getHost()).put("port", entry.getPort()));
        }
        return values;
    }
    private org.json.JSONArray paths() throws JSONException {
        org.json.JSONArray values = new org.json.JSONArray();
        for (int i = 0; i < filter.countDataPaths(); i++) {
            android.os.PatternMatcher entry = filter.getDataPath(i);
            values.put(new JSONObject().put("path", entry.getPath()).put("pattern_type", entry.getType()));
        }
        return values;
    }

    private org.json.JSONArray strings(java.util.Iterator<String> iterator) {
        org.json.JSONArray values = new org.json.JSONArray();
        if (iterator != null) { while (iterator.hasNext()) { values.put(iterator.next()); } }
        return values;
    }
}
