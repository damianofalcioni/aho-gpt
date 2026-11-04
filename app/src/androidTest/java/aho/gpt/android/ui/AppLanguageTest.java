package aho.gpt.android.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Switch;
import aho.gpt.R;
import aho.gpt.android.language.AppLanguage;
import aho.gpt.android.speech.SpeechPreferences;
import java.util.Locale;
import java.util.Map;

/** Opt-in checks restore display preferences and never start speech or authentication. */
public final class AppLanguageTest {
    private static final String SYSTEM = "use_system_language";
    private static final String SELECTED = "app_language";
    private final Instrumentation instrumentation;
    public AppLanguageTest(Instrumentation instrumentation) { this.instrumentation = instrumentation; }

    public void run() {
        Context context = instrumentation.getTargetContext();
        SharedPreferences preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Map<String, ?> saved = preferences.getAll();
        Locale device = Locale.getDefault();
        Locale speech = SpeechPreferences.locale(context, SpeechPreferences.INPUT);
        try {
            require(preferences.edit().remove(SYSTEM).remove(SELECTED).commit(), "Could not prepare display preferences");
            policy(context);
            screen(context, "en", "Use System Language");
            screen(context, "it", "Usa la lingua di sistema");
            require(device.equals(Locale.getDefault()), "Display language must not change the process locale");
            require(speech.equals(SpeechPreferences.locale(context, SpeechPreferences.INPUT)), "Speech locale must stay separate");
        } finally { restore(preferences, saved); }
    }
    private void policy(Context context) {
        require(AppLanguage.useSystem(context), "System language must default on");
        String system = AppLanguage.effective(context);
        require(system.equals(AppLanguage.selected(context)), "Initial picker choice must match the system");
        AppLanguage.useSystem(context, false);
        require(system.equals(AppLanguage.selected(context)), "First manual use must capture the system choice");
        AppLanguage.useSystem(context, true);
        AppLanguage.select(context, "it");
        require(system.equals(AppLanguage.effective(context)), "Saving in automatic mode must not change display language");
        AppLanguage.useSystem(context, false);
        require("it".equals(AppLanguage.effective(context)), "Manual mode must use the saved language");
        AppLanguage.useSystem(context, true);
        require("it".equals(AppLanguage.selected(context)), "Automatic mode must retain the manual language");
        require(system.equals(AppLanguage.effective(context)), "Automatic mode must restore system matching");
    }
    private void screen(Context context, String tag, String label) {
        AppLanguage.select(context, tag); AppLanguage.useSystem(context, false);
        Activity activity = instrumentation.startActivitySync(new Intent(context, SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                require(label.equals(activity.getString(R.string.use_system_language)), "Settings must use translated resources");
                Switch control = activity.findViewById(R.id.use_system_language);
                require(!control.isChecked(), "The switch must show manual mode");
                require(activity.findViewById(R.id.app_language_settings).isEnabled(), "The language gear must stay available");
                require(activity.findViewById(R.id.stt_provider_status).getVisibility() == View.GONE,
                        "STT must not show helper text below its row");
            });
            automaticMode(activity, tag);
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            instrumentation.waitForIdleSync();
        }
    }
    private void automaticMode(Activity activity, String previous) {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(SettingsActivity.class.getName(), null, false);
        try {
            instrumentation.runOnMainSync(() -> ((Switch) activity.findViewById(R.id.use_system_language)).performClick());
            if (previous.equals(AppLanguage.effective(activity))) { return; }
            Activity refreshed = instrumentation.waitForMonitorWithTimeout(monitor, 5000);
            require(refreshed != null, "Switching effective language must recreate settings");
            try {
                instrumentation.runOnMainSync(() -> {
                    require(((Switch) refreshed.findViewById(R.id.use_system_language)).isChecked(), "The system switch must stay on");
                    require(refreshed.findViewById(R.id.app_language_settings).isEnabled(), "The gear must also work in automatic mode");
                });
            } finally { instrumentation.runOnMainSync(refreshed::finish); }
        } finally { instrumentation.removeMonitor(monitor); }
    }
    private void restore(SharedPreferences preferences, Map<String, ?> saved) {
        SharedPreferences.Editor editor = preferences.edit().remove(SYSTEM).remove(SELECTED);
        if (saved.containsKey(SYSTEM)) { editor.putBoolean(SYSTEM, (Boolean) saved.get(SYSTEM)); }
        if (saved.containsKey(SELECTED)) { editor.putString(SELECTED, (String) saved.get(SELECTED)); }
        require(editor.commit(), "Could not restore display preferences");
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
