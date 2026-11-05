package aho.gpt.android.net;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.ServerModels;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.live.LiveProtocol;
import aho.gpt.custom.live.LiveInputPump;
import aho.gpt.custom.realtime.RealtimeAudio;
import aho.gpt.custom.realtime.RealtimeSocket;
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
        events = new CustomLiveEvents(socket); protocol = new LiveProtocol(events); events.attach(protocol);
        try {
            model = selected.isEmpty() ? ServerModels.first(models.json(base + "/models", token, null, scope)) : selected;
            this.config = config; started = false;
            socket.connectLive(base, token, scope); socket.readTimeout(15_000);
        } catch (Exception failure) { events.failed(failure, scope); throw failure; }
    }
    public void check(RequestScope scope) throws Exception {
        JSONObject request = new JSONObject().put("instructions", "Check session configuration.")
                .put("input", new JSONArray()).put("tools", new JSONArray());
        try {
            protocol.initialize(model, config, request, scope); events.checkpoint(); protocol.close(scope); events.completed();
        } catch (Exception failure) { events.failed(failure, scope); throw failure; }
    }
    public JSONObject respond(JSONObject request, byte[] wav, RequestScope scope) throws Exception {
        try {
            if (!started) {
                protocol.initialize(model, config, request, scope); events.checkpoint();
                active.readTimeout(60_000);
                RealtimeSocket socket = active; byte[] pcm = RealtimeAudio.pcm(wav); events.capture(pcm);
                pump.start(events, pcm, scope, socket::close); started = true;
            }
            JSONObject result = protocol.respond(request, scope); events.checkpoint(); return result;
        } catch (Exception failure) { events.failed(failure, scope); throw failure; }
    }
    public byte[] finish(boolean playback, RequestScope scope) throws Exception {
        try {
            byte[] pcm = protocol.finish(playback, scope); events.checkpoint();
            pump.close(); active.readTimeout(15_000); protocol.close(scope); events.completed(); return pcm;
        } catch (Exception failure) { events.failed(failure, scope); throw failure; }
    }
    public String transcript() { return protocol.transcript(); }
    private synchronized void activate(RealtimeSocket socket, RequestScope scope) throws InterruptedException {
        scope.check(); active = socket; owner = scope;
    }
    public synchronized void cancel(RequestScope scope) {
        models.cancel(scope);
        if (owner == scope && active != null) {
            if (events != null) { events.cancelled(); }
            pump.close(); active.close(); active = null; owner = null;
        }
    }
}
