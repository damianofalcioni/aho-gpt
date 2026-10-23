package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.speech.PlaybackLanguages;
import aho.gpt.android.speech.RecognitionLanguages;
import aho.gpt.android.speech.SpeechPreferences;
import java.util.List;

/** Owns the language dialog and shuts down provider queries with its activity. */
final class SpeechLanguageSettings implements AutoCloseable {
    private final Activity activity;
    private final RecognitionLanguages recognition;
    private final PlaybackLanguages playback = new PlaybackLanguages();
    private AlertDialog dialog;
    private AlertDialog picker;
    private View content;

    SpeechLanguageSettings(Activity activity) {
        this.activity = activity; recognition = new RecognitionLanguages(activity);
    }
    void show() {
        close();
        content = activity.getLayoutInflater().inflate(R.layout.dialog_speech_languages, new FrameLayout(activity), false);
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.speech_languages).setView(content)
                .setPositiveButton(R.string.close, null).create();
        dialog.setOnDismissListener(ignored -> stopQueries());
        labels();
        dialog.show();
        recognition.load(tags -> loaded(SpeechPreferences.INPUT, R.id.stt_language, R.id.stt_support, R.string.stt_language, tags));
        playback.load(activity, tags -> loaded(SpeechPreferences.OUTPUT, R.id.tts_language, R.id.tts_support, R.string.tts_language, tags));
    }
    private void labels() {
        Button input = content.findViewById(R.id.stt_language);
        input.setText(SpeechPreferences.selectionLabel(activity, SpeechPreferences.INPUT));
        input.setContentDescription(activity.getString(R.string.speech_language_selection,
                activity.getString(R.string.stt_language), input.getText()));
        Button output = content.findViewById(R.id.tts_language);
        output.setText(SpeechPreferences.selectionLabel(activity, SpeechPreferences.OUTPUT));
        output.setContentDescription(activity.getString(R.string.speech_language_selection,
                activity.getString(R.string.tts_language), output.getText()));
    }
    private void loaded(String key, int buttonId, int statusId, int title, List<String> tags) {
        Button button = content.findViewById(buttonId);
        button.setEnabled(true);
        button.setOnClickListener(view -> picker = SpeechLanguagePicker.show(activity, key, title, tags, this::labels));
        TextView status = content.findViewById(statusId);
        if (tags == null) { status.setText(R.string.language_support_unknown); return; }
        status.setText(tags.isEmpty() ? activity.getString(R.string.language_support_empty)
                : activity.getResources().getQuantityString(R.plurals.language_support_reported, tags.size(), tags.size()));
    }
    private void stopQueries() { recognition.close(); playback.close(); }
    @Override public void close() {
        stopQueries();
        if (picker != null) { picker.dismiss(); picker = null; }
        if (dialog != null) { dialog.dismiss(); dialog = null; }
    }
}
