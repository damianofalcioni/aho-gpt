package aho.gpt;

import aho.gpt.core.CapabilityMetadata;
import aho.gpt.core.CapabilityQuery;
import java.util.Arrays;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class CapabilityQueryTest {
    private static final String MIME = "mime_type";
    private static final String IMAGE = "image/png";
    private static final CapabilityMetadata BROWSER = new CapabilityMetadata("Chrome", "com.android.chrome",
            "com.android.chrome/Main", "activity", "android.intent.action.VIEW", false,
            Arrays.asList("http", "https"), Collections.emptyList());

    @Test public void combinesAppCategorySchemeAndPackageConstraints() throws Exception {
        JSONObject filters = new JSONObject().put("app", "CHROME").put("category", "open_link").put("scheme", "HTTPS")
                .put("package", "com.android.chrome");
        assertTrue(new CapabilityQuery(filters).matches(BROWSER));
        assertFalse(new CapabilityQuery(filters.put("scheme", "tel")).matches(BROWSER));
        assertFalse(new CapabilityQuery(filters.put("scheme", "https").put("package", "com.android")).matches(BROWSER));
    }

    @Test public void searchesStandardDescriptionsAndMultipleKeywords() throws Exception {
        assertTrue(new CapabilityQuery(new JSONObject().put("query", "website chrome")).matches(BROWSER));
        assertTrue(new CapabilityQuery(new JSONObject().put("query", "https")).matches(BROWSER));
        assertFalse(new CapabilityQuery(new JSONObject().put("query", "website scanner")).matches(BROWSER));
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
                () -> new CapabilityQuery(new JSONObject().put("category", "scan_document")));
    }

    private CapabilityQuery query(String mime) throws Exception { return new CapabilityQuery(new JSONObject().put(MIME, mime)); }
    private CapabilityMetadata share(String mime) {
        return new CapabilityMetadata("Share", "example.share", "example.share/Main", "activity", "android.intent.action.SEND",
                false, Collections.emptyList(), Collections.singletonList(mime));
    }
}
