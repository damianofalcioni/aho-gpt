package aho.gpt.openai;

import org.json.JSONObject;
import org.json.JSONArray;

/** Strict bounded JSON for tool argument objects; Android's general parser accepts extra syntax. */
final class ToolJson {
    private final ToolJsonInput input;

    private ToolJson(String source) { input = new ToolJsonInput(source); }
    static JSONObject parse(String source) throws Exception {
        if (source.length() > 80_000) { throw new IllegalArgumentException("Tool arguments too large"); }
        ToolJson parser = new ToolJson(source);
        JSONObject result = parser.object(0);
        parser.input.end();
        return result;
    }
    private JSONObject object(int depth) throws Exception {
        if (depth > 1) { invalid(); }
        input.expect('{');
        JSONObject result = new JSONObject();
        if (input.take('}')) { return result; }
        do {
            String key = input.string();
            if (result.has(key)) { invalid(); }
            input.expect(':');
            result.put(key, value(depth));
        } while (input.take(','));
        input.expect('}');
        return result;
    }
    private Object value(int depth) throws Exception {
        char first = input.first();
        if (first == '"') { return input.string(); }
        if (first == '{') { return object(depth + 1); }
        if (first == '[') { return array(depth + 1); }
        return input.integer();
    }
    private JSONArray array(int depth) throws Exception {
        if (depth > 1) { invalid(); }
        input.expect('[');
        JSONArray result = new JSONArray();
        if (input.take(']')) { return result; }
        do { result.put(input.string()); } while (input.take(','));
        input.expect(']');
        return result;
    }
    private void invalid() { throw new IllegalArgumentException("Invalid tool JSON"); }
}
