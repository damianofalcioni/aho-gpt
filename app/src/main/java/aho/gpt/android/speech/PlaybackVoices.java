package aho.gpt.android.speech;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Uses only installed voices advertised by the default engine. */
public final class PlaybackVoices {
    private PlaybackVoices() { }

    public static List<Voice> available(TextToSpeech engine) {
        List<Voice> voices = new ArrayList<>();
        Set<Voice> reported = engine.getVoices();
        if (reported != null) {
            for (Voice voice : reported) { if (installed(voice)) { voices.add(voice); } }
        }
        Collections.sort(voices, (left, right) -> (left.getLocale().getDisplayName() + left.getName())
                .compareToIgnoreCase(right.getLocale().getDisplayName() + right.getName()));
        return voices;
    }
    private static boolean installed(Voice voice) {
        return voice.getFeatures() == null || !voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
    }
    public static boolean apply(Context context, TextToSpeech engine) {
        String selected = SpeechPreferences.voice(context);
        if (selected.isEmpty()) { return true; }
        for (Voice voice : available(engine)) {
            if (selected.equals(voice.getName())) { return engine.setVoice(voice) == TextToSpeech.SUCCESS; }
        }
        return false;
    }
}
