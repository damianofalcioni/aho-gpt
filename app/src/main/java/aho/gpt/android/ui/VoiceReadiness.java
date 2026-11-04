package aho.gpt.android.ui;

import android.app.Activity;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.android.speech.SpeechAvailability;

final class VoiceReadiness {
    private VoiceReadiness() { }
    static int issue(Activity activity, boolean inputAvailable) {
        AssistantApplication app = (AssistantApplication) activity.getApplication();
        boolean custom = new ProviderSettings(app).customEnabled();
        if (!custom && !app.chatgptEnabled()) { return R.string.provider_disabled; }
        int speech = SpeechAvailability.inputIssue(activity, inputAvailable);
        if (speech != 0) { return speech; }
        return !custom && app.model().isEmpty() ? R.string.setup_required : 0;
    }
}
