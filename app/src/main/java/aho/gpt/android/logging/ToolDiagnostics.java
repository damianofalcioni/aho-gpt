package aho.gpt.android.logging;

import aho.gpt.logging.ToolLogEvent;

/** Only known function names and receipt states enter diagnostics. */
public final class ToolDiagnostics {
    private ToolDiagnostics() { }
    public static void completed(String name, String result, boolean replayed) {
        if (!AppLogger.isLoggingEnabled()) { return; }
        AppLogger.i("Tools", ToolLogEvent.format(name, result) + " replayed=" + replayed);
    }
}
