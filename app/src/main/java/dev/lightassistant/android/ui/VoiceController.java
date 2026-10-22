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
    private final VoicePanel panel;
    private final Conversation conversation = new Conversation();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SpeechInput input;
    private final SpeechOutput output;
    private final AnswerOverlay overlay;
    private Future<?> work;
    private boolean foreground;
    private boolean closed;
    private boolean followUp;
    private boolean busy;
    private long speechTime;
    private String lastAnswer = "";

    public VoiceController(Activity activity, VoicePanel panel) {
        this.activity = activity; this.panel = panel;
        app = (AssistantApplication) activity.getApplication();
        input = SpeechInput.available(activity) ? new SpeechInput(activity, this) : null;
        output = new SpeechOutput(activity);
        overlay = new AnswerOverlay(app);
    }

    public void foreground(boolean visible) {
        foreground = visible;
        if (!visible && !busy) { stopListening(); }
    }

    public void listen() {
        if (closed || busy) { return; }
        output.stop(); overlay.close(); main.removeCallbacksAndMessages(null);
        if (!canListen()) { return; }
        followUp = conversation.continues(SystemClock.elapsedRealtime());
        speechTime = SystemClock.elapsedRealtime();
        panel.show(activity.getString(R.string.listening), followUp ? lastAnswer + "\n\n" + activity.getString(R.string.follow_up) : "");
        try {
            input.cancel(); input.start();
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
        panel.show(activity.getString(R.string.thinking), text);
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
        String message = answer.isEmpty() ? activity.getString(R.string.no_answer) : answer;
        lastAnswer = message;
        panel.show(activity.getString(R.string.answer), message);
        if (!foreground) { overlay.show(message); }
        output.speak(message, () -> {
            if (closed) { return; }
            conversation.answered(SystemClock.elapsedRealtime());
            if (foreground) { listen(); }
            else { main.postDelayed(conversation::clear, 8000); }
        });
    }

    @Override public void error(int error) {
        main.removeCallbacksAndMessages(null);
        if (followUp && (error == 6 || error == 7)) { expired(); return; }
        fail(activity.getString(R.string.speech_error, error));
    }
    private void expired() {
        stopListening(); conversation.clear();
        panel.show(activity.getString(R.string.ready), lastAnswer + "\n\n" + activity.getString(R.string.session_ended));
    }
    private void stopListening() { main.removeCallbacksAndMessages(null); if (input != null) { input.cancel(); } }
    private void fail(String message) {
        if (closed) { return; }
        busy = false; conversation.clear(); stopListening(); output.stop();
        panel.show(activity.getString(R.string.unavailable), message == null ? activity.getString(R.string.try_again) : message);
    }
    public void close() {
        closed = true;
        if (work != null) { work.cancel(true); }
        app.http().cancelResponse();
        stopListening(); output.close(); overlay.close();
        if (input != null) { input.close(); }
    }
}
