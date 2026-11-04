package aho.gpt.custom;

/** Small PCM voice-activity gate, with bounded recording and trailing silence. */
public final class SpeechSilence {
    private int samples;
    private int lastVoice;
    private int firstVoice = -1;
    public boolean accept(short[] audio, int count) {
        long energy = 0;
        for (int index = 0; index < count; index++) { energy += (long) audio[index] * audio[index]; }
        samples += count;
        boolean voice = count > 0 && energy / count > 250_000;
        if (voice) {
            lastVoice = samples;
            if (firstVoice < 0) { firstVoice = samples; return true; }
        }
        return false;
    }
    public boolean began() { return firstVoice >= 0; }
    public boolean finished() {
        if (!began()) { return samples >= 16_000 * 15; }
        return samples - lastVoice >= 16_000 * 6 / 5 || samples - firstVoice >= 16_000 * 20;
    }
}
