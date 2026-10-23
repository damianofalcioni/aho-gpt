package aho.gpt.android.speech;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Uses the default playback engine, without speaking or downloading language data. */
public final class PlaybackLanguages implements AutoCloseable {
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech engine;
    private LanguageCatalogCallback callback;
    private long generation;

    public void load(Context context, LanguageCatalogCallback result) {
        close(); callback = result;
        long request = generation;
        main.postDelayed(() -> complete(request, null), 5000);
        try { engine = new TextToSpeech(context, status -> main.post(() -> initialized(request, status))); }
        catch (RuntimeException exception) { complete(request, null); }
    }
    private void initialized(long request, int status) {
        if (request != generation || callback == null) { return; }
        if (status != TextToSpeech.SUCCESS) { complete(request, null); return; }
        try { complete(request, available()); }
        catch (RuntimeException exception) { complete(request, null); }
    }
    private List<String> available() {
        Set<Locale> locales = engine.getAvailableLanguages();
        if (locales == null) { return null; }
        List<String> tags = new ArrayList<>();
        for (Locale locale : locales) {
            if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) { tags.add(locale.toLanguageTag()); }
        }
        return tags;
    }
    private void complete(long request, List<String> tags) {
        if (request != generation) { return; }
        LanguageCatalogCallback result = callback;
        close();
        if (result != null) { result.loaded(tags); }
    }
    @Override public void close() {
        generation++; callback = null; main.removeCallbacksAndMessages(null);
        if (engine != null) { engine.shutdown(); engine = null; }
    }
}
