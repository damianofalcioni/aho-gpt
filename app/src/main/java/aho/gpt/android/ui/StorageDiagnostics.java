package aho.gpt.android.ui;

import android.app.Activity;
import android.content.Context;
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
    private int generation;

    StorageDiagnostics(Activity activity) {
        this.activity = activity; context = activity.getApplicationContext();
        row = new DiagnosticRow(activity, R.id.diagnostic_access, R.string.diagnostic_log_folder,
                () -> activity.findViewById(R.id.log_settings).performClick());
    }
    void refresh() {
        int request = ++generation;
        row.update(R.string.diagnostic_checking, R.color.diagnostic_warning, activity.getString(R.string.diagnostic_folder_check));
        worker.execute(() -> {
            Result result = check();
            activity.runOnUiThread(() -> {
                if (request != generation || activity.isDestroyed()) { return; }
                row.update(result.status(), result.status() == R.string.diagnostic_writable
                                ? R.color.diagnostic_ok : R.color.diagnostic_error,
                        activity.getString(R.string.diagnostic_folder_help,
                                activity.getString(result.status()), result.path()));
            });
        });
    }
    private Result check() {
        AndroidOutputDestination target = AndroidOutputStorage.current(context);
        if (permissionMissing()) { return new Result(target.label, R.string.diagnostic_no_permission); }
        try {
            AndroidOutputDestination requested = AndroidOutputStorage.requested(context);
            AndroidOutputStorage.verify(context, requested);
            return new Result(requested.label, R.string.diagnostic_writable);
        } catch (IOException | RuntimeException exception) {
            return new Result(target.label, R.string.diagnostic_unavailable);
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
    void stop() { generation++; row.close(); }
    @Override public void close() { stop(); worker.shutdown(); }
    private record Result(String path, int status) { }
}
