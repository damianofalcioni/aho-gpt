package dev.lightassistant.android.auth;

import android.net.Uri;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Ephemeral IPv4 loopback listener, created before the authorization page is opened. */
public final class LoopbackCallback implements Closeable {
    private final ServerSocket server;

    public LoopbackCallback() throws IOException {
        server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        server.setSoTimeout(300_000);
    }
    public String redirect() { return "http://127.0.0.1:" + server.getLocalPort() + "/auth/callback"; }

    public Uri await(String expectedState) throws IOException {
        long deadline = System.nanoTime() + 300_000_000_000L;
        while (System.nanoTime() < deadline) {
            try (Socket socket = server.accept()) {
                socket.setSoTimeout(3000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                String request = reader.readLine();
                Uri callback = parse(request);
                boolean valid = callback != null && expectedState.equals(callback.getQueryParameter("state"));
                String body = valid ? "Sign-in received. Return to Light Assistant." : "Invalid callback.";
                socket.getOutputStream().write(("HTTP/1.1 " + (valid ? "200 OK" : "400 Bad Request")
                        + "\r\nContent-Type: text/plain\r\nConnection: close\r\nContent-Length: "
                        + body.length() + "\r\n\r\n" + body).getBytes(StandardCharsets.US_ASCII));
                if (valid) { return callback; }
            }
        }
        throw new IOException("Sign-in timed out");
    }

    private Uri parse(String request) {
        if (request == null || request.length() > 16_384 || !request.startsWith("GET /auth/callback?")) { return null; }
        int end = request.indexOf(' ', 4);
        if (end < 0) { return null; }
        return Uri.parse("http://127.0.0.1:" + server.getLocalPort() + request.substring(4, end));
    }
    @Override public void close() throws IOException { server.close(); }
}
