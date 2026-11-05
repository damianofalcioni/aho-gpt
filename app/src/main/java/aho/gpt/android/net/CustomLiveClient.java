package aho.gpt.android.net;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.ServerModels;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.live.LiveProtocol;
import aho.gpt.custom.live.LiveInputPump;
import aho.gpt.custom.realtime.RealtimeAudio;
import aho.gpt.custom.realtime.RealtimeSocket;
import aho.gpt.android.logging.AppLogger;
import org.json.JSONArray;
import org.json.JSONObject;

/** Owns Live transport and silence independently of capture, OAuth and speech HTTP. */
public final class CustomLiveClient {
    private final CustomHttp models = new CustomHttp();
    private final LiveInputPump pump = new LiveInputPump();
    private RealtimeSocket active;
    private RequestScope owner;
    private LiveProtocol protocol;
    private CustomLiveEvents events;
    private String model;
    private SpeechApiConfig config;
    private boolean started;
    public void open(String base, String token, String selected, SpeechApiConfig config, RequestScope scope) throws Exception {
        RealtimeSocket socket = new RealtimeSocket(); activate(socket, scope);
        model = selected.isEmpty() ? ServerModels.first(models.json(base + "/models", token, null, scope)) : selected;
        this.config = config; started = false;
        socket.connectLive(base, token, scope); socket.readTimeout(15_000);
        events = new CustomLiveEvents(socket); protocol = new LiveProtocol(events);
    }
    public void check(RequestScope scope) throws Exception {
        JSONObject request = new JSONObject().put("instructions", "Check session configuration.")
                .put("input", new JSONArray()).put("tools", new JSONArray());
        protocol.initialize(model, config, request, scope); protocol.close(scope);
    }
    public JSONObject respond(JSONObject request, byte[] wav, RequestScope scope) throws Exception {
        if (!started) {
            protocol.initialize(model, config, request, scope);
            active.readTimeout(60_000);
            RealtimeSocket socket = active;
            pump.start(events, RealtimeAudio.pcm(wav), scope, socket::close); started = true;
        }
        return protocol.respond(request, scope);
    }
    public byte[] finish(boolean playback, RequestScope scope) throws Exception {
        AppLogger.i("Live", "stage=ANSWER_READY");
        byte[] pcm = protocol.finish(playback, scope); pump.close(); active.readTimeout(15_000); protocol.close(scope); return pcm;
    }
    public String transcript() { return protocol.transcript(); }
    private synchronized void activate(RealtimeSocket socket, RequestScope scope) throws InterruptedException {
        scope.check(); active = socket; owner = scope;
    }
    public synchronized void cancel(RequestScope scope) {
        models.cancel(scope);
        if (owner == scope && active != null) { pump.close(); active.close(); active = null; owner = null; }
    }
}
