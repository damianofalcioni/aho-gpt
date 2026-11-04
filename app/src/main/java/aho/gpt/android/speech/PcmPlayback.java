package aho.gpt.android.speech;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.SystemClock;
import aho.gpt.core.RequestScope;
import java.io.IOException;

/** Plays API PCM directly from memory and releases it on cancellation. */
final class PcmPlayback {
    private AudioTrack active;
    void play(byte[] pcm, RequestScope scope, Runnable started) throws Exception {
        AudioTrack track = create(scope);
        try {
            scope.check(); started.run();
            int offset = 0;
            while (offset < pcm.length) {
                scope.check();
                int count = track.write(pcm, offset, Math.min(4800, pcm.length - offset), AudioTrack.WRITE_BLOCKING);
                if (count <= 0) { throw new IOException("Audio playback failed"); }
                offset += count;
            }
            awaitCompletion(track, pcm.length / 2, scope);
        } finally {
            synchronized (this) { active = null; track.release(); }
        }
    }
    private void awaitCompletion(AudioTrack track, int frames, RequestScope scope) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + frames * 1000L / 24_000 + 10_000;
        while (Integer.toUnsignedLong(track.getPlaybackHeadPosition()) < frames) {
            scope.check();
            if (SystemClock.elapsedRealtime() >= deadline) { throw new IOException("Audio playback stalled"); }
            Thread.sleep(20);
        }
    }
    private synchronized AudioTrack create(RequestScope scope) throws InterruptedException {
        scope.check();
        active = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(24_000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(9600).setTransferMode(AudioTrack.MODE_STREAM).build();
        try { active.play(); }
        catch (RuntimeException exception) { active.release(); active = null; throw exception; }
        return active;
    }
    synchronized void cancel() { if (active != null) { active.pause(); active.flush(); } }
}
