package dev.lightassistant.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import dev.lightassistant.R;
import dev.lightassistant.android.AssistantApplication;
import dev.lightassistant.android.auth.SignInActivity;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SettingsActivity extends Activity {
    private AssistantApplication app;
    private TextView status;
    private SpeechLanguageSettings speechLanguages;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); app = (AssistantApplication) getApplication();
        setContentView(R.layout.activity_settings);
        status = findViewById(R.id.connection_status);
        speechLanguages = new SpeechLanguageSettings(this);
        action(R.id.speech_languages, speechLanguages::show);
        TextView version = findViewById(R.id.app_version);
        version.setText(getString(R.string.app_version, dev.lightassistant.BuildConfig.VERSION_NAME));
        action(R.id.back, this::finish);
        action(R.id.connect, () -> startActivity(new Intent(this, SignInActivity.class)));
        action(R.id.choose_model, this::models);
        action(R.id.manage_usage, () -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/settings/usage"))));
        action(R.id.accessibility, this::accessibility);
        action(R.id.overlay, () -> open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        action(R.id.default_assistant, () -> open(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)));
        action(R.id.refresh_tools, () -> app.worker().execute(() -> { app.catalog().scan(); refresh(); }));
        action(R.id.sign_out, this::signOut);
    }

    private void action(int id, Runnable action) {
        findViewById(id).setOnClickListener(view -> action.run());
    }
    @Override protected void onResume() { super.onResume(); app.worker().execute(this::refresh); }
    @Override protected void onDestroy() { speechLanguages.close(); super.onDestroy(); }
    private void refresh() {
        try {
            JSONObject account = app.auth().account();
            String email = account.optString("email", getString(R.string.not_connected));
            boolean connected = !account.optString("access_token").isEmpty();
            String text = getString(R.string.connection_status, connected ? email : getString(R.string.not_connected),
                    app.model().isEmpty() ? getString(R.string.no_model) : app.model(), app.catalog().size());
            runOnUiThread(() -> { if (!isFinishing()) { status.setText(text); } });
        } catch (Exception exception) { showError(exception); }
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
