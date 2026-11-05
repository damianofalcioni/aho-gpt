package aho.gpt;

import aho.gpt.custom.AudioMode;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpeechRoutingTest {
    @Test public void serverModesIgnoreDisabledPlatformSpeechSwitches() {
        for (AudioMode mode : new AudioMode[]{AudioMode.SPEECH, AudioMode.REALTIME, AudioMode.LIVE}) {
            assertTrue(mode.speechEnabled(false));
            assertTrue(mode.speechEnabled(true));
        }
    }
    @Test public void platformModeHonorsSpeechSwitches() {
        assertFalse(AudioMode.PLATFORM.speechEnabled(false));
        assertTrue(AudioMode.PLATFORM.speechEnabled(true));
    }
    @Test public void onlyTranscriptionModesUseTheRecognitionLanguage() {
        assertTrue(AudioMode.PLATFORM.usesRecognitionLanguage());
        assertTrue(AudioMode.SPEECH.usesRecognitionLanguage());
        assertFalse(AudioMode.REALTIME.usesRecognitionLanguage());
        assertFalse(AudioMode.LIVE.usesRecognitionLanguage());
    }
}
