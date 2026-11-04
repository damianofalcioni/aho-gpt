package aho.gpt.android.ui;

import android.app.Activity;
import android.speech.tts.Voice;
import android.widget.Spinner;
import aho.gpt.R;
import aho.gpt.android.speech.SpeechPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Retains saved language-only and unavailable choices without silently changing playback. */
final class PlaybackVoiceSpinner {
    private record Choice(String name, String voice, String tag) { }
    private PlaybackVoiceSpinner() { }
    static void bind(Activity activity, Spinner spinner, List<Voice> voices) {
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice(activity.getString(R.string.device_language,
                SpeechPreferences.label(activity, Locale.getDefault())), "", ""));
        for (Voice voice : voices) {
            String name = activity.getString(R.string.voice_label,
                    SpeechPreferences.label(activity, voice.getLocale()), voice.getName());
            choices.add(new Choice(name, voice.getName(), voice.getLocale().toLanguageTag()));
        }
        int selected = selected(activity, choices);
        List<String> names = new ArrayList<>();
        for (Choice choice : choices) { names.add(choice.name()); }
        spinner.setEnabled(!voices.isEmpty());
        SpinnerChoices.bind(activity, spinner, names, selected, position -> {
            Choice choice = choices.get(position);
            SpeechPreferences.voice(activity, choice.voice());
            SpeechPreferences.select(activity, SpeechPreferences.OUTPUT, choice.tag());
        });
    }
    private static int selected(Activity activity, List<Choice> choices) {
        String voice = SpeechPreferences.voice(activity);
        String tag = SpeechPreferences.selected(activity, SpeechPreferences.OUTPUT);
        for (int i = 0; i < choices.size(); i++) {
            Choice choice = choices.get(i);
            if (voice.equals(choice.voice()) && tag.equals(choice.tag())) { return i; }
        }
        String name = voice.isEmpty() ? SpeechPreferences.selectionLabel(activity, SpeechPreferences.OUTPUT)
                : activity.getString(R.string.saved_choice_unavailable, voice);
        choices.add(new Choice(name, voice, tag));
        return choices.size() - 1;
    }
}
