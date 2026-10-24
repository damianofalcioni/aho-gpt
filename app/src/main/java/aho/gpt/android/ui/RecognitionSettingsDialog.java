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
    private AlertDialog picker;
    private View content;
    private SpeechInput input;

    RecognitionSettingsDialog(Activity activity) { this.activity = activity; languages = new RecognitionLanguages(activity); }
    void show() {
        close();
        content = activity.getLayoutInflater().inflate(R.layout.dialog_speech_settings, new FrameLayout(activity), false);
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.stt_language).setView(content)
                .setPositiveButton(R.string.close, null).create();
        dialog.setOnDismissListener(ignored -> stop());
        content.findViewById(R.id.speech_voice).setVisibility(View.GONE);
        ((Button) content.findViewById(R.id.android_speech_settings)).setText(R.string.android_stt_settings);
        content.findViewById(R.id.android_speech_settings).setOnClickListener(view -> SpeechSettingsNavigation.open(activity, false));
        content.findViewById(R.id.test_speech).setEnabled(SpeechAvailability.input(activity));
        content.findViewById(R.id.test_speech).setOnClickListener(view -> test());
        label(); dialog.show();
        if (!SpeechAvailability.input(activity)) { status(activity.getString(R.string.speech_unavailable)); return; }
        languages.load(this::loaded);
    }
    private void label() {
        ((Button) content.findViewById(R.id.speech_language)).setText(SpeechPreferences.selectionLabel(activity, SpeechPreferences.INPUT));
    }
    private void loaded(List<String> tags) {
        Button language = content.findViewById(R.id.speech_language);
        language.setEnabled(true);
        language.setOnClickListener(view -> {
            stopTest(); picker = SpeechLanguagePicker.show(activity, SpeechPreferences.INPUT, R.string.stt_language,
                    tags, () -> { label(); languages.load(this::loaded); });
        });
        if (tags == null) { status(activity.getString(R.string.language_support_unknown)); return; }
        status(tags.isEmpty() ? activity.getString(R.string.language_support_empty)
                : activity.getResources().getQuantityString(R.plurals.language_support_reported, tags.size(), tags.size()));
    }
    void test() {
        if (dialog == null || !dialog.isShowing()) { return; }
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
    private void status(String message) { ((TextView) content.findViewById(R.id.speech_support)).setText(message); }
    void stopTest() {
        main.removeCallbacksAndMessages(null);
        if (input != null) { input.close(); input = null; }
    }
    private void stop() {
        stopTest(); languages.close();
        if (picker != null) { picker.dismiss(); picker = null; }
    }
    @Override public void close() { stop(); if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
