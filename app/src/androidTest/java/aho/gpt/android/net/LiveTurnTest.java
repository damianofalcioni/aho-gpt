package aho.gpt.android.net;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.live.LiveInputPump;
import aho.gpt.custom.live.LiveProtocol;
import aho.gpt.custom.live.LiveStage;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.realtime.RealtimeSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/** Fixed harmless speech only; reports event categories and counts, never conversation data. */
public final class LiveTurnTest {
    private final Instrumentation instrumentation;
    private final Set<LiveStage> seen = new HashSet<>();
    private LiveTurnTest(Instrumentation instrumentation) { this.instrumentation = instrumentation; }
    public static boolean runRequested(Instrumentation instrumentation, Bundle arguments) {
        if (arguments == null || !"true".equals(arguments.getString("live_turn"))) { return false; }
        Bundle result = new Bundle();
        try { new LiveTurnTest(instrumentation).run(); result.putString("stream", "Live turn passed\n"); }
        catch (Exception failure) { result.putString("stream", "Live turn failed: " + failure.getClass().getSimpleName() + "\n"); }
        instrumentation.finish(Activity.RESULT_OK, result); return true;
    }
    private void run() throws Exception {
        ProviderSettings settings = new ProviderSettings(instrumentation.getTargetContext());
        ProviderSettings.CustomConfig config = settings.customConfig(); RequestScope scope = new RequestScope();
        byte[] pcm = new CustomSpeechApi().speech(config.endpoint(), config.token(), "What is two plus two? Answer briefly.", SpeechApiConfig.DEFAULT, scope);
        var watchdog = Executors.newSingleThreadScheduledExecutor();
        try (RealtimeSocket socket = new RealtimeSocket(); LiveInputPump pump = new LiveInputPump()) {
            watchdog.schedule(() -> { scope.cancel(); socket.close(); }, 35, TimeUnit.SECONDS);
            socket.connectLive(config.endpoint(), config.token(), scope); socket.readTimeout(15_000);
            RealtimeProtocol.Events events = new RealtimeProtocol.Events() {
                @Override public void send(JSONObject event, RequestScope request) throws Exception { socket.send(event, request); }
                @Override public JSONObject receive(RequestScope request) throws Exception {
                    JSONObject event = socket.receive(request); trace(event); return event;
                }
            };
            LiveProtocol protocol = new LiveProtocol(events);
            JSONObject request = new JSONObject().put("instructions", "Answer the question briefly. Do not perform actions.")
                    .put("input", new JSONArray()).put("tools", new JSONArray());
            protocol.initialize(settings.customModel(), config.speech(), request, scope); socket.readTimeout(60_000);
            pump.start(events, pcm, scope, socket::close); report("input_sent");
            JSONObject response = protocol.respond(request, scope);
            if (response.getJSONArray("output").length() == 0) { throw new IllegalStateException("Missing answer"); }
            report("answer_received");
            if (protocol.finish(true, scope).length == 0) { throw new IllegalStateException("Missing audio"); }
            report("audio_received"); pump.close(); protocol.close(scope); report("closed");
        } finally { scope.cancel(); watchdog.shutdownNow(); }
    }
    private void trace(JSONObject event) {
        LiveStage stage = LiveStage.of(event);
        if (seen.add(stage)) { report(stage.name()); }
    }
    private void report(String stage) {
        Bundle status = new Bundle(); status.putString("live_stage", stage); instrumentation.sendStatus(0, status);
    }
}
