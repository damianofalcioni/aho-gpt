package aho.gpt.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.access.AssistantAccessibilityService;
import java.util.ArrayList;
import java.util.List;

final class AccessDiagnostics implements AutoCloseable {
    private static final String GOOGLE = "com.google.android.googlequicksearchbox";
    private final Activity activity;
    private final List<DiagnosticRow> rows = new ArrayList<>();

    AccessDiagnostics(Activity activity) {
        this.activity = activity;
        add(R.string.diagnostic_microphone, () -> appSettings(activity.getPackageName()));
        add(R.string.diagnostic_google, () -> appSettings(GOOGLE));
        add(R.string.accessibility_name, () -> activity.findViewById(R.id.accessibility).performClick());
        add(R.string.diagnostic_overlay, () -> activity.findViewById(R.id.overlay).performClick());
        add(R.string.diagnostic_assistant, () -> activity.findViewById(R.id.default_assistant).performClick());
    }
    private void add(int title, Runnable action) {
        rows.add(new DiagnosticRow(activity, R.id.diagnostic_access, title, action));
    }
    void refresh() {
        int microphone = microphone();
        rows.get(0).state(microphone == R.string.diagnostic_ok, R.string.diagnostic_ok, microphone,
                activity.getString(R.string.diagnostic_microphone_help));
        google();
        boolean connected = AssistantAccessibilityService.connected() != null;
        rows.get(2).state(connected, R.string.diagnostic_connected, R.string.diagnostic_not_connected,
                activity.getString(R.string.accessibility_disclosure));
        rows.get(3).state(Settings.canDrawOverlays(activity), R.string.diagnostic_allowed, R.string.diagnostic_not_allowed,
                activity.getString(R.string.diagnostic_overlay_help, activity.getString(connected
                        ? R.string.diagnostic_connected : R.string.diagnostic_not_connected)));
        boolean assistant = isAssistant();
        rows.get(4).update(assistant ? R.string.diagnostic_selected : R.string.diagnostic_not_selected,
                assistant ? R.color.diagnostic_ok : R.color.diagnostic_warning,
                activity.getString(R.string.diagnostic_assistant_help));
    }
    private int microphone() {
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            return R.string.diagnostic_missing;
        }
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return R.string.diagnostic_no_permission;
        }
        AudioManager audio = activity.getSystemService(AudioManager.class);
        if (audio != null && audio.isMicrophoneMute()) { return R.string.diagnostic_muted; }
        return R.string.diagnostic_ok;
    }
    private void google() {
        try {
            boolean enabled = activity.getPackageManager().getApplicationInfo(GOOGLE, 0).enabled;
            rows.get(1).state(enabled, R.string.diagnostic_enabled, R.string.diagnostic_disabled,
                    activity.getString(R.string.diagnostic_google_help));
        } catch (PackageManager.NameNotFoundException exception) {
            rows.get(1).update(R.string.diagnostic_not_installed, R.color.diagnostic_warning,
                    activity.getString(R.string.diagnostic_google_help));
        }
    }
    private boolean isAssistant() {
        String selected = Settings.Secure.getString(activity.getContentResolver(), "assistant");
        ComponentName component = selected == null ? null : ComponentName.unflattenFromString(selected);
        return component != null && activity.getPackageName().equals(component.getPackageName());
    }
    private void appSettings(String name) {
        try { activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + name))); }
        catch (android.content.ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(activity, R.string.setting_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
    @Override public void close() { for (DiagnosticRow row : rows) { row.close(); } }
}
