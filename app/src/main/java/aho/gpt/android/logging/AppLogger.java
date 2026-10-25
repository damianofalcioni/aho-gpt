package aho.gpt.android.logging;

import android.content.Context;

import java.io.IOException;
import aho.gpt.logging.LogRecord;

import aho.gpt.android.logging.storage.AndroidOutputFolderCleaner;
import aho.gpt.android.logging.storage.AndroidOutputStorage;
import aho.gpt.android.logging.storage.AndroidOutputDestination;

public final class AppLogger {

    static final String TAG = "AppLogger";
    private static final Object LOCK = new Object();
    private static final String PREFS_NAME = "app_logging";
    private static final String KEY_LOG_ENABLED = "log_enabled";

    private static AppLogDestination destination;
    private static volatile boolean loggingEnabled;

    private AppLogger() {
    }

    public static void init(Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            replaceDestination(appContext, readLogEnabled(appContext));
        }
    }

    public static String getLogFilePath(Context context) {
        synchronized (LOCK) {
            if (destination != null) {
                return destination.path();
            }
        }
        return AndroidOutputStorage.current(context).label;
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public static boolean setLoggingEnabled(Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            if (loggingEnabled && enabled) {
                return false;
            }
            replaceDestination(appContext, enabled);
        }
        if (enabled) {
            write("INFO", TAG, "Logging enabled", null);
        }
        return loggingEnabled == enabled;
    }

    public static void d(String tag, String message) {
        write("DEBUG", tag, message, null);
    }

    public static void i(String tag, String message) {
        write("INFO", tag, message, null);
    }

    public static void w(String tag, String message) {
        write("WARN", tag, message, null);
    }

    public static void w(String tag, String message, Throwable throwable) {
        write("WARN", tag, message, throwable);
    }

    public static void e(String tag, String message, Throwable throwable) {
        write("ERROR", tag, message, throwable);
    }

    public static void anomaly(
            Context context,
            String tag,
            String message,
            Throwable throwable
    ) {
        synchronized (LOCK) {
            Context appContext = context.getApplicationContext();
            if (destination == null) {
                destination = AppLogDestination.start(appContext);
            }
            StringBuilder block = buildLogPrefix("ERROR", tag, message);
            appendThrowable(block, throwable);
            AppLogDestination.append(destination, block);
            AppLogDestination.flush(destination);
        }
    }

    private static void write(
            String level,
            String tag,
            String message,
            Throwable throwable
    ) {
        if (!loggingEnabled) {
            return;
        }
        StringBuilder block = buildLogPrefix(level, tag, message);
        appendThrowable(block, throwable);
        appendBlock(block);
        if (!"DEBUG".equals(level)) {
            synchronized (LOCK) {
                AppLogDestination.flush(destination);
            }
        }
    }

    private static void appendThrowable(StringBuilder block, Throwable throwable) {
        LogRecord.appendFailure(block, throwable);
    }

    static StringBuilder buildLogPrefix(String level, String tag, String message) {
        return LogRecord.prefix(level, tag, message);
    }
    private static void appendBlock(CharSequence block) {
        synchronized (LOCK) {
            if (loggingEnabled) {
                AppLogDestination.append(destination, block);
            }
        }
    }

    public static void refreshOutputFolder(Context context) {
        synchronized (LOCK) {
            if (destination != null) {
                replaceDestination(context, loggingEnabled);
            }
        }
    }

    public static void clearOutputFolder(Context context, AndroidOutputDestination selected) throws IOException {
        synchronized (LOCK) {
            AppLogDestination.close(destination);
            destination = null;
            try {
                AndroidOutputFolderCleaner.clear(context, selected);
            } finally {
                replaceDestination(context, loggingEnabled);
            }
        }
    }

    private static void replaceDestination(Context context, boolean enabled) {
        AppLogDestination.close(destination);
        Context appContext = context.getApplicationContext();
        destination = AppLogDestination.startIfEnabled(appContext, enabled);
        loggingEnabled = enabled && destination != null;
        writeLogEnabled(appContext, loggingEnabled);
    }

    private static boolean readLogEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_LOG_ENABLED, false);
    }

    private static void writeLogEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOG_ENABLED, enabled)
                .apply();
    }
}
