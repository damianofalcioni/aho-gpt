package aho.gpt.custom.live;

import java.net.SocketTimeoutException;

/** Distinguishes the local progress deadline from a transport read timeout in safe logs. */
public final class LiveInactivityException extends SocketTimeoutException {
    public LiveInactivityException() { super("Live response stalled"); }
}
