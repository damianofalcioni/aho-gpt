package aho.gpt.android.tools;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONObject;

public final class IntentExecutor {
    private final Activity context;
    public IntentExecutor(Activity context) { this.context = context; }

    @android.annotation.SuppressLint("WearRecents") // External app tools must launch in the target app's task, separate from the voice overlay.
    public String execute(IntentCapability cap, JSONObject arguments) throws Exception {
        Intent intent = build(cap, arguments);
        switch (cap.kind) {
            case "activity": context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); break;
            case "service":
                if (context.startService(intent) == null) { return "Service did not start"; }
                break;
            case "broadcast": context.sendBroadcast(intent); return "Broadcast dispatched; app completion cannot be verified.";
            default: throw new IllegalArgumentException("Unsupported component type");
        }
        return "Intent dispatched; this does not verify that the target app completed the task.";
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
