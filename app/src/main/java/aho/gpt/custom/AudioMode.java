package aho.gpt.custom;

/** One custom speech route at a time; neither means platform speech. */
public enum AudioMode {
    PLATFORM, SPEECH, REALTIME, LIVE;
    public static AudioMode saved(boolean speech, boolean realtime) {
        return saved(speech, realtime, false);
    }
    public static AudioMode saved(boolean speech, boolean realtime, boolean live) {
        if (live) { return LIVE; }
        if (realtime) { return REALTIME; }
        return speech ? SPEECH : PLATFORM;
    }
    public boolean requiresRecheck(SpeechApiConfig previous, SpeechApiConfig next) {
        return switch (this) {
            case SPEECH -> !previous.sameSpeech(next);
            case REALTIME -> !previous.realtimeVoice().equals(next.realtimeVoice());
            case LIVE -> !previous.liveModel().equals(next.liveModel()) || !previous.liveVoice().equals(next.liveVoice());
            case PLATFORM -> false;
        };
    }
}
