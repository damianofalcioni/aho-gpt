package aho.gpt.core;

import java.util.concurrent.Callable;

/** One spoken turn's authority, independent of conversation history and Android lifecycle. */
public final class RequestScope {
    public interface Clock { long now(); }
    private final Clock clock;
    private final long started;
    private final long duration;
    private volatile boolean cancelled;

    public RequestScope() { this(() -> System.nanoTime() / 1_000_000, 120_000); }
    public RequestScope(Clock clock, long duration) {
        this.clock = clock; this.duration = duration; started = clock.now();
    }
    public void cancel() { cancelled = true; }
    public void check() throws InterruptedException {
        if (cancelled || Thread.currentThread().isInterrupted()) { throw new InterruptedException("Request cancelled"); }
        if (clock.now() - started >= duration) { throw new InterruptedException("Request expired"); }
    }
    /** Queued adapters must check again on their execution thread. */
    public <T> T dispatch(Callable<T> action) throws Exception { check(); return action.call(); }
}
