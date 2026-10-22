package dev.lightassistant.android.speech;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionSupport;
import android.speech.RecognitionSupportCallback;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.List;

/** Queries capabilities only: never opens the microphone. Null means support was not reported. */
public final class RecognitionLanguages implements AutoCloseable {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private LanguageCatalogCallback callback;
    private long generation;

    public RecognitionLanguages(Context context) { this.context = context; }

    public void load(LanguageCatalogCallback result) {
        close(); callback = result;
        long request = generation;
        main.postDelayed(() -> complete(request, null), 5000);
        try { query(request); }
        catch (RuntimeException exception) { complete(request, null); }
    }

    private void query(long request) {
        if (Build.VERSION.SDK_INT < 33 || !SpeechRecognizerProvider.available(context)) { complete(request, null); return; }
        recognizer = SpeechRecognizerProvider.create(context);
        boolean onDevice = SpeechRecognizerProvider.onDevice(context);
        recognizer.checkRecognitionSupport(SpeechRecognizerProvider.request(SpeechPreferences.locale(context, SpeechPreferences.INPUT)),
                context.getMainExecutor(), new RecognitionSupportCallback() {
                    @Override public void onSupportResult(RecognitionSupport support) {
                        List<String> tags = new ArrayList<>(support.getInstalledOnDeviceLanguages());
                        if (!onDevice) { tags.addAll(support.getOnlineLanguages()); }
                        complete(request, tags);
                    }
                    @Override public void onError(int error) { complete(request, null); }
                });
    }

    private void complete(long request, List<String> tags) {
        if (request != generation) { return; }
        LanguageCatalogCallback result = callback;
        close();
        if (result != null) { result.loaded(tags); }
    }
    @Override public void close() {
        generation++; callback = null; main.removeCallbacksAndMessages(null);
        if (recognizer != null) { recognizer.destroy(); recognizer = null; }
    }
}
