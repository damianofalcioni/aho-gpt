package aho.gpt.android.ui;

import aho.gpt.android.language.LocalizedActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.net.ProviderSettings;

/** Launcher with local setup status and the primary voice action. */
public final class MainActivity extends LocalizedActivity {
    private boolean ready;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        findViewById(R.id.about_settings).setOnClickListener(view -> settings());
        findViewById(R.id.home_account).setOnClickListener(view -> settings(R.id.chatgpt_settings));
        findViewById(R.id.home_model).setOnClickListener(view -> settings(R.id.chatgpt_settings));
        findViewById(R.id.speak).setOnClickListener(view -> {
            if (ready) { startActivity(new Intent(this, VoiceActivity.class)); }
            else { settings(R.id.chatgpt_settings); }
        });
        findViewById(R.id.speak).requestFocus();
    }
    @Override protected void onResume() { super.onResume(); AccountSummary.load(this, this::summary); }
    private void summary(AccountSummary.Snapshot snapshot) {
        ready = snapshot.ready();
        ((TextView) findViewById(R.id.home_account)).setText(snapshot.account());
        ((TextView) findViewById(R.id.home_model)).setText(snapshot.model());
    }
    private void settings() { startActivity(new Intent(this, SettingsActivity.class)); }
    private void settings(int target) {
        int selected = new ProviderSettings(this).customEnabled() ? R.id.custom_settings : target;
        startActivity(new Intent(this, SettingsActivity.class).putExtra(SettingsActivity.TARGET, selected));
    }
}
