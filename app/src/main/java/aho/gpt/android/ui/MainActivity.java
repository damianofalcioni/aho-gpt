package aho.gpt.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import aho.gpt.R;

/** The launcher has only the app logo and the primary voice action. */
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        findViewById(R.id.about_settings).setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.speak).setOnClickListener(view ->
                startActivity(new Intent(this, VoiceActivity.class)));
        findViewById(R.id.speak).requestFocus();
    }
}
