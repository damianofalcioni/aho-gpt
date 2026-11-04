package aho.gpt.android.speech;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import aho.gpt.android.net.CustomSpeechApi;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.core.RequestScope;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Owns one cancellable speech job, bound to the saved provider revision. */
final class CustomSpeechWork implements AutoCloseable {
    interface Job { void run(RequestScope scope, ProviderSettings.CustomConfig config) throws Exception; }
    final CustomSpeechApi api = new CustomSpeechApi();
    private final ProviderSettings settings;
    private final String revision;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private RequestScope active;
    private Future<?> work;
    CustomSpeechWork(Context context) {
        settings = new ProviderSettings(context); revision = settings.revision();
    }
    void submit(Job job, Runnable failure) {
        cancel();
        RequestScope scope = new RequestScope(); active = scope;
        work = worker.submit(() -> {
            try {
                check(scope);
                ProviderSettings.CustomConfig config = settings.customConfig(); check(scope);
                if (!revision.equals(config.revision())) { throw new InterruptedException("Provider changed"); }
                job.run(scope, config);
            } catch (Exception exception) { post(scope, failure); }
        });
    }
    void check(RequestScope scope) throws InterruptedException {
        scope.check();
        if (!revision.equals(settings.revision()) || !settings.serverAudio()) {
            scope.cancel(); throw new InterruptedException("Provider changed");
        }
    }
    void post(RequestScope scope, Runnable action) {
        main.post(() -> {
            if (active != scope) { return; }
            try { check(scope); action.run(); }
            catch (InterruptedException exception) { cancel(); }
        });
    }
    void cancel() {
        if (active != null) { active.cancel(); api.cancel(active); active = null; }
        if (work != null) { work.cancel(true); work = null; }
        main.removeCallbacksAndMessages(null);
    }
    @Override public void close() { cancel(); worker.shutdownNow(); }
}
