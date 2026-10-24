package aho.gpt.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import aho.gpt.android.speech.SpeechPreferences;

/** Explicit component for TV button mappers: android.ui.VoiceActivity. */
public final class VoiceActivity extends Activity {
    private VoiceController controller;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        root.setFitsSystemWindows(true);
        VoicePanel panel = new VoicePanel(this, this::speak, this::finish);
        boolean watch = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        int height = Math.min(380, getResources().getConfiguration().screenHeightDp - 24);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1,
                watch ? -1 : Math.round(height * getResources().getDisplayMetrics().density), Gravity.BOTTOM);
        root.addView(panel, params); setContentView(root);
        controller = new VoiceController(this, panel);
        speak();
    }
    private void speak() {
        if (!SpeechPreferences.enabled(this, SpeechPreferences.INPUT)) { controller.listen(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else { controller.listen(); }
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == 1 && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) { controller.listen(); }
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        speak();
    }
    @Override protected void onResume() { super.onResume(); controller.foreground(true); }
    @Override protected void onPause() { controller.foreground(false); super.onPause(); }
    @Override public void finish() {
        if (controller != null) { controller.close(); }
        if (isTaskRoot()) { super.finishAndRemoveTask(); }
        else { super.finish(); }
    }
    @Override protected void onDestroy() { controller.close(); super.onDestroy(); }
}
