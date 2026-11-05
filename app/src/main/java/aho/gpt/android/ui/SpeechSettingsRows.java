package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.android.speech.PlaybackSettingsClient;
import aho.gpt.android.speech.SpeechAvailability;
import aho.gpt.android.speech.SpeechPreferences;

/** Keeps saved user choices separate from current provider availability. */
final class SpeechSettingsRows implements AutoCloseable {
    private final Activity activity;
    private final PlaybackSettingsClient probe;
    private final PlaybackSettingsDialog playback;
    private final RecognitionSettingsDialog recognition;
    private final SpeechInputToggle inputToggle;
    private AlertDialog information;

    SpeechSettingsRows(Activity activity) {
        this.activity = activity;
        inputToggle = new SpeechInputToggle(activity);
        probe = new PlaybackSettingsClient(activity);
        playback = new PlaybackSettingsDialog(activity); recognition = new RecognitionSettingsDialog(activity);
        activity.findViewById(R.id.tts_settings).setOnClickListener(view -> playback.show());
        activity.findViewById(R.id.stt_settings).setOnClickListener(view -> recognition.show());
        activity.findViewById(R.id.tts_info).setOnClickListener(view -> info(R.string.tts_language, R.string.tts_info, R.id.tts_provider_status));
        activity.findViewById(R.id.stt_info).setOnClickListener(view -> info(R.string.stt_language, R.string.stt_info, R.id.stt_provider_status));
        inputToggle.refresh(false);
        toggle(R.id.tts_enabled, SpeechPreferences.OUTPUT, false);
    }
    void refresh() {
        boolean input = SpeechAvailability.voiceInput(activity);
        inputToggle.refresh(input);
        toggle(R.id.tts_enabled, SpeechPreferences.OUTPUT, false);
        providerStatus(R.id.stt_provider_status, input ? R.string.stt_ready : R.string.speech_unavailable, input);
        if (new ProviderSettings(activity).serverAudio()) {
            probe.close(); toggle(R.id.tts_enabled, SpeechPreferences.OUTPUT, true);
            providerStatus(R.id.tts_provider_status, R.string.tts_ready, true); return;
        }
        providerStatus(R.id.tts_provider_status, R.string.language_support_loading, false);
        probe.load(voices -> {
            boolean available = voices != null && !voices.isEmpty();
            toggle(R.id.tts_enabled, SpeechPreferences.OUTPUT, available);
            providerStatus(R.id.tts_provider_status, available ? R.string.tts_ready : R.string.tts_provider_unavailable, available);
            probe.close();
        });
    }
    private void providerStatus(int id, int message, boolean available) {
        TextView label = activity.findViewById(id);
        label.setText(message);
        label.setVisibility(available || id == R.id.stt_provider_status ? View.GONE : View.VISIBLE);
    }
    private void toggle(int id, String key, boolean available) {
        Switch control = activity.findViewById(id);
        control.setOnCheckedChangeListener(null);
        control.setEnabled(available); control.setChecked(available && SpeechPreferences.enabled(activity, key));
        control.setOnCheckedChangeListener((button, checked) -> SpeechPreferences.enable(activity, key, checked));
    }
    private void info(int title, int message, int statusId) {
        TextView status = activity.findViewById(statusId);
        ProviderSettings provider = new ProviderSettings(activity);
        int details = provider.serverAudio() ? R.string.custom_speech_help : message;
        if (provider.customRealtime()) { details = R.string.custom_realtime_help; }
        if (provider.customLive()) { details = R.string.custom_live_help; }
        information = new AlertDialog.Builder(activity).setTitle(title)
                .setMessage(activity.getString(R.string.setting_info_with_status, status.getText(), activity.getString(details)))
                .setPositiveButton(R.string.close, null).show();
    }
    void permissionResult(boolean granted) { if (granted) { recognition.test(); } }
    void stopTests() { probe.close(); recognition.stopTest(); playback.close(); inputToggle.close(); }
    @Override public void close() {
        probe.close(); playback.close(); recognition.close(); inputToggle.close();
        if (information != null) { information.dismiss(); information = null; }
    }
}
