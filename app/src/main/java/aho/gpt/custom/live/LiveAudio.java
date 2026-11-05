package aho.gpt.custom.live;

import aho.gpt.custom.realtime.WireBase64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Bounded turn playback; a silence gate is not evidence of backend completion. */
public final class LiveAudio {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private boolean speech;
    private int quiet;
    public void append(String encoded) throws IOException {
        byte[] pcm = WireBase64.decode(encoded);
        if (pcm.length % 2 != 0) { throw new IOException("Invalid Live audio"); }
        int previousQuiet = quiet; boolean voiced = samples(pcm);
        if (speech && (voiced || previousQuiet < 48_000)) { store(pcm); }
    }
    private boolean samples(byte[] pcm) {
        boolean voiced = false;
        for (int index = 0; index < pcm.length; index += 2) {
            short sample = (short) ((pcm[index] & 255) | (pcm[index + 1] << 8));
            if (Math.abs((int) sample) > 180) { speech = true; voiced = true; quiet = 0; }
            else { quiet = Math.min(48_000, quiet + 1); }
        }
        return voiced;
    }
    private void store(byte[] pcm) throws IOException {
        if (output.size() + pcm.length > 24_000_000) { throw new IOException("Live audio too large"); }
        output.write(pcm);
    }
    public void backendFinished() { quiet = 0; }
    public boolean settled() { return speech && quiet >= 48_000; }
    public boolean speaking() { return speech && quiet < 2400; }
    public byte[] bytes() { return output.toByteArray(); }
    public void clear() { output.reset(); speech = false; quiet = 0; }
}
