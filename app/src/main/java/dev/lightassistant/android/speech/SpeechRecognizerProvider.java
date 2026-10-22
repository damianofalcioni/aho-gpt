package dev.lightassistant.android.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.Locale;

/** Recognition and capability discovery must use the same provider and request. */
final class SpeechRecognizerProvider {
    private SpeechRecognizerProvider() { }

    static boolean onDevice(Context context) {
        return Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context);
    }
    static boolean available(Context context) {
        return onDevice(context) || SpeechRecognizer.isRecognitionAvailable(context);
    }
    static SpeechRecognizer create(Context context) {
        if (Build.VERSION.SDK_INT >= 31 && onDevice(context)) {
            return SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        }
        return SpeechRecognizer.createSpeechRecognizer(context);
    }
    static Intent request(Locale locale) {
        return new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag());
    }
}
