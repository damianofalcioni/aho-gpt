package dev.lightassistant;

import dev.lightassistant.core.CapabilityIndex;
import dev.lightassistant.core.CapabilityMetadata;
import dev.lightassistant.core.CapabilityQuery;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class CapabilityIndexTest {
    @Test public void ranksExactAppBeforeSubstringAndPreservesLauncherPriority() throws Exception {
        Map<String, CapabilityMetadata> entries = new LinkedHashMap<>();
        entries.put("beta", launcher("Chrome Beta", "example.beta"));
        entries.put("view", new CapabilityMetadata("Chrome", "example.stable", "example.stable/View", "activity",
                "android.intent.action.VIEW", false, Collections.singletonList("https"), Collections.emptyList()));
        entries.put("stable", launcher("Chrome", "example.stable"));
        CapabilityIndex.Page page = new CapabilityIndex(entries).search(
                new CapabilityQuery(new JSONObject().put("app", "chrome")), 0);
        assertEquals("stable", page.ids().get(0));
        assertEquals("view", page.ids().get(1));
    }

    @Test public void boundsPagesAndHandlesExtremeOffsets() throws Exception {
        Map<String, CapabilityMetadata> entries = new LinkedHashMap<>();
        for (int i = 0; i < 2458; i++) { entries.put("id" + i, launcher("App " + i, "example.app" + i)); }
        CapabilityIndex index = new CapabilityIndex(entries);
        CapabilityQuery query = new CapabilityQuery(new JSONObject());
        CapabilityIndex.Page first = index.search(query, 0);
        assertEquals(2458, first.total()); assertEquals(25, first.ids().size()); assertEquals(25, first.nextOffset());
        assertEquals("id25", index.search(query, first.nextOffset()).ids().get(0));
        assertEquals(-1, index.search(query, 2450).nextOffset());
        assertTrue(index.search(query, Integer.MAX_VALUE).ids().isEmpty());
        assertEquals(first.ids(), index.search(query, -1).ids());
    }

    @Test public void overviewGroupsDeclarationsAndCountsDistinctAppsWithoutIdentityLists() throws Exception {
        Map<String, CapabilityMetadata> entries = new LinkedHashMap<>();
        entries.put("one", launcher("Private Label", "private.package"));
        entries.put("alias", launcher("Private Label", "private.package"));
        entries.put("two", launcher("Other Private Label", "other.private.package"));
        JSONObject overview = new CapabilityIndex(entries).overview(new CapabilityQuery(new JSONObject()));
        assertEquals(3, overview.getInt("total")); assertEquals(2, overview.getInt("apps"));
        JSONArray groups = overview.getJSONArray("categories");
        assertEquals(1, groups.length()); assertEquals("open_app", groups.getJSONObject(0).getString("category"));
        assertEquals(2, groups.getJSONObject(0).getInt("apps"));
        assertFalse(overview.toString().contains("Private Label"));
        assertFalse(overview.toString().contains("private.package"));
    }

    @Test public void overviewRespectsFiltersAndEmptyCatalogs() throws Exception {
        Map<String, CapabilityMetadata> entries = new LinkedHashMap<>();
        entries.put("one", launcher("One", "example.one"));
        CapabilityQuery query = new CapabilityQuery(new JSONObject().put("category", "open_link"));
        assertEquals(0, new CapabilityIndex(entries).overview(query).getInt("total"));
        CapabilityIndex empty = new CapabilityIndex(Collections.emptyMap());
        assertEquals(0, empty.overview(query).getJSONArray("categories").length());
        assertEquals(-1, empty.search(query, 0).nextOffset());
    }

    private CapabilityMetadata launcher(String app, String pkg) {
        return new CapabilityMetadata(app, pkg, pkg + "/Main", "activity", "android.intent.action.MAIN", true,
                Collections.emptyList(), Collections.emptyList());
    }
}
