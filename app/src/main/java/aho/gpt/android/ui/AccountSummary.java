package aho.gpt.android.ui;

import android.app.Activity;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.net.ProviderSettings;
import org.json.JSONObject;

/** Loads local account metadata on the worker; never refreshes tokens or contacts the model. */
final class AccountSummary {
    interface Loaded { void loaded(Snapshot summary); }
    record Snapshot(String account, String model, int tools, boolean connected, boolean ready) { }
    private AccountSummary() { }

    static void load(Activity activity, Loaded loaded) {
        load(activity, loaded, true);
    }
    static void loadChatGpt(Activity activity, Loaded loaded) {
        load(activity, loaded, false);
    }
    private static void load(Activity activity, Loaded loaded, boolean selected) {
        AssistantApplication app = (AssistantApplication) activity.getApplication();
        app.worker().execute(() -> {
            Snapshot summary = selected && new ProviderSettings(app).customEnabled()
                    ? custom(activity, app)
                    : read(activity, app);
            activity.runOnUiThread(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) { loaded.loaded(summary); }
            });
        });
    }
    private static Snapshot custom(Activity activity, AssistantApplication app) {
        String model = new ProviderSettings(app).customModel();
        return new Snapshot(activity.getString(R.string.custom), model.isEmpty() ? activity.getString(R.string.server_model) : model,
                app.catalog().size(), true, true);
    }
    private static Snapshot read(Activity activity, AssistantApplication app) {
        String model = app.model();
        try {
            JSONObject account = app.auth().account();
            boolean connected = !account.optString("access_token").isEmpty();
            String name = account.optString("email");
            if (name.isEmpty()) { name = activity.getString(R.string.connected); }
            return new Snapshot(connected ? name : activity.getString(R.string.not_connected),
                    model.isEmpty() ? activity.getString(R.string.no_model) : model, app.catalog().size(), connected,
                    connected && !model.isEmpty() && app.chatgptEnabled());
        } catch (Exception exception) {
            return new Snapshot(activity.getString(R.string.account_unavailable),
                    model.isEmpty() ? activity.getString(R.string.no_model) : model, app.catalog().size(), false, false);
        }
    }
}
