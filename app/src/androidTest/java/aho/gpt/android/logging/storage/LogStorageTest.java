package aho.gpt.android.logging.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import aho.gpt.android.logging.AndroidLogFolderWriter;
import aho.gpt.android.logging.AppLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Opt-in storage checks; clean up only their own generated files and restore logger preferences. */
public final class LogStorageTest {
    private static final String TAG = "Test";
    private final Context original;
    private final Context context;
    private final List<String> paths = new ArrayList<>();

    public LogStorageTest(Context original) {
        this.original = original;
        String prefix = "log_test_" + System.nanoTime() + "_";
        context = new ContextWrapper(original) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences(prefix + name, mode);
            }
        };
    }
    public void run() throws Exception {
        try {
            AppLogger.init(context);
            require(!AppLogger.isLoggingEnabled(), "Logging must default off");
            scopedDeletion();
            session();
            trim();
            unavailable();
        } finally {
            AppLogger.setLoggingEnabled(context, false);
            try { cleanup(); }
            finally {
                context.getSharedPreferences("app_logging", Context.MODE_PRIVATE).edit().clear().apply();
                context.getSharedPreferences("output_folders", Context.MODE_PRIVATE).edit().clear().apply();
                AppLogger.init(original);
            }
        }
    }
    private void session() throws Exception {
        AndroidOutputFile probe = AndroidOutputFile.create(context, AndroidOutputStorage.requested(context,
                AppOutputFolderSettings.Kind.LOGS), "text/plain", "aho-gpt-log-20000101000002.txt");
        remember(probe.path());
        try (var output = probe.open()) { output.write(0); }
        probe.publish();
        require(AppLogger.setLoggingEnabled(context, true), "Storage write must enable logging");
        String first = remember(AppLogger.getLogFilePath(context));
        AppLogger.e(TAG, "Test failure", new IOException("private-token-and-screen-text"));
        String content = read(first);
        require(content.indexOf("Log session system info") < content.indexOf("Test failure"), "System info must come first");
        require(content.contains("java.io.IOException"), "Exception type must be logged");
        require(!content.contains("private-token-and-screen-text"), "Exception messages must stay private");
        AppLogger.init(context);
        require(AppLogger.isLoggingEnabled(), "Saved choice must survive logger restart");
        String next = remember(AppLogger.getLogFilePath(context));
        require(!first.equals(next), "Restart must create a new session");
        AppLogger.setLoggingEnabled(context, false);
        long size = read(next).length();
        AppLogger.i(TAG, "Must stay off");
        require(read(next).length() == size, "Disabled logging must not append events");
        AppLogger.anomaly(context, TAG, "Test anomaly", new IOException("private-anomaly"));
        String anomaly = remember(AppLogger.getLogFilePath(context));
        require(!AppLogger.isLoggingEnabled(), "Anomalies must not enable routine logging");
        require(read(anomaly).contains("Test anomaly"), "Anomaly must be flushed while disabled");
        require(!read(anomaly).contains("private-anomaly"), "Anomaly messages must stay private");
        AppLogger.setLoggingEnabled(context, false);
    }
    private void trim() throws Exception {
        AndroidLogFolderWriter writer = AndroidLogFolderWriter.open(context, "aho-gpt-log-20000101000000.txt");
        require(writer != null, "Trim fixture must be writable");
        String path = remember(writer.path());
        try {
            StringBuilder large = new StringBuilder();
            for (int index = 0; index < 300_000; index++) { large.append("diagnostic line\n"); }
            require(writer.append(large), "Large fixture must append");
            require(writer.append("tail marker\n"), "Append must trim old lines");
            writer.flush();
            String content = read(path);
            require(content.length() <= 3 * 1024 * 1024 + 12, "Trimmed file must keep about 3 MiB");
            require(content.startsWith("diagnostic line\n"), "Trim must preserve line boundaries");
            require(content.endsWith("tail marker\n"), "Trim must preserve the latest event");
        } finally { writer.close(); }
    }
    private void unavailable() {
        AppOutputStorageSettings.select(context, "missing-test-card", null);
        require(!AppLogger.setLoggingEnabled(context, true), "Unavailable storage must refuse logging");
        require(!AppLogger.isLoggingEnabled(), "Failure must keep logging off");
        AppLogger.anomaly(context, TAG, "Unavailable anomaly", null);
        require(AppLogger.getLogFilePath(context).contains("missing-test-card"), "Failure must retain selected storage");
        require(AppOutputStorageSettings.useSdCard(context), "Failure must not select fallback storage");
    }
    private void scopedDeletion() throws Exception {
        File folder = new File(context.getCacheDir(), "log_test_" + System.nanoTime());
        require(folder.mkdir(), "Fixture folder must be created");
        File generated = new File(folder, "aho-gpt-log-20000101000000.txt");
        File unrelated = new File(folder, "notes.txt");
        File directory = new File(folder, "aho-gpt-log-20000101000001.txt");
        try {
            require(generated.createNewFile() && unrelated.createNewFile() && directory.mkdir(), "Fixtures must exist");
            AndroidOutputFolderCleaner.clear(context, AndroidOutputDestination.file(folder).token());
            require(!generated.exists(), "Generated log must be deleted");
            require(unrelated.exists() && directory.isDirectory() && folder.isDirectory(), "Other contents must survive");
        } finally {
            remove(generated); remove(unrelated); remove(directory); remove(folder);
        }
    }
    private String remember(String path) { paths.add(path); return path; }
    private String read(String path) throws IOException {
        if (path.startsWith("content:")) {
            try (var input = context.getContentResolver().openInputStream(android.net.Uri.parse(path))) {
                return readStream(input);
            }
        }
        try (var input = new FileInputStream(path)) { return readStream(input); }
    }
    private String readStream(java.io.InputStream input) throws IOException {
        if (input == null) { throw new IOException("Test file unavailable"); }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) { out.write(buffer, 0, count); }
        return out.toString(java.nio.charset.StandardCharsets.UTF_8.name());
    }
    private void cleanup() throws IOException {
        for (String path : paths) {
            if (path.startsWith("content:")) { context.getContentResolver().delete(android.net.Uri.parse(path), null, null); }
            else { remove(new File(path)); }
        }
    }
    private void remove(File file) throws IOException {
        if (file.exists() && !file.delete()) { throw new IOException("Could not remove test fixture"); }
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
