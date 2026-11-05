package aho.gpt.custom.live;

import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.openai.ResponseText;
import aho.gpt.openai.VoiceInstructions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

/** GPT-Live conversation and Responses reasoning have independent model choices. */
public final class LiveSession {
    private static final String TYPE = "type";
    private static final String MODEL = "model";
    private static final String RESPONSES = "responses";
    private LiveSession() { }
    static void validate(JSONObject session, String model, SpeechApiConfig config) throws Exception {
        require(config.liveModel(), session.getString(MODEL));
        JSONObject audio = session.getJSONObject("audio"); JSONObject format = audio.getJSONObject("format");
        require("audio/pcm", format.getString(TYPE));
        if (format.getInt("rate") != 24_000) { throw new IOException("Unsupported Live audio"); }
        require(config.liveVoice(), audio.getJSONObject("output").getString("voice"));
        JSONObject delegation = session.getJSONObject("delegation");
        require(RESPONSES, delegation.getString(TYPE)); require(model, delegation.getJSONObject(RESPONSES).getString(MODEL));
    }
    private static void require(String expected, String actual) throws IOException {
        if (!expected.equals(actual)) { throw new IOException("Unexpected Live configuration"); }
    }
    public static JSONObject start(String model, SpeechApiConfig config, JSONObject request) throws Exception {
        JSONObject backend = new JSONObject().put(MODEL, model).put("parallel_tool_calls", false)
                .put("instructions", request.getString("instructions")).put("tools", functions(request.getJSONArray("tools")));
        JSONObject audio = new JSONObject().put("format", new JSONObject().put(TYPE, "audio/pcm").put("rate", 24_000))
                .put("output", new JSONObject().put("voice", config.liveVoice()));
        JSONObject session = new JSONObject().put(MODEL, config.liveModel()).put("store", false).put("audio", audio)
                .put("instructions", VoiceInstructions.style()
                        + "Use that same language for spoken acknowledgments and summaries of backend results. "
                        + "Do not adopt the language of these instructions, previous assistant replies or backend text. "
                        + "For every user request, Generate <|SpawnThinking|>. Let the backend handle reasoning and all actions. "
                        + "Wait until the user finishes speaking. Never announce an action as successful without its backend result.")
                .put("input", history(request.getJSONArray("input")))
                .put("delegation", new JSONObject().put(TYPE, RESPONSES).put(RESPONSES, backend));
        return new JSONObject().put(TYPE, "session.start").put("session", session);
    }
    private static JSONArray functions(JSONArray definitions) throws Exception {
        JSONArray result = new JSONArray();
        for (int index = 0; index < definitions.length(); index++) {
            JSONArray tools = definitions.getJSONObject(index).getJSONArray("tools");
            for (int tool = 0; tool < tools.length(); tool++) { result.put(new JSONObject(tools.getJSONObject(tool).toString())); }
        }
        return result;
    }
    private static JSONArray history(JSONArray input) throws Exception {
        JSONArray result = new JSONArray(); int size = 0;
        for (int index = 0; index < input.length(); index++) {
            JSONObject item = input.getJSONObject(index); String role = item.optString("role");
            if (!"user".equals(role) && !"assistant".equals(role)) { continue; }
            String text = text(item);
            size += text.length();
            if (size > 20_000) { return new JSONArray(); }
            if (!text.isEmpty()) {
                result.put(historyMessage(role, text));
            }
        }
        return result;
    }
    private static JSONObject historyMessage(String role, String text) throws Exception {
        JSONObject part = new JSONObject().put(TYPE, "assistant".equals(role) ? "output_text" : "input_text").put("text", text);
        return new JSONObject().put(TYPE, "message").put("role", role).put("content", new JSONArray().put(part));
    }
    private static String text(JSONObject item) throws Exception {
        Object content = item.opt("content");
        if (content instanceof String) { return (String) content; }
        return ResponseText.extract(new JSONArray().put(new JSONObject().put(TYPE, "message").put("content", content)));
    }
}
