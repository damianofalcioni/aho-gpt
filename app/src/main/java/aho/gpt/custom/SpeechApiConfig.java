package aho.gpt.custom;

/** Provider identifiers are saved choices, not claims of model or voice availability. */
public record SpeechApiConfig(String transcriptionModel, String speechModel, String voice, String realtimeVoice,
                              String liveModel, String liveVoice) {
    public static final SpeechApiConfig DEFAULT = new SpeechApiConfig("whisper-1", "tts-1", "alloy", "alloy");
    public SpeechApiConfig(String transcriptionModel, String speechModel, String voice, String realtimeVoice) {
        this(transcriptionModel, speechModel, voice, realtimeVoice, "gpt-live-1", "marin");
    }
    public SpeechApiConfig(String transcriptionModel, String speechModel, String voice, String realtimeVoice, String liveModel, String liveVoice) {
        this.transcriptionModel = identifier(transcriptionModel); this.speechModel = identifier(speechModel);
        this.voice = identifier(voice); this.realtimeVoice = identifier(realtimeVoice);
        this.liveModel = identifier(liveModel); this.liveVoice = identifier(liveVoice);
    }
    public static String identifier(String value) {
        if (value == null) { throw new IllegalArgumentException("Missing speech identifier"); }
        String id = value.trim();
        if (!id.matches("[^\\p{Cntrl}]{1,256}")) { throw new IllegalArgumentException("Invalid speech identifier"); }
        return id;
    }
    public boolean sameSpeech(SpeechApiConfig other) {
        return transcriptionModel.equals(other.transcriptionModel) && speechModel.equals(other.speechModel) && voice.equals(other.voice);
    }
}
