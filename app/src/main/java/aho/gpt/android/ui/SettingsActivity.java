package aho.gpt.android.ui;

import aho.gpt.android.language.LocalizedActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import aho.gpt.R;

public final class SettingsActivity extends LocalizedActivity {
    static final String TARGET = "aho.gpt.settings_target";
    private SpeechSettingsRows speech;
    private AboutInformation about;
    private ToolsSettingsDialog tools;
    private LoggingSettingsRows logging;
    private DiagnosticSection diagnostics;
    private ChatGptSettingsDialog account;
    private AppLanguageSettings language;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_settings);
        language = new AppLanguageSettings(this);
        diagnostics = new DiagnosticSection(this);
        speech = new SpeechSettingsRows(this);
        about = new AboutInformation(this);
        tools = new ToolsSettingsDialog(this);
        logging = new LoggingSettingsRows(this, state);
        account = new ChatGptSettingsDialog(this);
        TextView version = findViewById(R.id.app_version);
        version.setText(getString(R.string.app_version, aho.gpt.BuildConfig.VERSION_NAME));
        int target = getIntent().getIntExtra(TARGET, 0);
        if (target != 0) { findViewById(target).post(() -> findViewById(target).requestFocusFromTouch()); }
    }
    @Override protected void onResume() {
        super.onResume(); diagnostics.start();
        account.refresh(); speech.refresh(); logging.refresh(); tools.start();
    }
    @Override protected void onPause() { diagnostics.stop(); speech.stopTests(); tools.stop(); super.onPause(); }
    @Override protected void onDestroy() {
        diagnostics.close(); logging.close(); speech.close(); about.close(); tools.close(); account.close(); language.close(); super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle state) { logging.save(state); super.onSaveInstanceState(state); }
    @SuppressWarnings("deprecation") // Required native activity-result callback.
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data); logging.storageResult(request, result, data);
        diagnostics.storageResult(request, result, data);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        logging.permissionResult(request);
        diagnostics.recheckStorage();
        if (request == RecognitionSettingsDialog.PERMISSION_REQUEST) {
            speech.permissionResult(grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED);
        }
    }
}
