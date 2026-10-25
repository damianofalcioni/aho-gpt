package aho.gpt.android.logging.storage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import java.io.IOException;
import java.util.concurrent.Executor;
import aho.gpt.R;

/** Requests access to the selected storage before enabling logging. */
public final class AndroidOutputPermissionRequest {
    public interface Result {
        void complete(boolean allowed);
    }

    private static final String STATE = "output_permission_request";
    private final Activity activity;
    private final Result result;
    private final Executor worker;
    private boolean pending;
    private boolean waiting;
    private int generation;

    public AndroidOutputPermissionRequest(Activity activity, Result result, Executor worker) {
        this.activity = activity;
        this.result = result;
        this.worker = worker;
    }

    public void request() {
        if (pending) {
            return;
        }
        pending = true;
        inspect(true, null);
    }

    public void cancel() {
        if (pending) {
            pending = false;
            generation++;
        }
    }
    public boolean pending() { return pending; }

    private void inspect(boolean mayPrompt, Intent grant) {
        int token = generation;
        waiting = false;
        worker.execute(() -> {
            boolean allowed = false;
            try {
                if (grant != null) {
                    AndroidOutputStorageAccess.accept(activity, grant);
                }
                AndroidOutputStorage.verify(activity, AndroidOutputStorage.requested(activity));
                allowed = true;
            } catch (IOException | RuntimeException e) {
                allowed = false;
            }
            boolean ready = allowed;
            activity.runOnUiThread(() -> deliver(token, ready, mayPrompt));
        });
    }

    private void deliver(int token, boolean ready, boolean mayPrompt) {
        if (token != generation || !pending || isClosed()) {
            return;
        }
        if (!ready && mayPrompt && !AndroidOutputStorage.hasAccess(activity)) {
            prompt();
        } else {
            finish(ready);
        }
    }

    private boolean isClosed() {
        return activity.isDestroyed() || activity.isFinishing();
    }

    private void prompt() {
        int token = generation;
        waiting = AndroidOutputPermissionPrompt.show(activity,
                () -> deliver(token, false, false), () -> waiting = true,
                () -> pending && token == generation && !isClosed());
    }

    public boolean handlePermissionResult(int requestCode) {
        if (requestCode != AndroidOutputPermissionPrompt.PHONE_REQUEST) {
            return false;
        }
        if (pending) {
            inspect(false, null);
        }
        return true;
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != AndroidOutputPermissionPrompt.CARD_REQUEST) {
            return false;
        }
        if (!pending) {
            return true;
        }
        if (resultCode == Activity.RESULT_OK && data != null) {
            inspect(false, data);
        } else {
            finish(false);
        }
        return true;
    }

    private void finish(boolean allowed) {
        if (!pending || isClosed()) {
            return;
        }
        pending = false;
        waiting = false;
        generation++;
        if (!allowed) {
            Toast.makeText(activity, R.string.log_storage_failed, Toast.LENGTH_LONG).show();
        }
        result.complete(allowed);
    }

    public void saveState(Bundle state) {
        // Keep the existing saved-state format across activity recreation.
        state.putString(STATE, pending ? "LOGS" : null);
        state.putBoolean(STATE + "_waiting", waiting);
    }

    public void restoreState(Bundle state) {
        String saved = state == null ? null : state.getString(STATE);
        if ("LOGS".equals(saved)) {
            pending = true;
            waiting = state.getBoolean(STATE + "_waiting");
            if (!waiting) {
                inspect(true, null);
            }
        }
    }
}
