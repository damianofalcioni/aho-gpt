package aho.gpt.android.ui;

import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/** Pulses only the translucent surface, keeping text and controls steady. */
final class VoicePanelBackground {
    private static final int REST_ALPHA = 184;
    private static final int SURFACE_RGB = 0x00151B22;
    private final GradientDrawable background = new GradientDrawable();
    private final ValueAnimator pulse = ValueAnimator.ofInt(REST_ALPHA, 224);
    private boolean listening;
    private boolean speaking;
    private boolean visible;

    VoicePanelBackground(View view) {
        float density = view.getResources().getDisplayMetrics().density;
        background.setCornerRadius(24 * density);
        background.setStroke(Math.round(density), 0x553F4B5A);
        background.setColor(REST_ALPHA << 24 | SURFACE_RGB); view.setBackground(background);
        pulse.setDuration(1800); pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.addUpdateListener(animation -> background.setColor((int) animation.getAnimatedValue() << 24 | SURFACE_RGB));
    }
    void listening(boolean active) { listening = active; update(); }
    void speaking(boolean active) { speaking = active; update(); }
    void visible(boolean shown) { visible = shown; update(); }
    private void update() {
        if (visible && (listening || speaking)) {
            if (!pulse.isStarted()) { pulse.start(); }
        } else {
            pulse.cancel(); background.setColor(REST_ALPHA << 24 | SURFACE_RGB);
        }
    }
}
