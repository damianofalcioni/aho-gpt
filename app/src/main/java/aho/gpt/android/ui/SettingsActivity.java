package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.auth.SignInActivity;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SettingsActivity extends Activity {
    static final String TARGET = "aho.gpt.settings_target";
    private AssistantApplication app;
    private TextView status;
    private SpeechSettingsRows speech;
    private AboutInformation about;
    private ToolsSettingsDialog tools;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); app = (AssistantApplication) getApplication();
        setContentView(R.layout.activity_settings);
        status = findViewById(R.id.connection_status);
        speech = new SpeechSettingsRows(this);
        about = new AboutInformation(this);
        tools = new ToolsSettingsDialog(this);
        TextView version = findViewById(R.id.app_version);
        version.setText(getString(R.string.app_version, aho.gpt.BuildConfig.VERSION_NAME));
        action(R.id.connect, () -> startActivity(new Intent(this, SignInActivity.class)));
        action(R.id.choose_model, this::models);
        action(R.id.manage_usage, () -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/settings/usage"))));
        action(R.id.accessibility, this::accessibility);
        action(R.id.overlay, () -> open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        action(R.id.default_assistant, () -> open(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)));
        action(R.id.refresh_tools, () -> app.worker().execute(() -> { app.catalog().scan(); refresh(); }));
        action(R.id.sign_out, this::signOut);
        int target = getIntent().getIntExtra(TARGET, 0);
        if (target != 0) { findViewById(target).post(() -> findViewById(target).requestFocusFromTouch()); }
    }

    private void action(int id, Runnable action) {
        findViewById(id).setOnClickListener(view -> action.run());
    }
    @Override protected void onResume() { super.onResume(); refresh(); speech.refresh(); }
    @Override protected void onPause() { speech.stopTests(); super.onPause(); }
    @Override protected void onDestroy() { speech.close(); about.close(); tools.close(); super.onDestroy(); }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == RecognitionSettingsDialog.PERMISSION_REQUEST) {
            speech.permissionResult(grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED);
        }
    }
    private void refresh() {
        AccountSummary.load(this, summary -> status.setText(getString(R.string.connection_status,
                summary.account(), summary.model(), summary.tools())));
    }

    private void models() {
        status.setText(R.string.loading_models);
        app.worker().execute(() -> {
            try {
                JSONObject result = app.http().json("https://api.openai.com/v1/models", app.auth().accessToken(), null, "");
                JSONArray catalog = result.getJSONArray("models");
                ArrayList<String> slugs = new ArrayList<>(); ArrayList<String> names = new ArrayList<>();
                for (int i = 0; i < catalog.length(); i++) {
                    JSONObject model = catalog.getJSONObject(i);
                    if ("list".equals(model.optString("visibility"))) {
                        slugs.add(model.getString("slug")); names.add(model.getString("display_name"));
                    }
                }
                runOnUiThread(() -> chooseModel(slugs, names));
            } catch (Exception exception) { showError(exception); }
        });
    }
    private void chooseModel(ArrayList<String> slugs, ArrayList<String> names) {
        if (isFinishing()) { return; }
        if (slugs.isEmpty()) { status.setText(R.string.no_available_models); return; }
        new AlertDialog.Builder(this).setTitle(R.string.choose_model).setItems(names.toArray(new String[0]),
                (dialog, which) -> { app.model(slugs.get(which)); app.worker().execute(this::refresh); }).show();
    }
    private void accessibility() {
        new AlertDialog.Builder(this).setTitle(R.string.accessibility_name).setMessage(R.string.accessibility_disclosure)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton(android.R.string.cancel, null).show();
    }
    private void signOut() {
        app.worker().execute(() -> {
            try { app.auth().signOut(); app.model(""); refresh(); }
            catch (Exception exception) { showError(new IllegalStateException(getString(R.string.revoke_failed), exception)); }
        });
    }
    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException exception) { status.setText(R.string.setting_unavailable); }
    }
    private void showError(Exception exception) {
        runOnUiThread(() -> { if (!isFinishing()) { status.setText(exception.getMessage()); } });
    }
}
