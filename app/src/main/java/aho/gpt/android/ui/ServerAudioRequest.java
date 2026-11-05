package aho.gpt.android.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.net.CustomAudioClient;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.android.tools.DeviceTools;
import aho.gpt.android.tools.IntentExecutor;
import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.RealtimeAudio;
import aho.gpt.openai.AgentLoop;
import java.util.concurrent.Future;

/** Uses the shared agent safety loop for complete Realtime or Live responses. */
final class ServerAudioRequest {
    interface Answer { void accept(String text, byte[] pcm); }
    private final Activity activity;
    private final AssistantApplication app;
    private final Conversation conversation;
    private final ProviderSettings settings;
    private final Handler main = new Handler(Looper.getMainLooper());
    private CustomAudioClient activeClient;
    private RequestScope active;
    private Future<?> work;
    ServerAudioRequest(Activity activity, Conversation conversation) {
        this.activity = activity; this.conversation = conversation;
        app = (AssistantApplication) activity.getApplication(); settings = new ProviderSettings(activity);
    }
    void start(byte[] wav, long time, AgentLoop.Progress progress, Answer answer, VoiceRequest.Callback failure) {
        cancel(); RequestScope scope = new RequestScope(); active = scope;
        CustomAudioClient client = new CustomAudioClient(); activeClient = client;
        String revision = settings.revision();
        work = app.worker().submit(() -> run(client, scope, revision, wav, time, progress, answer, failure));
    }
    private void run(CustomAudioClient client, RequestScope scope, String revision, byte[] wav, long time,
                     AgentLoop.Progress progress, Answer answer, VoiceRequest.Callback failure) {
        try {
            check(scope, revision);
            ProviderSettings.CustomConfig config = settings.customConfig(); check(scope, revision);
            conversation.beginAudio(RealtimeAudio.message(wav), time);
            boolean playback = SpeechPreferences.voiceEnabled(activity, SpeechPreferences.OUTPUT);
            client.open(settings.audioMode(), config, settings.customModel(), playback, scope);
            DeviceTools tools = new DeviceTools(app.catalog(), new IntentExecutor(activity), scope);
            AgentLoop agent = new AgentLoop(request -> {
                check(scope, revision); var result = client.respond(request, wav, scope); check(scope, revision); return result;
            }, tools, executing -> post(scope, () -> progress.update(executing)));
            String text = agent.run(conversation, "", scope); check(scope, revision);
            byte[] pcm = client.finish(scope); check(scope, revision);
            conversation.transcribeLatest(client.transcript());
            post(scope, () -> deliver(scope, () -> answer.accept(text, pcm)));
        } catch (Exception exception) {
            AppLogger.w("Audio", "Request failed", exception);
            int message = settings.customLive() ? R.string.custom_live_failed : R.string.custom_realtime_failed;
            post(scope, () -> deliver(scope, () -> failure.accept(activity.getString(message))));
        } finally { client.cancel(scope); }
    }
    private void check(RequestScope scope, String revision) throws InterruptedException {
        scope.check();
        if (!revision.equals(settings.revision()) || !settings.serverAudio()) {
            scope.cancel(); throw new InterruptedException("Provider changed");
        }
    }
    private void post(RequestScope scope, Runnable action) { main.post(() -> { if (active == scope) { action.run(); } }); }
    private void deliver(RequestScope scope, Runnable action) {
        active = null; activeClient = null; work = null; scope.cancel(); action.run();
    }
    void cancel() {
        if (active != null) { RequestScope scope = active; active = null; scope.cancel(); activeClient.cancel(scope); activeClient = null; }
        if (work != null) { work.cancel(true); work = null; }
        main.removeCallbacksAndMessages(null);
    }
}
