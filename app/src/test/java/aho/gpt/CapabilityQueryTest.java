package aho.gpt;

import aho.gpt.core.CapabilityMetadata;
import aho.gpt.core.CapabilityQuery;
import java.util.Arrays;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class CapabilityQueryTest {
    private static final String MIME = "mime_type";
    private static final String IMAGE = "image/png";
    private static final String HTTPS = "https";
    private static final String CATEGORY = "category";
    private static final String SCHEME = "scheme";
    private static final String QUERY = "query";
    private static final String APPS = "apps";
    private static final CapabilityMetadata BROWSER = new CapabilityMetadata("Chrome", "com.android.chrome",
            "com.android.chrome/Main", "activity", "android.intent.action.VIEW", false,
            Arrays.asList("http", HTTPS), Collections.emptyList());

    @Test public void combinesAppCategorySchemeAndPackageConstraints() throws Exception {
        JSONObject filters = new JSONObject().put("app", "CHROME").put(CATEGORY, "open_link").put(SCHEME, "HTTPS")
                .put("package", "com.android.chrome");
        assertTrue(new CapabilityQuery(filters).matches(BROWSER));
        assertFalse(new CapabilityQuery(filters.put(SCHEME, "tel")).matches(BROWSER));
        assertFalse(new CapabilityQuery(filters.put(SCHEME, HTTPS).put("package", "com.android")).matches(BROWSER));
    }

    @Test public void searchesStandardDescriptionsAndMultipleKeywords() throws Exception {
        assertTrue(new CapabilityQuery(new JSONObject().put(QUERY, "website chrome")).matches(BROWSER));
        assertTrue(new CapabilityQuery(new JSONObject().put(QUERY, HTTPS)).matches(BROWSER));
        assertFalse(new CapabilityQuery(new JSONObject().put(QUERY, "website scanner")).matches(BROWSER));
    }

    @Test public void matchesAnyAlternativeByNameOrPackageWithOtherFiltersStillRequired() throws Exception {
        JSONObject filters = new JSONObject().put(APPS, new JSONArray().put("messenger").put(" CHROME "))
                .put(CATEGORY, "open_link").put(SCHEME, HTTPS).put(QUERY, "website");
        assertTrue(new CapabilityQuery(filters).matches(BROWSER));
        assertFalse(new CapabilityQuery(filters.put(CATEGORY, "send_message")).matches(BROWSER));
        filters.put(CATEGORY, "open_link").put(APPS, new JSONArray().put("com.android.chrome"));
        assertTrue(new CapabilityQuery(filters).matches(BROWSER));
        assertFalse(new CapabilityQuery(filters.put("app", "messenger")).matches(BROWSER));
    }

    @Test public void alternativesDoNotInterpretRegexOrCorrectSpelling() throws Exception {
        JSONObject filters = new JSONObject().put(APPS, new JSONArray().put("chrmoe").put("chrome|messenger"));
        assertFalse(new CapabilityQuery(filters).matches(BROWSER));
        assertTrue(new CapabilityQuery(filters.put(APPS, new JSONArray().put("chrom"))).matches(BROWSER));
    }

    @Test public void filtersMimeWildcardsInBothDirections() throws Exception {
        assertTrue(query(IMAGE).matches(share("image/*")));
        assertTrue(query("image/*").matches(share(IMAGE)));
        assertTrue(query(IMAGE).matches(share("*/*")));
        assertFalse(query("text/plain").matches(share("image/*")));
        assertFalse(query(IMAGE).matches(BROWSER));
    }

    @Test public void filtersComponentKindAndExactAction() throws Exception {
        assertFalse(new CapabilityQuery(new JSONObject().put("kind", "service")).matches(BROWSER));
        assertFalse(new CapabilityQuery(new JSONObject().put("action", "VIEW")).matches(BROWSER));
        assertTrue(new CapabilityQuery(new JSONObject().put("action", BROWSER.action())).matches(BROWSER));
    }

    @Test public void rejectsUnknownCategoriesInsteadOfSilentlyReturningEverything() {
        assertThrows(IllegalArgumentException.class,
                () -> new CapabilityQuery(new JSONObject().put(CATEGORY, "scan_document")));
    }

    private CapabilityQuery query(String mime) throws Exception { return new CapabilityQuery(new JSONObject().put(MIME, mime)); }
    private CapabilityMetadata share(String mime) {
        return new CapabilityMetadata("Share", "example.share", "example.share/Main", "activity", "android.intent.action.SEND",
                false, Collections.emptyList(), Collections.singletonList(mime));
    }
}
