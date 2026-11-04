package aho.gpt.custom.realtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Strict Base64 without relying on Java APIs absent on Android 6. */
public final class WireBase64 {
    private static final String DIGITS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private WireBase64() { }
    public static String encode(byte[] data) {
        StringBuilder text = new StringBuilder((data.length + 2) / 3 * 4);
        for (int index = 0; index < data.length; index += 3) { encodeGroup(text, data, index); }
        return text.toString();
    }
    private static void encodeGroup(StringBuilder text, byte[] data, int index) {
        int first = data[index] & 255;
        int second = index + 1 < data.length ? data[index + 1] & 255 : 0;
        int third = index + 2 < data.length ? data[index + 2] & 255 : 0;
        text.append(DIGITS.charAt(first >> 2)).append(DIGITS.charAt(((first & 3) << 4) | (second >> 4)));
        text.append(index + 1 < data.length ? DIGITS.charAt(((second & 15) << 2) | (third >> 6)) : '=');
        text.append(index + 2 < data.length ? DIGITS.charAt(third & 63) : '=');
    }
    public static byte[] decode(String text) throws IOException {
        if (text.length() % 4 != 0) { throw new IOException("Invalid Base64 length"); }
        ByteArrayOutputStream output = new ByteArrayOutputStream(text.length() / 4 * 3);
        for (int index = 0; index < text.length(); index += 4) { decodeGroup(output, text, index); }
        return output.toByteArray();
    }
    private static void decodeGroup(ByteArrayOutputStream output, String text, int index) throws IOException {
        int first = digit(text.charAt(index)); int second = digit(text.charAt(index + 1));
        output.write((first << 2) | (second >> 4));
        char third = text.charAt(index + 2); char fourth = text.charAt(index + 3);
        if (third == '=') {
            if (fourth != '=') { throw new IOException("Invalid Base64 padding"); }
            padding(index, text.length(), second & 15);
            return;
        }
        int value = digit(third); output.write((second << 4) | (value >> 2));
        if (fourth == '=') {
            padding(index, text.length(), value & 3);
            return;
        }
        output.write((value << 6) | digit(fourth));
    }
    private static void padding(int index, int length, int remainder) throws IOException {
        if (index + 4 != length || remainder != 0) { throw new IOException("Invalid Base64 padding"); }
    }
    private static int digit(char value) throws IOException {
        int digit = DIGITS.indexOf(value);
        if (digit < 0) { throw new IOException("Invalid Base64 character"); }
        return digit;
    }
}
