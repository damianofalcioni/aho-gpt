package aho.gpt.android.access;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.ui.SettingsActivity;

/** Checks grant matching and the actual diagnostic row without reading screen content. */
public final class ScreenAccessTest {
    private final Instrumentation instrumentation;

    public ScreenAccessTest(Instrumentation instrumentation) { this.instrumentation = instrumentation; }
    public void run() {
        matching();
        var context = instrumentation.getTargetContext();
        Activity activity = instrumentation.startActivitySync(new Intent(context, SettingsActivity.class)
                .setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try { instrumentation.runOnMainSync(() -> diagnostic(activity)); }
        finally {
            instrumentation.runOnMainSync(activity::finish);
            instrumentation.waitForIdleSync();
        }
    }
    private void matching() {
        ComponentName service = new ComponentName("example.assistant", "example.assistant.ScreenService");
        require(ScreenAccessGrant.contains("example.assistant/.ScreenService", service), "Match short component names");
        require(ScreenAccessGrant.contains("other.app/.Service:" + service.flattenToString(), service), "Match full names in a list");
        require(!ScreenAccessGrant.contains(null, service), "A missing setting must not grant access");
        require(!ScreenAccessGrant.contains("", service), "An empty setting must not grant access");
        require(!ScreenAccessGrant.contains("example.assistant/.ScreenServiceExtra", service), "Reject partial name matches");
        require(!ScreenAccessGrant.contains("example.assistant.debug/.ScreenService", service), "Grants are variant-specific");
    }
    private void diagnostic(Activity activity) {
        boolean globallyEnabled = Settings.Secure.getInt(activity.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;
        String enabled = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean expected = globallyEnabled && ScreenAccessGrant.contains(enabled,
                new ComponentName(activity, AssistantAccessibilityService.class));
        require(ScreenAccessGrant.granted(activity) == expected, "Read the grant rather than requiring a service connection");
        LinearLayout rows = activity.findViewById(R.id.diagnostic_access);
        for (int index = 0; index < rows.getChildCount(); index++) {
            View row = rows.getChildAt(index);
            TextView label = row.findViewById(R.id.diagnostic_label);
            if (activity.getString(R.string.accessibility_name).contentEquals(label.getText())) {
                TextView status = row.findViewById(R.id.diagnostic_status);
                require(activity.getString(expected ? R.string.diagnostic_ok : R.string.diagnostic_ko).contentEquals(status.getText()),
                        "Screen access diagnostic must match the Android grant");
                return;
            }
        }
        throw new AssertionError("Screen access row is missing");
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
