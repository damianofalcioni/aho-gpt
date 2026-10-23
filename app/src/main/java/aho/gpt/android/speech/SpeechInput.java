package aho.gpt.android.speech;

import android.content.Context;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.Locale;
import aho.gpt.R;

public final class SpeechInput implements RecognitionListener {
    public interface Listener {
        void began();
        void result(String text);
        void error(int error);
    }
    private final SpeechRecognizer recognizer;
    private final Listener listener;
    private boolean active;

    public SpeechInput(Context context, Listener listener) {
        this.listener = listener;
        recognizer = SpeechRecognizerProvider.create(context);
        recognizer.setRecognitionListener(this);
    }

    public static boolean available(Context context) { return SpeechRecognizerProvider.available(context); }
    public void start(Locale locale) {
        active = true; recognizer.startListening(SpeechRecognizerProvider.request(locale));
    }
    public static String errorText(Context context, int error) {
        if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
            return context.getString(R.string.stt_language_unavailable,
                    SpeechPreferences.label(context, SpeechPreferences.locale(context, SpeechPreferences.INPUT)));
        }
        return context.getString(R.string.speech_error, error);
    }
    public void cancel() { active = false; recognizer.cancel(); }
    public void close() { cancel(); recognizer.destroy(); }
    @Override public void onBeginningOfSpeech() { if (active) { listener.began(); } }
    @Override public void onResults(Bundle results) {
        if (!active) { return; }
        active = false;
        ArrayList<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (texts == null || texts.isEmpty()) { listener.error(SpeechRecognizer.ERROR_NO_MATCH); }
        else { listener.result(texts.get(0)); }
    }
    @Override public void onError(int error) { if (active) { active = false; listener.error(error); } }
    @Override public void onReadyForSpeech(Bundle params) { }
    @Override public void onRmsChanged(float rms) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { }
    @Override public void onPartialResults(Bundle partial) { }
    @Override public void onEvent(int type, Bundle params) { }
}
