package aho.gpt.android.tools;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.widget.Switch;
import aho.gpt.R;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.logging.storage.AndroidLegacyExternalStorageAccess;
import aho.gpt.android.logging.storage.AppOutputStorageSettings;
import aho.gpt.android.ui.SettingsActivity;

/** Intercepts the real Android permission launch and returns cancellation without granting access. */
final class LogPermissionTest {
    private final Instrumentation instrumentation;
    LogPermissionTest(Instrumentation instrumentation) { this.instrumentation = instrumentation; }
    void run() throws Exception {
        var context = instrumentation.getTargetContext();
        require(AndroidLegacyExternalStorageAccess.needsOutputPermission(context), "Run with storage permission denied");
        require(!AppLogger.isLoggingEnabled() && !AppOutputStorageSettings.useSdCard(context), "Run with logging and SD off");
        var monitor = new Instrumentation.ActivityMonitor(new IntentFilter("android.content.pm.action.REQUEST_PERMISSIONS"),
                new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true);
        instrumentation.addMonitor(monitor);
        Activity activity = instrumentation.startActivitySync(new Intent(context, SettingsActivity.class)
                .setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> ((Switch) activity.findViewById(R.id.log_enabled)).setChecked(true));
            long deadline = SystemClock.elapsedRealtime() + 5_000;
            while (monitor.getHits() == 0 && SystemClock.elapsedRealtime() < deadline) { Thread.sleep(25); }
            require(monitor.getHits() == 1, "Enabling logs must launch the Android permission prompt");
            while (!ready(activity) && SystemClock.elapsedRealtime() < deadline) { Thread.sleep(25); }
            require(ready(activity) && !AppLogger.isLoggingEnabled(), "Cancelling permission must leave logging off");
            require(AndroidLegacyExternalStorageAccess.needsOutputPermission(context), "Test must not grant access");
        } finally {
            instrumentation.removeMonitor(monitor);
            instrumentation.runOnMainSync(activity::finish);
            instrumentation.waitForIdleSync();
        }
    }
    private boolean ready(Activity activity) {
        boolean[] ready = {false};
        instrumentation.runOnMainSync(() -> {
            Switch enabled = activity.findViewById(R.id.log_enabled);
            ready[0] = enabled.isEnabled() && !enabled.isChecked();
        });
        return ready[0];
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
