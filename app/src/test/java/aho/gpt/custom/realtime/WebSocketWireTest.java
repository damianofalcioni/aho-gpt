package aho.gpt.custom.realtime;

import aho.gpt.core.RequestScope;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class WebSocketWireTest {
    private static final String KEY = "dGhlIHNhbXBsZSBub25jZQ==";
    private static final String UPGRADE = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: keep-alive, Upgrade\r\n"
            + "Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=\r\n\r\n";
    @Test public void upgradeRequiresChallengeAndRefusesRedirectsAndExtensions() throws Exception {
        WebSocketHandshake.validate(UPGRADE, KEY);
        assertThrows(IOException.class, () -> WebSocketHandshake.validate(UPGRADE.replace("101 Switching Protocols", "302 Found"), KEY));
        assertThrows(IOException.class, () -> WebSocketHandshake.validate(UPGRADE, "wrong"));
        assertThrows(IOException.class, () -> WebSocketHandshake.validate(UPGRADE.replace("\r\n\r\n", "\r\nSec-WebSocket-Extensions: permessage-deflate\r\n\r\n"), KEY));
        assertThrows(IOException.class, () -> WebSocketHandshake.validate(UPGRADE.replace("keep-alive, Upgrade", "keep-alive"), KEY));
    }
    @Test public void base64MatchesStandardEncodingAndRejectsInvalidPadding() throws Exception {
        Random random = new Random(17);
        for (int size = 0; size < 200; size++) {
            byte[] original = new byte[size]; random.nextBytes(original);
            String encoded = WireBase64.encode(original);
            assertEquals(Base64.getEncoder().encodeToString(original), encoded);
            assertArrayEquals(original, WireBase64.decode(encoded));
        }
        for (String invalid : new String[]{"x", "A===", "AA=A", "AA==AAAA", "AB==", "AAB=", "AA!!"}) {
            assertThrows(IOException.class, () -> WireBase64.decode(invalid));
        }
    }
    @Test public void clientFramesAreMaskedForAllLengthEncodings() throws Exception {
        for (int size : new int[]{2, 126, 65536}) {
            String text = "a".repeat(size); ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            new WebSocketFrames(new ByteArrayInputStream(new byte[0]), bytes).text(text);
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
            assertEquals(129, input.readUnsignedByte()); int second = input.readUnsignedByte();
            assertTrue((second & 128) != 0); assertEquals(size, length(input, second & 127));
            byte[] mask = input.readNBytes(4); byte[] body = input.readNBytes(size);
            for (int index = 0; index < size; index++) { body[index] ^= mask[index % 4]; }
            assertEquals(text, new String(body, StandardCharsets.UTF_8));
        }
    }
    @Test public void fragmentedTextSurvivesInterleavedPing() throws Exception {
        ByteArrayOutputStream server = new ByteArrayOutputStream();
        server.write(frame(1, "hel")); server.write(frame(137, "ping")); server.write(frame(128, "lo"));
        ByteArrayOutputStream client = new ByteArrayOutputStream();
        assertEquals("hello", new WebSocketFrames(new ByteArrayInputStream(server.toByteArray()), client).text(new RequestScope()));
        assertEquals(138, client.toByteArray()[0] & 255); assertTrue((client.toByteArray()[1] & 128) != 0);
    }
    @Test public void invalidServerFramesAndUtf8AreRefused() throws Exception {
        byte[][] invalid = {new byte[]{(byte) 129, (byte) 128}, frame(128, "orphan"), frame(130, "binary"),
                frame(9, "fragmented ping"), new byte[]{(byte) 129, 1, (byte) 255},
                new byte[]{(byte) 129, 126, 0, 1, 0}, new byte[]{(byte) 129, 127, -1, -1, -1, -1, -1, -1, -1, -1}};
        for (byte[] bytes : invalid) {
            assertThrows(Exception.class, () -> new WebSocketFrames(new ByteArrayInputStream(bytes), new ByteArrayOutputStream()).text(new RequestScope()));
        }
    }
    @Test public void cancelledReadsDoNotConsumePayload() {
        RequestScope scope = new RequestScope(); scope.cancel(); ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{1, 2});
        assertThrows(InterruptedException.class, () -> new WebSocketFrames(input, new ByteArrayOutputStream()).text(scope));
        assertEquals(2, input.available());
    }
    private int length(DataInputStream input, int marker) throws IOException {
        if (marker == 126) { return input.readUnsignedShort(); }
        return marker == 127 ? (int) input.readLong() : marker;
    }
    private byte[] frame(int first, String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream output = new DataOutputStream(bytes);
        byte[] body = text.getBytes(StandardCharsets.UTF_8); output.writeByte(first); output.writeByte(body.length); output.write(body);
        return bytes.toByteArray();
    }
}
