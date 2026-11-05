package aho.gpt.android.net;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.realtime.RealtimeProtocol;
import org.json.JSONObject;

/** Adapts both audio APIs to the same complete-response agent loop. */
public final class CustomAudioClient {
    private final CustomRealtimeClient realtime = new CustomRealtimeClient();
    private final CustomLiveClient live = new CustomLiveClient();
    private RealtimeProtocol protocol;
    private boolean useLive;
    private boolean playback;
    public void open(AudioMode mode, ProviderSettings.CustomConfig config, String model, boolean playback, RequestScope scope) throws Exception {
        useLive = mode == AudioMode.LIVE; this.playback = playback;
        if (useLive) { live.open(config.endpoint(), config.token(), model, config.speech(), scope); }
        else { protocol = realtime.open(config.endpoint(), config.token(), model, playback, config.speech().realtimeVoice(), scope); }
    }
    public JSONObject respond(JSONObject request, byte[] wav, RequestScope scope) throws Exception {
        return useLive ? live.respond(request, wav, scope) : protocol.respond(request, scope);
    }
    public byte[] finish(RequestScope scope) throws Exception { return useLive ? live.finish(playback, scope) : protocol.audio(); }
    public String transcript() { return useLive ? live.transcript() : ""; }
    public void cancel(RequestScope scope) { realtime.cancel(scope); live.cancel(scope); }
}
