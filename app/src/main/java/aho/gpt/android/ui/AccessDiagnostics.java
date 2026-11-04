package aho.gpt.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import aho.gpt.R;
import aho.gpt.android.access.ScreenAccessGrant;

/** Reports grants only, independently of hardware, providers and app switches. */
final class AccessDiagnostics {
    private final Activity activity;
    private final DiagnosticRow microphone;
    private final DiagnosticRow overlay;
    private final DiagnosticRow screen;
    private final DiagnosticRow assistant;

    AccessDiagnostics(Activity activity) {
        this.activity = activity;
        microphone = new DiagnosticRow(activity, R.string.diagnostic_microphone,
                () -> PermissionSettings.app(activity));
        overlay = new DiagnosticRow(activity, R.string.diagnostic_overlay,
                () -> PermissionSettings.open(activity, new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()))));
        screen = new DiagnosticRow(activity, R.string.accessibility_name,
                () -> PermissionSettings.open(activity, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        assistant = new DiagnosticRow(activity, R.string.diagnostic_assistant,
                () -> PermissionSettings.open(activity, new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)));
    }
    void refresh() {
        microphone.state(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
        overlay.state(Settings.canDrawOverlays(activity));
        screen.state(ScreenAccessGrant.granted(activity));
        assistant.state(isAssistant());
    }
    private boolean isAssistant() {
        String selected = Settings.Secure.getString(activity.getContentResolver(), "assistant");
        ComponentName component = selected == null ? null : ComponentName.unflattenFromString(selected);
        return component != null && activity.getPackageName().equals(component.getPackageName());
    }
}
