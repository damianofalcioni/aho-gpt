package dev.lightassistant.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import dev.lightassistant.R;
import dev.lightassistant.android.speech.SpeechPreferences;
import dev.lightassistant.core.SpeechLanguageOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class SpeechLanguagePicker {
    private SpeechLanguagePicker() { }

    static AlertDialog show(Activity activity, String key, int title, List<String> supported, Runnable changed) {
        List<String> tags = SpeechLanguageOptions.choices(supported, Arrays.asList(Locale.getAvailableLocales()));
        Collections.sort(tags, (left, right) -> Locale.forLanguageTag(left).getDisplayName()
                .compareToIgnoreCase(Locale.forLanguageTag(right).getDisplayName()));
        tags.add(0, "");
        List<String> names = new ArrayList<>();
        names.add(activity.getString(R.string.device_language, SpeechPreferences.label(activity, Locale.getDefault())));
        for (int i = 1; i < tags.size(); i++) { names.add(SpeechPreferences.label(activity, Locale.forLanguageTag(tags.get(i)))); }
        return new AlertDialog.Builder(activity).setTitle(title)
                .setSingleChoiceItems(names.toArray(new String[0]), tags.indexOf(SpeechPreferences.selected(activity, key)),
                        (dialog, which) -> {
                            SpeechPreferences.select(activity, key, tags.get(which)); changed.run(); dialog.dismiss();
                        })
                .setNegativeButton(android.R.string.cancel, null).show();
    }
}
