package aho.gpt.openai;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import org.json.JSONArray;
import org.json.JSONObject;

/** Claim checks for an ID token whose signature has already been verified. */
public final class IdentityClaims {
    private IdentityClaims() { }
    public static void validate(JSONObject claims, String issuer, String client, String nonce, long now) throws GeneralSecurityException {
        validateIdentity(claims, issuer);
        if (!audience(claims, client)) { throw new GeneralSecurityException("Invalid ID audience"); }
        validateAuthorizedParty(claims, client);
        validateTime(claims, now);
        if (!MessageDigest.isEqual(nonce.getBytes(StandardCharsets.UTF_8),
                claims.optString("nonce").getBytes(StandardCharsets.UTF_8))) {
            throw new GeneralSecurityException("Invalid ID nonce");
        }
    }

    private static void validateAuthorizedParty(JSONObject claims, String client) throws GeneralSecurityException {
        if (claims.has("azp") && !client.equals(claims.optString("azp"))) {
            throw new GeneralSecurityException("Invalid authorized client");
        }
    }

    private static void validateIdentity(JSONObject claims, String issuer) throws GeneralSecurityException {
        if (!issuer.equals(claims.optString("iss")) || claims.optString("sub").isEmpty()) {
            throw new GeneralSecurityException("Invalid OpenAI identity");
        }
    }
    private static void validateTime(JSONObject claims, long now) throws GeneralSecurityException {
        if (claims.optLong("exp") <= now || claims.optLong("nbf") > now + 60) {
            throw new GeneralSecurityException("ID token expired or not yet valid");
        }
    }

    private static boolean audience(JSONObject claims, String client) {
        if (client.equals(claims.optString("aud"))) { return true; }
        JSONArray audiences = claims.optJSONArray("aud");
        if (audiences == null) { return false; }
        if (audiences.length() > 1 && !client.equals(claims.optString("azp"))) { return false; }
        for (int i = 0; i < audiences.length(); i++) {
            if (client.equals(audiences.optString(i))) { return true; }
        }
        return false;
    }

}
