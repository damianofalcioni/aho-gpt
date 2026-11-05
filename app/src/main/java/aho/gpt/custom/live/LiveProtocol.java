package aho.gpt.custom.live;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.realtime.RealtimeProtocol;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

/** Complete delegated backend rounds share the app's existing agent safety loop. */
public final class LiveProtocol {
    private static final String TYPE = "type";
    private final LiveEvents events;
    private final LiveResponses responses = new LiveResponses();
    private final LiveDirectAnswer direct = new LiveDirectAnswer();
    private int forwarded;
    private boolean directAnswer;
    private String phase = "CONNECTING";
    public LiveProtocol(RealtimeProtocol.Events events) { this.events = new LiveEvents(events); }
    public void initialize(String model, SpeechApiConfig config, JSONObject request, RequestScope scope) throws Exception {
        phase = "STARTING";
        forwarded = request.getJSONArray("input").length();
        events.send(LiveSession.start(model, config, request), scope);
        LiveSession.validate(events.await("session.started", scope).getJSONObject("session"), model, config);
        phase = "WAITING_RESPONSE";
    }
    public JSONObject respond(JSONObject request, RequestScope scope) throws Exception {
        phase = "WAITING_RESPONSE";
        events.progress();
        continueTools(request.getJSONArray("input"), scope);
        while (true) {
            JSONObject event = events.receive(scope);
            direct.consume(event);
            if ("response.event".equals(event.getString(TYPE))) {
                phase = "WAITING_BACKEND";
                JSONObject completed = responses.consume(event);
                if (completed != null) { phase = backendPhase(completed); return completed; }
            }
            if (direct.ready(events.audio)) { phase = "DIRECT_ANSWER_READY"; directAnswer = true; return direct.response(); }
        }
    }
    private String backendPhase(JSONObject completed) {
        JSONArray output = completed.optJSONArray("output");
        if (output == null) { return "BACKEND_RESULT_READY"; }
        for (int index = 0; index < output.length(); index++) {
            JSONObject item = output.optJSONObject(index);
            if (item != null && "function_call".equals(item.optString(TYPE))) { return "WAITING_TOOL_RESULTS"; }
        }
        return "BACKEND_ANSWER_READY";
    }
    private void continueTools(JSONArray input, RequestScope scope) throws Exception {
        boolean continued = false;
        for (int index = forwarded; index < input.length(); index++) {
            JSONObject item = input.getJSONObject(index);
            if ("function_call_output".equals(item.optString(TYPE))) {
                events.send(new JSONObject().put(TYPE, "response.item.create").put("item", item), scope); continued = true;
            }
        }
        forwarded = input.length();
        if (continued) { events.audio.clear(); events.send("response.create", scope); phase = "WAITING_BACKEND"; }
    }
    public byte[] finish(boolean playback, RequestScope scope) throws Exception {
        phase = "WAITING_AUDIO";
        events.progress();
        if (!directAnswer) { events.audio.backendFinished(); }
        if (playback) {
            while (!events.audio.settled()) {
                JSONObject event = events.receive(scope);
                if ("response.event".equals(event.getString(TYPE))) { throw new IOException("Unexpected Live backend work"); }
            }
        }
        phase = "ANSWER_READY"; return playback ? events.audio.bytes() : new byte[0];
    }
    public void close(RequestScope scope) throws Exception {
        phase = "CLOSING"; events.close(scope); phase = "CLOSED";
    }
    public String diagnostics() { return "phase=" + phase + " " + events.diagnostics() + " " + direct.diagnostics(); }
    public String transcript() { return events.transcript(); }
}
