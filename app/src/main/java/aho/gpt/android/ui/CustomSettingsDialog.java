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

final class CustomSettingsDialog implements AutoCloseable {
    private final Activity activity;
    private final AssistantApplication app;
    private final ProviderSettings settings;
    private AlertDialog dialog;
    private CustomModelPicker models;
    private int generation;
    CustomSettingsDialog(Activity activity) {
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
        models = new CustomModelPicker(activity, content);
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.custom).setView(content)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnDismissListener(ignored -> { generation++; models.close(); token.setText(""); });
        dialog.show(); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ignored -> save(endpoint, token, status));
        load(token, status);
    }
    private void load(EditText token, TextView status) {
        int request = ++generation;
        app.authWorker().execute(() -> {
            try {
                String value = settings.token();
                activity.runOnUiThread(() -> {
                    if (current(request)) { loaded(token, value); }
                });
            } catch (Exception exception) { activity.runOnUiThread(() -> { if (current(request)) { status.setText(R.string.custom_load_failed); } }); }
        });
    }
    private void loaded(EditText token, String value) {
        token.setText(value); token.setEnabled(true); models.start(); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
    }
    private void save(EditText endpoint, EditText token, TextView status) {
        String base;
        try { base = ServerEndpoint.normalize(endpoint.getText().toString()); }
        catch (IllegalArgumentException exception) { endpoint.setError(activity.getString(R.string.custom_endpoint_invalid)); return; }
        String secret = token.getText().toString();
        String model = models.model();
        int request = ++generation;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        app.authWorker().execute(() -> {
            try {
                settings.save(base, secret, model);
                activity.runOnUiThread(() -> saved(request));
            } catch (Exception exception) {
                activity.runOnUiThread(() -> saveFailed(request, status));
            }
        });
    }
    private void saved(int request) {
        if (current(request)) { dialog.dismiss(); CustomNetworkAccess.request(activity); }
    }
    private void saveFailed(int request, TextView status) {
        if (!current(request)) { return; }
        status.setText(R.string.custom_save_failed); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
    }
    private boolean current(int request) { return request == generation && dialog != null && dialog.isShowing(); }
    @Override public void close() { generation++; if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
