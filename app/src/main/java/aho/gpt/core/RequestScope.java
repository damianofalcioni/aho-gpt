package aho.gpt.core;

import java.util.concurrent.Callable;

/** One spoken turn's authority, independent of conversation history and Android lifecycle. */
public final class RequestScope {
    private volatile boolean cancelled;

    public void cancel() { cancelled = true; }
    public boolean isCancelled() { return cancelled || Thread.currentThread().isInterrupted(); }
    public void check() throws InterruptedException {
        if (isCancelled()) { throw new InterruptedException("Request cancelled"); }
    }
    /** Queued adapters must check again on their execution thread. */
    public <T> T dispatch(Callable<T> action) throws Exception { check(); return action.call(); }
}
