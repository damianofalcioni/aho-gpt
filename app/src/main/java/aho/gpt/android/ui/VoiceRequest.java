package aho.gpt.android.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.tools.DeviceTools;
import aho.gpt.android.tools.IntentExecutor;
import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.openai.AgentLoop;
import java.util.concurrent.Future;

/** Owns inference lifetime and suppresses callbacks from retired spoken turns. */
final class VoiceRequest {
    private static final String TAG = "Voice";
    interface Callback { void accept(String value); }
    private final Activity activity;
    private final AssistantApplication app;
    private final Conversation conversation;
    private final Handler main = new Handler(Looper.getMainLooper());
    private RequestScope active;
    private Future<?> work;

    VoiceRequest(Activity activity, Conversation conversation) {
        this.activity = activity; this.conversation = conversation;
        app = (AssistantApplication) activity.getApplication();
    }
    void start(String text, long speechTime, AgentLoop.Progress progress, Callback answer, Callback failure) {
        cancel();
        RequestScope scope = new RequestScope();
        active = scope;
        AppLogger.i(TAG, "Request started");
        work = app.worker().submit(() -> run(scope, text, speechTime, progress, answer, failure));
    }
    private void run(RequestScope scope, String text, long speechTime, AgentLoop.Progress progress, Callback answer, Callback failure) {
        try {
            scope.check(); conversation.begin(text, speechTime);
            DeviceTools tools = new DeviceTools(app.catalog(), new IntentExecutor(activity), scope);
            AgentLoop agent = new AgentLoop(body -> app.http().response(app.auth().accessToken(), body, scope), tools,
                    executing -> main.post(() -> { if (active == scope) { progress.update(executing); } }));
            String result = agent.run(conversation, app.model(), scope);
            AppLogger.i(TAG, "Request answered");
            main.post(() -> deliver(scope, answer, result));
        } catch (InterruptedException exception) {
            AppLogger.i(TAG, "Request stopped");
            main.post(() -> deliver(scope, failure, activity.getString(R.string.request_stopped)));
        } catch (Exception exception) {
            AppLogger.e(TAG, "Request failed", exception);
            main.post(() -> deliver(scope, failure, exception.getMessage()));
        }
    }
    private void deliver(RequestScope scope, Callback callback, String value) {
        if (active != scope) { return; }
        active = null; work = null; scope.cancel();
        callback.accept(value);
    }
    void cancel() {
        if (active != null) {
            AppLogger.i(TAG, "Request cancelled");
            RequestScope retiring = active;
            active = null; retiring.cancel(); app.http().cancelResponse(retiring);
        }
        if (work != null) { work.cancel(true); work = null; }
    }
}
