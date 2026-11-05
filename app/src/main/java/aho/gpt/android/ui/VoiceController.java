package aho.gpt.android.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.SpeechRecognizer;
import aho.gpt.R;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.speech.SpeechInput;
import aho.gpt.android.speech.VoiceSpeech;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.core.Conversation;
import aho.gpt.core.VoiceLifecycle;

public final class VoiceController implements SpeechInput.Listener {
    private static final String TAG = "Voice";
    private static final String SPEECH_TAG = "Speech";
    private static final long INITIAL_LISTENING_MS = 15_000;
    private static final long SPEECH_TIMEOUT_MS = 20_000;
    private final Activity activity;
    private final VoicePresentation presentation;
    private final Conversation conversation = new Conversation();
    private final VoiceLifecycle lifecycle = new VoiceLifecycle();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final VoiceSpeech speech;
    private final VoiceRequest request;
    private final VoiceDismissal dismissal;
    private final VoiceAccountWatch accountWatch;
    private long speechTime;

    VoiceController(Activity activity, VoicePanel panel, VoiceDismissal dismissal) {
        this.activity = activity;
        presentation = new VoicePresentation(activity, panel);
        this.dismissal = dismissal;
        request = new VoiceRequest(activity, conversation);
        speech = new VoiceSpeech(activity, this);
        accountWatch = new VoiceAccountWatch(activity, this::fail);
    }

    public void foreground(boolean visible) {
        if (lifecycle.closed()) { return; }
        AppLogger.d(TAG, "Foreground=" + visible + " state=" + lifecycle.state());
        if (lifecycle.foreground(visible)) {
            stopListening();
            dismissal.schedule();
        }
        presentation.foreground(visible);
        if (lifecycle.listening()) { listen(); }
    }

    public void listen() {
        if (!lifecycle.requestListening()) { return; }
        AppLogger.i(SPEECH_TAG, "Listening started");
        prepareListening();
        int issue = VoiceReadiness.issue(activity, speech.available());
        if (issue != 0) { fail(activity.getString(issue)); return; }
        boolean followUp = conversation.continues(SystemClock.elapsedRealtime());
        speechTime = SystemClock.elapsedRealtime();
        try {
            speech.cancel();
            speech.start(presentation.listening(followUp));
            long timeout = followUp ? conversation.remaining(speechTime) : INITIAL_LISTENING_MS;
            main.postDelayed(this::expired, timeout);
        } catch (RuntimeException exception) {
            AppLogger.w(SPEECH_TAG, "Listening could not start", exception);
            fail(exception.getMessage());
        }
    }

    private void prepareListening() {
        dismissal.cancel();
        speech.stop();
        presentation.closeOverlay();
        main.removeCallbacksAndMessages(null);
    }

    @Override public void began() {
        if (!lifecycle.listening()) { return; }
        speechTime = SystemClock.elapsedRealtime();
        main.removeCallbacksAndMessages(null);
        main.postDelayed(this::expired, SPEECH_TIMEOUT_MS);
    }
    @Override public void processing() {
        if (!lifecycle.listening()) { return; }
        main.removeCallbacksAndMessages(null); presentation.transcribing();
    }
    @Override public void result(String text) {
        if (!lifecycle.recognized()) { return; }
        AppLogger.i(SPEECH_TAG, "Recognition completed");
        main.removeCallbacksAndMessages(null);
        presentation.thinking(text);
        request.start(text, speechTime, this::progress, this::answered, this::fail);
    }
    @Override public void audio(byte[] wav) {
        if (!lifecycle.recognized()) { return; }
        main.removeCallbacksAndMessages(null); presentation.listeningStopped(); presentation.thinking("");
        request.startAudio(wav, speechTime, this::progress, this::answered, this::fail);
    }

    private void progress(boolean executingTool) {
        if (!lifecycle.progress(executingTool)) { return; }
        dismissal.cancel();
        presentation.progress(executingTool);
    }

    private void answered(String answer) { answered(answer, null); }
    private void answered(String answer, byte[] pcm) {
        if (!lifecycle.answered()) { return; }
        String message = presentation.answered(answer);
        if (!SpeechPreferences.voiceEnabled(activity, SpeechPreferences.OUTPUT)) {
            AppLogger.d(SPEECH_TAG, "Playback disabled");
            playbackCompleted();
            return;
        }
        speech.answer(message, pcm, presentation::playbackStarted, this::playbackCompleted, this::playbackFailed);
    }

    private void playbackCompleted() {
        if (!lifecycle.playbackCompleted()) { return; }
        presentation.playbackStopped();
        conversation.answered(SystemClock.elapsedRealtime());
        if (lifecycle.listening()) { listen(); }
        else { dismissal.schedule(); }
    }
    private void playbackFailed(String reason) {
        AppLogger.w(SPEECH_TAG, "Playback failed");
        lifecycle.finished();
        conversation.clear();
        presentation.playbackFailed(reason);
        dismissal.schedule();
    }

    @Override public void error(int error) {
        AppLogger.w(SPEECH_TAG, "Recognition error=" + error);
        main.removeCallbacksAndMessages(null);
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            expired();
            return;
        }
        fail(SpeechInput.errorText(activity, error));
    }
    private void expired() {
        AppLogger.i(TAG, "Listening finished");
        lifecycle.finished();
        stopListening();
        conversation.clear();
        presentation.finished();
        dismissal.schedule();
    }
    private void stopListening() {
        presentation.listeningStopped();
        main.removeCallbacksAndMessages(null);
        speech.cancel();
    }
    private void fail(String message) {
        if (lifecycle.closed()) { return; }
        AppLogger.w(TAG, "Session failed state=" + lifecycle.state());
        lifecycle.finished();
        request.cancel();
        conversation.clear();
        stopListening();
        speech.stop();
        presentation.failed(message);
        dismissal.schedule();
    }
    public void close() {
        if (!lifecycle.close()) { return; }
        accountWatch.close();
        AppLogger.i(TAG, "Session closed");
        request.cancel();
        conversation.clear();
        dismissal.cancel();
        stopListening();
        speech.close();
        presentation.closeOverlay();
    }
}
