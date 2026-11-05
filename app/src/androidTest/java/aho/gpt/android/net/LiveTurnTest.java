package aho.gpt.android.net;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import aho.gpt.core.RequestScope;
import aho.gpt.core.Conversation;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.live.LiveInputPump;
import aho.gpt.custom.live.LiveProtocol;
import aho.gpt.custom.live.LiveStage;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.realtime.RealtimeSocket;
import aho.gpt.openai.AgentLoop;
import aho.gpt.logging.LiveLogEvent;
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
    private final long started = System.nanoTime();
    private LiveTurnTest(Instrumentation instrumentation) { this.instrumentation = instrumentation; }
    public static boolean runRequested(Instrumentation instrumentation, Bundle arguments) {
        if (arguments == null || !"true".equals(arguments.getString("live_turn"))) { return false; }
        Bundle result = new Bundle();
        try { new LiveTurnTest(instrumentation).run("true".equals(arguments.getString("live_tools"))); result.putString("stream", "Live turn passed\n"); }
        catch (Exception failure) { result.putString("stream", "Live turn failed: " + failure.getClass().getSimpleName() + "\n"); }
        instrumentation.finish(Activity.RESULT_OK, result); return true;
    }
    private void run(boolean tools) throws Exception {
        ProviderSettings settings = new ProviderSettings(instrumentation.getTargetContext());
        ProviderSettings.CustomConfig config = settings.customConfig(); RequestScope scope = new RequestScope();
        String spoken = tools ? "Use the device discovery tool to list the available action categories." : "What is two plus two? Answer briefly.";
        byte[] pcm = new CustomSpeechApi().speech(config.endpoint(), config.token(), spoken, SpeechApiConfig.DEFAULT, scope);
        var watchdog = Executors.newSingleThreadScheduledExecutor();
        try (RealtimeSocket socket = new RealtimeSocket(); LiveInputPump pump = new LiveInputPump()) {
            watchdog.schedule(() -> { scope.cancel(); socket.close(); }, tools ? 90 : 35, TimeUnit.SECONDS);
            socket.connectLive(config.endpoint(), config.token(), scope); socket.readTimeout(15_000);
            RealtimeProtocol.Events events = new RealtimeProtocol.Events() {
                @Override public void send(JSONObject event, RequestScope request) throws Exception { socket.send(event, request); }
                @Override public JSONObject receive(RequestScope request) throws Exception {
                    JSONObject event = socket.receive(request); trace(event); return event;
                }
            };
            LiveProtocol protocol = new LiveProtocol(events);
            LiveToolFixture fixture = new LiveToolFixture();
            JSONObject request = tools ? fixture.request() : new JSONObject().put("instructions", "Answer the question briefly. Do not perform actions.")
                    .put("input", new JSONArray()).put("tools", new JSONArray());
            protocol.initialize(settings.customModel(), config.speech(), request, scope); socket.readTimeout(60_000);
            pump.start(events, pcm, scope, socket::close); report("input_sent");
            if (tools) { toolAnswer(protocol, fixture, scope, settings.customModel()); }
            else { directAnswer(protocol, request, scope); }
            report("answer_received");
            if (protocol.finish(true, scope).length == 0) { throw new IllegalStateException("Missing audio"); }
            report("audio_received"); pump.close(); protocol.close(scope); report("closed");
        } finally { scope.cancel(); watchdog.shutdownNow(); }
    }
    private void toolAnswer(LiveProtocol protocol, LiveToolFixture fixture, RequestScope scope, String model) throws Exception {
        String text = new AgentLoop(body -> protocol.respond(body, scope), fixture).run(new Conversation(), model, scope);
        fixture.verify();
        if (text.isEmpty()) { throw new IllegalStateException("Missing tool answer"); }
        report("tool_exchange_completed");
    }
    private void directAnswer(LiveProtocol protocol, JSONObject request, RequestScope scope) throws Exception {
        if (protocol.respond(request, scope).getJSONArray("output").length() == 0) { throw new IllegalStateException("Missing answer"); }
    }
    private void trace(JSONObject event) {
        LiveStage stage = LiveStage.of(event);
        if (seen.add(stage)) { report(stage.name()); }
        String failure = LiveLogEvent.failure(event);
        if (!failure.isEmpty()) { report(failure); }
    }
    private void report(String stage) {
        Bundle status = new Bundle(); status.putString("live_stage", stage);
        status.putLong("elapsed_ms", (System.nanoTime() - started) / 1_000_000); instrumentation.sendStatus(0, status);
    }
}
