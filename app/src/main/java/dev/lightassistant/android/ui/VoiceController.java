package dev.lightassistant.android.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import dev.lightassistant.R;
import dev.lightassistant.android.AssistantApplication;
import dev.lightassistant.android.speech.SpeechInput;
import dev.lightassistant.android.speech.SpeechOutput;
import dev.lightassistant.android.tools.DeviceTools;
import dev.lightassistant.android.tools.IntentExecutor;
import dev.lightassistant.core.Conversation;
import dev.lightassistant.openai.AgentLoop;
import java.util.concurrent.Future;

public final class VoiceController implements SpeechInput.Listener {
    private final Activity activity;
    private final AssistantApplication app;
    private final VoicePresentation presentation;
    private final Conversation conversation = new Conversation();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SpeechInput input;
    private final SpeechOutput output;
    private Future<?> work;
    private boolean foreground;
    private boolean closed;
    private boolean followUp;
    private boolean busy;
    private long speechTime;

    public VoiceController(Activity activity, VoicePanel panel) {
        this.activity = activity; presentation = new VoicePresentation(activity, panel);
        app = (AssistantApplication) activity.getApplication();
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
        if (closed) { return; }
        main.removeCallbacksAndMessages(null); busy = true;
        presentation.thinking(text);
        work = app.worker().submit(() -> run(text));
    }

    private void run(String text) {
        try {
            conversation.begin(text, speechTime);
            DeviceTools tools = new DeviceTools(app.catalog(), new IntentExecutor(activity));
            AgentLoop agent = new AgentLoop(body -> app.http().response(app.auth().accessToken(), body), tools);
            String answer = agent.run(conversation, app.model());
            main.post(() -> answered(answer));
        } catch (Exception exception) {
            main.post(() -> fail(exception.getMessage()));
        }
    }

    private void answered(String answer) {
        if (closed) { return; }
        busy = false;
        String message = presentation.answered(answer, foreground);
        output.speak(message, this::playbackCompleted, this::playbackFailed);
    }

    private void playbackCompleted() {
        if (closed) { return; }
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
    private void stopListening() { main.removeCallbacksAndMessages(null); if (input != null) { input.cancel(); } }
    private void fail(String message) {
        if (closed) { return; }
        busy = false; conversation.clear(); stopListening(); output.stop();
        presentation.failed(message);
    }
    public void close() {
        closed = true;
        if (work != null) { work.cancel(true); }
        app.http().cancelResponse();
        stopListening(); output.close(); presentation.closeOverlay();
        if (input != null) { input.close(); }
    }
}
