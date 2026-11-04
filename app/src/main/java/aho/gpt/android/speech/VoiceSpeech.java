package aho.gpt.android.speech;

import android.content.Context;
import aho.gpt.android.net.ProviderSettings;
import java.util.Locale;

/** Keeps platform previews separate from the selected voice-session speech route. */
public final class VoiceSpeech implements AutoCloseable {
    private final SpeechInput input;
    private final SpeechOutput output;
    private final CustomSpeechInput remoteInput;
    private final CustomSpeechOutput remoteOutput;
    public VoiceSpeech(Context context, SpeechInput.Listener listener) {
        boolean custom = new ProviderSettings(context).serverAudio();
        input = !custom && SpeechInput.available(context) ? new SpeechInput(context, listener) : null;
        output = custom ? null : new SpeechOutput(context);
        remoteInput = custom ? new CustomSpeechInput(context, listener) : null;
        remoteOutput = custom ? new CustomSpeechOutput(context) : null;
    }
    public boolean available() { return remoteInput != null || input != null; }
    public void start(Locale locale) {
        if (remoteInput != null) { remoteInput.start(locale); }
        else if (input != null) { input.start(locale); }
    }
    public void cancel() {
        if (remoteInput != null) { remoteInput.cancel(); }
        if (input != null) { input.cancel(); }
    }
    public void speak(String text, Runnable started, Runnable done, SpeechOutput.ErrorListener failure) {
        if (remoteOutput != null) { remoteOutput.speak(text, started, done, failure); }
        else { output.speak(text, started, done, failure); }
    }
    public void stop() { if (remoteOutput != null) { remoteOutput.stop(); } else { output.stop(); } }
    public void play(byte[] pcm, Runnable started, Runnable done, SpeechOutput.ErrorListener failure) {
        remoteOutput.play(pcm, started, done, failure);
    }
    public void answer(String text, byte[] pcm, Runnable started, Runnable done, SpeechOutput.ErrorListener failure) {
        if (pcm == null) { speak(text, started, done, failure); }
        else { play(pcm, started, done, failure); }
    }
    @Override public void close() {
        if (input != null) { input.close(); }
        if (output != null) { output.close(); }
        if (remoteInput != null) { remoteInput.close(); remoteOutput.close(); }
    }
}
