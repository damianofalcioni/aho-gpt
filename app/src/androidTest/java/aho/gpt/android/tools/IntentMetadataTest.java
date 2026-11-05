package aho.gpt.android.tools;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PatternMatcher;
import aho.gpt.core.CapabilityIdentity;
import org.json.JSONArray;
import org.json.JSONObject;

/** Compact model records preserve declared filters and existing saved action choices. */
final class IntentMetadataTest {
    void run() throws Exception {
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addDataScheme("https");
        filter.addDataAuthority("example.org", "443");
        filter.addDataPath("/messages", PatternMatcher.PATTERN_PREFIX);
        IntentCapability cap = new IntentCapability("Example", "activity", new ComponentName("example.app", "example.app.Main"),
                Intent.ACTION_VIEW, filter);
        JSONObject record = cap.describe();
        require("example.app/example.app.Main".equals(record.getString("component")), "Preserve exact component");
        require("https".equals(record.getJSONArray("schemes").getString(0)), "Preserve URI schemes");
        require(Intent.CATEGORY_DEFAULT.equals(record.getJSONArray("categories").getString(0)), "Preserve intent categories");
        JSONObject authority = record.getJSONArray("authorities").getJSONObject(0);
        require("example.org".equals(authority.getString("host")) && authority.getInt("port") == 443, "Preserve URI authority");
        JSONObject path = record.getJSONArray("paths").getJSONObject(0);
        require("/messages".equals(path.getString("path")) && path.getInt("pattern_type") == PatternMatcher.PATTERN_PREFIX,
                "Preserve path matching contract");
        compact(record);
        require(!record.has("mime_types"), "Omit empty MIME filters");
        JSONObject legacy = new JSONObject(record.toString()).put("mime_types", new JSONArray());
        require(CapabilityIdentity.key(legacy).equals(cap.preferenceKey()), "Compact records must preserve saved action choices");
        emptyFilters();
    }

    private void emptyFilters() throws Exception {
        IntentCapability cap = new IntentCapability("Example", "service", new ComponentName("example.app", "example.app.Service"),
                "example.ACTION", new IntentFilter("example.ACTION"));
        JSONObject record = cap.describe();
        require("custom".equals(record.getString("category")), "Service semantics must remain unknown");
        require(record.length() == 6, "Unfiltered records need only identity and category fields");
        compact(record);
        JSONObject legacy = new JSONObject(record.toString());
        for (String field : new String[]{"schemes", "mime_types", "authorities", "paths", "categories"}) {
            legacy.put(field, new JSONArray());
        }
        require(CapabilityIdentity.key(legacy).equals(cap.preferenceKey()), "Empty filters must retain their saved identity");
    }

    private void compact(JSONObject record) {
        for (String field : new String[]{"description", "usage", "note", "opens_app", "semantics_known"}) {
            require(!record.has(field), "Omit repeated or redundant metadata: " + field);
        }
    }

    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
