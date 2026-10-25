package aho.gpt.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Stable preference identity, independent of scan IDs, labels and declaration ordering. */
public final class CapabilityIdentity {
    private CapabilityIdentity() { }

    public static String key(JSONObject declaration) {
        JSONArray identity = new JSONArray();
        for (String field : new String[]{"kind", "component", "action", "schemes", "mime_types", "authorities", "paths", "categories"}) {
            identity.put(canonical(declaration.opt(field)));
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(identity.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("intent:");
            for (byte value : hash) { result.append(Character.forDigit((value & 255) >>> 4, 16)).append(Character.forDigit(value & 15, 16)); }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }

    private static String canonical(Object value) {
        List<String> parts = new ArrayList<>();
        if (value instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) { parts.add(canonical(array.opt(i))); }
        } else if (value instanceof JSONObject object) {
            var keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                parts.add(new JSONArray().put(key).put(canonical(object.opt(key))).toString());
            }
        } else { return String.valueOf(value); }
        Collections.sort(parts);
        return new JSONArray(parts).toString();
    }
}
