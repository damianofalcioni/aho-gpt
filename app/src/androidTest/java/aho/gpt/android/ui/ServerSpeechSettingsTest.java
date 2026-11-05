package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import aho.gpt.R;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.SpeechApiConfig;

/** Tests separate native dialogs, discovered choices, drafts and manual entry without live providers. */
public final class ServerSpeechSettingsTest {
    private static final String REALTIME_SAVED = "realtime-saved";
    private static final String TTS_SERVER = "tts-server";
    private static final String VOICE_SERVER = "voice-server";
    private final Instrumentation instrumentation;
    private Activity activity;
    private CustomSpeechToggle speech;
    private ServerSpeechSettingsDialog playback;
    private ServerSpeechSettingsDialog realtime;
    private ServerSpeechSettingsDialog live;
    private AlertDialog window;
    public ServerSpeechSettingsTest(Instrumentation instrumentation) { this.instrumentation = instrumentation; }
    public void run() throws Exception {
        var context = instrumentation.getTargetContext();
        SpeechApiConfig saved = new ProviderSettings(context).speechConfig();
        activity = instrumentation.startActivitySync(new Intent(context, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try (SpeechCatalogFixture fixture = new SpeechCatalogFixture()) {
            main(() -> prepare(fixture.base()));
            speechChoices(); realtimeChoices(); liveChoices(); manualValue();
            require(saved.equals(new ProviderSettings(context).speechConfig()), "Draft dialogs must not change saved provider settings");
        } finally {
            main(() -> { close(); activity.finish(); }); instrumentation.waitForIdleSync();
        }
    }
    private void prepare(String base) {
        View parent = activity.getLayoutInflater().inflate(R.layout.dialog_custom, new FrameLayout(activity), false);
        ((EditText) parent.findViewById(R.id.custom_endpoint)).setText(base);
        speech = new CustomSpeechToggle(activity, parent); speech.start();
        speech.configure(new SpeechApiConfig("stt-saved", "tts-saved", "voice-saved", REALTIME_SAVED));
        playback = new ServerSpeechSettingsDialog(activity, parent, speech, AudioMode.SPEECH);
        realtime = new ServerSpeechSettingsDialog(activity, parent, speech, AudioMode.REALTIME);
        live = new ServerSpeechSettingsDialog(activity, parent, speech, AudioMode.LIVE);
        playback.ready(); realtime.ready(); live.ready();
    }
    private void speechChoices() throws Exception {
        main(() -> {
            window = playback.show();
            require(window.findViewById(R.id.server_realtime_voice) == null, "Speech settings must not contain Realtime voice");
        });
        choose(R.id.server_transcription_model, "stt-server"); choose(R.id.server_speech_model, TTS_SERVER);
        choose(R.id.server_speech_voice, VOICE_SERVER);
        click(AlertDialog.BUTTON_POSITIVE);
        main(() -> {
            require(TTS_SERVER.equals(speech.configuration().speechModel()), "OK must update the parent speech draft");
            require(REALTIME_SAVED.equals(speech.configuration().realtimeVoice()), "Speech edits must preserve Realtime voice");
        });
    }
    private void realtimeChoices() throws Exception {
        main(() -> {
            window = realtime.show();
            require(window.findViewById(R.id.server_speech_model) == null, "Realtime settings must not contain speech model choices");
        });
        choose(R.id.server_realtime_voice, "realtime-server");
        click(AlertDialog.BUTTON_NEGATIVE);
        main(() -> {
            require(REALTIME_SAVED.equals(speech.configuration().realtimeVoice()), "Cancel must discard Realtime edits");
            window = realtime.show();
        });
        choose(R.id.server_realtime_voice, "realtime-server");
        click(AlertDialog.BUTTON_POSITIVE);
        main(() -> {
            require(TTS_SERVER.equals(speech.configuration().speechModel()), "Realtime edits must preserve speech models");
            require("realtime-server".equals(speech.configuration().realtimeVoice()), "Realtime OK must update only its voice");
        });
    }
    private void manualValue() {
        main(() -> {
            try (ServerValueSpinner picker = new ServerValueSpinner(activity, new Spinner(activity),
                    R.string.server_speech_voice, "saved-voice", () -> { })) {
                AlertDialog entry = picker.custom(); EditText input = entry.findViewById(android.R.id.edit);
                input.setText(" "); entry.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                require(entry.isShowing(), "Blank identifiers must keep the editor open");
                input.setText("custom-voice"); entry.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                require("custom-voice".equals(picker.value()), "Manual entry must accept an unlisted voice");
            }
        });
    }
    private void liveChoices() throws Exception {
        SpeechApiConfig[] before = {null};
        main(() -> { before[0] = speech.configuration(); window = live.show(); });
        choose(R.id.server_live_model, TTS_SERVER); choose(R.id.server_live_voice, VOICE_SERVER);
        click(AlertDialog.BUTTON_NEGATIVE);
        main(() -> { require(before[0].equals(speech.configuration()), "Cancel must discard Live changes"); window = live.show(); });
        choose(R.id.server_live_model, TTS_SERVER); choose(R.id.server_live_voice, VOICE_SERVER);
        click(AlertDialog.BUTTON_POSITIVE);
        main(() -> {
            require(TTS_SERVER.equals(speech.configuration().liveModel()), "Live model must be saved in the parent draft");
            require(VOICE_SERVER.equals(speech.configuration().liveVoice()), "Live voice must be saved in the parent draft");
            require(before[0].sameSpeech(speech.configuration()), "Live edits must preserve speech settings");
            require(before[0].realtimeVoice().equals(speech.configuration().realtimeVoice()), "Live edits must preserve Realtime voice");
        });
    }
    private void choose(int id, String value) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + 5000; boolean[] found = {false};
        while (!found[0] && SystemClock.elapsedRealtime() < deadline) {
            main(() -> found[0] = select(window.findViewById(id), value));
            instrumentation.waitForIdleSync();
            if (!found[0]) { Thread.sleep(20); }
        }
        require(found[0], "The provider choice must be discovered");
    }
    private boolean select(Spinner spinner, String value) {
        for (int index = 0; index < spinner.getCount(); index++) {
            if (value.equals(spinner.getItemAtPosition(index))) { spinner.setSelection(index); return true; }
        }
        return false;
    }
    private void close() {
        if (playback != null) { playback.close(); realtime.close(); live.close(); speech.close(); }
    }
    private void click(int button) {
        main(() -> window.getButton(button).performClick()); instrumentation.waitForIdleSync();
    }
    private void main(Runnable action) {
        Throwable[] failed = {null};
        instrumentation.runOnMainSync(() -> {
            try { action.run(); }
            catch (RuntimeException | AssertionError failure) { failed[0] = failure; }
        });
        if (failed[0] != null) { throw new AssertionError(failed[0]); }
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
