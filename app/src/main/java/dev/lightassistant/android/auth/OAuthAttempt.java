package dev.lightassistant.android.auth;

import android.net.Uri;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import org.json.JSONObject;

public final class OAuthAttempt {
    public static final String RESOURCE = "https://api.openai.com/v1";
    public static final String ISSUER = "https://auth.openai.com";
    public static final String TOKEN_URL = ISSUER + "/api/accounts/oauth/token";
    public static final String DIRECT_SCOPE = "chatgpt.tokens.use.direct";
    public final String state = random();
    public final String nonce = random();
    public final String verifier = random();
    public final String redirect;
    public final String previousClient;

    public OAuthAttempt(String redirect, String previousClient) {
        this.redirect = redirect;
        this.previousClient = previousClient;
    }

    public Uri authorization(String host, JSONObject previous) throws GeneralSecurityException {
        Uri.Builder builder = Uri.parse(ISSUER + "/api/accounts/authorize").buildUpon()
                .appendQueryParameter("client_id", previousClient.isEmpty() ? "dynamic_agent_client" : previousClient)
                .appendQueryParameter("response_type", "code").appendQueryParameter("redirect_uri", redirect)
                .appendQueryParameter("scope", "openid profile email offline_access resource.invoke " + DIRECT_SCOPE)
                .appendQueryParameter("resource", RESOURCE).appendQueryParameter("ext_agent_host_id", host)
                .appendQueryParameter("state", state).appendQueryParameter("nonce", nonce)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("code_challenge", encode(MessageDigest.getInstance("SHA-256")
                        .digest(verifier.getBytes(StandardCharsets.US_ASCII))));
        if (previousClient.isEmpty()) { builder.appendQueryParameter("agent_name_hint", "Light Assistant"); }
        if (!previous.optString("id_token").isEmpty()) {
            builder.appendQueryParameter("id_token_hint", previous.optString("id_token"));
        }
        return builder.build();
    }

    public String clientFrom(Uri callback) throws GeneralSecurityException {
        validateState(callback);
        String issued = value(callback, "client_id");
        if (!previousClient.isEmpty()) { return returningClient(issued); }
        if (!issued.startsWith("oaiapp_")) { throw new GeneralSecurityException("Missing registered client ID"); }
        return issued;
    }

    private void validateState(Uri callback) throws GeneralSecurityException {
        if (!MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8),
                value(callback, "state").getBytes(StandardCharsets.UTF_8))) {
            throw new GeneralSecurityException("Sign-in state mismatch");
        }
        if (callback.getQueryParameter("error") != null) { throw new GeneralSecurityException("Sign-in declined"); }
    }

    private String returningClient(String issued) throws GeneralSecurityException {
        if (!issued.isEmpty() && !previousClient.equals(issued)) { throw new GeneralSecurityException("Sign-in client changed"); }
        return previousClient;
    }

    private static String value(Uri uri, String key) {
        String value = uri.getQueryParameter(key);
        return value == null ? "" : value;
    }
    private static String random() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return encode(bytes);
    }
    private static String encode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
