package aho.gpt.android.net;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.SpeechApiConfig;

/** A Realtime check configures a session without recording or requesting an answer. */
public final class CustomAudioProbe {
    private final CustomSpeechApi speech = new CustomSpeechApi();
    private final CustomRealtimeClient realtime = new CustomRealtimeClient();
    public void check(AudioMode mode, String base, String token, String model, SpeechApiConfig config, RequestScope scope) throws Exception {
        if (mode == AudioMode.SPEECH) { speech.check(base, token, config, scope); return; }
        try { realtime.open(base, token, model, true, config.realtimeVoice(), scope); }
        finally { realtime.cancel(scope); }
    }
    public void cancel(RequestScope scope) { speech.cancel(scope); realtime.cancel(scope); }
}
