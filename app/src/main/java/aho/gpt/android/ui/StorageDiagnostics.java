package aho.gpt.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import aho.gpt.R;
import aho.gpt.android.logging.storage.AndroidOutputDestination;
import aho.gpt.android.logging.storage.AndroidOutputStorage;
import aho.gpt.android.logging.storage.AndroidLegacyExternalStorageAccess;
import aho.gpt.android.logging.storage.AndroidOutputStorageAccess;
import aho.gpt.android.logging.storage.AndroidOutputStorageVolumes;
import aho.gpt.android.logging.storage.AppOutputStorageSettings;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A worker-owned write probe of the selected destination. Never grants access or changes logging. */
final class StorageDiagnostics implements AutoCloseable {
    private final Activity activity;
    private final Context context;
    private final DiagnosticRow row;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final LogAccessSettings settings;
    private int generation;

    StorageDiagnostics(Activity activity) {
        this.activity = activity; context = activity.getApplicationContext();
        settings = new LogAccessSettings(activity, worker);
        row = new DiagnosticRow(activity, R.string.diagnostic_log_folder, settings::open);
    }
    void refresh() {
        int request = ++generation;
        row.checking();
        worker.execute(() -> {
            boolean allowed = check();
            activity.runOnUiThread(() -> {
                if (request != generation || activity.isDestroyed()) { return; }
                row.state(allowed);
            });
        });
    }
    private boolean check() {
        if (permissionMissing()) { return false; }
        try {
            AndroidOutputDestination requested = AndroidOutputStorage.requested(context);
            AndroidOutputStorage.verify(context, requested);
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }
    private boolean permissionMissing() {
        if (!AppOutputStorageSettings.useSdCard(context)) {
            return AndroidLegacyExternalStorageAccess.needsOutputPermission(context);
        }
        AndroidOutputStorageVolumes.Card card = AndroidOutputStorageVolumes.find(context,
                AppOutputStorageSettings.cardId(context));
        return card != null && AndroidOutputStorageAccess.needsCardGrant(context, card);
    }
    void result(int request, int result, Intent data) { settings.result(request, result, data); }
    void stop() { generation++; }
    @Override public void close() { stop(); worker.shutdown(); }
}
