package aho.gpt.android.net;

import android.content.Context;
import android.content.SharedPreferences;
import aho.gpt.android.auth.CredentialStore;
import aho.gpt.custom.ServerEndpoint;
import org.json.JSONObject;
import java.util.UUID;

/** Provider selection is committed atomically; custom credentials never join OAuth records. */
public final class ProviderSettings {
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
    public String token() throws Exception { return credentials.load().optString("token"); }
    public void enable(boolean custom, boolean enabled) {
        SharedPreferences.Editor edit = settings.edit().putBoolean(custom ? "custom_enabled" : "chatgpt_enabled", enabled);
        if (enabled) { edit.putBoolean(custom ? "chatgpt_enabled" : "custom_enabled", false); }
        edit.putString("provider_revision", UUID.randomUUID().toString()).apply();
    }
    public void save(String endpoint, String token, String model) throws Exception {
        String base = ServerEndpoint.normalize(endpoint);
        credentials.save(new JSONObject().put("token", token));
        settings.edit().putString("custom_endpoint", base).putString("custom_model", model)
                .putString("provider_revision", UUID.randomUUID().toString()).apply();
    }
}
