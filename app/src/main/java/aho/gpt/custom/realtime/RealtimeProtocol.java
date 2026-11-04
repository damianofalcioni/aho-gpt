package aho.gpt.custom.realtime;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.SpeechApiConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

/** GA Realtime, with explicit per-response history and no automatic server turns. */
public final class RealtimeProtocol {
    private static final String TYPE = "type";
    private static final String INPUT = "input";
    private static final String FORMAT = "format";
    public interface Events {
        void send(JSONObject event, RequestScope scope) throws Exception;
        JSONObject receive(RequestScope scope) throws Exception;
    }
    private final Events events;
    private byte[] audio = new byte[0];
    private String responseId = "";
    public RealtimeProtocol(Events events) { this.events = events; }
    public void initialize(boolean playback, RequestScope scope) throws Exception {
        initialize(playback, SpeechApiConfig.DEFAULT.realtimeVoice(), scope);
    }
    public void initialize(boolean playback, String voice, RequestScope scope) throws Exception {
        await("session.created", scope);
        events.send(session(playback, voice), scope);
        JSONObject session = await("session.updated", scope).getJSONObject("session");
        JSONObject formats = session.getJSONObject("audio");
        validateFormat(formats.getJSONObject(INPUT).getJSONObject(FORMAT));
        if (playback) { validateFormat(formats.getJSONObject("output").getJSONObject(FORMAT)); }
    }
    private JSONObject await(String type, RequestScope scope) throws Exception {
        while (true) {
            JSONObject event = receive(scope);
            if (type.equals(event.getString(TYPE))) { return event; }
        }
    }
    private JSONObject receive(RequestScope scope) throws Exception {
        scope.check(); JSONObject event = events.receive(scope); scope.check();
        if ("error".equals(event.optString(TYPE))) { throw new IOException("Realtime server error"); }
        return event;
    }
    private static JSONObject format() throws Exception { return new JSONObject().put(TYPE, "audio/pcm").put("rate", 24_000); }
    private static void validateFormat(JSONObject format) throws Exception {
        if (!"audio/pcm".equals(format.optString(TYPE)) || format.optInt("rate") != 24_000) {
            throw new IOException("Unsupported Realtime audio format");
        }
    }
    public static JSONObject session(boolean playback) throws Exception {
        return session(playback, SpeechApiConfig.DEFAULT.realtimeVoice());
    }
    public static JSONObject session(boolean playback, String voice) throws Exception {
        JSONObject input = new JSONObject().put(FORMAT, format()).put("turn_detection", JSONObject.NULL);
        JSONObject output = new JSONObject().put(FORMAT, format()).put("voice", voice);
        JSONObject session = new JSONObject().put(TYPE, "realtime").put("output_modalities", new JSONArray().put(playback ? "audio" : "text"))
                .put("audio", new JSONObject().put(INPUT, input).put("output", output));
        return new JSONObject().put(TYPE, "session.update").put("session", session);
    }
    public JSONObject respond(JSONObject request, RequestScope scope) throws Exception {
        audio = new byte[0]; responseId = "";
        JSONObject response = new JSONObject().put("conversation", "none").put(INPUT, RealtimeMessages.input(request.getJSONArray(INPUT)))
                .put("instructions", request.getString("instructions")).put("tools", tools(request.getJSONArray("tools")));
        events.send(new JSONObject().put(TYPE, "response.create").put("response", response), scope);
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        while (true) {
            JSONObject event = receive(scope);
            JSONObject completed = consume(event, pcm);
            if (completed != null) { return completed; }
        }
    }
    private JSONObject consume(JSONObject event, ByteArrayOutputStream pcm) throws Exception {
        String type = event.getString(TYPE);
        if ("response.created".equals(type)) { responseId = event.getJSONObject("response").getString("id"); return null; }
        if ("response.output_audio.delta".equals(type)) {
            appendAudio(event, pcm); return null;
        }
        if (!"response.done".equals(type)) { return null; }
        JSONObject response = event.getJSONObject("response"); requireId(response.getString("id"));
        JSONArray output = RealtimeMessages.completed(response);
        if (pcm.size() % 2 != 0) { throw new IOException("Invalid Realtime PCM"); }
        audio = pcm.toByteArray();
        return new JSONObject().put("output", output);
    }
    private void appendAudio(JSONObject event, ByteArrayOutputStream pcm) throws Exception {
        requireId(event.getString("response_id"));
        byte[] chunk = WireBase64.decode(event.getString("delta"));
        if (pcm.size() + chunk.length > 24_000_000) { throw new IOException("Realtime audio too large"); }
        pcm.write(chunk);
    }
    private void requireId(String id) throws IOException {
        if (responseId.isEmpty() || !responseId.equals(id)) { throw new IOException("Unexpected Realtime response"); }
    }
    private JSONArray tools(JSONArray definitions) throws Exception {
        JSONArray result = new JSONArray();
        for (int index = 0; index < definitions.length(); index++) {
            JSONArray functions = definitions.getJSONObject(index).getJSONArray("tools");
            for (int function = 0; function < functions.length(); function++) { result.put(tool(functions.getJSONObject(function))); }
        }
        return result;
    }
    private JSONObject tool(JSONObject source) throws Exception {
        return new JSONObject().put(TYPE, "function").put("name", source.getString("name"))
                .put("description", source.getString("description")).put("parameters", source.getJSONObject("parameters"));
    }
    public byte[] audio() { return audio; }
}
