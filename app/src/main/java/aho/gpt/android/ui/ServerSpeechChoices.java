package aho.gpt.android.ui;

import android.app.Activity;
import android.view.View;
import aho.gpt.R;
import aho.gpt.android.net.CustomSpeechCatalog;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.SpeechCatalog;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Owns the selected mode's spinners without changing another mode's draft. */
final class ServerSpeechChoices implements AutoCloseable {
    private final Activity activity;
    private final View content;
    private final CustomSpeechToggle speech;
    private final AudioMode mode;
    private final Map<Integer, ServerValueSpinner> pickers = new HashMap<>();
    private CustomSpeechCatalog.Choices available;
    ServerSpeechChoices(Activity activity, View content, CustomSpeechToggle speech, AudioMode mode) {
        this.activity = activity; this.content = content; this.speech = speech; this.mode = mode;
        SpeechApiConfig config = speech.configuration();
        switch (mode) {
            case LIVE:
                add(R.id.server_live_model, R.string.server_live_model, config.liveModel());
                add(R.id.server_live_voice, R.string.server_live_voice, config.liveVoice()); break;
            case REALTIME: add(R.id.server_realtime_voice, R.string.server_realtime_voice, config.realtimeVoice()); break;
            default:
                add(R.id.server_transcription_model, R.string.server_transcription_model, config.transcriptionModel());
                add(R.id.server_speech_model, R.string.server_speech_model, config.speechModel());
                add(R.id.server_speech_voice, R.string.server_speech_voice, config.voice()); break;
        }
    }
    private void add(int id, int title, String value) {
        pickers.put(id, new ServerValueSpinner(activity, content.findViewById(id), title, value, this::voices));
    }
    void loaded(CustomSpeechCatalog.Choices choices) {
        available = choices;
        if (mode == AudioMode.SPEECH) {
            pickers.get(R.id.server_transcription_model).choices(choices.models());
            pickers.get(R.id.server_speech_model).choices(choices.models());
        }
        if (mode == AudioMode.LIVE) { pickers.get(R.id.server_live_model).choices(choices.models()); }
        voices();
    }
    private void voices() {
        if (available == null) { return; }
        List<String> voices = available.voices();
        if (available.openAi()) {
            voices = switch (mode) {
                case LIVE -> SpeechCatalog.liveVoices();
                case REALTIME -> SpeechCatalog.realtimeVoices();
                default -> SpeechCatalog.speechVoices(value(R.id.server_speech_model));
            };
        }
        int id = switch (mode) { case LIVE -> R.id.server_live_voice; case REALTIME -> R.id.server_realtime_voice; default -> R.id.server_speech_voice; };
        pickers.get(id).choices(voices);
    }
    void apply() {
        SpeechApiConfig current = speech.configuration();
        speech.configure(switch (mode) {
            case SPEECH -> new SpeechApiConfig(value(R.id.server_transcription_model), value(R.id.server_speech_model), value(R.id.server_speech_voice),
                    current.realtimeVoice(), current.liveModel(), current.liveVoice());
            case LIVE -> new SpeechApiConfig(current.transcriptionModel(), current.speechModel(), current.voice(), current.realtimeVoice(),
                    value(R.id.server_live_model), value(R.id.server_live_voice));
            default -> new SpeechApiConfig(current.transcriptionModel(), current.speechModel(), current.voice(), value(R.id.server_realtime_voice),
                    current.liveModel(), current.liveVoice());
        });
    }
    private String value(int id) { return pickers.get(id).value(); }
    @Override public void close() { for (ServerValueSpinner picker : pickers.values()) { picker.close(); } }
}
