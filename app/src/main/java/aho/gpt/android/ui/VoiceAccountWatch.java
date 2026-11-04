package aho.gpt.android.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;

/** Retires active work when the provider is disabled or sign-out clears the model. */
final class VoiceAccountWatch implements AutoCloseable {
    interface Unavailable { void accept(String message); }
    private final SharedPreferences settings;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener;

    VoiceAccountWatch(Activity activity, Unavailable unavailable) {
        AssistantApplication app = (AssistantApplication) activity.getApplication();
        settings = activity.getSharedPreferences("settings", Activity.MODE_PRIVATE);
        listener = (preferences, key) -> {
            if ("chatgpt_enabled".equals(key) || "model".equals(key)) {
                if (!app.chatgptEnabled()) { unavailable.accept(activity.getString(R.string.chatgpt_disabled)); }
                else if (app.model().isEmpty()) { unavailable.accept(activity.getString(R.string.setup_required)); }
            }
        };
        settings.registerOnSharedPreferenceChangeListener(listener);
    }
    @Override public void close() { settings.unregisterOnSharedPreferenceChangeListener(listener); }
}
