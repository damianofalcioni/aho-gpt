package dev.lightassistant.android.speech;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

public final class SpeechOutput {
    private final TextToSpeech engine;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean ready;
    private boolean failed;
    private Runnable completed;
    private String pending;
    private long sequence;
    private String currentId = "";

    public SpeechOutput(Context context) {
        engine = new TextToSpeech(context, status -> main.post(() -> initialized(status)));
        engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) { main.post(() -> finish(id)); }
            @SuppressWarnings("deprecation") // Framework requires this abstract compatibility callback.
            @Override public void onError(String id) { main.post(() -> finish(id)); }
            @Override public void onError(String id, int code) { main.post(() -> finish(id)); }
        });
    }

    private void initialized(int status) {
        ready = status == TextToSpeech.SUCCESS;
        failed = !ready;
        if (ready) { engine.setLanguage(Locale.getDefault()); }
        if (pending != null) { deliver(); }
    }
    public void speak(String text, Runnable done) {
        stop(); pending = text; completed = done; currentId = Long.toString(++sequence);
        if (ready || failed) { deliver(); }
    }
    private void deliver() {
        if (failed) { finish(); return; }
        String text = pending;
        pending = null;
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentId) == TextToSpeech.ERROR) { finish(); }
    }
    private void finish(String id) { if (id.equals(currentId)) { finish(); } }
    private void finish() {
        Runnable callback = completed;
        completed = null; pending = null;
        if (callback != null) { callback.run(); }
    }
    public void stop() { completed = null; pending = null; currentId = ""; engine.stop(); }
    public void close() { stop(); engine.shutdown(); main.removeCallbacksAndMessages(null); }
}
