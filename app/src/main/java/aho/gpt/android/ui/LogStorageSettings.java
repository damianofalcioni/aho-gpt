package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Switch;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.logging.storage.AndroidOutputStorageAccess;
import aho.gpt.android.logging.storage.AndroidOutputStorageVolumes;
import aho.gpt.android.logging.storage.AppOutputStorageSettings;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

final class LogStorageSettings {
    private static final int REQUEST = 4021;
    private final Activity activity;
    private final ExecutorService worker;
    private final Runnable changed;
    private final Switch enabled;
    private boolean checking;

    LogStorageSettings(Activity activity, ExecutorService worker, Runnable changed) {
        this.activity = activity; this.worker = worker; this.changed = changed;
        enabled = activity.findViewById(R.id.log_external_storage);
        activity.findViewById(R.id.log_storage_info).setOnClickListener(view -> new AlertDialog.Builder(activity)
                .setTitle(R.string.log_external_storage).setMessage(R.string.log_storage_help)
                .setPositiveButton(android.R.string.ok, null).show());
    }
    void refresh() {
        enabled.setOnCheckedChangeListener(null);
        boolean selected = AppOutputStorageSettings.useSdCard(activity);
        enabled.setChecked(selected);
        enabled.setEnabled(!checking && (selected || AndroidOutputStorageVolumes.find(activity, "") != null));
        enabled.setOnCheckedChangeListener((button, value) -> change(value));
    }
    private void change(boolean value) {
        refresh();
        if (!value) { check(() -> AppOutputStorageSettings.disable(activity)); return; }
        AndroidOutputStorageVolumes.Card card = AndroidOutputStorageVolumes.find(activity, "");
        if (card == null) { toast(); return; }
        if (!AndroidOutputStorageAccess.needsCardGrant(activity, card)) {
            check(() -> AndroidOutputStorageAccess.enable(activity, card,
                    AndroidOutputStorageAccess.existingGrant(activity, card)));
            return;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.log_external_storage).setMessage(R.string.log_sd_access)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> picker(card.id)).show();
    }
    @SuppressWarnings("deprecation") // Native activity-result API supports Android 6.
    private void picker(String id) {
        try { activity.startActivityForResult(AndroidOutputStorageAccess.picker(id), REQUEST); }
        catch (ActivityNotFoundException | SecurityException exception) { toast(); }
    }
    void result(int request, int result, Intent data) {
        if (request == REQUEST && result == Activity.RESULT_OK && data != null) {
            check(() -> AndroidOutputStorageAccess.accept(activity, data));
        }
    }
    private void check(Action action) {
        checking = true; refresh();
        worker.execute(() -> {
            boolean success;
            try { action.run(); AppLogger.refreshOutputFolder(activity); success = true; }
            catch (IOException | RuntimeException exception) { success = false; }
            boolean completed = success;
            activity.runOnUiThread(() -> {
                checking = false;
                if (activity.isDestroyed()) { return; }
                changed.run();
                if (!completed) { toast(); }
            });
        });
    }
    private void toast() { Toast.makeText(activity, R.string.log_storage_failed, Toast.LENGTH_LONG).show(); }
    private interface Action { void run() throws IOException; }
}
