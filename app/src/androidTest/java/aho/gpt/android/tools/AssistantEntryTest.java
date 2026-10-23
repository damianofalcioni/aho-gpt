package aho.gpt.android.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.voice.VoiceInteractionService;
import aho.gpt.android.ui.VoiceActivity;

/** Verifies the assistant picker can select the voice activity without replacing the system recognizer. */
final class AssistantEntryTest {
    private final Context context;

    AssistantEntryTest(Context context) { this.context = context; }

    void run() {
        entry(Intent.ACTION_ASSIST);
        entry(Intent.ACTION_VOICE_COMMAND);
        Intent service = new Intent(VoiceInteractionService.SERVICE_INTERFACE).setPackage(context.getPackageName());
        require(context.getPackageManager().queryIntentServices(service, 0).isEmpty(),
                "A voice interaction service must not shadow the activity in the assistant picker");
    }

    private void entry(String action) {
        Intent intent = new Intent(action).setPackage(context.getPackageName());
        ResolveInfo resolved = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        require(resolved != null && resolved.activityInfo != null, "Voice entry point must resolve for " + action);
        ComponentName actual = new ComponentName(resolved.activityInfo.packageName, resolved.activityInfo.name);
        require(new ComponentName(context, VoiceActivity.class).equals(actual), "Must resolve to the voice activity");
        require(resolved.activityInfo.exported && resolved.activityInfo.enabled,
                "System assistant and external mappers must be able to open the voice activity");
    }

    private void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
