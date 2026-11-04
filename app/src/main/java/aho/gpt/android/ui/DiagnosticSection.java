package aho.gpt.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

/** Refreshes access status only while settings is foreground. */
final class DiagnosticSection implements AutoCloseable {
    private final AccessDiagnostics access;
    private final StorageDiagnostics storage;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SharedPreferences folders;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener;
    private final Runnable tick = this::tick;
    private boolean active;

    DiagnosticSection(Activity activity) {
        access = new AccessDiagnostics(activity); storage = new StorageDiagnostics(activity);
        folders = activity.getSharedPreferences("output_folders", Activity.MODE_PRIVATE);
        listener = (preferences, key) -> main.post(this::recheckStorage);
        folders.registerOnSharedPreferenceChangeListener(listener);
    }
    void storageResult(int request, int result, Intent data) { storage.result(request, result, data); }
    void recheckStorage() { if (active) { storage.refresh(); } }
    void start() { stop(); active = true; storage.refresh(); tick(); }
    private void tick() {
        if (!active) { return; }
        access.refresh(); main.postDelayed(tick, 1000);
    }
    void stop() {
        active = false; main.removeCallbacksAndMessages(null); storage.stop();
    }
    @Override public void close() {
        stop(); folders.unregisterOnSharedPreferenceChangeListener(listener); storage.close();
    }
}
