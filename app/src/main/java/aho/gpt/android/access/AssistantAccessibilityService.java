package aho.gpt.android.access;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import aho.gpt.android.logging.AppLogger;

/** No event logging, passive screen capture, key interception or background model requests. */
public final class AssistantAccessibilityService extends AccessibilityService {
    private static volatile AssistantAccessibilityService connected;
    private final AccessibleScreen screen = new AccessibleScreen();

    @Override protected void onServiceConnected() { connected = this; AppLogger.i("Accessibility", "Connected"); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* Read only on explicit tool request. */ }
    @Override public void onInterrupt() { AppLogger.w("Accessibility", "Interrupted"); screen.clear(); }
    @Override public void onDestroy() {
        AppLogger.i("Accessibility", "Destroyed");
        if (equals(connected)) { connected = null; } screen.clear(); super.onDestroy();
    }
    public static AssistantAccessibilityService connected() { return connected; }
    public AccessibleScreen screen() { return screen; }
}
