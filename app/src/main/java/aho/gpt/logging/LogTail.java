package aho.gpt.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Retains complete UTF-8 lines after the size boundary. */
public final class LogTail {
    private LogTail() { }
    public static byte[] read(InputStream input, long skipBytes) throws IOException {
        long remaining = skipBytes;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped == 0) {
                if (input.read() == -1) { return new byte[0]; }
                skipped = 1;
            }
            remaining -= skipped;
        }
        if (skipBytes > 0) { skipLine(input); }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) { out.write(buffer, 0, count); }
        return out.toByteArray();
    }
    private static void skipLine(InputStream input) throws IOException {
        int value;
        do { value = input.read(); } while (value != -1 && value != '\n');
    }
}
