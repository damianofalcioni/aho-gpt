package aho.gpt.android.net;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.ServerModels;
import aho.gpt.custom.SpeechCatalog;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Read-only discovery owns its worker and connection; no speech is generated. */
public final class CustomSpeechCatalog implements AutoCloseable {
    public record Choices(List<String> models, List<String> voices, boolean openAi) { }
    public interface Loaded { void accept(Choices choices); }
    private final CustomHttp http = new CustomHttp();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private RequestScope active;
    private Future<?> work;
    public void load(String base, String token, boolean speech, Loaded loaded) {
        cancel(); RequestScope scope = new RequestScope(); active = scope;
        work = worker.submit(() -> {
            try {
                List<String> models = speech ? models(base, token, scope) : Collections.emptyList(); scope.check();
                boolean openAi = SpeechCatalog.openAi(base);
                List<String> voices = openAi ? null : voices(base, token, scope); scope.check();
                loaded.accept(new Choices(models, voices, openAi));
            } catch (InterruptedException ignored) { /* Cancelled discovery cannot publish choices. */ }
        });
    }
    private List<String> models(String base, String token, RequestScope scope) {
        try { return ServerModels.read(http.json(base + "/models", token, null, scope)); }
        catch (Exception exception) { return null; }
    }
    private List<String> voices(String base, String token, RequestScope scope) {
        try { return SpeechCatalog.voices(http.json(base + "/audio/voices", token, null, scope)); }
        catch (Exception exception) { return null; }
    }
    public void cancel() {
        if (active != null) { active.cancel(); http.cancel(active); active = null; }
        if (work != null) { work.cancel(true); work = null; }
    }
    @Override public void close() { cancel(); worker.shutdownNow(); }
}
