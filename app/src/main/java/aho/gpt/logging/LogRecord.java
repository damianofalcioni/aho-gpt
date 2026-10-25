package aho.gpt.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/** Diagnostic formatting never includes exception messages or suppressed exceptions. */
public final class LogRecord {
    private static final Pattern FILE_NAME = Pattern.compile("aho-gpt-log-\\d{14}(?:-\\d+)?(?: \\(\\d+\\))?\\.txt");
    private LogRecord() { }

    public static String fileName(Date now) {
        return "aho-gpt-log-" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(now) + ".txt";
    }
    public static boolean isLogFile(String name) { return name != null && FILE_NAME.matcher(name).matches(); }
    public static StringBuilder prefix(String level, String tag, String message) {
        return new StringBuilder(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date()))
                .append(' ').append(singleLine(level)).append('/').append(singleLine(tag))
                .append(" [").append(singleLine(Thread.currentThread().getName())).append("] ")
                .append(singleLine(message)).append('\n');
    }
    public static void appendFailure(StringBuilder out, Throwable failure) {
        Throwable current = failure;
        for (int cause = 0; current != null && cause < 8; cause++) {
            out.append(current.getClass().getName()).append('\n');
            StackTraceElement[] frames = current.getStackTrace();
            for (int index = 0; index < Math.min(64, frames.length); index++) {
                out.append("\tat ").append(singleLine(frames[index].toString())).append('\n');
            }
            current = current.getCause();
        }
    }
    private static String singleLine(String text) { return text.replace("\r", "\\r").replace("\n", "\\n"); }
}
