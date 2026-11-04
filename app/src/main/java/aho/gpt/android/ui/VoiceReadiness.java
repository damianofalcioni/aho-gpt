package aho.gpt.android.ui;

import android.app.Activity;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.speech.SpeechAvailability;

final class VoiceReadiness {
    private VoiceReadiness() { }
    static int issue(Activity activity, boolean inputAvailable) {
        AssistantApplication app = (AssistantApplication) activity.getApplication();
        if (!app.chatgptEnabled()) { return R.string.chatgpt_disabled; }
        int speech = SpeechAvailability.inputIssue(activity, inputAvailable);
        if (speech != 0) { return speech; }
        return app.model().isEmpty() ? R.string.setup_required : 0;
    }
}
