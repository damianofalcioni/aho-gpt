package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.speech.PlaybackSettingsClient;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.android.speech.SpeechAvailability;
import java.util.ArrayList;
import java.util.List;

/** Voice choices and an explicit preview; never starts a conversation. */
final class PlaybackSettingsDialog implements AutoCloseable {
    private final Activity activity;
    private final PlaybackSettingsClient client;
    private AlertDialog dialog;
    private AlertDialog picker;
    private View content;

    PlaybackSettingsDialog(Activity activity) { this.activity = activity; client = new PlaybackSettingsClient(activity); }
    void show() {
        close();
        content = activity.getLayoutInflater().inflate(R.layout.dialog_speech_settings, new FrameLayout(activity), false);
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.tts_language).setView(content)
                .setPositiveButton(R.string.close, null).create();
        dialog.setOnDismissListener(ignored -> stop());
        content.findViewById(R.id.speech_language).setVisibility(View.GONE);
        ((Button) content.findViewById(R.id.android_speech_settings)).setText(R.string.android_tts_settings);
        content.findViewById(R.id.android_speech_settings).setOnClickListener(view -> SpeechSettingsNavigation.open(activity, true));
        dialog.show();
        if (!SpeechAvailability.googleAppEnabled(activity)) { status(activity.getString(R.string.speech_provider_disabled)); return; }
        client.load(this::loaded);
    }
    private void loaded(List<Voice> voices) {
        if (voices == null) { status(activity.getString(R.string.tts_unavailable)); return; }
        Button choose = content.findViewById(R.id.speech_voice);
        choose.setEnabled(true); label(voices);
        choose.setOnClickListener(view -> choose(voices));
        List<String> tags = new ArrayList<>();
        for (Voice voice : voices) { tags.add(voice.getLocale().toLanguageTag()); }
        Button language = content.findViewById(R.id.speech_language);
        language.setVisibility(View.VISIBLE); language.setEnabled(true);
        language.setText(SpeechPreferences.selectionLabel(activity, SpeechPreferences.OUTPUT));
        language.setOnClickListener(view -> picker = SpeechLanguagePicker.show(activity, SpeechPreferences.OUTPUT,
                R.string.tts_language, tags, () -> {
                    SpeechPreferences.voice(activity, ""); label(voices);
                    language.setText(SpeechPreferences.selectionLabel(activity, SpeechPreferences.OUTPUT));
                }));
        content.findViewById(R.id.test_speech).setEnabled(true);
        content.findViewById(R.id.test_speech).setOnClickListener(view -> {
            status(activity.getString(R.string.speech_test_playing)); client.test(this::status);
        });
        status(activity.getString(voices.isEmpty() ? R.string.language_support_empty : R.string.tts_voice_help));
    }
    private void label(List<Voice> voices) {
        Button button = content.findViewById(R.id.speech_voice);
        button.setText(R.string.default_voice);
        for (Voice voice : voices) {
            if (voice.getName().equals(SpeechPreferences.voice(activity))) { button.setText(name(voice)); }
        }
    }
    private String name(Voice voice) {
        return activity.getString(R.string.voice_label, SpeechPreferences.label(activity, voice.getLocale()), voice.getName());
    }
    private void choose(List<Voice> voices) {
        List<String> labels = new ArrayList<>(); labels.add(activity.getString(R.string.default_voice));
        int selected = 0;
        for (Voice voice : voices) {
            labels.add(name(voice));
            if (voice.getName().equals(SpeechPreferences.voice(activity))) { selected = labels.size() - 1; }
        }
        picker = new AlertDialog.Builder(activity).setTitle(R.string.choose_voice)
                .setSingleChoiceItems(labels.toArray(new String[0]), selected, (ignored, which) -> {
                    select(voices, which); label(voices); picker.dismiss();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }
    private void select(List<Voice> voices, int which) {
        SpeechPreferences.voice(activity, which == 0 ? "" : voices.get(which - 1).getName());
        if (which > 0) {
            SpeechPreferences.select(activity, SpeechPreferences.OUTPUT, voices.get(which - 1).getLocale().toLanguageTag());
            ((Button) content.findViewById(R.id.speech_language)).setText(SpeechPreferences.selectionLabel(activity, SpeechPreferences.OUTPUT));
        }
    }
    private void status(String message) { ((TextView) content.findViewById(R.id.speech_support)).setText(message); }
    private void stop() { client.close(); if (picker != null) { picker.dismiss(); picker = null; } }
    @Override public void close() { stop(); if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
