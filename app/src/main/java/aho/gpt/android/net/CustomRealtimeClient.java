package aho.gpt.android.net;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.ServerModels;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.realtime.RealtimeSocket;

/** Separates Realtime connections and discovery from speech HTTP and OAuth traffic. */
public final class CustomRealtimeClient {
    private final CustomHttp models = new CustomHttp();
    private RealtimeSocket active;
    private RequestScope owner;
    public RealtimeProtocol open(String base, String token, String selected, boolean playback, String voice, RequestScope scope) throws Exception {
        RealtimeSocket socket = new RealtimeSocket(); activate(socket, scope);
        String model = selected.isEmpty() ? ServerModels.first(models.json(base + "/models", token, null, scope)) : selected;
        scope.check(); socket.connect(base, model, token, scope);
        RealtimeProtocol protocol = new RealtimeProtocol(socket); protocol.initialize(playback, voice, scope);
        return protocol;
    }
    private synchronized void activate(RealtimeSocket socket, RequestScope scope) throws InterruptedException {
        scope.check(); active = socket; owner = scope;
    }
    public synchronized void cancel(RequestScope scope) {
        models.cancel(scope);
        if (owner == scope && active != null) { active.close(); active = null; owner = null; }
    }
}
