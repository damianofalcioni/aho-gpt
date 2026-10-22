package dev.lightassistant.android.ui;

import android.content.Context;
import dev.lightassistant.R;
import dev.lightassistant.android.speech.SpeechPreferences;
import java.util.Locale;

/** Keeps voice state messages and transient answer surfaces separate from session control. */
final class VoicePresentation {
    private static final String SECTION_BREAK = "\n\n";
    private final Context context;
    private final VoicePanel panel;
    private final AnswerOverlay overlay;
    private String lastAnswer = "";

    VoicePresentation(Context context, VoicePanel panel) {
        this.context = context; this.panel = panel; overlay = new AnswerOverlay(context.getApplicationContext());
    }
    Locale listening(boolean followUp) {
        Locale language = SpeechPreferences.locale(context, SpeechPreferences.INPUT);
        panel.show(context.getString(R.string.listening_language, SpeechPreferences.label(context, language)),
                followUp ? lastAnswer + SECTION_BREAK + context.getString(R.string.follow_up) : "");
        return language;
    }
    void thinking(String text) { panel.show(context.getString(R.string.thinking), text); }
    String answered(String answer, boolean foreground) {
        lastAnswer = answer.isEmpty() ? context.getString(R.string.no_answer) : answer;
        panel.show(context.getString(R.string.answer), lastAnswer);
        if (!foreground) { overlay.show(lastAnswer); }
        return lastAnswer;
    }
    void playbackFailed(String reason, boolean foreground) {
        String message = lastAnswer + SECTION_BREAK + reason;
        panel.show(context.getString(R.string.unavailable), message);
        if (!foreground) { overlay.show(message); }
    }
    void expired() {
        panel.show(context.getString(R.string.ready), lastAnswer + SECTION_BREAK + context.getString(R.string.session_ended));
    }
    void failed(String message) {
        panel.show(context.getString(R.string.unavailable), message == null ? context.getString(R.string.try_again) : message);
    }
    void closeOverlay() { overlay.close(); }
}
