package dev.lightassistant.android.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

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
        if (android.os.Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
        } else { recognizer = SpeechRecognizer.createSpeechRecognizer(context); }
        recognizer.setRecognitionListener(this);
    }

    public static boolean available(Context context) { return SpeechRecognizer.isRecognitionAvailable(context); }
    public void start() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        active = true; recognizer.startListening(intent);
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
