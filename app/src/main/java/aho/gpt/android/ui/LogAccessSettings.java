package aho.gpt.android.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.logging.storage.AndroidLegacyExternalStorageAccess;
import aho.gpt.android.logging.storage.AndroidOutputDestination;
import aho.gpt.android.logging.storage.AndroidOutputStorage;
import aho.gpt.android.logging.storage.AndroidOutputStorageAccess;
import aho.gpt.android.logging.storage.AndroidOutputStorageBrowser;
import aho.gpt.android.logging.storage.AppOutputStorageSettings;
import java.io.IOException;
import java.util.concurrent.Executor;

/** Opens system access UI without enabling logs or changing the selected storage. */
final class LogAccessSettings {
    private static final int REQUEST = 4022;
    private final Activity activity;
    private final Executor worker;

    LogAccessSettings(Activity activity, Executor worker) {
        this.activity = activity; this.worker = worker;
    }
    void open() {
        if (!AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()) {
            AndroidOutputDestination destination = AndroidOutputStorage.current(activity);
            worker.execute(() -> browse(destination));
        } else if (AppOutputStorageSettings.useSdCard(activity)) {
            picker();
        } else { PermissionSettings.app(activity); }
    }
    @SuppressWarnings("deprecation") // Native activity-result API supports Android 6.
    private void picker() {
        try {
            activity.startActivityForResult(AndroidOutputStorageAccess.picker(AppOutputStorageSettings.cardId(activity)), REQUEST);
        } catch (ActivityNotFoundException | SecurityException exception) { unavailable(); }
    }
    void result(int request, int result, Intent data) {
        if (request != REQUEST || result != Activity.RESULT_OK || data == null) { return; }
        worker.execute(() -> {
            try { AndroidOutputStorageAccess.acceptSelected(activity, data); }
            catch (IOException | RuntimeException exception) { activity.runOnUiThread(this::unavailable); }
        });
    }
    private void browse(AndroidOutputDestination destination) {
        try { AndroidOutputStorageBrowser.prepare(activity, destination); }
        catch (IOException | RuntimeException exception) { AppLogger.w("Logs", "Could not prepare folder"); }
        activity.runOnUiThread(() -> {
            if (activity.isDestroyed() || activity.isFinishing()) { return; }
            try { AndroidOutputStorageBrowser.open(activity, destination); }
            catch (ActivityNotFoundException | SecurityException exception) { unavailable(); }
        });
    }
    private void unavailable() {
        if (!activity.isDestroyed()) { Toast.makeText(activity, R.string.log_storage_failed, Toast.LENGTH_LONG).show(); }
    }
}
