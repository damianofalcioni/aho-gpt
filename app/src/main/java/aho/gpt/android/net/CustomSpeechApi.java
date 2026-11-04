package aho.gpt.android.net;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.ServerSpeech;
import aho.gpt.custom.SpeechApiConfig;

/** Uses only the configured server's public compatible audio endpoints. */
public final class CustomSpeechApi {
    private final CustomHttp http = new CustomHttp();
    public byte[] speech(String base, String token, String text, SpeechApiConfig config, RequestScope scope) throws Exception {
        return new ServerSpeech(http::exchange, config).speech(base, token, text, scope);
    }
    public String transcribe(String base, String token, byte[] wav, String language, SpeechApiConfig config, RequestScope scope) throws Exception {
        return new ServerSpeech(http::exchange, config).transcribe(base, token, wav, language, scope);
    }
    public void check(String base, String token, SpeechApiConfig config, RequestScope scope) throws Exception {
        new ServerSpeech(http::exchange, config).check(base, token, scope);
    }
    public void cancel(RequestScope scope) { http.cancel(scope); }
}
