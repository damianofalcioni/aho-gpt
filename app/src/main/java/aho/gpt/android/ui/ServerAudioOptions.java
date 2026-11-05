package aho.gpt.android.ui;

import aho.gpt.R;
import aho.gpt.custom.AudioMode;

/** Native resources for each independent speech configuration dialog. */
record ServerAudioOptions(int button, int layout, int title) {
    static ServerAudioOptions of(AudioMode mode) {
        return switch (mode) {
            case SPEECH -> new ServerAudioOptions(R.id.custom_speech_settings, R.layout.dialog_server_speech, R.string.server_speech_settings);
            case LIVE -> new ServerAudioOptions(R.id.custom_live_settings, R.layout.dialog_server_live, R.string.server_live_settings);
            default -> new ServerAudioOptions(R.id.custom_realtime_settings, R.layout.dialog_server_realtime, R.string.server_realtime_settings);
        };
    }
}
