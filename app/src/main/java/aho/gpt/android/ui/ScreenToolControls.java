package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.access.AssistantAccessibilityService;
import aho.gpt.android.tools.ToolPreferences;

/** Masks saved screen-tool choices while access is unavailable. */
final class ScreenToolControls implements AutoCloseable {
    private final Activity activity;
    private final ToolPreferences preferences;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::refresh;
    private Runnable changed;
    private AlertDialog dialog;
    private boolean active;
    private boolean connected;

    ScreenToolControls(Activity activity, ToolPreferences preferences) {
        this.activity = activity; this.preferences = preferences;
    }
    boolean available(RecognizedTool tool) {
        return !tool.requiresScreenAccess() || AssistantAccessibilityService.connected() != null;
    }
    void bind(Switch toggle, RecognizedTool tool) {
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(available(tool) && preferences.selected(tool.key()));
        toggle.setContentDescription(activity.getString(R.string.enable_tool, tool.name(), tool.summary()));
        toggle.setOnCheckedChangeListener((button, checked) -> select(toggle, tool, checked));
    }
    private void select(Switch toggle, RecognizedTool tool, boolean checked) {
        if (checked && !available(tool)) {
            bind(toggle, tool); showPermission();
        } else { preferences.select(tool.key(), checked); }
    }
    void observe(Runnable changed) {
        this.changed = changed;
        if (changed == null) { dismiss(); }
    }
    void start() { stop(); active = true; refresh(); }
    private void refresh() {
        if (!active) { return; }
        boolean available = AssistantAccessibilityService.connected() != null;
        if (connected != available) {
            connected = available;
            if (changed != null) { changed.run(); }
        }
        main.postDelayed(tick, 1000);
    }
    private void showPermission() {
        dismiss();
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.accessibility_name)
                .setMessage(activity.getString(R.string.diagnostic_detail,
                        activity.getString(R.string.diagnostic_not_connected), activity.getString(R.string.accessibility_disclosure)))
                .setPositiveButton(R.string.open_settings, (ignored, which) -> openSettings())
                .setNegativeButton(R.string.close, null).show();
    }
    private void openSettings() {
        try { activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(activity, R.string.setting_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
    private void dismiss() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
    void stop() { active = false; main.removeCallbacksAndMessages(null); dismiss(); }
    @Override public void close() { stop(); changed = null; }
}
