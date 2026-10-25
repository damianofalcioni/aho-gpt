package aho.gpt.android.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.logging.AppLogger;
import java.util.ArrayList;
import java.util.List;

/** Refreshes local status only while settings is foreground. */
final class DiagnosticSection implements AutoCloseable {
    private final Activity activity;
    private final AssistantApplication app;
    private final AccessDiagnostics access;
    private final SpeechDiagnostics speech;
    private final StorageDiagnostics storage;
    private final NetworkDiagnostics network;
    private final List<DiagnosticRow> setup = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SharedPreferences folders;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener;
    private final Runnable tick = this::tick;
    private boolean active;

    DiagnosticSection(Activity activity) {
        this.activity = activity; app = (AssistantApplication) activity.getApplication();
        access = new AccessDiagnostics(activity); storage = new StorageDiagnostics(activity);
        network = new NetworkDiagnostics(activity);
        speech = new SpeechDiagnostics(activity);
        add(R.string.diagnostic_account, R.id.connect); add(R.string.diagnostic_model, R.id.choose_model);
        add(R.string.tools, R.id.tools_settings); add(R.string.logs, R.id.log_settings);
        folders = activity.getSharedPreferences("output_folders", Activity.MODE_PRIVATE);
        listener = (preferences, key) -> main.post(() -> { if (active) { storage.refresh(); } });
        folders.registerOnSharedPreferenceChangeListener(listener);
    }
    private void add(int title, int action) {
        setup.add(new DiagnosticRow(activity, R.id.diagnostic_services, title,
                () -> activity.findViewById(action).performClick()));
    }
    SpeechDiagnostics speech() { return speech; }
    void recheckStorage() { if (active) { storage.refresh(); } }
    void start() {
        stop(); active = true; speech.checking(); storage.refresh(); tick();
    }
    private void tick() {
        if (!active) { return; }
        access.refresh(); network.refresh(); speech.refresh();
        boolean enabled = app.catalog().preferences().enabled();
        setup.get(2).update(enabled ? R.string.diagnostic_on : R.string.diagnostic_off,
                enabled ? R.color.diagnostic_ok : R.color.diagnostic_warning,
                activity.getString(R.string.diagnostic_tools_help, app.catalog().size()));
        boolean logging = AppLogger.isLoggingEnabled();
        setup.get(3).update(logging ? R.string.diagnostic_on : R.string.diagnostic_off,
                logging ? R.color.diagnostic_ok : R.color.diagnostic_warning, activity.getString(R.string.log_help));
        main.postDelayed(tick, 1000);
    }
    void account(AccountSummary.Snapshot summary) {
        setup.get(0).state(summary.connected(), R.string.diagnostic_saved, R.string.diagnostic_missing,
                activity.getString(R.string.diagnostic_account_help));
        setup.get(1).state(!app.model().isEmpty(), R.string.diagnostic_selected, R.string.diagnostic_not_selected,
                activity.getString(R.string.diagnostic_model_help, summary.model()));
    }
    void stop() {
        active = false; main.removeCallbacksAndMessages(null); storage.stop();
        access.close(); network.close(); speech.close(); for (DiagnosticRow row : setup) { row.close(); }
    }
    @Override public void close() {
        stop(); folders.unregisterOnSharedPreferenceChangeListener(listener); storage.close();
    }
}
