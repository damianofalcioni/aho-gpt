package aho.gpt.android.logging;

import android.content.Context;

import aho.gpt.logging.LogTail;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import aho.gpt.android.logging.storage.AndroidOutputStorage;
import aho.gpt.android.logging.storage.AndroidOutputFile;
import aho.gpt.android.logging.storage.AppOutputFolderSettings.Kind;

/** A single selected Downloads destination; no fallback or app-local mirror is created. */
public final class AndroidLogFolderWriter {
    private static final long MAX_BYTES = 4L * 1024L * 1024L;
    private static final long KEEP_BYTES = 3L * 1024L * 1024L;
    private final Context context;
    private final AndroidOutputFile file;
    private OutputStream stream;
    private long size;
    private boolean published;
    private long lastFlushNanos;

    private AndroidLogFolderWriter(Context context, AndroidOutputFile file, OutputStream stream) {
        this.context = context;
        this.file = file;
        this.stream = new BufferedOutputStream(stream, 8192);
    }

    public static AndroidLogFolderWriter open(Context context, String name) {
        AndroidOutputFile file = null;
        try {
            file = AndroidOutputStorage.create(context, Kind.LOGS, "text/plain", name);
            return new AndroidLogFolderWriter(context.getApplicationContext(), file, file.open());
        } catch (IOException | RuntimeException e) {
            if (file != null) {
                file.removeQuietly();
            }
            return null;
        }
    }

    public boolean append(CharSequence block) {
        try {
            if (!file.destination.token().equals(AndroidOutputStorage.requested(context, Kind.LOGS).token())) {
                close();
                return false;
            }
            if (size > MAX_BYTES) {
                trim();
            }
            byte[] bytes = block.toString().getBytes(StandardCharsets.UTF_8);
            stream.write(bytes);
            // Batch debug writes; flush on the next append after two seconds, without a timer.
            if (!published || System.nanoTime() - lastFlushNanos >= 2_000_000_000L) {
                flush();
            }
            size += bytes.length;
            publish();
            return true;
        } catch (IOException | RuntimeException e) {
            if (!published) {
                file.removeQuietly();
            }
            close();
            return false;
        }
    }

    private void publish() throws IOException {
        if (!published) {
            file.publish();
            published = true;
        }
    }

    private void trim() throws IOException {
        close();
        byte[] tail = readTail();
        stream = new BufferedOutputStream(file.open(), 8192);
        stream.write(tail);
        size = tail.length;
    }

    private byte[] readTail() throws IOException {
        try (InputStream in = file.read()) {
            return LogTail.read(in, size - KEEP_BYTES);
        }
    }

    public String path() {
        return file.path();
    }

    public void flush() throws IOException {
        if (stream != null) {
            stream.flush();
            lastFlushNanos = System.nanoTime();
        }
    }

    public void close() {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // Closing unavailable storage must not stop the app.
        }
        stream = null;
    }
}
