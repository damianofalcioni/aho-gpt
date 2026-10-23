package aho.gpt.android.tools;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import aho.gpt.android.ui.MainActivity;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in integration check: launch only the installed package explicitly named by the test operator. */
final class InstalledAppLaunchTest {
    private final Instrumentation instrumentation;
    private final String targetPackage;

    InstalledAppLaunchTest(Instrumentation instrumentation, String targetPackage) {
        this.instrumentation = instrumentation; this.targetPackage = targetPackage;
    }

    void run() {
        Bundle result = new Bundle();
        try {
            if (targetPackage.isEmpty()) { throw new IllegalArgumentException("Specify launch_package explicitly"); }
            launch(result);
            instrumentation.finish(Activity.RESULT_OK, result);
        } catch (Exception | AssertionError failure) {
            result.putString("stream", failure.toString());
            instrumentation.finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void launch(Bundle result) throws Exception {
        Activity activity = instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.waitForIdleSync();
        IntentCatalog catalog = new IntentCatalog(instrumentation.getTargetContext());
        catalog.scan();
        JSONObject search = catalog.search(new JSONObject().put("package", targetPackage).put("category", "open_app"));
        IntentCapability launcher = launcher(catalog, search.getJSONArray("capabilities"));
        DeviceTools tools = new DeviceTools(catalog, new IntentExecutor(activity));
        String dispatch = tools.execute("invoke_intent", new JSONObject().put("capability_id", launcher.id)
                .put("data_uri", "").put("mime_type", "").put("string_extras", new JSONObject()));
        instrumentation.waitForIdleSync();
        result.putInt("catalog_entries", catalog.size());
        result.putInt("matching_entries", search.getInt("total"));
        result.putInt("https_handlers", catalog.search(new JSONObject().put("package", targetPackage)
                .put("category", "open_link").put("scheme", "https")).getInt("total"));
        result.putString("launcher_component", launcher.component.flattenToString());
        result.putString("stream", dispatch + "\nCheck the foreground package separately to verify the app opened.\n");
    }

    private IntentCapability launcher(IntentCatalog catalog, JSONArray entries) throws Exception {
        for (int i = 0; i < entries.length(); i++) {
            IntentCapability cap = catalog.get(entries.getJSONObject(i).getString("id"));
            if (targetPackage.equals(cap.component.getPackageName()) && !cap.launcherCategory().isEmpty()) { return cap; }
        }
        throw new AssertionError("No exported app launcher found in catalog search");
    }
}
