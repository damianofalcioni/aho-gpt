package aho.gpt.android.speech;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.SpeechAudio;
import aho.gpt.custom.SpeechSilence;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Records mono 16 kHz audio only during an authorized foreground listening session. */
final class PcmCapture {
    private final Context context;
    private AudioRecord active;
    PcmCapture(Context context) { this.context = context; }
    byte[] record(RequestScope scope, Runnable began) throws Exception {
        AudioRecord recorder = create(scope);
        try { return read(recorder, scope, began); }
        finally {
            synchronized (this) { active = null; recorder.release(); }
        }
    }
    private synchronized AudioRecord create(RequestScope scope) throws InterruptedException, IOException {
        scope.check();
        AudioRecord recorder = initialize();
        try { recorder.startRecording(); }
        catch (RuntimeException exception) { recorder.release(); throw exception; }
        active = recorder;
        return recorder;
    }
    private AudioRecord initialize() throws IOException {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Microphone permission required");
        }
        int size = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (size <= 0) { throw new IOException("Audio recording unavailable"); }
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16_000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(size, 6400));
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) { recorder.release(); throw new IOException("Audio recording unavailable"); }
        return recorder;
    }
    private byte[] read(AudioRecord recorder, RequestScope scope, Runnable began) throws Exception {
        ByteArrayOutputStream audio = new ByteArrayOutputStream();
        SpeechSilence silence = new SpeechSilence();
        short[] buffer = new short[1600];
        while (!silence.finished()) {
            scope.check();
            int count = recorder.read(buffer, 0, buffer.length);
            if (count <= 0) { throw new IOException("Audio recording failed"); }
            if (silence.accept(buffer, count)) { began.run(); }
            for (int index = 0; index < count; index++) { audio.write(buffer[index] & 255); audio.write((buffer[index] >> 8) & 255); }
        }
        scope.check();
        return silence.began() ? SpeechAudio.wav(audio.toByteArray(), 16_000) : new byte[0];
    }
    synchronized void cancel() {
        if (active != null && active.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) { active.stop(); }
    }
}
