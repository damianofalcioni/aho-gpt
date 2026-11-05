package aho.gpt.openai;

import org.json.JSONTokener;

/** Strict scalar and punctuation reader for the bounded tool argument parser. */
final class ToolJsonInput {
    private final String source;
    private int position;

    ToolJsonInput(String source) { this.source = source; }

    void end() {
        space();
        if (position != source.length()) { invalid(); }
    }
    char first() { space(); return peek(); }
    long integer() {
        int start = position;
        while (position < source.length() && "-0123456789".indexOf(source.charAt(position)) >= 0) { position++; }
        String number = source.substring(start, position);
        if (!number.matches("-?(0|[1-9][0-9]*)")) { invalid(); }
        return Long.parseLong(number);
    }
    String string() throws Exception {
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
    boolean take(char value) {
        space();
        if (peek() != value) { return false; }
        position++; return true;
    }
    void expect(char value) { if (!take(value)) { invalid(); } }
    private char peek() {
        if (position >= source.length()) { invalid(); }
        return source.charAt(position);
    }
    private void space() {
        while (position < source.length() && " \r\n\t".indexOf(source.charAt(position)) >= 0) { position++; }
    }
    private void invalid() { throw new IllegalArgumentException("Invalid tool JSON"); }
}
