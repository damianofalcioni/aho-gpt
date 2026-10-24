package aho.gpt.android.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.speech.SpeechInput;
import aho.gpt.android.speech.SpeechOutput;
import aho.gpt.android.speech.SpeechAvailability;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.core.Conversation;

public final class VoiceController implements SpeechInput.Listener {
    private final Activity activity;
    private final AssistantApplication app;
    private final VoicePresentation presentation;
    private final Conversation conversation = new Conversation();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SpeechInput input;
    private final SpeechOutput output;
    private final VoiceRequest request;
    private boolean foreground;
    private boolean closed;
    private boolean followUp;
    private boolean busy;
    private long speechTime;

    public VoiceController(Activity activity, VoicePanel panel) {
        this.activity = activity; presentation = new VoicePresentation(activity, panel);
        app = (AssistantApplication) activity.getApplication();
        request = new VoiceRequest(activity, conversation);
        input = SpeechInput.available(activity) ? new SpeechInput(activity, this) : null;
        output = new SpeechOutput(activity);
    }

    public void foreground(boolean visible) {
        foreground = visible;
        if (!visible && !busy) { stopListening(); }
    }

    public void listen() {
        if (closed || busy) { return; }
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
        if (!SpeechPreferences.enabled(activity, SpeechPreferences.INPUT)) {
            fail(activity.getString(R.string.stt_disabled)); return false;
        }
        if (!SpeechAvailability.googleAppEnabled(activity)) {
            fail(activity.getString(R.string.speech_provider_disabled)); return false;
        }
        if (input == null) { fail(activity.getString(R.string.speech_unavailable)); return false; }
        if (app.model().isEmpty()) { fail(activity.getString(R.string.setup_required)); return false; }
        return true;
    }

    @Override public void began() {
        speechTime = SystemClock.elapsedRealtime();
        main.removeCallbacksAndMessages(null);
        main.postDelayed(this::expired, 20_000);
    }
    @Override public void result(String text) {
        if (closed || busy) { return; }
        main.removeCallbacksAndMessages(null); busy = true;
        presentation.thinking(text);
        request.start(text, speechTime, this::answered, this::fail);
    }

    private void answered(String answer) {
        if (closed) { return; }
        busy = false;
        String message = presentation.answered(answer, foreground);
        if (!SpeechPreferences.enabled(activity, SpeechPreferences.OUTPUT)) { conversation.clear(); return; }
        output.speak(message, presentation::playbackStarted, this::playbackCompleted, this::playbackFailed);
    }

    private void playbackCompleted() {
        if (closed) { return; }
        presentation.playbackStopped();
        conversation.answered(SystemClock.elapsedRealtime());
        if (foreground) { listen(); }
        else { main.postDelayed(conversation::clear, 8000); }
    }
    private void playbackFailed(String reason) {
        conversation.clear();
        presentation.playbackFailed(reason, foreground);
    }

    @Override public void error(int error) {
        main.removeCallbacksAndMessages(null);
        if (followUp && (error == 6 || error == 7)) { expired(); return; }
        fail(SpeechInput.errorText(activity, error));
    }
    private void expired() {
        stopListening(); conversation.clear();
        presentation.expired();
    }
    private void stopListening() {
        presentation.listeningStopped();
        main.removeCallbacksAndMessages(null); if (input != null) { input.cancel(); }
    }
    private void fail(String message) {
        if (closed) { return; }
        request.cancel();
        busy = false; conversation.clear(); stopListening(); output.stop();
        presentation.failed(message);
    }
    public void close() {
        if (closed) { return; }
        closed = true;
        request.cancel();
        stopListening(); output.close(); presentation.closeOverlay();
        if (input != null) { input.close(); }
    }
}
