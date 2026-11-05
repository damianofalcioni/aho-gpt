package aho.gpt.custom.live;

import java.io.IOException;

/** A fixed local failure; provider error messages never enter UI or diagnostics. */
public final class LiveSetupException extends IOException {
    public LiveSetupException() { super("Live setup rejected"); }
}
