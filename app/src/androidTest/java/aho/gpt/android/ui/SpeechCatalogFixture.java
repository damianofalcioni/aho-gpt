package aho.gpt.android.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loopback-only discovery fixture; accepts no speech, credentials or saved configuration. */
final class SpeechCatalogFixture implements AutoCloseable {
    private final ServerSocket server;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    SpeechCatalogFixture() throws IOException {
        server = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1")); worker.submit(this::serve);
    }
    String base() { return "http://127.0.0.1:" + server.getLocalPort() + "/v1"; }
    private void serve() {
        while (!server.isClosed()) {
            try (Socket socket = server.accept()) { respond(socket); }
            catch (IOException ignored) { /* Fixture shutdown interrupts accept and abandoned discovery. */ }
        }
    }
    private void respond(Socket socket) throws IOException {
        socket.setSoTimeout(3000);
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        String request = input.readLine(); String line;
        while ((line = input.readLine()) != null && !line.isEmpty()) { /* Consume only test request headers. */ }
        String body = request != null && request.startsWith("GET /v1/models ")
                ? "{\"data\":[{\"id\":\"stt-server\"},{\"id\":\"tts-server\"}]}"
                : "{\"voices\":[\"voice-server\",\"realtime-server\"]}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
                + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().write(bytes); socket.getOutputStream().flush();
    }
    @Override public void close() throws IOException { server.close(); worker.shutdownNow(); }
}
