package aho.gpt.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import aho.gpt.R;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.android.logging.AppLogger;

/** Explicit component for TV button mappers: android.ui.VoiceActivity. */
public final class VoiceActivity extends Activity {
    private VoiceController controller;
    private final VoiceDismissal dismissal = new VoiceDismissal(this::finish);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        root.setFitsSystemWindows(true);
        VoicePanel panel = new VoicePanel(this, this::speak, this::finish);
        int margin = getResources().getDimensionPixelSize(R.dimen.voice_panel_margin);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(VoicePanelSize.width(this), VoicePanelSize.height(this),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.setMargins(margin, margin, margin, margin);
        root.addView(panel, params); setContentView(root);
        controller = new VoiceController(this, panel, dismissal);
        speak();
    }
    private void speak() {
        dismissal.cancel();
        if (!SpeechPreferences.enabled(this, SpeechPreferences.INPUT)) { controller.listen(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            AppLogger.i("Speech", "Microphone permission requested");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else { controller.listen(); }
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != 1) { return; }
        AppLogger.i("Speech", "Microphone permission granted=" + (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED));
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) { controller.listen(); }
        else { controller.error(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS); }
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
