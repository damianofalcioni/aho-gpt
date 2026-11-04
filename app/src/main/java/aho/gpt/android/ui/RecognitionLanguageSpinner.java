package aho.gpt.android.ui;

import android.app.Activity;
import android.widget.Spinner;
import aho.gpt.R;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.core.SpeechLanguageOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class RecognitionLanguageSpinner {
    private RecognitionLanguageSpinner() { }
    static void bind(Activity activity, Spinner spinner, List<String> supported, Runnable changed) {
        List<String> tags = SpeechLanguageOptions.choices(supported, Arrays.asList(Locale.getAvailableLocales()));
        Collections.sort(tags, (left, right) -> Locale.forLanguageTag(left).getDisplayName()
                .compareToIgnoreCase(Locale.forLanguageTag(right).getDisplayName()));
        tags.add(0, "");
        List<String> names = new ArrayList<>();
        names.add(activity.getString(R.string.device_language_short));
        for (int i = 1; i < tags.size(); i++) { names.add(SpeechPreferences.label(activity, Locale.forLanguageTag(tags.get(i)))); }
        String saved = SpeechPreferences.selected(activity, SpeechPreferences.INPUT);
        if (!tags.contains(saved)) {
            tags.add(saved); names.add(activity.getString(R.string.saved_choice_unavailable, saved));
        }
        spinner.setEnabled(true);
        SpinnerChoices.bind(activity, spinner, names, tags.indexOf(saved), position -> {
            SpeechPreferences.select(activity, SpeechPreferences.INPUT, tags.get(position)); changed.run();
        });
    }
}
