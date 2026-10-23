package aho.gpt;

import aho.gpt.core.CapabilityCategory;
import aho.gpt.core.CapabilityMetadata;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class CapabilityCategoryTest {
    private static final String VIEW = "android.intent.action.VIEW";
    private static final String ACTIVITY = "activity";

    @Test public void distinguishesLauncherAndWebHandlers() {
        assertEquals(CapabilityCategory.OPEN_APP, metadata(ACTIVITY, "android.intent.action.MAIN", true).category());
        assertEquals(CapabilityCategory.OPEN_LINK, metadata(ACTIVITY, VIEW, false, "https").category());
        assertEquals(CapabilityCategory.VIEW_CONTENT, metadata(ACTIVITY, VIEW, false, "geo").category());
    }

    @Test public void standardContractsIncludeExecutionLimitations() {
        CapabilityCategory share = metadata(ACTIVITY, "android.intent.action.SEND", false).category();
        assertEquals(CapabilityCategory.SHARE_CONTENT, share);
        assertTrue(share.usage.contains("unsupported"));
        assertEquals(CapabilityCategory.CAPTURE_PHOTO,
                metadata(ACTIVITY, "android.media.action.IMAGE_CAPTURE", false).category());
        assertTrue(CapabilityCategory.CAPTURE_PHOTO.usage.contains("does not prove capture"));
    }

    @Test public void leavesCustomAndNonActivitySemanticsUnknown() {
        assertEquals(CapabilityCategory.CUSTOM, metadata(ACTIVITY, "example.SCAN_DOCUMENT", false).category());
        assertEquals(CapabilityCategory.CUSTOM, metadata(ACTIVITY, "SEND", false).category());
        assertEquals(CapabilityCategory.CUSTOM, metadata("service", VIEW, false, "https").category());
        assertEquals(CapabilityCategory.CUSTOM, metadata("broadcast", "android.intent.action.DIAL", false).category());
        assertEquals(CapabilityCategory.CUSTOM, metadata(ACTIVITY, "android.intent.action.MAIN", false).category());
    }

    private CapabilityMetadata metadata(String kind, String action, boolean launcher, String... schemes) {
        return new CapabilityMetadata("Scanner Camera Browser", "example.app", "example.app/Main", kind,
                action, launcher, Arrays.asList(schemes), Collections.emptyList());
    }
}
