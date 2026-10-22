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
        Context context = access == null ? app : access;
        if (access == null && !Settings.canDrawOverlays(app)) { return; }
        manager = context.getSystemService(WindowManager.class);
        view = new LinearLayout(context); view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(24, 20, 24, 20); view.setBackgroundColor(0xF210191F);
        TextView text = new TextView(context); text.setText(answer); text.setTextColor(0xFFFFFFFF);
        text.setTextSize(18); text.setMaxLines(8); view.addView(text);
        Button speak = new Button(context); speak.setText(R.string.speak);
        speak.setOnClickListener(button -> { close(); app.startActivity(new Intent(app, VoiceActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); });
        view.addView(speak);
        Button dismiss = new Button(context); dismiss.setText(R.string.close);
        dismiss.setOnClickListener(button -> close()); view.addView(dismiss);
        int type = access != null ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY : overlayType();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(-2, -2, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        try { manager.addView(view, params); main.postDelayed(this::close, 12_000); }
        catch (RuntimeException exception) { view = null; }
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
