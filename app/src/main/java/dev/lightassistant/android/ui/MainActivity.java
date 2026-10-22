package dev.lightassistant.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import dev.lightassistant.R;
import dev.lightassistant.android.AssistantApplication;
import dev.lightassistant.android.auth.SignInActivity;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private AssistantApplication app;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); app = (AssistantApplication) getApplication();
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round((getResources().getConfiguration().isScreenRound() ? 32 : 24)
                * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding); scroll.addView(layout);
        TextView title = new TextView(this); title.setText(R.string.app_name); title.setTextSize(26); layout.addView(title);
        status = new TextView(this); status.setTextSize(15); layout.addView(status);
        button(layout, R.string.speak, () -> startActivity(new Intent(this, VoiceActivity.class)));
        button(layout, R.string.continue_chatgpt, () -> startActivity(new Intent(this, SignInActivity.class)));
        button(layout, R.string.choose_model, this::models);
        button(layout, R.string.manage_usage, () -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/settings/usage"))));
        button(layout, R.string.accessibility_name, this::accessibility);
        button(layout, R.string.overlay_settings, () -> open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        button(layout, R.string.default_assistant, () -> open(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)));
        button(layout, R.string.refresh_tools, () -> app.worker().execute(() -> { app.catalog().scan(); refresh(); }));
        button(layout, R.string.sign_out, this::signOut);
        TextView help = new TextView(this); help.setText(R.string.setup_help); layout.addView(help);
        setContentView(scroll);
    }

    private void button(LinearLayout parent, int label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setMinHeight(48);
        button.setOnClickListener(view -> action.run()); parent.addView(button);
    }
    @Override protected void onResume() { super.onResume(); app.worker().execute(this::refresh); }
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
