package aho.gpt.openai;

import org.json.JSONObject;
import org.json.JSONTokener;

/** Strict bounded JSON for tool argument objects; Android's general parser accepts extra syntax. */
final class ToolJson {
    private final String source;
    private int position;

    private ToolJson(String source) { this.source = source; }
    static JSONObject parse(String source) throws Exception {
        if (source.length() > 80_000) { throw new IllegalArgumentException("Tool arguments too large"); }
        ToolJson parser = new ToolJson(source);
        JSONObject result = parser.object(0);
        parser.space();
        if (parser.position != source.length()) { parser.invalid(); }
        return result;
    }
    private JSONObject object(int depth) throws Exception {
        if (depth > 1) { invalid(); }
        expect('{');
        JSONObject result = new JSONObject();
        if (take('}')) { return result; }
        do {
            String key = string();
            if (result.has(key)) { invalid(); }
            expect(':');
            result.put(key, value(depth));
        } while (take(','));
        expect('}');
        return result;
    }
    private Object value(int depth) throws Exception {
        space();
        char first = peek();
        if (first == '"') { return string(); }
        if (first == '{') { return object(depth + 1); }
        int start = position;
        while (position < source.length() && "-0123456789".indexOf(source.charAt(position)) >= 0) { position++; }
        String number = source.substring(start, position);
        if (!number.matches("-?(0|[1-9][0-9]*)")) { invalid(); }
        return Long.valueOf(number);
    }
    private String string() throws Exception {
        space();
        int start = position;
        expect('"');
        while (peek() != '"') { character(); }
        position++;
        return (String) new JSONTokener(source.substring(start, position)).nextValue();
    }
    private void character() {
        char value = peek(); position++;
        if (value < 32) { invalid(); }
        if (value == '\\') { escape(); }
    }
    private void escape() {
        char value = peek(); position++;
        if (value == 'u') {
            for (int i = 0; i < 4; i++) {
                if ("0123456789abcdefABCDEF".indexOf(peek()) < 0) { invalid(); }
                position++;
            }
        } else if ("\"\\/bfnrt".indexOf(value) < 0) { invalid(); }
    }
    private boolean take(char value) {
        space();
        if (peek() != value) { return false; }
        position++; return true;
    }
    private void expect(char value) { if (!take(value)) { invalid(); } }
    private char peek() {
        if (position >= source.length()) { invalid(); }
        return source.charAt(position);
    }
    private void space() {
        while (position < source.length() && " \r\n\t".indexOf(source.charAt(position)) >= 0) { position++; }
    }
    private void invalid() { throw new IllegalArgumentException("Invalid tool JSON"); }
}
