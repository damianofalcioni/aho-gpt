package dev.lightassistant.android.assist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import dev.lightassistant.android.ui.VoiceActivity;

final class AssistantSession extends VoiceInteractionSession {
    private final Context context;
    AssistantSession(Context context) { super(context); this.context = context; }
    @android.annotation.SuppressLint({"ObsoleteSdkInt", "WearRecents"}) // The pre-26 branch is shared phone-only service code.
    @Override public void onShow(Bundle args, int flags) {
        super.onShow(args, flags);
        Intent intent = new Intent(context, VoiceActivity.class);
        if (android.os.Build.VERSION.SDK_INT >= 26) { startAssistantActivity(intent); }
        else { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        hide();
    }
}
