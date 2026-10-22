package dev.lightassistant.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** JVM-testable ranking, pagination and aggregate overview; contains no Android objects or execution. */
public final class CapabilityIndex {
    private static final int PAGE_SIZE = 25;
    private final Map<String, CapabilityMetadata> entries;

    public CapabilityIndex(Map<String, CapabilityMetadata> entries) { this.entries = new LinkedHashMap<>(entries); }

    public record Page(List<String> ids, int total, int nextOffset) { }

    public Page search(CapabilityQuery query, int offset) {
        List<String> matches = new ArrayList<>();
        for (Map.Entry<String, CapabilityMetadata> entry : entries.entrySet()) {
            if (query.matches(entry.getValue())) { matches.add(entry.getKey()); }
        }
        Collections.sort(matches, (first, second) -> Integer.compare(query.score(entries.get(second)), query.score(entries.get(first))));
        int start = Math.min(matches.size(), Math.max(0, offset));
        int end = start + Math.min(PAGE_SIZE, matches.size() - start);
        return new Page(new ArrayList<>(matches.subList(start, end)), matches.size(), end < matches.size() ? end : -1);
    }

    public JSONObject overview(CapabilityQuery query) throws Exception {
        Map<CapabilityCategory, Group> groups = new EnumMap<>(CapabilityCategory.class);
        for (CapabilityCategory category : CapabilityCategory.values()) { groups.put(category, new Group()); }
        Set<String> apps = new HashSet<>();
        int total = 0;
        for (CapabilityMetadata metadata : entries.values()) {
            if (!query.matches(metadata)) { continue; }
            groups.get(metadata.category()).add(metadata.packageName()); apps.add(metadata.packageName()); total++;
        }
        return new JSONObject().put("categories", describe(groups)).put("total", total).put("apps", apps.size())
                .put("note", "Counts describe discovered declarations, not verified task completion. "
                        + "Search a category for exact filters and execution limitations. Custom task meanings remain unknown.");
    }

    private JSONArray describe(Map<CapabilityCategory, Group> groups) throws Exception {
        JSONArray result = new JSONArray();
        for (CapabilityCategory category : CapabilityCategory.values()) {
            Group group = groups.get(category);
            if (group.count == 0) { continue; }
            result.put(new JSONObject().put("category", category.id).put("description", category.description)
                    .put("usage", category.usage).put("capabilities", group.count).put("apps", group.apps.size()));
        }
        return result;
    }

    private static final class Group {
        private int count;
        private final Set<String> apps = new HashSet<>();
        void add(String app) { count++; apps.add(app); }
    }
}
