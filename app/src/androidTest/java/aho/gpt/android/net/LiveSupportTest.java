package aho.gpt.android.net;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.live.LiveProtocol;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.realtime.RealtimeSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.io.IOException;

/** Opt-in configuration check with saved credentials; never records audio or changes settings. */
public final class LiveSupportTest {
    private static final String STAGE = "stage";
    private final Instrumentation instrumentation;
    private final String model;
    private final boolean save;
    public LiveSupportTest(Instrumentation instrumentation, String model, boolean save) {
        this.instrumentation = instrumentation; this.model = model; this.save = save;
    }
    public static boolean runRequested(Instrumentation instrumentation, Bundle arguments) {
        if (LiveTurnTest.runRequested(instrumentation, arguments)) { return true; }
        String model = arguments == null ? "" : arguments.getString("live_probe", "");
        if (model.isEmpty()) { return false; }
        Bundle results = new Bundle();
        try {
            new LiveSupportTest(instrumentation, model, "true".equals(arguments.getString("live_probe_save"))).run();
            results.putString("stream", "Tests run: 1, Failures: 0\n");
        } catch (AssertionError failure) { results.putString("stream", "Tests run: 1, Failures: 1\n"); }
        instrumentation.finish(Activity.RESULT_OK, results); return true;
    }
    public void run() {
        ProviderSettings settings = new ProviderSettings(instrumentation.getTargetContext());
        RequestScope scope = new RequestScope();
        try (RealtimeSocket socket = new RealtimeSocket()) {
            ProviderSettings.CustomConfig config = settings.customConfig();
            report(STAGE, "connecting"); socket.connectLive(config.endpoint(), config.token(), scope); socket.readTimeout(15_000);
            report(STAGE, "starting");
            LiveProtocol protocol = new LiveProtocol(new RealtimeProtocol.Events() {
                @Override public void send(JSONObject event, RequestScope request) throws Exception { socket.send(event, request); }
                @Override public JSONObject receive(RequestScope request) throws Exception {
                    JSONObject event = socket.receive(request); inspect(event); return event;
                }
            });
            JSONObject request = new JSONObject().put("instructions", "Check configuration.").put("input", new JSONArray()).put("tools", new JSONArray());
            protocol.initialize("saved".equals(model) ? settings.customModel() : model, config.speech(), request, scope);
            report(STAGE, "started"); protocol.close(scope); report(STAGE, "closed");
            if (save) { save(settings, config); }
        } catch (Exception exception) {
            throw new AssertionError("Live check failed: " + exception.getClass().getSimpleName(), exception);
        } finally { scope.cancel(); }
    }
    private void save(ProviderSettings settings, ProviderSettings.CustomConfig config) throws Exception {
        if (!settings.revision().equals(config.revision())) { throw new IllegalStateException("Configuration changed"); }
        settings.save(config.endpoint(), config.token(), "saved".equals(model) ? settings.customModel() : model, AudioMode.LIVE, config.speech());
        if (!instrumentation.getTargetContext().getSharedPreferences("settings", Context.MODE_PRIVATE).edit().commit()) {
            throw new IOException("Settings write failed");
        }
        report(STAGE, "saved");
    }
    private void inspect(JSONObject event) {
        if ("error".equals(event.optString("type"))) {
            JSONObject error = event.optJSONObject("error");
            if (error != null) {
                report("error_code", allowed(error.optString("code"), "invalid_api_key", "model_not_found", "invalid_value", "invalid_request_error", "unsupported_model", "insufficient_quota", "rate_limit_exceeded"));
                report("error_type", allowed(error.optString("type"), "invalid_request_error", "server_error", "authentication_error", "permission_error"));
                report("error_param", allowed(error.optString("param"), "model", "session.model", "delegation.responses.model", "session.delegation.responses.model", "session", "audio", "session.audio"));
            }
        }
    }
    private String allowed(String value, String... choices) { return Arrays.asList(choices).contains(value) ? value : "other"; }
    private void report(String name, String value) {
        Bundle status = new Bundle(); status.putString(name, value); instrumentation.sendStatus(0, status);
    }
}
