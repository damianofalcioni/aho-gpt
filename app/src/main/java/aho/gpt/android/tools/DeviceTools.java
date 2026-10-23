package aho.gpt.android.tools;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import aho.gpt.android.access.AssistantAccessibilityService;
import aho.gpt.openai.AgentLoop;
import aho.gpt.core.RequestScope;
import aho.gpt.openai.ToolArguments;
import aho.gpt.openai.ToolOutcome;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public final class DeviceTools implements AgentLoop.Tools {
    private final IntentCatalog catalog;
    private final IntentExecutor executor;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final RequestScope scope;

    public DeviceTools(IntentCatalog catalog, IntentExecutor executor) {
        this(catalog, executor, new RequestScope());
    }
    public DeviceTools(IntentCatalog catalog, IntentExecutor executor, RequestScope scope) {
        this.catalog = catalog; this.executor = executor; this.scope = scope;
    }
    @Override public boolean accessibilityAvailable() { return AssistantAccessibilityService.connected() != null; }

    @Override public String execute(String name, JSONObject args) throws Exception {
        scope.check();
        try { ToolArguments.validate(name, args, accessibilityAvailable()); }
        catch (Exception exception) { return ToolOutcome.refused("INVALID_ARGUMENTS"); }
        try { return executeValidated(name, args); }
        catch (InterruptedException exception) { throw exception; }
        catch (ExecutionException exception) { return executionFailure(exception); }
        catch (Exception exception) { return ToolOutcome.unknown("TOOL_EXECUTION_UNCONFIRMED"); }
    }
    private String executionFailure(ExecutionException exception) throws Exception {
        if (exception.getCause() instanceof InterruptedException) { throw (InterruptedException) exception.getCause(); }
        return ToolOutcome.unknown("TOOL_EXECUTION_UNCONFIRMED");
    }
    private String executeValidated(String name, JSONObject args) throws Exception {
        switch (name) {
            case "find_intents": return catalog.search(args).toString();
            case "invoke_intent": return invoke(args);
            case "read_screen": return onTarget(() -> service().screen().read(service()).toString());
            case "ui_action":
                return onTarget(() -> act(args));
            default: return ToolOutcome.refused("UNKNOWN_TOOL");
        }
    }

    private String invoke(JSONObject args) throws Exception {
        IntentCapability cap;
        try { cap = catalog.get(args.getString("capability_id")); }
        catch (IllegalArgumentException exception) { return ToolOutcome.refused("STALE_CAPABILITY"); }
        return onMain(() -> executor.execute(cap, args, scope));
    }

    private String act(JSONObject args) throws Exception {
        AssistantAccessibilityService current = service();
        boolean success;
        try {
            scope.check();
            switch (args.getString("action")) {
                case "back": success = current.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); break;
                case "home": success = current.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME); break;
                case "recents": success = current.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS); break;
                default: success = current.screen().act(args.optString("node_id"), args.getString("action"), args.optString("text")); break;
            }
        } finally { current.screen().clear(); }
        return success ? ToolOutcome.accepted("UI_ACTION_ACCEPTED") : ToolOutcome.rejected("UI_ACTION_REJECTED");
    }
    private AssistantAccessibilityService service() {
        AssistantAccessibilityService current = AssistantAccessibilityService.connected();
        if (current == null) { throw new IllegalStateException("Accessibility is not enabled"); }
        return current;
    }
    private String onMain(Callable<String> action) throws Exception {
        scope.check();
        FutureTask<String> task = new FutureTask<>(() -> scope.dispatch(action));
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
