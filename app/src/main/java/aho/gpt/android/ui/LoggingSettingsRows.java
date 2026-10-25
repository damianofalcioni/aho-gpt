package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.logging.storage.AndroidOutputPermissionRequest;
import aho.gpt.android.logging.storage.AppOutputFolderSettings.Kind;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class LoggingSettingsRows {
    private final Activity activity;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Switch enabled;
    private final LogStorageSettings storage;
    private final LogFolderDialog folder;
    private final AndroidOutputPermissionRequest permissions;
    private final SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener;
    private boolean checking;

    LoggingSettingsRows(Activity activity, Bundle state) {
        this.activity = activity;
        preferences = activity.getSharedPreferences("app_logging", Activity.MODE_PRIVATE);
        listener = (prefs, key) -> activity.runOnUiThread(() -> { if (!activity.isDestroyed()) { refresh(); } });
        enabled = activity.findViewById(R.id.log_enabled);
        storage = new LogStorageSettings(activity, worker, this::refresh);
        folder = new LogFolderDialog(activity, worker, this::refresh);
        permissions = new AndroidOutputPermissionRequest(activity, (kind, allowed) -> apply(allowed), worker);
        preferences.registerOnSharedPreferenceChangeListener(listener);
        activity.findViewById(R.id.log_settings).setOnClickListener(view -> folder.show());
        activity.findViewById(R.id.log_info).setOnClickListener(view -> new AlertDialog.Builder(activity)
                .setTitle(R.string.logs).setMessage(R.string.log_help).setPositiveButton(android.R.string.ok, null).show());
        refresh();
        permissions.restoreState(state);
        refresh();
    }
    void refresh() {
        enabled.setOnCheckedChangeListener(null);
        enabled.setChecked(AppLogger.isLoggingEnabled());
        enabled.setEnabled(!checking && !permissions.pending());
        enabled.setOnCheckedChangeListener((button, value) -> change(value));
        storage.refresh();
    }
    private void change(boolean value) {
        if (value) { permissions.request(Kind.LOGS); refresh(); }
        else { permissions.cancel(Kind.LOGS); apply(false); }
    }
    private void apply(boolean value) {
        checking = true; refresh();
        worker.execute(() -> {
            AppLogger.setLoggingEnabled(activity.getApplicationContext(), value);
            activity.runOnUiThread(() -> {
                checking = false;
                if (activity.isDestroyed()) { return; }
                refresh();
                if (value && !AppLogger.isLoggingEnabled()) {
                    Toast.makeText(activity, R.string.log_storage_failed, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    void permissionResult(int request) { permissions.handlePermissionResult(request); }
    void save(Bundle state) { permissions.saveState(state); }
    void storageResult(int request, int result, android.content.Intent data) {
        if (!permissions.handleActivityResult(request, result, data)) { storage.result(request, result, data); }
    }
    void close() {
        permissions.cancel(Kind.LOGS);
        preferences.unregisterOnSharedPreferenceChangeListener(listener); folder.close(); worker.shutdown();
    }
}
