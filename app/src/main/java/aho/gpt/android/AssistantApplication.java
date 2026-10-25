package aho.gpt.android;

import android.app.Application;
import aho.gpt.android.auth.OAuthManager;
import aho.gpt.android.net.OpenAiHttp;
import aho.gpt.android.logging.AppDiagnostics;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.tools.IntentCatalog;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AssistantApplication extends Application {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService authWorker = Executors.newSingleThreadExecutor();
    private OAuthManager auth;
    private IntentCatalog catalog;
    private final OpenAiHttp http = new OpenAiHttp();

    @Override public void onCreate() {
        super.onCreate();
        AppDiagnostics.install(this);
        auth = new OAuthManager(this, http); catalog = new IntentCatalog(this);
        worker.execute(catalog::scan);
    }
    public ExecutorService worker() { return worker; }
    public ExecutorService authWorker() { return authWorker; }
    public OAuthManager auth() { return auth; }
    public IntentCatalog catalog() { return catalog; }
    public OpenAiHttp http() { return http; }
    public String model() { return getSharedPreferences("settings", MODE_PRIVATE).getString("model", ""); }
    public void model(String slug) { getSharedPreferences("settings", MODE_PRIVATE).edit().putString("model", slug).apply(); }
    @SuppressWarnings("deprecation") // Still delivered on older supported Android versions.
    @Override public void onLowMemory() { AppLogger.w("App", "Low memory"); super.onLowMemory(); }
    @SuppressWarnings("deprecation") // Still delivered on older supported Android versions.
    @Override public void onTrimMemory(int level) { AppLogger.i("App", "Trim memory level=" + level); super.onTrimMemory(level); }
}
