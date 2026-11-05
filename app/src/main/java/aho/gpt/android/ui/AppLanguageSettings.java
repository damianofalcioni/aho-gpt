package aho.gpt.android.ui;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Spinner;
import android.widget.Switch;
import aho.gpt.R;
import aho.gpt.android.language.AppLanguage;
import aho.gpt.android.language.LocalizedActivity;
import java.util.Arrays;

/** The display-language row follows the reference's saved choice and OK/Cancel behavior. */
final class AppLanguageSettings implements AutoCloseable {
    private final LocalizedActivity activity;
    private AlertDialog dialog;

    AppLanguageSettings(LocalizedActivity activity) {
        this.activity = activity;
        Switch control = activity.findViewById(R.id.use_system_language);
        control.setChecked(AppLanguage.useSystem(activity));
        control.setOnCheckedChangeListener((button, checked) -> {
            AppLanguage.useSystem(activity, checked); activity.refreshDisplay();
        });
        activity.findViewById(R.id.app_language_settings).setOnClickListener(view -> configure());
        activity.findViewById(R.id.app_language_info).setOnClickListener(view -> info());
    }
    private void configure() {
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_app_language, null);
        Spinner spinner = content.findViewById(R.id.app_language_choice);
        SpinnerChoices.bind(activity, spinner,
                Arrays.asList(activity.getString(R.string.language_english), activity.getString(R.string.language_italian)),
                "it".equals(AppLanguage.selected(activity)) ? 1 : 0, position -> { });
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.app_language_title).setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (window, which) -> {
                    AppLanguage.select(activity, spinner.getSelectedItemPosition() == 1 ? "it" : "en");
                    activity.refreshDisplay();
                }).show();
    }
    private void info() {
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.use_system_language)
                .setMessage(R.string.app_language_info).setPositiveButton(R.string.close, null).show();
    }
    @Override public void close() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
