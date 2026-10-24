package aho.gpt.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import aho.gpt.R;

/** Launcher with local setup status and the primary voice action. */
public final class MainActivity extends Activity {
    private boolean ready;
    private int setupTarget = R.id.connect;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        findViewById(R.id.about_settings).setOnClickListener(view -> settings());
        findViewById(R.id.home_account).setOnClickListener(view -> settings(R.id.connect));
        findViewById(R.id.home_model).setOnClickListener(view -> settings(R.id.choose_model));
        findViewById(R.id.home_tools).setOnClickListener(view -> settings(R.id.refresh_tools));
        findViewById(R.id.speak).setOnClickListener(view -> {
            if (ready) { startActivity(new Intent(this, VoiceActivity.class)); }
            else { settings(setupTarget); }
        });
        findViewById(R.id.speak).requestFocus();
    }
    @Override protected void onResume() { super.onResume(); AccountSummary.load(this, this::summary); }
    private void summary(AccountSummary.Snapshot snapshot) {
        ready = snapshot.ready();
        setupTarget = snapshot.connected() ? R.id.choose_model : R.id.connect;
        ((TextView) findViewById(R.id.home_account)).setText(getString(R.string.home_account, snapshot.account()));
        ((TextView) findViewById(R.id.home_model)).setText(getString(R.string.home_model, snapshot.model()));
        ((TextView) findViewById(R.id.home_tools)).setText(getString(R.string.home_tools, snapshot.tools()));
        ((TextView) findViewById(R.id.setup_hint)).setText(ready ? R.string.home_ready : R.string.home_setup_hint);
    }
    private void settings() { startActivity(new Intent(this, SettingsActivity.class)); }
    private void settings(int target) {
        startActivity(new Intent(this, SettingsActivity.class).putExtra(SettingsActivity.TARGET, target));
    }
}
