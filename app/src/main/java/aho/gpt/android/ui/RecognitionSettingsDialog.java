package aho.gpt.android.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.speech.RecognitionLanguages;
import aho.gpt.android.speech.SpeechInput;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.android.speech.SpeechAvailability;
import java.util.List;

/** Recognition preview stays local: it never sends recognized text to the model or tools. */
final class RecognitionSettingsDialog implements AutoCloseable, SpeechInput.Listener {
    static final int PERMISSION_REQUEST = 7;
    private final Activity activity;
    private final RecognitionLanguages languages;
    private final Handler main = new Handler(Looper.getMainLooper());
    private AlertDialog dialog;
    private View content;
    private SpeechInput input;

    RecognitionSettingsDialog(Activity activity) { this.activity = activity; languages = new RecognitionLanguages(activity); }
    void show() {
        close();
        content = activity.getLayoutInflater().inflate(R.layout.dialog_speech_settings, new FrameLayout(activity), false);
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.stt_language).setView(content)
                .setNeutralButton(R.string.test_speech, null)
                .setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnDismissListener(ignored -> stop());
        content.findViewById(R.id.test_speech).setVisibility(View.GONE);
        ((Button) content.findViewById(R.id.android_speech_settings)).setText(R.string.android_stt_settings);
        content.findViewById(R.id.android_speech_settings).setOnClickListener(view -> SpeechSettingsNavigation.open(activity, false));
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> test());
        if (!SpeechAvailability.input(activity)) { status(activity.getString(R.string.speech_unavailable)); return; }
        languages.load(this::loaded);
    }
    private void loaded(List<String> tags) {
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(SpeechAvailability.input(activity));
        RecognitionLanguageSpinner.bind(activity, content.findViewById(R.id.speech_choice), tags,
                () -> { stopTest(); languages.load(this::loaded); });
        if (tags == null) { status(activity.getString(R.string.language_support_unknown)); return; }
        if (tags.isEmpty()) { status(activity.getString(R.string.language_support_empty)); }
        else { content.findViewById(R.id.speech_support).setVisibility(View.GONE); }
    }
    void test() {
        if (dialog == null || !dialog.isShowing()) { return; }
        languages.close();
        if (!SpeechAvailability.input(activity)) { status(activity.getString(R.string.speech_unavailable)); return; }
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST); return;
        }
        stopTest();
        try {
            input = new SpeechInput(activity, this);
            status(activity.getString(R.string.listening_language,
                    SpeechPreferences.selectionLabel(activity, SpeechPreferences.INPUT)));
            input.start(SpeechPreferences.locale(activity, SpeechPreferences.INPUT));
            main.postDelayed(() -> { stopTest(); status(activity.getString(R.string.speech_test_timeout)); }, 15000);
        } catch (RuntimeException exception) { stopTest(); status(activity.getString(R.string.speech_unavailable)); }
    }
    @Override public void began() { }
    @Override public void result(String text) { stopTest(); status(activity.getString(R.string.speech_test_result, text)); }
    @Override public void error(int error) { stopTest(); status(SpeechInput.errorText(activity, error)); }
    private void status(String message) {
        TextView status = content.findViewById(R.id.speech_support); status.setVisibility(View.VISIBLE); status.setText(message);
    }
    void stopTest() {
        main.removeCallbacksAndMessages(null);
        if (input != null) { input.close(); input = null; }
    }
    private void stop() {
        stopTest(); languages.close();
        if (content != null) { ((Spinner) content.findViewById(R.id.speech_choice)).setOnItemSelectedListener(null); }
    }
    @Override public void close() { stop(); if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
