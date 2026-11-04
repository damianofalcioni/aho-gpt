package aho.gpt.android.speech;

import android.content.Context;
import aho.gpt.R;
import aho.gpt.custom.SpeechAudio;

final class CustomSpeechOutput implements AutoCloseable {
    private final Context context;
    private final CustomSpeechWork work;
    private final PcmPlayback playback = new PcmPlayback();
    CustomSpeechOutput(Context context) { this.context = context; work = new CustomSpeechWork(context); }
    void speak(String text, Runnable started, Runnable done, SpeechOutput.ErrorListener failure) {
        stop();
        work.submit((scope, config) -> {
            byte[] pcm = work.api.speech(config.endpoint(), config.token(), text, config.speech(), scope);
            work.check(scope);
            playback.play(pcm, scope, () -> work.post(scope, started));
            work.post(scope, done);
        }, () -> failure.failed(context.getString(R.string.custom_speech_failed)));
    }
    void stop() { work.cancel(); playback.cancel(); }
    void play(byte[] pcm, Runnable started, Runnable done, SpeechOutput.ErrorListener failure) {
        stop();
        work.submit((scope, config) -> {
            SpeechAudio.validatePcm(pcm);
            playback.play(pcm, scope, () -> work.post(scope, started)); work.post(scope, done);
        }, () -> failure.failed(context.getString(R.string.custom_speech_failed)));
    }
    @Override public void close() { stop(); work.close(); }
}
