package aho.gpt.android.speech;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import aho.gpt.R;
import java.util.List;

/** Lifecycle-owned default-engine probe and explicit settings preview. */
public final class PlaybackSettingsClient implements AutoCloseable {
    public interface Loaded { void loaded(List<Voice> voices); }
    public interface PreviewResult { void completed(String message); }
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech engine;
    private Loaded loaded;
    private PreviewResult previewResult;
    private long generation;

    public PlaybackSettingsClient(Context context) { this.context = context; }
    public void load(Loaded callback) {
        close(); loaded = callback;
        long request = generation;
        main.postDelayed(() -> complete(request, null), 5000);
        try { engine = new TextToSpeech(context, status -> main.post(() -> initialized(request, status))); }
        catch (RuntimeException exception) { complete(request, null); }
    }
    private void initialized(long request, int status) {
        if (request != generation || loaded == null) { return; }
        if (status != TextToSpeech.SUCCESS) { complete(request, null); return; }
        try { complete(request, PlaybackVoices.available(engine)); }
        catch (RuntimeException exception) { complete(request, null); }
    }
    private void complete(long request, List<Voice> voices) {
        if (request != generation || loaded == null) { return; }
        Loaded callback = loaded;
        loaded = null; main.removeCallbacksAndMessages(null);
        callback.loaded(voices);
    }
    public void test(PreviewResult result) {
        previewResult = result;
        try {
            engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onDone(String id) { report(R.string.speech_test_done); }
                @SuppressWarnings("deprecation") // Required framework compatibility callback.
                @Override public void onError(String id) { report(R.string.speech_test_failed); }
                @Override public void onError(String id, int code) { report(R.string.speech_test_failed); }
            });
            int support = engine.setLanguage(SpeechPreferences.locale(context, SpeechPreferences.OUTPUT));
            if (support < TextToSpeech.LANG_AVAILABLE || !PlaybackVoices.apply(context, engine)) {
                report(R.string.tts_voice_unavailable); return;
            }
            if (engine.speak(context.getString(R.string.speech_test_sample), TextToSpeech.QUEUE_FLUSH, null, "preview")
                    == TextToSpeech.ERROR) { report(R.string.speech_test_failed); }
        } catch (RuntimeException exception) { report(R.string.speech_test_failed); }
    }
    private void report(int message) {
        main.post(() -> { if (previewResult != null) { previewResult.completed(context.getString(message)); } });
    }
    @Override public void close() {
        generation++; loaded = null; previewResult = null; main.removeCallbacksAndMessages(null);
        if (engine != null) { engine.stop(); engine.shutdown(); engine = null; }
    }
}
