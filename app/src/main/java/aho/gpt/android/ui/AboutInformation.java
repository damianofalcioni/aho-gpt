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
    private static final String SITE = "https://damianofalcioni.github.io/aho-gpt/";
    private final Activity activity;
    private AlertDialog dialog;

    AboutInformation(Activity activity) {
        this.activity = activity;
        link(R.id.about_homepage, SITE);
        link(R.id.about_source, REPOSITORY);
        link(R.id.about_issues, REPOSITORY + "/issues/new");
        link(R.id.about_changelog, SITE + "CHANGELOG/");
        link(R.id.about_privacy, SITE + "privacy-policy/");
        link(R.id.about_terms, SITE + "terms-of-service/");
        settingsHelp();
    }
    private void settingsHelp() {
        information(R.id.chatgpt_info, R.string.chatgpt, R.string.chatgpt_help);
        information(R.id.custom_info, R.string.custom, R.string.custom_help);
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
