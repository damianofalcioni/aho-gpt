package aho.gpt.android.speech;

import android.content.Context;
import android.speech.SpeechRecognizer;
import aho.gpt.android.net.ProviderSettings;
import java.util.Locale;

final class CustomSpeechInput implements AutoCloseable {
    private final CustomSpeechWork work;
    private final PcmCapture capture;
    private final SpeechInput.Listener listener;
    private final boolean realtime;
    CustomSpeechInput(Context context, SpeechInput.Listener listener) {
        work = new CustomSpeechWork(context); capture = new PcmCapture(context); this.listener = listener;
        realtime = new ProviderSettings(context).customRealtime();
    }
    void start(Locale language) {
        cancel();
        work.submit((scope, config) -> {
            byte[] wav = capture.record(scope, () -> work.post(scope, listener::began));
            if (wav.length == 0) { work.post(scope, () -> listener.error(SpeechRecognizer.ERROR_NO_MATCH)); return; }
            if (realtime) { work.post(scope, () -> listener.audio(wav)); return; }
            work.post(scope, listener::processing);
            work.check(scope);
            String text = work.api.transcribe(config.endpoint(), config.token(), wav, language.getLanguage(), config.speech(), scope);
            work.post(scope, () -> {
                if (text.isEmpty()) { listener.error(SpeechRecognizer.ERROR_NO_MATCH); }
                else { listener.result(text); }
            });
        }, () -> listener.error(SpeechRecognizer.ERROR_NETWORK));
    }
    void cancel() { work.cancel(); capture.cancel(); }
    @Override public void close() { cancel(); work.close(); }
}
