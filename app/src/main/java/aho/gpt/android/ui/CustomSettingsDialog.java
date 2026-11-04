package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.custom.ServerEndpoint;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.SpeechApiConfig;

final class CustomSettingsDialog implements AutoCloseable {
    private final Activity activity;
    private final AssistantApplication app;
    private final ProviderSettings settings;
    private AlertDialog dialog;
    private AlertDialog information;
    private CustomModelPicker models;
    private CustomSpeechToggle speech;
    private ServerSpeechSettingsDialog speechSettings;
    private ServerSpeechSettingsDialog realtimeSettings;
    private final Runnable changed;
    private int generation;
    CustomSettingsDialog(Activity activity, Runnable changed) {
        this.changed = changed;
        this.activity = activity; app = (AssistantApplication) activity.getApplication(); settings = new ProviderSettings(activity);
        activity.findViewById(R.id.custom_settings).setOnClickListener(view -> show());
    }
    private void show() {
        close();
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_custom, new FrameLayout(activity), false);
        EditText endpoint = content.findViewById(R.id.custom_endpoint);
        EditText token = content.findViewById(R.id.custom_token);
        token.setEnabled(false);
        TextView status = content.findViewById(R.id.custom_status);
        endpoint.setText(settings.endpoint());
        speech = new CustomSpeechToggle(activity, content);
        speechSettings = new ServerSpeechSettingsDialog(activity, content, speech, AudioMode.SPEECH);
        realtimeSettings = new ServerSpeechSettingsDialog(activity, content, speech, AudioMode.REALTIME);
        models = new CustomModelPicker(activity, content, speech::modelChanged);
        information(content, R.id.custom_endpoint_info, R.string.custom_endpoint, R.string.custom_config_help);
        information(content, R.id.custom_speech_info, R.string.custom_speech_apis, R.string.custom_speech_help);
        information(content, R.id.custom_realtime_info, R.string.custom_realtime_api, R.string.custom_realtime_help);
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.custom).setView(content)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnDismissListener(ignored -> { generation++; closeInformation(); models.close(); speechSettings.close(); realtimeSettings.close(); speech.close(); token.setText(""); });
        dialog.show(); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ignored -> save(endpoint, token, status));
        load(token, status);
    }
    private void information(View content, int id, int title, int message) {
        View button = content.findViewById(id);
        button.setContentDescription(activity.getString(R.string.setting_information_for, activity.getString(title)));
        button.setOnClickListener(view -> {
            closeInformation();
            information = new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setPositiveButton(R.string.close, null).show();
        });
    }
    private void closeInformation() { if (information != null) { information.dismiss(); information = null; } }
    private void load(EditText token, TextView status) {
        int request = ++generation;
        app.authWorker().execute(() -> {
            try {
                String value = settings.token();
                activity.runOnUiThread(() -> {
                    if (current(request)) { loaded(token, value); }
                });
            } catch (Exception exception) { activity.runOnUiThread(() -> { if (current(request)) { status(status, R.string.custom_load_failed); } }); }
        });
    }
    private void loaded(EditText token, String value) {
        token.setText(value); token.setEnabled(true); models.start(); speech.start(); speechSettings.ready(); realtimeSettings.ready();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
    }
    private void save(EditText endpoint, EditText token, TextView status) {
        if (speech.checking()) { return; }
        String base;
        try { base = ServerEndpoint.normalize(endpoint.getText().toString()); }
        catch (IllegalArgumentException exception) { endpoint.setError(activity.getString(R.string.custom_endpoint_invalid)); return; }
        String secret = token.getText().toString();
        String model = models.model();
        AudioMode mode = speech.mode();
        SpeechApiConfig config = speech.configuration();
        int request = ++generation;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        app.authWorker().execute(() -> {
            try {
                settings.save(base, secret, model, mode, config);
                activity.runOnUiThread(() -> saved(request));
            } catch (Exception exception) {
                activity.runOnUiThread(() -> saveFailed(request, status));
            }
        });
    }
    private void saved(int request) {
        if (current(request)) { dialog.dismiss(); changed.run(); CustomNetworkAccess.request(activity); }
    }
    private void saveFailed(int request, TextView status) {
        if (!current(request)) { return; }
        status(status, R.string.custom_save_failed); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
    }
    private void status(TextView status, int message) { status.setText(message); status.setVisibility(View.VISIBLE); }
    private boolean current(int request) { return request == generation && dialog != null && dialog.isShowing(); }
    void pause() { if (speech != null) { speech.stop(); speechSettings.pause(); realtimeSettings.pause(); } }
    @Override public void close() { generation++; if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
