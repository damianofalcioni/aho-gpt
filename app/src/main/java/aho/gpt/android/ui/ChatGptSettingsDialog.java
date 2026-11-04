package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.auth.SignInActivity;
import aho.gpt.android.logging.AppLogger;

final class ChatGptSettingsDialog implements AutoCloseable {
    private final Activity activity;
    private final AssistantApplication app;
    private AlertDialog dialog;
    private AlertDialog confirmation;
    private AccountModelPicker models;
    private View content;
    private int generation;

    ChatGptSettingsDialog(Activity activity) {
        this.activity = activity; app = (AssistantApplication) activity.getApplication();
        Switch toggle = activity.findViewById(R.id.chatgpt_enabled);
        toggle.setChecked(app.chatgptEnabled());
        toggle.setOnCheckedChangeListener((button, enabled) -> app.chatgptEnabled(enabled));
        activity.findViewById(R.id.chatgpt_settings).setOnClickListener(view -> show());
    }
    private void show() {
        close();
        content = activity.getLayoutInflater().inflate(R.layout.dialog_chatgpt, new FrameLayout(activity), false);
        models = new AccountModelPicker(activity, content.findViewById(R.id.account_model));
        content.findViewById(R.id.account_configure).setOnClickListener(view -> SignInActivity.open(activity, false));
        content.findViewById(R.id.account_manage).setOnClickListener(view -> SignInActivity.open(activity, true));
        content.findViewById(R.id.account_sign_out).setOnClickListener(view -> confirm());
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.chatgpt).setView(content)
                .setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnDismissListener(ignored -> { generation++; models.stop(); });
        dialog.show(); refresh();
    }
    void refresh() {
        if (dialog == null || !dialog.isShowing()) { return; }
        int request = ++generation;
        AccountSummary.load(activity, summary -> {
            if (!current(request)) { return; }
            ((TextView) content.findViewById(R.id.account_name)).setText(summary.connected()
                    ? summary.account() : activity.getString(R.string.not_logged_in));
            content.findViewById(R.id.account_sign_out).setEnabled(summary.connected());
            models.load(summary.connected());
        });
    }
    private boolean current(int request) {
        return request == generation && dialog != null && dialog.isShowing();
    }
    private void confirm() {
        confirmation = new AlertDialog.Builder(activity).setTitle(R.string.sign_out)
                .setMessage(R.string.sign_out_confirm).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.sign_out, (ignored, which) -> signOut()).show();
        confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.getColor(R.color.log_danger));
    }
    private void signOut() {
        int request = ++generation;
        app.model("");
        models.stop(); content.findViewById(R.id.account_sign_out).setEnabled(false);
        app.worker().execute(() -> {
            int message = 0;
            try { app.auth().signOut(); }
            catch (Exception exception) { AppLogger.w("Auth", "Remote sign-out not confirmed", exception); message = R.string.revoke_failed; }
            int result = message;
            activity.runOnUiThread(() -> { if (current(request)) { signedOut(result); } });
        });
    }
    private void signedOut(int message) {
        TextView status = content.findViewById(R.id.account_status);
        if (message != 0) { status.setText(message); status.setVisibility(View.VISIBLE); }
        refresh();
    }
    @Override public void close() {
        generation++;
        if (models != null) { models.stop(); }
        if (confirmation != null) { confirmation.dismiss(); confirmation = null; }
        if (dialog != null) { dialog.dismiss(); dialog = null; }
    }
}
