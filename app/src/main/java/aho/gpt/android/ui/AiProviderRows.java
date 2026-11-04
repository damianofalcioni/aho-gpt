package aho.gpt.android.ui;

import android.app.Activity;
import android.widget.Switch;
import aho.gpt.R;
import aho.gpt.android.net.ProviderSettings;

/** Both toggles reflect one atomic provider choice. Turning either off may leave both off. */
final class AiProviderRows implements AutoCloseable {
    private final ProviderSettings settings;
    private final Activity activity;
    private final Switch chatgpt;
    private final Switch custom;
    private final ChatGptSettingsDialog account;
    private final CustomSettingsDialog server;
    private boolean refreshing;
    AiProviderRows(Activity activity) {
        this.activity = activity;
        settings = new ProviderSettings(activity);
        account = new ChatGptSettingsDialog(activity); server = new CustomSettingsDialog(activity);
        chatgpt = activity.findViewById(R.id.chatgpt_enabled); custom = activity.findViewById(R.id.custom_enabled);
        refresh();
        chatgpt.setOnCheckedChangeListener((button, enabled) -> toggle(false, enabled));
        custom.setOnCheckedChangeListener((button, enabled) -> toggle(true, enabled));
    }
    private void toggle(boolean isCustom, boolean enabled) {
        if (refreshing) { return; }
        settings.enable(isCustom, enabled); refresh();
        if (isCustom && enabled) { CustomNetworkAccess.request(activity); }
    }
    void refresh() {
        refreshing = true;
        chatgpt.setChecked(settings.chatgptEnabled()); custom.setChecked(settings.customEnabled());
        refreshing = false; account.refresh();
    }
    @Override public void close() { account.close(); server.close(); }
}
