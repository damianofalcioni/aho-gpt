package aho.gpt.android.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.WindowManager;
import aho.gpt.R;
import aho.gpt.android.access.AssistantAccessibilityService;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.android.language.AppLanguage;

/** Mirrors the voice panel over target apps without taking their input focus. */
final class VoiceOverlay {
    private static final String TAG = "Overlay";
    private final Context app;
    private final Runnable onClose;
    private WindowManager manager;
    private VoicePanel view;

    VoiceOverlay(Context context, Runnable onClose) { app = context.getApplicationContext(); this.onClose = onClose; }

    void show(String status, String message) {
        if (status.isEmpty()) { return; }
        if (view == null) { attach(); }
        if (view != null) { view.show(status, message); }
    }

    private void attach() {
        AssistantAccessibilityService access = AssistantAccessibilityService.connected();
        if (access != null && attach(access, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)) { return; }
        if (!Settings.canDrawOverlays(app)) { AppLogger.d(TAG, "Permission unavailable"); return; }
        attach(app, overlayType());
    }

    boolean attach(Context owner, int type) {
        try {
            // The service's manager holds its accessibility token; a configuration context loses it.
            WindowManager windows = owner.getSystemService(WindowManager.class);
            Context context = new ContextThemeWrapper(AppLanguage.wrap(owner), AppTheme.style(app));
            VoicePanel candidate = new VoicePanel(context, this::speak, onClose);
            windows.addView(candidate, parameters(context, type));
            manager = windows; view = candidate;
            AppLogger.d(TAG, "Attached accessibility=" + (type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY));
            return true;
        } catch (RuntimeException exception) {
            AppLogger.w(TAG, "Attach failed", exception);
            return false;
        }
    }

    private WindowManager.LayoutParams parameters(Context context, int type) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                VoicePanelSize.width(context), VoicePanelSize.height(context), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = context.getResources().getDimensionPixelSize(R.dimen.voice_panel_margin);
        return params;
    }

    @android.annotation.SuppressLint("WearRecents") // Returning from an overlay requires NEW_TASK.
    private void speak() {
        app.startActivity(new Intent(app, VoiceActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    void speaking(boolean active) { if (view != null) { view.speaking(active); } }

    @android.annotation.SuppressLint("ObsoleteSdkInt") // Shared phone code supports Android 6–7.
    @SuppressWarnings("deprecation") // Android 6–7 overlay window type.
    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    }

    void close() {
        if (view == null) { return; }
        try { manager.removeView(view); }
        catch (IllegalArgumentException exception) {
            AppLogger.w(TAG, "Detach failed", exception); view.setVisibility(android.view.View.GONE);
        }
        view = null;
        AppLogger.d(TAG, "Closed");
    }
}
