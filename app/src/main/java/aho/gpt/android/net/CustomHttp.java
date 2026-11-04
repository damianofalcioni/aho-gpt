package aho.gpt.android.net;

import aho.gpt.core.RequestScope;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Only explicitly configured servers receive custom credentials; redirects are refused. */
public final class CustomHttp {
    private HttpURLConnection active;
    private RequestScope owner;
    public synchronized void cancel(RequestScope scope) {
        if (owner == scope && active != null) { active.disconnect(); }
    }
    private synchronized void activate(HttpURLConnection connection, RequestScope scope) throws InterruptedException {
        scope.check(); active = connection; owner = scope;
    }
    private synchronized void release(HttpURLConnection connection) {
        if (connection.equals(active)) { active = null; owner = null; }
    }
    public JSONObject json(String url, String token, JSONObject body, RequestScope scope) throws Exception {
        byte[] data = body == null ? null : body.toString().getBytes(StandardCharsets.UTF_8);
        return new JSONObject(new String(exchange(url, token, data, "application/json", 2_000_000, scope), StandardCharsets.UTF_8));
    }
    public byte[] exchange(String url, String token, byte[] body, String type, int limit, RequestScope scope) throws Exception {
        scope.check();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try (HttpDiagnostics diagnostics = new HttpDiagnostics("custom", body != null)) {
            activate(connection, scope);
            configure(connection, token);
            if (body != null) { write(connection, body, type); }
            int status = connection.getResponseCode();
            diagnostics.status(status);
            if (status < 200 || status >= 300) { throw new IOException("Custom HTTP " + status); }
            try (InputStream input = connection.getInputStream()) {
                byte[] result = read(input, limit, scope);
                scope.check(); diagnostics.completed(); return result;
            }
        } finally { release(connection); connection.disconnect(); }
    }
    private void configure(HttpURLConnection connection, String token) {
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15_000); connection.setReadTimeout(60_000);
        connection.setRequestProperty("Accept", "*/*");
        if (!token.isEmpty()) { connection.setRequestProperty("Authorization", "Bearer " + token); }
    }
    private void write(HttpURLConnection connection, byte[] body, String type) throws IOException {
        connection.setRequestMethod("POST"); connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", type);
        connection.setFixedLengthStreamingMode(body.length);
        try (var output = connection.getOutputStream()) { output.write(body); }
    }
    private byte[] read(InputStream input, int limit, RequestScope scope) throws IOException, InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            scope.check();
            if (output.size() + count > limit) { throw new IOException("Custom response too large"); }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
