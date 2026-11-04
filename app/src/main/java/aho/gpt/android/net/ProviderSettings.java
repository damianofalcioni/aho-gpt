package aho.gpt.android.net;

import android.content.Context;
import android.content.SharedPreferences;
import aho.gpt.android.auth.CredentialStore;
import aho.gpt.custom.ServerEndpoint;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.SpeechApiConfig;
import org.json.JSONObject;
import java.util.UUID;

/** Provider selection is committed atomically; custom credentials never join OAuth records. */
public final class ProviderSettings {
    private static final Object CUSTOM_LOCK = new Object();
    public record CustomConfig(String endpoint, String token, String revision, SpeechApiConfig speech) { }
    private final SharedPreferences settings;
    private final CredentialStore credentials;
    public ProviderSettings(Context context) {
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        credentials = new CredentialStore(context, "custom.enc");
    }
    public boolean customEnabled() { return settings.getBoolean("custom_enabled", false); }
    public boolean chatgptEnabled() { return settings.getBoolean("chatgpt_enabled", true); }
    public String revision() { return settings.getString("provider_revision", ""); }
    public String endpoint() { return settings.getString("custom_endpoint", ServerEndpoint.DEFAULT); }
    public String customModel() { return settings.getString("custom_model", ""); }
    public AudioMode audioMode() {
        return AudioMode.saved(settings.getBoolean("custom_speech_apis", false), settings.getBoolean("custom_realtime_api", false));
    }
    public boolean speechApis() { return audioMode() == AudioMode.SPEECH; }
    public boolean customSpeech() { return customEnabled() && speechApis(); }
    public boolean customRealtime() { return customEnabled() && audioMode() == AudioMode.REALTIME; }
    public boolean serverAudio() { return customEnabled() && audioMode() != AudioMode.PLATFORM; }
    public String token() throws Exception { return credentials.load().optString("token"); }
    public SpeechApiConfig speechConfig() {
        SpeechApiConfig defaults = SpeechApiConfig.DEFAULT;
        return new SpeechApiConfig(settings.getString("custom_transcription_model", defaults.transcriptionModel()),
                settings.getString("custom_speech_model", defaults.speechModel()), settings.getString("custom_speech_voice", defaults.voice()),
                settings.getString("custom_realtime_voice", defaults.realtimeVoice()));
    }
    public CustomConfig customConfig() throws Exception {
        synchronized (CUSTOM_LOCK) { return new CustomConfig(endpoint(), token(), revision(), speechConfig()); }
    }
    public void enable(boolean custom, boolean enabled) {
        SharedPreferences.Editor edit = settings.edit().putBoolean(custom ? "custom_enabled" : "chatgpt_enabled", enabled);
        if (enabled) { edit.putBoolean(custom ? "chatgpt_enabled" : "custom_enabled", false); }
        edit.putString("provider_revision", UUID.randomUUID().toString()).apply();
    }
    public void save(String endpoint, String token, String model, AudioMode mode, SpeechApiConfig speech) throws Exception {
        String base = ServerEndpoint.normalize(endpoint);
        synchronized (CUSTOM_LOCK) {
            credentials.save(new JSONObject().put("token", token));
            settings.edit().putString("custom_endpoint", base).putString("custom_model", model)
                    .putBoolean("custom_speech_apis", mode == AudioMode.SPEECH).putBoolean("custom_realtime_api", mode == AudioMode.REALTIME)
                    .putString("custom_transcription_model", speech.transcriptionModel()).putString("custom_speech_model", speech.speechModel())
                    .putString("custom_speech_voice", speech.voice()).putString("custom_realtime_voice", speech.realtimeVoice())
                    .putString("provider_revision", UUID.randomUUID().toString()).apply();
        }
    }
}
