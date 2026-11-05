package aho.gpt.android.ui;

import android.app.AlertDialog;
import android.widget.Switch;
import aho.gpt.R;
import aho.gpt.android.language.LocalizedActivity;

/** Display controls apply immediately and keep the native settings layout. */
final class DisplaySettings implements AutoCloseable {
    private final AppLanguageSettings language;
    private AlertDialog information;

    DisplaySettings(LocalizedActivity activity) {
        language = new AppLanguageSettings(activity);
        Switch control = activity.findViewById(R.id.light_theme);
        control.setChecked(AppTheme.isLight(activity));
        control.setOnCheckedChangeListener((button, checked) -> {
            AppTheme.setLight(activity, checked); activity.refreshDisplay();
        });
        activity.findViewById(R.id.light_theme_info).setOnClickListener(view -> {
            information = new AlertDialog.Builder(activity).setTitle(R.string.light_theme)
                    .setMessage(R.string.light_theme_info).setPositiveButton(R.string.close, null).show();
        });
    }
    @Override public void close() {
        language.close();
        if (information != null) { information.dismiss(); information = null; }
    }
}
