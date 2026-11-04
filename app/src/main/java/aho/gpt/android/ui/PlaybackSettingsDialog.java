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
import java.util.List;

/** Installed voice spinner and explicit playback preview. */
final class PlaybackSettingsDialog implements AutoCloseable {
    private final Activity activity;
    private final PlaybackSettingsClient client;
    private AlertDialog dialog;
    private View content;

    PlaybackSettingsDialog(Activity activity) { this.activity = activity; client = new PlaybackSettingsClient(activity); }
    void show() {
        close();
        content = activity.getLayoutInflater().inflate(R.layout.dialog_speech_settings, new FrameLayout(activity), false);
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.tts_language).setView(content)
                .setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnDismissListener(ignored -> client.close());
        ((TextView) content.findViewById(R.id.speech_choice_label)).setText(R.string.speech_voice_label);
        content.findViewById(R.id.speech_choice).setContentDescription(activity.getString(R.string.choose_voice));
        ((Button) content.findViewById(R.id.android_speech_settings)).setText(R.string.android_tts_settings);
        content.findViewById(R.id.android_speech_settings).setOnClickListener(view -> SpeechSettingsNavigation.open(activity, true));
        dialog.show();
        client.load(this::loaded);
    }
    private void loaded(List<Voice> voices) {
        if (voices == null) { status(activity.getString(R.string.tts_unavailable)); return; }
        PlaybackVoiceSpinner.bind(activity, content.findViewById(R.id.speech_choice), voices);
        content.findViewById(R.id.test_speech).setEnabled(!voices.isEmpty());
        content.findViewById(R.id.test_speech).setOnClickListener(view -> {
            status(activity.getString(R.string.speech_test_playing)); client.test(this::status);
        });
        if (voices.isEmpty()) { status(activity.getString(R.string.language_support_empty)); }
        else { content.findViewById(R.id.speech_support).setVisibility(View.GONE); }
    }
    private void status(String message) {
        TextView status = content.findViewById(R.id.speech_support); status.setVisibility(View.VISIBLE); status.setText(message);
    }
    @Override public void close() { client.close(); if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
