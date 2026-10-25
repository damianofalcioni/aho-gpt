package aho.gpt.android.logging;

import android.content.Context;

import java.util.Date;
import java.io.IOException;

import aho.gpt.android.logging.AndroidLogFolderWriter;

/** One live selected output; unavailable storage never creates a fallback session. */
final class AppLogDestination {
    private final Context context;
    private AndroidLogFolderWriter custom;

    private AppLogDestination(Context context) {
        this.context = context.getApplicationContext();
    }

    static AppLogDestination start(Context context) {
        AppLogDestination destination = new AppLogDestination(context);
        return destination.openCustom() ? destination : null;
    }

    static AppLogDestination startIfEnabled(Context context, boolean enabled) {
        return enabled ? start(context) : null;
    }

    private boolean openCustom() {
        custom = AndroidLogFolderWriter.open(context, aho.gpt.logging.LogRecord.fileName(new Date()));
        if (custom == null) {
            return false;
        }
        try {
            if (custom.append(AppLogger.buildLogPrefix("INFO", AppLogger.TAG,
                    AppLogSessionInfo.formatDestination(context, custom.path())))) {
                return true;
            }
        } catch (RuntimeException ignored) {
            // Metadata collection must not stop the app on restricted OEM builds.
        }
        custom.close();
        custom = null;
        return false;
    }

    boolean append(CharSequence block) {
        return custom != null && custom.append(block);
    }

    static void append(AppLogDestination destination, CharSequence block) {
        if (destination != null && !destination.append(block)) {
            AppLogger.setLoggingEnabled(destination.context, false);
        }
    }

    String path() {
        flush();
        return custom.path();
    }

    void flush() {
        try {
            if (custom != null) {
                custom.flush();
            }
        } catch (IOException e) {
            close();
            AppLogger.setLoggingEnabled(context, false);
        }
    }

    static void flush(AppLogDestination destination) {
        if (destination != null) {
            destination.flush();
        }
    }

    void close() {
        if (custom != null) {
            custom.close();
        }
    }

    static void close(AppLogDestination destination) {
        if (destination != null) {
            destination.close();
        }
    }
}
