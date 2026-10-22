package dev.lightassistant.android.net;

import dev.lightassistant.openai.SseResponseReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;

public final class OpenAiHttp {
    private volatile HttpsURLConnection activeResponse;
    public void cancelResponse() {
        HttpsURLConnection current = activeResponse;
        if (current != null) { current.disconnect(); }
    }
    public void emptyPost(String url, String body) throws IOException {
        HttpsURLConnection connection = connect(url, "", body, "application/x-www-form-urlencoded");
        try (InputStream input = checkedInput(connection)) { read(input); }
        finally { connection.disconnect(); }
    }

    public JSONObject json(String url, String token, String body, String contentType)
            throws IOException, JSONException {
        HttpsURLConnection connection = connect(url, token, body, contentType);
        try (InputStream input = checkedInput(connection)) {
            return new JSONObject(read(input));
        } finally { connection.disconnect(); }
    }

    public JSONObject response(String token, JSONObject body) throws IOException, JSONException {
        HttpsURLConnection connection = connect("https://api.openai.com/v1/responses", token,
                body.toString(), "application/json");
        activeResponse = connection;
        try (InputStream input = checkedInput(connection);
                InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return SseResponseReader.read(reader);
        } finally { activeResponse = null; connection.disconnect(); }
    }

    private HttpsURLConnection connect(String url, String token, String body, String contentType)
            throws IOException {
        URL target = new URL(url);
        if (!"api.openai.com".equals(target.getHost()) && !"auth.openai.com".equals(target.getHost())) {
            throw new IOException("Untrusted OpenAI endpoint");
        }
        HttpsURLConnection connection = (HttpsURLConnection) target.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        connection.setRequestProperty("Accept", "application/json, text/event-stream");
        if (!token.isEmpty()) { connection.setRequestProperty("Authorization", "Bearer " + token); }
        if (body != null) {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", contentType);
            try (var output = connection.getOutputStream()) {
                output.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        return connection;
    }

    private InputStream checkedInput(HttpsURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status >= 200 && status < 300) { return connection.getInputStream(); }
        String detail = "";
        InputStream error = connection.getErrorStream();
        if (error != null) { try (error) { detail = read(error); } }
        throw new IOException("OpenAI HTTP " + status + " (" + connection.getHeaderField("x-request-id")
                + "): " + detail);
    }

    private String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] bytes = new byte[4096];
        int count;
        while ((count = input.read(bytes)) != -1) {
            if (output.size() + count > 2_000_000) { throw new IOException("Response too large"); }
            output.write(bytes, 0, count);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
