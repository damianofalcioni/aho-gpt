package aho.gpt.custom;

/** One custom speech route at a time; neither means platform speech. */
public enum AudioMode {
    PLATFORM, SPEECH, REALTIME;
    public static AudioMode saved(boolean speech, boolean realtime) {
        if (realtime) { return REALTIME; }
        return speech ? SPEECH : PLATFORM;
    }
    public boolean requiresRecheck(SpeechApiConfig previous, SpeechApiConfig next) {
        return switch (this) {
            case SPEECH -> !previous.sameSpeech(next);
            case REALTIME -> !previous.realtimeVoice().equals(next.realtimeVoice());
            case PLATFORM -> false;
        };
    }
}
