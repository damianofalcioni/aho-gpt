package dev.lightassistant.android.tools;

import android.app.Activity;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only integration of the real catalog, discovery tool dispatch and structured result metadata. */
final class CatalogDiscoveryTest {
    private final Context context;
    private final Activity activity;

    CatalogDiscoveryTest(Context context, Activity activity) { this.context = context; this.activity = activity; }

    void run() throws Exception {
        IntentCatalog catalog = new IntentCatalog(context);
        catalog.scan();
        DeviceTools tools = new DeviceTools(catalog, new IntentExecutor(activity));
        JSONObject overview = new JSONObject(tools.execute("find_intents", new JSONObject().put("mode", "overview")));
        require(overview.getInt("total") == catalog.size(), "Overview must account for the catalog");
        require(!overview.has("capabilities"), "Overview must not transmit the inventory");
        JSONObject page = new JSONObject(tools.execute("find_intents", new JSONObject().put("category", "open_app")));
        JSONArray capabilities = page.getJSONArray("capabilities");
        require(capabilities.length() <= 25, "Discovery pages must stay bounded");
        for (int i = 0; i < capabilities.length(); i++) {
            JSONObject entry = capabilities.getJSONObject(i);
            require(entry.getBoolean("opens_app"), "Structured launcher search must exclude other actions");
            require(entry.getBoolean("semantics_known"), "Launcher contract must be described");
        }
        unknownMode(catalog);
    }

    private void unknownMode(IntentCatalog catalog) throws Exception {
        try {
            catalog.search(new JSONObject().put("mode", "invented"));
            throw new AssertionError("Unknown discovery mode must be rejected");
        } catch (IllegalArgumentException expected) {
            require(expected.getMessage().contains("mode"), "Report why discovery was rejected");
        }
    }

    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
