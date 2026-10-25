package aho.gpt.logging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class LogTailTest {
    @Test public void trimsToCompleteUtf8Lines() throws Exception {
        byte[] bytes = "old line\nè漢字\nnew line\n".getBytes(StandardCharsets.UTF_8);
        byte[] tail = LogTail.read(new ByteArrayInputStream(bytes), 12);
        assertEquals("new line\n", new String(tail, StandardCharsets.UTF_8));
    }
    @Test public void smallLogKeepsFirstLine() throws Exception {
        byte[] bytes = "first\nlast\n".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(bytes, LogTail.read(new ByteArrayInputStream(bytes), 0));
    }
    @Test public void nonSkippingProviderStillMakesProgress() throws Exception {
        InputStream provider = new ByteArrayInputStream("old\nkeep\n".getBytes(StandardCharsets.UTF_8)) {
            @Override public long skip(long count) { return 0; }
        };
        assertEquals("keep\n", new String(LogTail.read(provider, 2), StandardCharsets.UTF_8));
    }
    @Test public void truncatedProviderReturnsEmptyTail() throws Exception {
        assertEquals(0, LogTail.read(new ByteArrayInputStream(new byte[2]), 100).length);
    }
    @Test public void keepsAboutThreeMiBAfterFourMiBThreshold() throws Exception {
        byte[] bytes = ("diagnostic line\n".repeat(300_000)).getBytes(StandardCharsets.UTF_8);
        byte[] tail = LogTail.read(new ByteArrayInputStream(bytes), bytes.length - 3L * 1024 * 1024);
        assertTrue(tail.length <= 3 * 1024 * 1024);
        assertTrue(tail.length >= 3 * 1024 * 1024 - 16);
        assertEquals('d', tail[0]);
    }
}
