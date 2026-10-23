package aho.gpt.android.speech;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import aho.gpt.R;

public final class SpeechOutput {
    public interface ErrorListener { void failed(String message); }
    private final TextToSpeech engine;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean ready;
    private boolean failed;
    private boolean closed;
    private Runnable completed;
    private ErrorListener errorHandler;
    private String pending;
    private long sequence;
    private String currentId = "";

    public SpeechOutput(Context context) {
        this.context = context;
        engine = new TextToSpeech(context, status -> main.post(() -> initialized(status)));
        engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) { main.post(() -> finish(id)); }
            @SuppressWarnings("deprecation") // Framework requires this abstract compatibility callback.
            @Override public void onError(String id) { main.post(() -> playbackError(id)); }
            @Override public void onError(String id, int code) { main.post(() -> playbackError(id)); }
        });
    }

    private void initialized(int status) {
        if (closed) { return; }
        ready = status == TextToSpeech.SUCCESS;
        failed = !ready;
        if (pending != null) { deliver(); }
    }
    public void speak(String text, Runnable done, ErrorListener onError) {
        if (closed) { return; }
        stop(); pending = text; completed = done; errorHandler = onError; currentId = Long.toString(++sequence);
        if (ready || failed) { deliver(); }
    }
    private void deliver() {
        if (failed) { fail(R.string.tts_unavailable); return; }
        try {
            int support = engine.setLanguage(SpeechPreferences.locale(context, SpeechPreferences.OUTPUT));
            if (support == TextToSpeech.LANG_MISSING_DATA) { fail(R.string.tts_language_missing); return; }
            if (support < TextToSpeech.LANG_AVAILABLE) { fail(R.string.tts_language_unavailable); return; }
            String text = pending; pending = null;
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentId) == TextToSpeech.ERROR) { fail(R.string.tts_playback_failed); }
        } catch (RuntimeException exception) { fail(R.string.tts_playback_failed); }
    }
    private void finish(String id) { if (id.equals(currentId)) { finish(); } }
    private void finish() {
        Runnable callback = completed;
        clear();
        if (callback != null) { callback.run(); }
    }
    private void playbackError(String id) { if (id.equals(currentId)) { fail(R.string.tts_playback_failed); } }
    private void fail(int message) {
        ErrorListener callback = errorHandler;
        clear();
        if (callback != null) {
            callback.failed(context.getString(message, SpeechPreferences.label(context,
                    SpeechPreferences.locale(context, SpeechPreferences.OUTPUT))));
        }
    }
    private void clear() { completed = null; errorHandler = null; pending = null; currentId = ""; }
    public void stop() { clear(); engine.stop(); }
    public void close() { closed = true; stop(); engine.shutdown(); main.removeCallbacksAndMessages(null); }
}
