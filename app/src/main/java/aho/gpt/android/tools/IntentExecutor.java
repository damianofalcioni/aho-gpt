package aho.gpt.android.tools;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import aho.gpt.openai.ToolOutcome;
import aho.gpt.core.RequestScope;
import org.json.JSONObject;

public final class IntentExecutor {
    private final Activity context;
    public IntentExecutor(Activity context) { this.context = context; }

    public String execute(IntentCapability cap, JSONObject arguments) throws Exception {
        return execute(cap, arguments, new RequestScope());
    }
    public String execute(IntentCapability cap, JSONObject arguments, RequestScope scope) throws Exception {
        scope.check();
        Intent intent;
        try { intent = build(cap, arguments); }
        catch (IllegalArgumentException | SecurityException exception) { return ToolOutcome.refused("INVALID_INTENT_ARGUMENTS"); }
        try { return scope.dispatch(() -> dispatch(cap, intent)); }
        catch (SecurityException | android.content.ActivityNotFoundException exception) {
            return ToolOutcome.rejected("ANDROID_DISPATCH_REJECTED");
        } catch (RuntimeException exception) { return ToolOutcome.unknown("INTENT_DISPATCH_UNCONFIRMED"); }
    }
    @android.annotation.SuppressLint("WearRecents") // External tools must launch in the target app's task, separate from the voice overlay.
    private String dispatch(IntentCapability cap, Intent intent) throws Exception {
        if (!allowed(cap)) { return ToolOutcome.refused("TOOL_DISABLED"); }
        switch (cap.kind) {
            case "activity": context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); break;
            case "service":
                if (context.startService(intent) == null) { return ToolOutcome.rejected("SERVICE_NOT_STARTED"); }
                break;
            case "broadcast": context.sendBroadcast(intent); break;
            default: throw new IllegalArgumentException("Unsupported component type");
        }
        return ToolOutcome.accepted("INTENT_DISPATCHED");
    }

    private boolean allowed(IntentCapability cap) {
        ToolPreferences preferences = new ToolPreferences(context);
        return preferences.allows("invoke_intent") && preferences.allows(cap.preferenceKey());
    }

    public void revealTargetApp() { context.moveTaskToBack(true); }

    private Intent build(IntentCapability cap, JSONObject args) throws Exception {
        Intent intent = baseIntent(cap);
        String data = args.optString("data_uri");
        String mime = args.optString("mime_type");
        Uri uri = data.isEmpty() ? null : Uri.parse(data);
        if (blocked(uri)) {
            throw new SecurityException("File, content and nested intent URIs are not supported");
        }
        intent.setDataAndType(uri, mime.isEmpty() ? null : mime);
        if (cap.filter.match(context.getContentResolver(), intent, false, "aho-gpt") < 0) {
            throw new IllegalArgumentException("Arguments do not match the discovered intent filter");
        }
        extras(intent, args.optJSONObject("string_extras"));
        return intent;
    }

    private Intent baseIntent(IntentCapability cap) {
        Intent intent = new Intent(cap.action).setComponent(cap.component);
        String category = cap.launcherCategory();
        if (!category.isEmpty()) {
            intent.addCategory(category).addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }
        return intent;
    }

    private void extras(Intent intent, JSONObject extras) throws Exception {
        if (extras != null) { addExtras(intent, extras); }
    }

    private boolean blocked(Uri uri) {
        if (uri == null) { return false; }
        return "file".equals(uri.getScheme()) || "content".equals(uri.getScheme()) || "intent".equals(uri.getScheme());
    }

    private void addExtras(Intent intent, JSONObject extras) throws Exception {
        if (extras.length() > 16) { throw new IllegalArgumentException("Too many extras"); }
        var keys = extras.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = extras.getString(key);
            if (value.length() > 4096) { throw new IllegalArgumentException("Extra is too long"); }
            intent.putExtra(key, value);
        }
    }
}
