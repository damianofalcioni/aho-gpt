package aho.gpt.android.tools;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import aho.gpt.core.CapabilityCategory;
import aho.gpt.core.CapabilityMetadata;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public final class IntentCapability {
    public final String id;
    public final String label;
    public final String kind;
    public final ComponentName component;
    public final String action;
    public final IntentFilter filter;
    private final CapabilityMetadata metadata;

    public IntentCapability(String label, String kind, ComponentName component, String action, IntentFilter filter) {
        this.label = label; this.kind = kind; this.component = component; this.action = action; this.filter = filter;
        id = kind + ":" + component.flattenToString() + ":" + action + ":" + filter.toString().hashCode();
        metadata = new CapabilityMetadata(label, component.getPackageName(), component.flattenToString(), kind, action,
                !launcherCategory().isEmpty(), values(filter.schemesIterator()), mimeTypes());
    }

    public JSONObject describe() throws JSONException {
        CapabilityCategory category = metadata.category();
        return new JSONObject().put("id", id).put("app", label).put("kind", kind)
                .put("opens_app", !launcherCategory().isEmpty())
                .put("category", category.id).put("description", category.description).put("usage", category.usage)
                .put("semantics_known", category != CapabilityCategory.CUSTOM)
                .put("component", component.flattenToString()).put("action", action)
                .put("schemes", strings(filter.schemesIterator())).put("mime_types", new org.json.JSONArray(metadata.mimeTypes()))
                .put("authorities", authorities()).put("paths", paths()).put("categories", strings(filter.categoriesIterator()))
                .put("note", "Exported intent; standard usage is described where known. Do not invent other extra parameter semantics.");
    }

    public CapabilityMetadata metadata() { return metadata; }

    public String launcherCategory() {
        if (!"activity".equals(kind) || !Intent.ACTION_MAIN.equals(action)) { return ""; }
        if (filter.hasCategory(Intent.CATEGORY_LAUNCHER)) { return Intent.CATEGORY_LAUNCHER; }
        return filter.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) ? Intent.CATEGORY_LEANBACK_LAUNCHER : "";
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

    private org.json.JSONArray strings(Iterator<String> iterator) {
        return new org.json.JSONArray(values(iterator));
    }

    private List<String> values(Iterator<String> iterator) {
        List<String> result = new ArrayList<>();
        if (iterator != null) { while (iterator.hasNext()) { result.add(iterator.next()); } }
        return result;
    }

    private List<String> mimeTypes() {
        List<String> result = new ArrayList<>();
        for (String type : values(filter.typesIterator())) { result.add(type.contains("/") ? type : type + "/*"); }
        return result;
    }
}
