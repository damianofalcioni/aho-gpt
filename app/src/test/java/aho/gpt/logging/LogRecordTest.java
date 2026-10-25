package aho.gpt.logging;

import java.util.Date;
import org.junit.Test;
import static org.junit.Assert.*;

public class LogRecordTest {
    @Test public void namesMatchGeneratedFilesAndProviderCollisions() {
        assertTrue(LogRecord.isLogFile(LogRecord.fileName(new Date(0))));
        assertTrue(LogRecord.isLogFile("aho-gpt-log-20261025123456-2.txt"));
        assertTrue(LogRecord.isLogFile("aho-gpt-log-20261025123456 (1).txt"));
        assertTrue(LogRecord.isLogFile("aho-gpt-log-20261025123456-2 (1).txt"));
    }
    @Test public void deletionExcludesOtherFiles() {
        assertFalse(LogRecord.isLogFile(null));
        assertFalse(LogRecord.isLogFile("notes.txt"));
        assertFalse(LogRecord.isLogFile("aho-gpt-log-20261025123456.txt.backup"));
        assertFalse(LogRecord.isLogFile("aho-gpt-route-20261025123456.gpx"));
        assertFalse(LogRecord.isLogFile("vibro-navigator-log-20261025123456.txt"));
        assertFalse(LogRecord.isLogFile("../aho-gpt-log-20261025123456.txt"));
    }
    @Test public void exceptionsNeverExposeMessagesCausesOrSuppressedMessages() {
        Exception failure = new IllegalStateException("private answer", new Exception("Bearer secret"));
        failure.addSuppressed(new Exception("https://auth.example/?code=private"));
        StringBuilder out = new StringBuilder();
        LogRecord.appendFailure(out, failure);
        assertTrue(out.toString().contains("java.lang.IllegalStateException"));
        assertTrue(out.toString().contains("LogRecordTest.java"));
        assertFalse(out.toString().contains("private"));
        assertFalse(out.toString().contains("Bearer"));
        assertFalse(out.toString().contains("https://"));
    }
    @Test public void cyclicCausesAreBounded() {
        Exception first = new Exception("secret one");
        Exception second = new Exception("secret two", first);
        first.initCause(second);
        StringBuilder out = new StringBuilder();
        LogRecord.appendFailure(out, first);
        assertTrue(out.length() > 0);
        assertTrue(out.length() < 100_000);
        assertFalse(out.toString().contains("secret"));
    }
    @Test public void injectedNewlinesCannotForgeEntries() {
        String entry = LogRecord.prefix("INFO", "Test\r\nERROR", "Started\nforged").toString();
        assertEquals(1, entry.chars().filter(value -> value == '\n').count());
        assertTrue(entry.contains("Started\\nforged"));
    }
}
