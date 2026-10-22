package dev.lightassistant.android.auth;

import android.content.Context;
import android.net.Uri;
import dev.lightassistant.android.net.OpenAiHttp;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;
import org.json.JSONObject;

public final class OAuthManager {
    private static final String CLIENT = "client_id";
    private static final String ACCESS = "access_token";
    private static final String REFRESH = "refresh_token";
    private final CredentialStore store;
    private final OpenAiHttp http;
    private final String host;

    public OAuthManager(Context context, OpenAiHttp http) {
        this.store = new CredentialStore(context);
        this.http = http;
        var settings = context.getSharedPreferences("installation", Context.MODE_PRIVATE);
        String saved = settings.getString("host", "");
        host = saved.isEmpty() ? "urn:uuid:" + UUID.randomUUID() : saved;
        settings.edit().putString("host", host).apply();
    }

    public synchronized Uri authorization(OAuthAttempt attempt) throws Exception {
        return attempt.authorization(host, store.load());
    }
    public synchronized String client() throws Exception { return store.load().optString(CLIENT); }
    public synchronized JSONObject account() throws Exception { return store.load(); }

    public synchronized void complete(OAuthAttempt attempt, Uri callback) throws Exception {
        String client = attempt.clientFrom(callback);
        JSONObject previous = store.load();
        // Preserve issued registration even if its one-use authorization code expires.
        previous.put(CLIENT, client);
        store.save(previous);
        String code = callback.getQueryParameter("code");
        if (code == null || code.isEmpty()) { throw new GeneralSecurityException("Missing authorization code"); }
        JSONObject tokens = token("grant_type", "authorization_code", CLIENT, client, "code", code,
                "code_verifier", attempt.verifier, "redirect_uri", attempt.redirect, "resource", OAuthAttempt.RESOURCE);
        JSONObject identity = new OidcVerifier(http).verify(tokens.getString("id_token"), client, attempt.nonce);
        String subject = previous.optString("subject");
        if (!subject.isEmpty() && !subject.equals(identity.getString("sub"))) {
            throw new GeneralSecurityException("The signed-in account changed");
        }
        previous.put("subject", identity.getString("sub")).put("email", identity.optString("email"));
        saveTokens(previous, tokens);
    }

    public synchronized String accessToken() throws Exception {
        JSONObject saved = store.load();
        requirePermission(saved);
        if (saved.optLong("expires_at") < System.currentTimeMillis() + 60_000) {
            JSONObject fresh = token("grant_type", REFRESH, CLIENT, saved.getString(CLIENT),
                    REFRESH, saved.getString(REFRESH), "resource", OAuthAttempt.RESOURCE);
            saveTokens(saved, fresh);
        }
        requirePermission(saved);
        return saved.getString(ACCESS);
    }

    public synchronized void signOut() throws Exception {
        JSONObject saved = store.load();
        String refresh = saved.optString(REFRESH);
        try {
            if (!refresh.isEmpty()) { revoke(saved, refresh); }
        } finally {
            saved.remove(ACCESS); saved.remove(REFRESH); saved.remove("id_token");
            saved.remove("scope"); saved.remove("expires_at"); store.save(saved);
        }
    }

    private void revoke(JSONObject saved, String refresh) throws Exception {
        JSONObject discovery = http.json(OAuthAttempt.ISSUER + "/.well-known/openid-configuration", "", null, "");
        http.emptyPost(discovery.getString("revocation_endpoint"), form("token", refresh,
                "token_type_hint", REFRESH, CLIENT, saved.getString(CLIENT)));
    }

    private void saveTokens(JSONObject saved, JSONObject tokens) throws Exception {
        saved.put(ACCESS, tokens.getString(ACCESS));
        copyIfPresent(saved, tokens, REFRESH); copyIfPresent(saved, tokens, "id_token");
        copyIfPresent(saved, tokens, "scope");
        saved.put("expires_at", System.currentTimeMillis() + tokens.getLong("expires_in") * 1000);
        store.save(saved);
    }
    private void copyIfPresent(JSONObject saved, JSONObject tokens, String key) throws Exception {
        if (tokens.has(key)) { saved.put(key, tokens.getString(key)); }
    }
    private void requirePermission(JSONObject saved) throws IOException {
        if (!Arrays.asList(saved.optString("scope").split(" ")).contains(OAuthAttempt.DIRECT_SCOPE)) {
            throw new IOException("Continue with ChatGPT in setup and allow ChatGPT plan usage.");
        }
    }
    private JSONObject token(String... pairs) throws Exception {
        return http.json(OAuthAttempt.TOKEN_URL, "", form(pairs), "application/x-www-form-urlencoded");
    }
    private String form(String... pairs) throws IOException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (result.length() > 0) { result.append('&'); }
            result.append(URLEncoder.encode(pairs[i], StandardCharsets.UTF_8.name())).append('=')
                    .append(URLEncoder.encode(pairs[i + 1], StandardCharsets.UTF_8.name()));
        }
        return result.toString();
    }
}
