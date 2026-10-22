package dev.lightassistant;

import dev.lightassistant.openai.IdentityClaims;
import java.security.GeneralSecurityException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class IdentityClaimsTest {
    private static final String ISSUER = "https://auth.openai.com";
    private static final String CLIENT = "oaiapp_test";
    private static final String NONCE = "test-nonce";

    @Test public void acceptsValidIdentity() throws Exception { verify(claims()); }
    @Test public void rejectsExpiredIdentity() throws Exception {
        JSONObject claims = claims().put("exp", 1000);
        assertThrows(GeneralSecurityException.class, () -> verify(claims));
    }
    @Test public void rejectsWrongIssuerOrNonce() throws Exception {
        JSONObject issuer = claims().put("iss", "https://example.com");
        JSONObject nonce = claims().put("nonce", "different");
        assertThrows(GeneralSecurityException.class, () -> verify(issuer));
        assertThrows(GeneralSecurityException.class, () -> verify(nonce));
    }
    @Test public void rejectsWrongAudienceAndAuthorizedParty() throws Exception {
        JSONObject audience = claims().put("aud", "other-app");
        JSONObject authorized = claims().put("azp", "other-app");
        assertThrows(GeneralSecurityException.class, () -> verify(audience));
        assertThrows(GeneralSecurityException.class, () -> verify(authorized));
    }
    @Test public void requiresAuthorizedPartyForMultipleAudiences() throws Exception {
        JSONObject claims = claims().put("aud", new JSONArray().put(CLIENT).put("other"));
        assertThrows(GeneralSecurityException.class, () -> verify(claims));
        claims.put("azp", CLIENT); verify(claims);
    }
    private JSONObject claims() throws Exception {
        return new JSONObject().put("iss", ISSUER).put("aud", CLIENT).put("sub", "account")
                .put("exp", 2000).put("nonce", NONCE);
    }
    private void verify(JSONObject claims) throws GeneralSecurityException {
        IdentityClaims.validate(claims, ISSUER, CLIENT, NONCE, 1000);
    }
}
