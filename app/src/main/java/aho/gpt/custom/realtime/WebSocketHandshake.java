package aho.gpt.custom.realtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;

/** Validates the HTTP upgrade, including its challenge; never follows redirects. */
final class WebSocketHandshake {
    private WebSocketHandshake() { }
    static void upgrade(URI base, String model, String token, InputStream input, OutputStream output) throws Exception {
        if (token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0) { throw new IOException("Invalid server token"); }
        byte[] nonce = new byte[16]; new SecureRandom().nextBytes(nonce);
        String key = WireBase64.encode(nonce);
        String path = base.getRawPath() + "/realtime?model=" + URLEncoder.encode(model, StandardCharsets.UTF_8.name());
        String host = base.getRawAuthority();
        String request = "GET " + path + " HTTP/1.1\r\nHost: " + host + "\r\nUpgrade: websocket\r\n"
                + "Connection: Upgrade\r\nSec-WebSocket-Key: " + key + "\r\nSec-WebSocket-Version: 13\r\n";
        if (!token.isEmpty()) { request += "Authorization: Bearer " + token + "\r\n"; }
        output.write((request + "\r\n").getBytes(StandardCharsets.UTF_8)); output.flush();
        validate(read(input), key);
    }
    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int suffix = 0;
        while (suffix != 0x0d0a0d0a) {
            int value = input.read();
            if (value < 0 || bytes.size() >= 16_384) { throw new IOException("Invalid WebSocket upgrade"); }
            bytes.write(value); suffix = (suffix << 8) | value;
        }
        return bytes.toString(StandardCharsets.US_ASCII.name());
    }
    static void validate(String header, String key) throws Exception {
        String[] lines = header.split("\r\n");
        if (!lines[0].startsWith("HTTP/1.1 101 ")) { throw new IOException("Realtime upgrade refused"); }
        Map<String, String> fields = fields(lines);
        validateUpgrade(fields);
        String expected = WireBase64.encode(MessageDigest.getInstance("SHA-1")
                .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
        if (!expected.equals(fields.get("sec-websocket-accept"))) { throw new IOException("Invalid WebSocket challenge"); }
    }
    private static void validateUpgrade(Map<String, String> fields) throws IOException {
        if (!"websocket".equalsIgnoreCase(fields.get("upgrade"))) { throw new IOException("Invalid WebSocket upgrade"); }
        String connection = connection(fields.get("connection"));
        boolean upgrade = Arrays.asList(connection.split("\\s*,\\s*")).contains("upgrade");
        if (!upgrade) { throw new IOException("Invalid WebSocket connection"); }
        if (fields.containsKey("sec-websocket-extensions")) { throw new IOException("Unsupported WebSocket extension"); }
    }
    private static String connection(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private static Map<String, String> fields(String[] lines) throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (int index = 1; index < lines.length; index++) {
            int colon = lines[index].indexOf(':');
            if (colon < 1) { throw new IOException("Invalid WebSocket header"); }
            String name = lines[index].substring(0, colon).trim().toLowerCase(Locale.ROOT);
            if (fields.put(name, lines[index].substring(colon + 1).trim()) != null) { throw new IOException("Duplicate WebSocket header"); }
        }
        return fields;
    }
}
