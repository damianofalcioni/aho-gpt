package aho.gpt.android.net;

import android.os.SystemClock;
import aho.gpt.android.logging.AppLogger;
import java.util.concurrent.atomic.AtomicLong;

/** Local sequence and timing only. Never records URLs, headers or payloads. */
final class HttpDiagnostics implements AutoCloseable {
    private static final String TAG = "HTTP";
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private final long id = SEQUENCE.incrementAndGet();
    private final long started = SystemClock.elapsedRealtime();
    private final String operation;
    private boolean completed;

    HttpDiagnostics(String url, boolean post) {
        operation = operation(url);
        AppLogger.d(TAG, prefix() + " started method=" + (post ? "POST" : "GET"));
    }
    void status(int status) { AppLogger.i(TAG, prefix() + " status=" + status); }
    void completed() { completed = true; }
    private String prefix() { return "request=" + id + " operation=" + operation; }
    @Override public void close() {
        String event = prefix() + " state=" + (completed ? "completed" : "failed_or_cancelled")
                + " elapsedMs=" + (SystemClock.elapsedRealtime() - started);
        if (completed) { AppLogger.d(TAG, event); }
        else { AppLogger.w(TAG, event); }
    }
    private static String operation(String url) {
        return switch (url) {
            case "https://api.openai.com/v1/responses" -> "responses";
            case "https://api.openai.com/v1/models" -> "models";
            case "https://auth.openai.com/api/accounts/oauth/token" -> "token";
            case "https://auth.openai.com/.well-known/openid-configuration" -> "discovery";
            case "https://auth.openai.com/.well-known/jwks.json" -> "signing_keys";
            default -> "other";
        };
    }
}
