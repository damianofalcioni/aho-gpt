package dev.lightassistant.android.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.ContextThemeWrapper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import dev.lightassistant.R;
import dev.lightassistant.android.access.AssistantAccessibilityService;

public final class AnswerOverlay {
    private final Context app;
    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager manager;
    private LinearLayout view;

    public AnswerOverlay(Context context) { app = context; }

    @android.annotation.SuppressLint("WearRecents") // A service overlay has no Activity context; NEW_TASK is required.
    public void show(String answer) {
        close();
        AssistantAccessibilityService access = AssistantAccessibilityService.connected();
        if (access == null && !Settings.canDrawOverlays(app)) { return; }
        Context context = new ContextThemeWrapper(access == null ? app : access, R.style.AppTheme);
        manager = context.getSystemService(WindowManager.class);
        view = new LinearLayout(context); view.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * context.getResources().getDisplayMetrics().density);
        view.setPadding(padding, padding, padding, padding); view.setBackgroundResource(R.drawable.bg_panel);
        TextView text = new TextView(context); text.setText(answer); text.setTextColor(0xFFFFFFFF);
        text.setTextSize(18); text.setMaxLines(8); view.addView(text);
        Button speak = new Button(context); speak.setText(R.string.speak);
        speak.setOnClickListener(button -> { close(); app.startActivity(new Intent(app, VoiceActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); });
        addButton(speak);
        Button dismiss = new Button(context); dismiss.setText(R.string.close);
        dismiss.setOnClickListener(button -> close()); addButton(dismiss);
        int type = access != null ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY : overlayType();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(-2, -2, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        try { manager.addView(view, params); main.postDelayed(this::close, 12_000); }
        catch (RuntimeException exception) { view = null; }
    }

    private void addButton(Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = Math.round(8 * app.getResources().getDisplayMetrics().density);
        view.addView(button, params);
    }

    @android.annotation.SuppressLint("ObsoleteSdkInt") // Shared phone code also supports Android 6–7.
    @SuppressWarnings("deprecation") // Android 6–7 overlay window type.
    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    }
    public void close() {
        main.removeCallbacksAndMessages(null);
        if (view != null) {
            try { manager.removeView(view); }
            catch (IllegalArgumentException exception) { view.setVisibility(android.view.View.GONE); }
            view = null;
        }
    }
}
