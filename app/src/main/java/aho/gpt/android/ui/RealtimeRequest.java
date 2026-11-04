package aho.gpt.android.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.net.CustomRealtimeClient;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.android.tools.DeviceTools;
import aho.gpt.android.tools.IntentExecutor;
import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.RealtimeAudio;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.openai.AgentLoop;
import java.util.concurrent.Future;

/** Uses the shared agent safety loop for complete Realtime responses and audio. */
final class RealtimeRequest {
    interface Answer { void accept(String text, byte[] pcm); }
    private final Activity activity;
    private final AssistantApplication app;
    private final Conversation conversation;
    private final ProviderSettings settings;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CustomRealtimeClient client = new CustomRealtimeClient();
    private RequestScope active;
    private Future<?> work;
    RealtimeRequest(Activity activity, Conversation conversation) {
        this.activity = activity; this.conversation = conversation;
        app = (AssistantApplication) activity.getApplication(); settings = new ProviderSettings(activity);
    }
    void start(byte[] wav, long time, AgentLoop.Progress progress, Answer answer, VoiceRequest.Callback failure) {
        cancel(); RequestScope scope = new RequestScope(); active = scope;
        String revision = settings.revision();
        work = app.worker().submit(() -> run(scope, revision, wav, time, progress, answer, failure));
    }
    private void run(RequestScope scope, String revision, byte[] wav, long time,
                     AgentLoop.Progress progress, Answer answer, VoiceRequest.Callback failure) {
        try {
            check(scope, revision);
            ProviderSettings.CustomConfig config = settings.customConfig(); check(scope, revision);
            conversation.beginAudio(RealtimeAudio.message(wav), time);
            boolean playback = SpeechPreferences.enabled(activity, SpeechPreferences.OUTPUT);
            RealtimeProtocol protocol = client.open(config.endpoint(), config.token(), settings.customModel(), playback, config.speech().realtimeVoice(), scope);
            DeviceTools tools = new DeviceTools(app.catalog(), new IntentExecutor(activity), scope);
            AgentLoop agent = new AgentLoop(request -> {
                check(scope, revision); var result = protocol.respond(request, scope); check(scope, revision); return result;
            }, tools, executing -> post(scope, () -> progress.update(executing)));
            String text = agent.run(conversation, "", scope); check(scope, revision);
            byte[] pcm = protocol.audio();
            post(scope, () -> deliver(scope, () -> answer.accept(text, pcm)));
        } catch (Exception exception) {
            AppLogger.w("Realtime", "Request failed", exception);
            post(scope, () -> deliver(scope, () -> failure.accept(activity.getString(R.string.custom_realtime_failed))));
        } finally { client.cancel(scope); }
    }
    private void check(RequestScope scope, String revision) throws InterruptedException {
        scope.check();
        if (!revision.equals(settings.revision()) || !settings.customRealtime()) {
            scope.cancel(); throw new InterruptedException("Provider changed");
        }
    }
    private void post(RequestScope scope, Runnable action) { main.post(() -> { if (active == scope) { action.run(); } }); }
    private void deliver(RequestScope scope, Runnable action) {
        active = null; work = null; scope.cancel(); action.run();
    }
    void cancel() {
        if (active != null) { RequestScope scope = active; active = null; scope.cancel(); client.cancel(scope); }
        if (work != null) { work.cancel(true); work = null; }
        main.removeCallbacksAndMessages(null);
    }
}
