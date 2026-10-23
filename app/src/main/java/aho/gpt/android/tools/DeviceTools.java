package aho.gpt.android.tools;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import aho.gpt.android.access.AssistantAccessibilityService;
import aho.gpt.openai.AgentLoop;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public final class DeviceTools implements AgentLoop.Tools {
    private final IntentCatalog catalog;
    private final IntentExecutor executor;
    private final Handler main = new Handler(Looper.getMainLooper());

    public DeviceTools(IntentCatalog catalog, IntentExecutor executor) {
        this.catalog = catalog; this.executor = executor;
    }
    @Override public boolean accessibilityAvailable() { return AssistantAccessibilityService.connected() != null; }

    @Override public String execute(String name, JSONObject args) throws Exception {
        switch (name) {
            case "find_intents": return catalog.search(args).toString();
            case "invoke_intent": return invoke(args);
            case "read_screen": return onTarget(() -> service().screen().read(service()).toString());
            case "ui_action":
                return onTarget(() -> act(args));
            default: return "Unknown device tool";
        }
    }

    private String invoke(JSONObject args) throws Exception {
        IntentCapability cap = catalog.get(args.getString("capability_id"));
        return onMain(() -> executor.execute(cap, args));
    }

    private String act(JSONObject args) throws Exception {
        AssistantAccessibilityService current = service();
        boolean success;
        switch (args.getString("action")) {
            case "back": success = current.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); break;
            case "home": success = current.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME); break;
            case "recents": success = current.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS); break;
            default: success = current.screen().act(args.optString("node_id"), args.getString("action"), args.optString("text")); break;
        }
        return new JSONObject().put("accepted_by_android", success).toString();
    }
    private AssistantAccessibilityService service() {
        AssistantAccessibilityService current = AssistantAccessibilityService.connected();
        if (current == null) { throw new IllegalStateException("Accessibility is not enabled"); }
        return current;
    }
    private String onMain(Callable<String> action) throws Exception {
        FutureTask<String> task = new FutureTask<>(action);
        main.post(task);
        try { return task.get(15, TimeUnit.SECONDS); }
        finally { task.cancel(false); }
    }
    private String onTarget(Callable<String> action) throws Exception {
        onMain(() -> { executor.revealTargetApp(); return ""; });
        // Allow the window manager to restore the target's focus before screen/global actions.
        Thread.sleep(200);
        return onMain(action);
    }
}
