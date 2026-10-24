package aho.gpt.android.tools;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import org.json.JSONObject;

/** Dependency-free framework tests; run with connectedHandheldDebugAndroidTest. */
public final class IntentLaunchTests extends Instrumentation {
    private static final ComponentName TARGET = new ComponentName("example.browser", "example.browser.Main");
    private String targetPackage;
    private String shortcutLabel;
    private String screenMarker;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        targetPackage = arguments == null ? "" : arguments.getString("launch_package", "");
        shortcutLabel = arguments == null ? "" : arguments.getString("home_shortcut", "");
        screenMarker = arguments == null ? "" : arguments.getString("screen_marker", "");
        start();
    }

    @Override public void onStart() {
        if (!targetPackage.isEmpty()) { new InstalledAppLaunchTest(this, targetPackage).run(); return; }
        if (!shortcutLabel.isEmpty()) { new HomeShortcutLaunchTest(this, shortcutLabel, screenMarker).run(); return; }
        Check[] checks = {() -> launcher(Intent.CATEGORY_LAUNCHER), () -> launcher(Intent.CATEGORY_LEANBACK_LAUNCHER),
                this::nonLauncher, this::deepLink, this::invalidData, this::rejectedLaunch,
                this::mimeMetadata, this::discovery, () -> new AssistantEntryTest(getTargetContext()).run()};
        String[] names = {"phoneLauncher", "tvLauncher", "nonLauncher", "deepLink", "invalidData", "rejectedLaunch",
                "mimeMetadata", "discovery", "assistantEntry"};
        int failures = 0;
        for (int i = 0; i < checks.length; i++) {
            if (!check(i, names[i], checks[i], checks.length)) { failures++; }
        }
        Bundle results = new Bundle();
        results.putString("stream", "Tests run: " + checks.length + ", Failures: " + failures + "\n");
        finish(Activity.RESULT_OK, results);
    }

    private boolean check(int index, String name, Check action, int total) {
        Bundle status = new Bundle();
        status.putString("id", "IntentLaunchTests");
        status.putString("class", getClass().getName());
        status.putString("test", name);
        status.putInt("numtests", total);
        status.putInt("current", index + 1);
        sendStatus(1, status);
        try {
            action.run();
            sendStatus(0, status);
            return true;
        } catch (Exception | AssertionError failure) {
            status.putString("stack", failure.toString());
            sendStatus(-2, status);
            return false;
        }
    }

    private interface Check { void run() throws Exception; }

    private void launcher(String category) throws Exception {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(category);
        IntentCapability cap = capability(Intent.ACTION_MAIN, filter);
        RecordingActivity activity = activity();
        new IntentExecutor(activity).execute(cap, new JSONObject());
        Intent launched = activity.launched;
        require(cap.describe().getBoolean("opens_app"), "Launcher must be marked as opening the app");
        require(launched.hasCategory(category), "App launches must include their discovered launcher category");
        require(TARGET.equals(launched.getComponent()), "Must launch the exact discovered component");
        require(launched.getFlags() == (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
                "App launches must use launcher task flags");
        require(!activity.hidden, "Launching must not minimize the assistant first");
    }

    private void nonLauncher() throws Exception {
        IntentCapability cap = capability(Intent.ACTION_MAIN, new IntentFilter(Intent.ACTION_MAIN));
        RecordingActivity activity = activity();
        new IntentExecutor(activity).execute(cap, new JSONObject());
        require(!cap.describe().getBoolean("opens_app"), "MAIN alone is not an app launcher");
        require(activity.launched.getCategories() == null, "Do not invent launcher categories");
        require(activity.launched.getFlags() == Intent.FLAG_ACTIVITY_NEW_TASK, "Keep ordinary activity flags");
    }

    private void deepLink() throws Exception {
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addDataScheme("https");
        IntentCapability cap = capability(Intent.ACTION_VIEW, filter);
        RecordingActivity activity = activity();
        new IntentExecutor(activity).execute(cap, new JSONObject().put("data_uri", "https://example.com"));
        require(!cap.describe().getBoolean("opens_app"), "Deep links are not app launchers");
        require(activity.launched.getCategories() == null, "Do not add launcher categories to deep links");
        require("https://example.com".equals(activity.launched.getDataString()), "Preserve requested URI");
    }

    private void invalidData() throws Exception {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        RecordingActivity activity = activity();
        JSONObject result = new JSONObject(new IntentExecutor(activity).execute(capability(Intent.ACTION_MAIN, filter),
                new JSONObject().put("data_uri", "https://example.com")));
        require("refused".equals(result.getString("status")), "Launcher filter must refuse unexpected data");
        require(activity.launched == null, "Invalid arguments must not dispatch");
        require(!activity.hidden, "Invalid arguments must not hide the assistant");
    }

    private void rejectedLaunch() throws Exception {
        RecordingActivity activity = activity();
        activity.reject = true;
        JSONObject result = new JSONObject(new IntentExecutor(activity).execute(
                capability(Intent.ACTION_MAIN, new IntentFilter(Intent.ACTION_MAIN)), new JSONObject()));
        require("rejected".equals(result.getString("status")), "Android launch rejection must reach the tool loop");
        require(!result.getBoolean("completion_verified"), "A rejection cannot verify task completion");
        require(!activity.hidden, "A rejected launch must not hide the assistant");
    }

    private void mimeMetadata() throws Exception {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SEND);
        filter.addDataType("image/*");
        JSONObject described = capability(Intent.ACTION_SEND, filter).describe();
        require("image/*".equals(described.getJSONArray("mime_types").getString(0)), "Normalize Android wildcard MIME storage");
        require("share_content".equals(described.getString("category")), "Describe the declared share action");
        require(described.getString("usage").contains("unsupported"), "Do not claim image URI streams can be sent");
    }

    private void discovery() throws Exception {
        new CatalogDiscoveryTest(getTargetContext(), activity()).run();
    }

    private IntentCapability capability(String action, IntentFilter filter) {
        return new IntentCapability("Example browser", "activity", TARGET, action, filter);
    }
    private RecordingActivity activity() {
        RecordingActivity[] result = new RecordingActivity[1];
        runOnMainSync(() -> result[0] = new RecordingActivity(getTargetContext().getContentResolver()));
        return result[0];
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }

    private static final class RecordingActivity extends Activity {
        private final ContentResolver resolver;
        private Intent launched;
        private boolean hidden;
        private boolean reject;
        RecordingActivity(ContentResolver resolver) { this.resolver = resolver; }
        @Override public ContentResolver getContentResolver() { return resolver; }
        @Override public void startActivity(Intent intent) {
            if (reject) { throw new SecurityException("Launch rejected"); }
            launched = intent;
        }
        @Override public boolean moveTaskToBack(boolean nonRoot) { hidden = true; return true; }
    }
}
