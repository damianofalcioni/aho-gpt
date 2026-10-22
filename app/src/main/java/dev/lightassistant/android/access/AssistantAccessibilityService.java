package dev.lightassistant.android.access;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/** No event logging, passive screen capture, key interception or background model requests. */
public final class AssistantAccessibilityService extends AccessibilityService {
    private static volatile AssistantAccessibilityService connected;
    private final AccessibleScreen screen = new AccessibleScreen();

    @Override protected void onServiceConnected() { connected = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* Read only on explicit tool request. */ }
    @Override public void onInterrupt() { screen.clear(); }
    @Override public void onDestroy() { if (equals(connected)) { connected = null; } screen.clear(); super.onDestroy(); }
    public static AssistantAccessibilityService connected() { return connected; }
    public AccessibleScreen screen() { return screen; }
}
