package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.logging.storage.AndroidOutputDestination;
import aho.gpt.android.logging.storage.AndroidOutputStorage;
import aho.gpt.android.logging.storage.AndroidOutputStorageBrowser;
import aho.gpt.android.logging.storage.AppOutputFolderSettings.Kind;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

final class LogFolderDialog {
    private final Activity activity;
    private final ExecutorService worker;
    private final Runnable changed;
    private AlertDialog folder;
    private AlertDialog confirmation;

    LogFolderDialog(Activity activity, ExecutorService worker, Runnable changed) {
        this.activity = activity; this.worker = worker; this.changed = changed;
    }
    void show() {
        AndroidOutputDestination destination = AndroidOutputStorage.current(activity, Kind.LOGS);
        folder = new AlertDialog.Builder(activity).setTitle(R.string.logs).setMessage(destination.label)
                .setNeutralButton(R.string.log_open, null).setNegativeButton(R.string.log_delete, null)
                .setPositiveButton(android.R.string.ok, null).show();
        align(folder);
        folder.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(activity.getColor(R.color.log_danger));
        folder.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> confirm(destination));
        folder.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> worker.execute(() -> prepare(destination)));
    }
    private void confirm(AndroidOutputDestination destination) {
        confirmation = new AlertDialog.Builder(activity).setTitle(R.string.log_delete)
                .setMessage(activity.getString(R.string.log_delete_confirm, destination.label))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.log_delete, (dialog, which) -> {
                    folder.dismiss(); worker.execute(() -> delete(destination.token()));
                }).show();
        confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.getColor(R.color.log_danger));
    }
    private void delete(String token) {
        int result;
        try { AppLogger.clearOutputFolder(activity, token); result = R.string.log_deleted; }
        catch (IOException | RuntimeException exception) { result = R.string.log_storage_failed; }
        int message = result;
        activity.runOnUiThread(() -> {
            if (!activity.isDestroyed()) { changed.run(); toast(message); }
        });
    }
    private void prepare(AndroidOutputDestination destination) {
        try { AndroidOutputStorageBrowser.prepare(activity, destination); }
        catch (IOException | RuntimeException exception) {
            AppLogger.w("Logs", "Could not prepare folder");
        }
        activity.runOnUiThread(() -> {
            if (activity.isDestroyed()) { return; }
            try { AndroidOutputStorageBrowser.open(activity, destination); }
            catch (ActivityNotFoundException | SecurityException exception) { toast(R.string.log_browser_unavailable); }
        });
    }
    private static void align(AlertDialog dialog) {
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE).getParent() instanceof LinearLayout panel) {
            for (int index = 0; index < panel.getChildCount(); index++) {
                View child = panel.getChildAt(index);
                if (!(child instanceof Button)) { child.setVisibility(View.GONE); }
            }
            panel.setGravity(Gravity.END);
        }
    }
    private void toast(int message) { Toast.makeText(activity, message, Toast.LENGTH_LONG).show(); }
    void close() {
        if (confirmation != null) { confirmation.dismiss(); }
        if (folder != null) { folder.dismiss(); }
    }
}
