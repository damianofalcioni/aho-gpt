package aho.gpt.android.logging.storage;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Exercises file deletion and destination identity without Android services. */
public class OutputDestinationTest {
    private static final String LOG_NAME = "aho-gpt-log-20000101000000.txt";
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void deletionUsesCapturedFolderAndPreservesOtherContents() throws Exception {
        File selected = temporary.newFolder("selected");
        File other = temporary.newFolder("other");
        File log = new File(selected, LOG_NAME);
        File unrelated = new File(selected, "notes.txt");
        File subfolder = new File(selected, "nested");
        File otherLog = new File(other, LOG_NAME);
        assertTrue(log.createNewFile());
        assertTrue(unrelated.createNewFile());
        assertTrue(subfolder.mkdir());
        assertTrue(otherLog.createNewFile());
        AndroidOutputFolderCleaner.clear(null, AndroidOutputDestination.file(selected));
        assertFalse(log.exists());
        assertTrue(unrelated.isFile());
        assertTrue(subfolder.isDirectory());
        assertTrue(selected.isDirectory());
        assertTrue(otherLog.isFile());
    }

    @Test public void changingFileDestinationRetiresTheOldLocation() throws Exception {
        File first = temporary.newFolder();
        File second = temporary.newFolder();
        AndroidOutputDestination selected = AndroidOutputDestination.file(first);
        assertTrue(selected.sameLocation(AndroidOutputDestination.file(new File(first.getAbsolutePath()))));
        assertFalse(selected.sameLocation(AndroidOutputDestination.file(second)));
    }

    @Test public void downloadIdentityUsesVolumeAndPathRatherThanDisplayRoot() {
        AndroidOutputDestination selected = AndroidOutputDestination.downloads("abcd-1234", "/storage/ABCD-1234");
        AndroidOutputDestination alias = AndroidOutputDestination.downloads("abcd-1234", "/mnt/media_rw/ABCD-1234");
        assertTrue(selected.sameLocation(alias));
        assertFalse(selected.sameLocation(AndroidOutputDestination.downloads("efgh-5678", "/storage/EFGH-5678")));
    }
}
