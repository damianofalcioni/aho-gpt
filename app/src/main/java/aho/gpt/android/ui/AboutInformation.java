package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import aho.gpt.R;

/** Project links, local app details and contextual settings help. */
final class AboutInformation implements AutoCloseable {
    private static final String REPOSITORY = "https://github.com/damianofalcioni/aho-gpt";
    private final Activity activity;
    private AlertDialog dialog;

    AboutInformation(Activity activity) {
        this.activity = activity;
        link(R.id.about_homepage, "https://damianofalcioni.github.io/aho-gpt/");
        link(R.id.about_source, REPOSITORY);
        link(R.id.about_issues, REPOSITORY + "/issues/new");
        link(R.id.about_changelog, REPOSITORY + "/commits");
        activity.findViewById(R.id.about_privacy).setOnClickListener(view -> show(R.string.about_privacy, R.string.privacy_information));
        activity.findViewById(R.id.about_terms).setOnClickListener(view -> show(R.string.about_terms, R.string.usage_information));
        settingsHelp();
    }
    private void settingsHelp() {
        information(R.id.account_info, R.string.continue_chatgpt, R.string.account_help);
        information(R.id.model_info, R.string.choose_model, R.string.model_help);
        information(R.id.usage_info, R.string.manage_usage, R.string.usage_help);
        information(R.id.sign_out_info, R.string.sign_out, R.string.sign_out_help);
        information(R.id.accessibility_info, R.string.accessibility_name, R.string.accessibility_disclosure);
        information(R.id.overlay_info, R.string.overlay_settings, R.string.overlay_help);
        information(R.id.assistant_info, R.string.default_assistant, R.string.assistant_help);
        information(R.id.refresh_tools_info, R.string.refresh_tools, R.string.tools_help);
        information(R.id.tools_info, R.string.tools, R.string.tools_information);
    }
    private void information(int id, int title, int message) {
        activity.findViewById(id).setContentDescription(activity.getString(R.string.setting_information_for, activity.getString(title)));
        activity.findViewById(id).setOnClickListener(view -> show(title, message));
    }
    private void link(int id, String url) {
        activity.findViewById(id).setOnClickListener(view -> {
            try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (ActivityNotFoundException | SecurityException exception) {
                Toast.makeText(activity, R.string.setting_unavailable, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void show(int title, int message) {
        dialog = new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setPositiveButton(R.string.close, null).show();
    }
    @Override public void close() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
