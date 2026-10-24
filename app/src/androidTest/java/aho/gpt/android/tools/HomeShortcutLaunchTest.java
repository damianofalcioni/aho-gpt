package aho.gpt.android.tools;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import aho.gpt.android.access.AssistantAccessibilityService;
import aho.gpt.android.ui.MainActivity;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in real shortcut launch through the same screen tools available to the model; never log screen text. */
final class HomeShortcutLaunchTest {
    private static final String PHASE = "phase";
    private final Instrumentation instrumentation;
    private final String label;
    private final String marker;

    HomeShortcutLaunchTest(Instrumentation instrumentation, String label, String marker) {
        this.instrumentation = instrumentation; this.label = label; this.marker = marker;
    }

    void run() {
        Bundle result = new Bundle();
        try {
            require(!marker.isEmpty(), "Specify screen_marker to check the opened target");
            launch(result);
            instrumentation.finish(Activity.RESULT_OK, result);
        } catch (Exception | AssertionError failure) {
            result.putString("stream", "Shortcut check failed: " + failure.getClass().getSimpleName());
            instrumentation.finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void launch(Bundle result) throws Exception {
        result.putString(PHASE, "launch_assistant");
        Activity activity = instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.waitForIdleSync();
        IntentCatalog catalog = new IntentCatalog(instrumentation.getTargetContext());
        catalog.scan();
        result.putInt("webapk_launchers", catalog.search(new JSONObject().put("app", "org.chromium.webapk")
                .put("category", "open_app")).getInt("total"));
        result.putInt("named_app_launchers", catalog.search(new JSONObject().put("app", label)
                .put("category", "open_app")).getInt("total"));
        DeviceTools tools = new DeviceTools(catalog, new IntentExecutor(activity));
        result.putString(PHASE, "connect_accessibility");
        awaitAccessibility(tools);
        result.putString(PHASE, "navigate_home");
        accepted(tools.execute("ui_action", new JSONObject().put("action", "home")));
        Thread.sleep(700);
        result.putString(PHASE, "find_shortcut");
        JSONObject before = new JSONObject(tools.execute("read_screen", new JSONObject()));
        result.putInt("home_nodes", before.getJSONArray("nodes").length());
        String homePackage = activePackage();
        require(!homePackage.isEmpty(), "Home must have an active accessibility root");
        String node = shortcut(before.getJSONArray("nodes"));
        result.putString(PHASE, "click_shortcut");
        String dispatch = tools.execute("ui_action", new JSONObject().put("action", "click").put("node_id", node));
        accepted(dispatch);
        result.putString(PHASE, "check_target");
        require(targetVisible(tools, homePackage), "Expected target marker did not appear outside Home after clicking the shortcut");
        result.putString(PHASE, "complete");
        result.putString("stream", dispatch);
        result.putBoolean("shortcut_clickable", true);
        result.putBoolean("target_marker_visible", true);
        result.putBoolean("foreground_changed", true);
    }

    private void awaitAccessibility(DeviceTools tools) throws Exception {
        // Instrumentation restarts the app process; allow the operator to reconnect the enabled service.
        for (int attempt = 0; attempt < 300; attempt++) {
            if (tools.accessibilityAvailable()) { return; }
            Thread.sleep(100);
        }
        throw new AssertionError("Accessibility service is not connected");
    }

    private String shortcut(JSONArray nodes) throws Exception {
        String found = "";
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (matches(node, label) && node.getBoolean("clickable")) {
                require(found.isEmpty(), "Shortcut label must identify exactly one clickable node");
                found = node.getString("node_id");
            }
        }
        require(!found.isEmpty(), "No matching clickable shortcut on the current Home page");
        return found;
    }

    private boolean matches(JSONObject node, String expected) {
        String text = node.optString("text") + " " + node.optString("description");
        return text.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private boolean targetVisible(DeviceTools tools, String homePackage) throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            Thread.sleep(500);
            JSONArray nodes = new JSONObject(tools.execute("read_screen", new JSONObject())).getJSONArray("nodes");
            if (containsMarker(nodes) && externalTarget(homePackage)) { return true; }
        }
        return false;
    }

    private boolean containsMarker(JSONArray nodes) throws Exception {
        for (int i = 0; i < nodes.length(); i++) {
            if (matches(nodes.getJSONObject(i), marker)) { return true; }
        }
        return false;
    }

    private boolean externalTarget(String homePackage) {
        String current = activePackage();
        return !current.isEmpty() && !homePackage.equals(current)
                && !instrumentation.getTargetContext().getPackageName().equals(current);
    }

    private String activePackage() {
        String[] result = {""};
        instrumentation.runOnMainSync(() -> {
            AccessibilityNodeInfo root = AssistantAccessibilityService.connected().getRootInActiveWindow();
            result[0] = root == null ? "" : String.valueOf(root.getPackageName());
        });
        return result[0];
    }

    private void accepted(String receipt) throws Exception {
        require("accepted".equals(new JSONObject(receipt).getString("status")), "UI dispatch must be accepted");
    }

    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
