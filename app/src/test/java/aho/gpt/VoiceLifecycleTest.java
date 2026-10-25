package aho.gpt;

import aho.gpt.core.VoiceLifecycle;
import org.junit.Test;
import static org.junit.Assert.*;

public class VoiceLifecycleTest {
    @Test public void foregroundDoesNotAcquireMicrophoneBeforeInvocationOrPermission() {
        VoiceLifecycle voice = new VoiceLifecycle();
        assertFalse(voice.foreground(true));
        assertFalse(voice.listening());
        assertTrue(voice.requestListening());
        assertTrue(voice.listening());
    }
    @Test public void invocationBeforeResumeAutomaticallyListensWhenForeground() {
        VoiceLifecycle voice = new VoiceLifecycle();
        assertFalse(voice.requestListening());
        voice.foreground(true);
        assertTrue(voice.listening());
    }
    @Test public void completedAnswerAutomaticallyAllowsAnotherSpokenTurn() {
        VoiceLifecycle voice = answering();
        assertTrue(voice.playbackCompleted());
        assertTrue(voice.listening());
        assertTrue(voice.recognized());
    }
    @Test public void backgroundCompletionWaitsForForegroundBeforeListening() {
        VoiceLifecycle voice = answering();
        assertFalse(voice.foreground(false));
        assertTrue(voice.playbackCompleted());
        assertFalse(voice.listening());
        assertFalse(voice.recognized());
        voice.foreground(true);
        assertTrue(voice.listening());
    }
    @Test public void pausedRecognitionRestartsOnResumeAndIgnoresLateResults() {
        VoiceLifecycle voice = listening();
        assertTrue(voice.foreground(false));
        assertFalse(voice.recognized());
        assertFalse(voice.listening());
        voice.foreground(true);
        assertTrue(voice.listening());
        assertTrue(voice.recognized());
    }
    @Test public void silenceOrFailureEndsListeningWithoutAnAutomaticRetryLoop() {
        VoiceLifecycle voice = listening();
        voice.finished();
        voice.foreground(false); voice.foreground(true);
        assertFalse(voice.listening());
        assertFalse(voice.recognized());
        assertTrue(voice.requestListening());
    }
    @Test public void failedPlaybackCannotReopenFollowUpListening() {
        VoiceLifecycle voice = answering();
        voice.finished();
        assertFalse(voice.playbackCompleted());
        voice.foreground(false); voice.foreground(true);
        assertFalse(voice.listening());
    }
    @Test public void microphoneDoesNotInterruptProcessingAndCloseRejectsCallbacks() {
        VoiceLifecycle voice = listening();
        assertTrue(voice.recognized());
        assertFalse(voice.requestListening());
        assertTrue(voice.close());
        assertFalse(voice.close());
        assertFalse(voice.answered());
        assertFalse(voice.playbackCompleted());
        assertFalse(voice.requestListening());
        voice.foreground(true);
        assertFalse(voice.listening());
    }
    @Test public void toolsStayActiveAcrossForegroundChangesAndReturnToModel() {
        VoiceLifecycle voice = listening(); voice.recognized();
        assertEquals(VoiceLifecycle.State.THINKING, voice.state());
        assertTrue(voice.progress(true));
        assertEquals(VoiceLifecycle.State.EXECUTING_TOOL, voice.state());
        assertFalse(voice.requestListening());
        assertFalse(voice.answered());
        assertFalse(voice.foreground(false));
        assertTrue(voice.working());
        assertTrue(voice.progress(false));
        assertEquals(VoiceLifecycle.State.THINKING, voice.state());
        assertTrue(voice.answered());
        assertTrue(voice.playbackCompleted());
        assertFalse(voice.listening());
        voice.foreground(true);
        assertTrue(voice.listening());
    }
    @Test public void retiredToolProgressCannotRestartAClosedOrFailedSession() {
        VoiceLifecycle voice = listening(); voice.recognized(); voice.progress(true);
        voice.finished();
        assertFalse(voice.progress(false));
        assertFalse(voice.answered());
        voice.close();
        assertFalse(voice.progress(true));
        assertFalse(voice.requestListening());
    }

    private VoiceLifecycle listening() {
        VoiceLifecycle voice = new VoiceLifecycle();
        voice.foreground(true); voice.requestListening();
        return voice;
    }
    private VoiceLifecycle answering() {
        VoiceLifecycle voice = listening();
        voice.recognized(); voice.answered();
        return voice;
    }
}
