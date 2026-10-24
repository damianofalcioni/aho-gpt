package aho.gpt.android.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.SpeechRecognizer;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.speech.SpeechInput;
import aho.gpt.android.speech.SpeechOutput;
import aho.gpt.android.speech.SpeechAvailability;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.core.Conversation;
import aho.gpt.core.VoiceLifecycle;

public final class VoiceController implements SpeechInput.Listener {
    private final Activity activity;
    private final AssistantApplication app;
    private final VoicePresentation presentation;
    private final Conversation conversation = new Conversation();
    private final VoiceLifecycle lifecycle = new VoiceLifecycle();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SpeechInput input;
    private final SpeechOutput output;
    private final VoiceRequest request;
    private final VoiceDismissal dismissal;
    private boolean followUp;
    private long speechTime;

    VoiceController(Activity activity, VoicePanel panel, VoiceDismissal dismissal) {
        this.activity = activity; presentation = new VoicePresentation(activity, panel);
        this.dismissal = dismissal;
        app = (AssistantApplication) activity.getApplication();
        request = new VoiceRequest(activity, conversation);
        input = SpeechInput.available(activity) ? new SpeechInput(activity, this) : null;
        output = new SpeechOutput(activity);
    }

    public void foreground(boolean visible) {
        if (lifecycle.foreground(visible)) { stopListening(); dismissal.schedule(); }
        if (lifecycle.listening()) { listen(); }
    }

    public void listen() {
        if (!lifecycle.requestListening()) { return; }
        dismissal.cancel();
        output.stop(); presentation.closeOverlay(); main.removeCallbacksAndMessages(null);
        if (!canListen()) { return; }
        followUp = conversation.continues(SystemClock.elapsedRealtime());
        speechTime = SystemClock.elapsedRealtime();
        try {
            input.cancel(); input.start(presentation.listening(followUp));
            long timeout = followUp ? conversation.remaining(speechTime) : 15_000;
            main.postDelayed(this::expired, timeout);
        } catch (RuntimeException exception) { fail(exception.getMessage()); }
    }

    private boolean canListen() {
        int issue = SpeechAvailability.inputIssue(activity, input != null);
        if (issue != 0) { fail(activity.getString(issue)); return false; }
        if (app.model().isEmpty()) { fail(activity.getString(R.string.setup_required)); return false; }
        return true;
    }

    @Override public void began() {
        if (!lifecycle.listening()) { return; }
        speechTime = SystemClock.elapsedRealtime();
        main.removeCallbacksAndMessages(null);
        main.postDelayed(this::expired, 20_000);
    }
    @Override public void result(String text) {
        if (!lifecycle.recognized()) { return; }
        main.removeCallbacksAndMessages(null);
        presentation.thinking(text);
        request.start(text, speechTime, this::answered, this::fail);
    }

    private void answered(String answer) {
        if (!lifecycle.answered()) { return; }
        String message = presentation.answered(answer, lifecycle.visible());
        if (!SpeechPreferences.enabled(activity, SpeechPreferences.OUTPUT)) { playbackCompleted(); return; }
        output.speak(message, presentation::playbackStarted, this::playbackCompleted, this::playbackFailed);
    }

    private void playbackCompleted() {
        if (!lifecycle.playbackCompleted()) { return; }
        presentation.playbackStopped();
        conversation.answered(SystemClock.elapsedRealtime());
        if (lifecycle.listening()) { listen(); }
        else { dismissal.schedule(); }
    }
    private void playbackFailed(String reason) {
        lifecycle.finished(); conversation.clear();
        presentation.playbackFailed(reason, lifecycle.visible());
        dismissal.schedule();
    }

    @Override public void error(int error) {
        main.removeCallbacksAndMessages(null);
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) { expired(); return; }
        fail(SpeechInput.errorText(activity, error));
    }
    private void expired() {
        lifecycle.finished(); stopListening(); conversation.clear();
        presentation.finished();
        dismissal.schedule();
    }
    private void stopListening() {
        presentation.listeningStopped();
        main.removeCallbacksAndMessages(null); if (input != null) { input.cancel(); }
    }
    private void fail(String message) {
        if (lifecycle.closed()) { return; }
        lifecycle.finished();
        request.cancel();
        conversation.clear(); stopListening(); output.stop();
        presentation.failed(message);
        dismissal.schedule();
    }
    public void close() {
        if (!lifecycle.close()) { return; }
        request.cancel();
        conversation.clear(); dismissal.cancel();
        stopListening(); output.close(); presentation.closeOverlay();
        if (input != null) { input.close(); }
    }
}
