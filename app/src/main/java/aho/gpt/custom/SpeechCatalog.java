package aho.gpt.custom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Optional provider voice inventory; published OpenAI choices remain suggestions. */
public final class SpeechCatalog {
    private SpeechCatalog() { }
    public static boolean openAi(String base) { return "https://api.openai.com/v1".equals(ServerEndpoint.normalize(base)); }
    public static List<String> voices(JSONObject response) throws IOException {
        JSONArray data = response.optJSONArray("voices");
        if (data == null) { data = response.optJSONArray("data"); }
        if (data == null) { throw new IOException("Voice listing unavailable"); }
        Set<String> voices = new LinkedHashSet<>();
        for (int index = 0; index < data.length(); index++) { add(voices, data.opt(index)); }
        return new ArrayList<>(voices);
    }
    private static void add(Set<String> values, Object value) {
        Object id = value instanceof JSONObject object ? object.opt("id") : value;
        if (!(id instanceof String)) { return; }
        try { values.add(SpeechApiConfig.identifier((String) id)); }
        catch (IllegalArgumentException ignored) { /* Malformed inventory entries cannot become request identifiers. */ }
    }
    public static List<String> identifiers(List<String> entries) {
        Set<String> ids = new LinkedHashSet<>();
        if (entries != null) { for (String entry : entries) { add(ids, entry); } }
        return new ArrayList<>(ids);
    }
    public static List<String> speechVoices(String model) {
        if ("tts-1".equals(model) || "tts-1-hd".equals(model)) {
            return Arrays.asList("alloy", "ash", "coral", "echo", "fable", "onyx", "nova", "sage", "shimmer");
        }
        return Arrays.asList("alloy", "ash", "ballad", "coral", "echo", "fable", "nova", "onyx", "sage", "shimmer", "verse", "marin", "cedar");
    }
    public static List<String> realtimeVoices() {
        return Arrays.asList("alloy", "ash", "ballad", "coral", "echo", "sage", "shimmer", "verse", "marin", "cedar");
    }
}
