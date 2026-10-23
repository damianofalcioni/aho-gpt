package aho.gpt.openai;

import org.json.JSONArray;
import org.json.JSONObject;

/** Small validator for the object, string and integer schemas used by device tools. */
final class ArgumentSchema {
    private ArgumentSchema() { }
    static void object(JSONObject value, JSONObject schema) throws Exception {
        if (value.length() > schema.optInt("maxProperties", 16)) { invalid(); }
        JSONArray required = schema.optJSONArray("required");
        if (required != null) { required(value, required); }
        JSONObject properties = schema.optJSONObject("properties");
        var keys = value.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.length() > 256) { invalid(); }
            check(value.get(key), property(schema, properties, key));
        }
    }
    private static JSONObject property(JSONObject schema, JSONObject properties, String key) throws Exception {
        if (properties != null && properties.has(key)) { return properties.getJSONObject(key); }
        JSONObject additional = schema.optJSONObject("additionalProperties");
        if (additional == null) { invalid(); }
        return additional;
    }
    private static void required(JSONObject value, JSONArray names) throws Exception {
        for (int i = 0; i < names.length(); i++) { if (!value.has(names.getString(i))) { invalid(); } }
    }
    private static void check(Object value, JSONObject schema) throws Exception {
        switch (schema.getString("type")) {
            case "string": string(value, schema); break;
            case "integer": integer(value); break;
            case "object":
                if (!(value instanceof JSONObject)) { invalid(); }
                object((JSONObject) value, schema); break;
            default: invalid(); break;
        }
    }
    private static void string(Object value, JSONObject schema) throws Exception {
        if (!(value instanceof String)) { invalid(); }
        String text = (String) value;
        if (text.length() > schema.optInt("maxLength", 512)) { invalid(); }
        JSONArray choices = schema.optJSONArray("enum");
        if (choices != null && !contains(choices, text)) { invalid(); }
    }
    private static boolean contains(JSONArray choices, String value) throws Exception {
        for (int i = 0; i < choices.length(); i++) { if (value.equals(choices.getString(i))) { return true; } }
        return false;
    }
    private static void integer(Object value) {
        if (!(value instanceof Integer || value instanceof Long)) { invalid(); }
        long number = ((Number) value).longValue();
        if (number < 0 || number > Integer.MAX_VALUE) { invalid(); }
    }
    private static void invalid() { throw new IllegalArgumentException("Invalid tool arguments"); }
}
