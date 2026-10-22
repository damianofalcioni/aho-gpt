package dev.lightassistant.android.auth;

import android.util.Base64;
import dev.lightassistant.android.net.OpenAiHttp;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import dev.lightassistant.openai.IdentityClaims;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;

public final class OidcVerifier {
    private final OpenAiHttp http;
    public OidcVerifier(OpenAiHttp http) { this.http = http; }

    public JSONObject verify(String token, String client, String nonce) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) { throw new GeneralSecurityException("Invalid ID token"); }
        JSONObject header = object(parts[0]);
        if (!"RS256".equals(header.optString("alg"))) { throw new GeneralSecurityException("Unsupported ID signature"); }
        JSONObject jwks = http.json(OAuthAttempt.ISSUER + "/.well-known/jwks.json", "", null, "");
        JSONObject key = findKey(jwks.getJSONArray("keys"), header.getString("kid"));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                new BigInteger(1, decode(key.getString("n"))), new BigInteger(1, decode(key.getString("e"))))));
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        if (!signature.verify(decode(parts[2]))) { throw new GeneralSecurityException("Invalid ID signature"); }
        JSONObject claims = object(parts[1]);
        IdentityClaims.validate(claims, OAuthAttempt.ISSUER, client, nonce, System.currentTimeMillis() / 1000);
        return claims;
    }

    private JSONObject findKey(JSONArray keys, String id) throws GeneralSecurityException {
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.optJSONObject(i);
            if (key != null && id.equals(key.optString("kid")) && "RSA".equals(key.optString("kty"))) {
                return key;
            }
        }
        throw new GeneralSecurityException("OpenAI signing key not found");
    }
    private JSONObject object(String part) throws Exception { return new JSONObject(new String(decode(part), StandardCharsets.UTF_8)); }
    private byte[] decode(String value) { return Base64.decode(value, Base64.URL_SAFE | Base64.NO_WRAP); }
}
