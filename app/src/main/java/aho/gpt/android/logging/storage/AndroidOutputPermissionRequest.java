package aho.gpt.android.logging.storage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import java.io.IOException;
import java.util.concurrent.Executor;
import aho.gpt.R;
import aho.gpt.android.logging.storage.AppOutputFolderSettings.Kind;

/** Requests access to the selected storage before enabling logging. */
public final class AndroidOutputPermissionRequest {
    public interface Result {
        void complete(Kind kind, boolean allowed);
    }

    private static final String STATE = "output_permission_request";
    private final Activity activity;
    private final Result result;
    private final Executor worker;
    private Kind pending;
    private boolean waiting;
    private int generation;

    public AndroidOutputPermissionRequest(Activity activity, Result result, Executor worker) {
        this.activity = activity;
        this.result = result;
        this.worker = worker;
    }

    public void request(Kind kind) {
        if (pending != null) {
            return;
        }
        pending = kind;
        inspect(true, null);
    }

    public void cancel(Kind kind) {
        if (pending == kind) {
            pending = null;
            generation++;
        }
    }
    public boolean pending() { return pending != null; }

    private void inspect(boolean mayPrompt, Intent grant) {
        Kind kind = pending;
        int token = generation;
        waiting = false;
        worker.execute(() -> {
            boolean allowed = false;
            try {
                if (grant != null) {
                    AndroidOutputStorageAccess.accept(activity, grant);
                }
                AndroidOutputStorage.verify(activity, AndroidOutputStorage.requested(activity, kind));
                allowed = true;
            } catch (IOException | RuntimeException e) {
                allowed = false;
            }
            boolean ready = allowed;
            activity.runOnUiThread(() -> deliver(kind, token, ready, mayPrompt));
        });
    }

    private void deliver(Kind kind, int token, boolean ready, boolean mayPrompt) {
        if (token != generation || pending != kind || isClosed()) {
            return;
        }
        if (!ready && mayPrompt && !AndroidOutputStorage.hasAccess(activity, kind)) {
            prompt(kind);
        } else {
            finish(ready);
        }
    }

    private boolean isClosed() {
        return activity.isDestroyed() || activity.isFinishing();
    }

    private void prompt(Kind kind) {
        int token = generation;
        waiting = AndroidOutputPermissionPrompt.show(activity, kind,
                () -> deliver(kind, token, false, false), () -> waiting = true,
                () -> pending == kind && token == generation && !isClosed());
    }

    public boolean handlePermissionResult(int requestCode) {
        Kind kind = AndroidOutputPermissionPrompt.kindFor(requestCode, AndroidOutputPermissionPrompt.PHONE_REQUEST);
        if (kind == null) {
            return false;
        }
        if (pending == kind) {
            inspect(false, null);
        }
        return true;
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Kind kind = AndroidOutputPermissionPrompt.kindFor(requestCode, AndroidOutputPermissionPrompt.CARD_REQUEST);
        if (kind == null) {
            return false;
        }
        if (pending != kind) {
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
        Kind kind = pending;
        if (kind == null || isClosed()) {
            return;
        }
        pending = null;
        waiting = false;
        generation++;
        if (!allowed) {
            Toast.makeText(activity, R.string.log_storage_failed, Toast.LENGTH_LONG).show();
        }
        result.complete(kind, allowed);
    }

    public void saveState(Bundle state) {
        state.putString(STATE, pending == null ? null : pending.name());
        state.putBoolean(STATE + "_waiting", waiting);
    }

    public void restoreState(Bundle state) {
        String saved = state == null ? null : state.getString(STATE);
        if (saved != null) {
            pending = Kind.valueOf(saved);
            waiting = state.getBoolean(STATE + "_waiting");
            if (!waiting) {
                inspect(true, null);
            }
        }
    }
}
