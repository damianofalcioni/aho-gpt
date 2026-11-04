package aho.gpt.custom.realtime;

import aho.gpt.core.RequestScope;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class RealtimeSocketTest {
    @Test public void authenticatesOnlyToConfiguredHostAndCloseInterruptsReads() throws Exception {
        ExecutorService worker = Executors.newFixedThreadPool(2);
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")); RealtimeSocket client = new RealtimeSocket()) {
            var fixture = worker.submit(() -> serve(server));
            RequestScope scope = new RequestScope();
            client.connect("http://127.0.0.1:" + server.getLocalPort() + "/v1", "voice model", "fixture-token", scope);
            assertEquals("session.created", client.receive(scope).getString("type"));
            CountDownLatch reading = new CountDownLatch(1);
            var pending = worker.submit(() -> {
                reading.countDown();
                try { client.receive(scope); return false; }
                catch (IOException | InterruptedException exception) { return true; }
            });
            assertTrue(reading.await(2, TimeUnit.SECONDS)); scope.cancel(); client.close();
            assertTrue(pending.get(2, TimeUnit.SECONDS));
            String request = fixture.get(2, TimeUnit.SECONDS);
            assertTrue(request.startsWith("GET /v1/realtime?model=voice+model HTTP/1.1\r\n"));
            assertTrue(request.contains("Authorization: Bearer fixture-token\r\n"));
        } finally { worker.shutdownNow(); }
    }
    @Test public void headerInjectionAndRetiredScopesCannotSendCredentials() {
        try (RealtimeSocket client = new RealtimeSocket()) {
            RequestScope scope = new RequestScope(); scope.cancel();
            assertThrows(InterruptedException.class, () -> client.connect("http://127.0.0.1:1/v1", "voice", "fixture-token", scope));
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThrows(IOException.class, () -> WebSocketHandshake.upgrade(java.net.URI.create("http://127.0.0.1/v1"), "voice",
                "fixture\r\nInjected: true", new java.io.ByteArrayInputStream(new byte[0]), output));
        assertEquals(0, output.size());
    }
    private String serve(ServerSocket server) throws Exception {
        try (Socket socket = server.accept()) {
            socket.setSoTimeout(5000); String request = header(socket.getInputStream());
            String key = line(request, "Sec-WebSocket-Key: ");
            String accept = WireBase64.encode(MessageDigest.getInstance("SHA-1").digest(
                    (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
            socket.getOutputStream().write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            byte[] event = "{\"type\":\"session.created\"}".getBytes(StandardCharsets.UTF_8);
            socket.getOutputStream().write(129); socket.getOutputStream().write(event.length); socket.getOutputStream().write(event);
            socket.getOutputStream().flush(); assertEquals(-1, socket.getInputStream().read()); return request;
        }
    }
    private String header(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); int suffix = 0;
        while (suffix != 0x0d0a0d0a) {
            int value = input.read();
            if (value < 0 || output.size() > 16384) { throw new IOException("Missing test handshake"); }
            output.write(value); suffix = (suffix << 8) | value;
        }
        return output.toString(StandardCharsets.US_ASCII.name());
    }
    private String line(String request, String prefix) {
        for (String line : request.split("\r\n")) { if (line.startsWith(prefix)) { return line.substring(prefix.length()); } }
        throw new AssertionError("Missing WebSocket challenge");
    }
}
