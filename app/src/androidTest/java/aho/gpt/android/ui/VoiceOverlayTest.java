package aho.gpt.android.ui;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import java.lang.reflect.Proxy;

/** Checks window ownership without drawing over apps, recording speech or changing permissions. */
public final class VoiceOverlayTest {
    private final Instrumentation instrumentation;

    public VoiceOverlayTest(Instrumentation instrumentation) { this.instrumentation = instrumentation; }

    public void run() {
        instrumentation.runOnMainSync(() -> {
            mirrorsProgressAndCloses();
            rejectedWindowCanRetry();
        });
    }

    private void mirrorsProgressAndCloses() {
        RecordingOwner owner = new RecordingOwner(instrumentation.getTargetContext());
        boolean[] closed = {false};
        VoiceOverlay overlay = new VoiceOverlay(owner, () -> closed[0] = true);
        require(overlay.attach(owner, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY), "Overlay must attach");
        require(owner.requests == 1, "Use the original owner's window manager before wrapping language resources");
        require(owner.params.type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, "Keep the accessibility window type");
        require((owner.params.flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0, "Do not take target-app input focus");
        overlay.show("Executing", "Request");
        require(contains(owner.view, "Executing"), "Keep tool progress visible over the target app");
        overlay.show("Answer", "Done");
        require(contains(owner.view, "Done") && owner.added == 1, "Update the existing overlay with the answer");
        ViewGroup controls = (ViewGroup) ((ViewGroup) owner.view).getChildAt(1);
        controls.getChildAt(1).performClick();
        require(closed[0], "The overlay close control must reach session cancellation");
        overlay.close(); overlay.close();
        require(owner.removed == owner.view && owner.removals == 1, "Remove the overlay once through its owning manager");
    }

    private void rejectedWindowCanRetry() {
        RecordingOwner rejected = new RecordingOwner(instrumentation.getTargetContext());
        rejected.reject = true;
        VoiceOverlay overlay = new VoiceOverlay(rejected, () -> { });
        require(!overlay.attach(rejected, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY), "Report rejected attachment");
        overlay.close();
        require(rejected.removals == 0, "A rejected window must not be retained");
        RecordingOwner fallback = new RecordingOwner(instrumentation.getTargetContext());
        require(overlay.attach(fallback, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY), "Allow a permitted fallback window");
        overlay.show("Thinking", "Request");
        require(contains(fallback.view, "Thinking"), "Fallback must display current progress");
        overlay.close();
        require(fallback.removals == 1, "Detach through the successful fallback manager");
    }

    private boolean contains(View view, String text) {
        if (view instanceof TextView) { return text.contentEquals(((TextView) view).getText()); }
        if (!(view instanceof ViewGroup)) { return false; }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (contains(group.getChildAt(i), text)) { return true; }
        }
        return false;
    }

    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }

    private static final class RecordingOwner extends ContextWrapper {
        private final WindowManager windows;
        private View view;
        private View removed;
        private WindowManager.LayoutParams params;
        private int requests;
        private int added;
        private int removals;
        private boolean reject;

        RecordingOwner(Context context) {
            super(context);
            windows = (WindowManager) Proxy.newProxyInstance(WindowManager.class.getClassLoader(),
                    new Class<?>[]{WindowManager.class}, (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "addView":
                                if (reject) { throw new WindowManager.BadTokenException(); }
                                view = (View) arguments[0]; params = (WindowManager.LayoutParams) arguments[1]; added++;
                                return null;
                            case "removeView": removed = (View) arguments[0]; removals++; return null;
                            default: throw new AssertionError("Unexpected window operation: " + method.getName());
                        }
                    });
        }

        @Override public Object getSystemService(String name) {
            if (Context.WINDOW_SERVICE.equals(name)) { requests++; return windows; }
            return super.getSystemService(name);
        }
    }
}
