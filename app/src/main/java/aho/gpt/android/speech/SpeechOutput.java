package aho.gpt.android.speech;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import aho.gpt.R;
import aho.gpt.android.logging.AppLogger;

public final class SpeechOutput {
    private static final String TAG = "TTS";
    public interface ErrorListener { void failed(String message); }
    private final TextToSpeech engine;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean ready;
    private boolean failed;
    private boolean closed;
    private Runnable started;
    private Runnable completed;
    private ErrorListener errorHandler;
    private String pending;
    private long sequence;
    private String currentId = "";

    public SpeechOutput(Context context) {
        this.context = context;
        engine = new TextToSpeech(context, status -> main.post(() -> initialized(status)));
        engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { main.post(() -> began(id)); }
            @Override public void onDone(String id) { main.post(() -> finish(id)); }
            @SuppressWarnings("deprecation") // Framework requires this abstract compatibility callback.
            @Override public void onError(String id) { main.post(() -> playbackError(id, -1)); }
            @Override public void onError(String id, int code) { main.post(() -> playbackError(id, code)); }
        });
    }

    private void initialized(int status) {
        if (closed) { return; }
        AppLogger.i(TAG, "Initialization status=" + status);
        ready = status == TextToSpeech.SUCCESS;
        failed = !ready;
        if (pending != null) { deliver(); }
    }
    public void speak(String text, Runnable onStart, Runnable done, ErrorListener onError) {
        if (closed) { return; }
        AppLogger.d(TAG, "Playback requested ready=" + ready);
        stop(); pending = text; started = onStart; completed = done;
        errorHandler = onError; currentId = Long.toString(++sequence);
        if (ready || failed) { deliver(); }
    }
    private void deliver() {
        try {
            int issue = configurationIssue();
            if (issue != 0) { fail(issue); return; }
            String text = pending; pending = null;
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentId) == TextToSpeech.ERROR) { fail(R.string.tts_playback_failed); }
        } catch (RuntimeException exception) {
            AppLogger.w(TAG, "Playback exception", exception); fail(R.string.tts_playback_failed);
        }
    }
    private int configurationIssue() {
        if (failed) { return R.string.tts_unavailable; }
        if (!SpeechPreferences.enabled(context, SpeechPreferences.OUTPUT)) { return R.string.tts_disabled; }
        if (!SpeechAvailability.googleAppEnabled(context)) { return R.string.speech_provider_disabled; }
        int support = engine.setLanguage(SpeechPreferences.locale(context, SpeechPreferences.OUTPUT));
        if (support == TextToSpeech.LANG_MISSING_DATA) { return R.string.tts_language_missing; }
        if (support < TextToSpeech.LANG_AVAILABLE) { return R.string.tts_language_unavailable; }
        return PlaybackVoices.apply(context, engine) ? 0 : R.string.tts_voice_unavailable;
    }
    private void began(String id) {
        if (id.equals(currentId) && started != null) { AppLogger.i(TAG, "Playback started"); started.run(); }
    }
    private void finish(String id) { if (id.equals(currentId)) { finish(); } }
    private void finish() {
        AppLogger.i(TAG, "Playback completed");
        Runnable callback = completed;
        clear();
        if (callback != null) { callback.run(); }
    }
    private void playbackError(String id, int code) {
        if (id.equals(currentId)) { AppLogger.w(TAG, "Playback error=" + code); fail(R.string.tts_playback_failed); }
    }
    private void fail(int message) {
        AppLogger.w(TAG, "Playback unavailable reason=" + context.getResources().getResourceEntryName(message));
        ErrorListener callback = errorHandler;
        clear();
        if (callback != null) {
            callback.failed(context.getString(message, SpeechPreferences.label(context,
                    SpeechPreferences.locale(context, SpeechPreferences.OUTPUT))));
        }
    }
    private void clear() { started = null; completed = null; errorHandler = null; pending = null; currentId = ""; }
    public void stop() { if (!currentId.isEmpty()) { AppLogger.d(TAG, "Playback stopped"); } clear(); engine.stop(); }
    public void close() { closed = true; stop(); engine.shutdown(); main.removeCallbacksAndMessages(null); }
}
