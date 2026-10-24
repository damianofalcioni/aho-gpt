package aho.gpt.core;

/** Monotonic-time continuity after successful playback or a displayed answer with playback disabled. */
public final class FollowUpWindow {
    public static final long TIMEOUT_MS = 8_000;
    private long expiresAt;

    public void open(long now) { expiresAt = now + TIMEOUT_MS; }
    public boolean isOpen(long now) { return expiresAt > 0 && now < expiresAt; }
    public long remaining(long now) { return Math.max(0, expiresAt - now); }
    public void close() { expiresAt = 0; }
}
