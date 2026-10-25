package aho.gpt.android.ui;

import android.app.Activity;
import android.content.Context;
import aho.gpt.R;
import aho.gpt.android.speech.SpeechPreferences;
import java.util.Locale;

/** Keeps voice state messages and transient answer surfaces separate from session control. */
final class VoicePresentation {
    private static final String SECTION_BREAK = "\n\n";
    private final Context context;
    private final VoicePanel panel;
    private final VoiceOverlay overlay;
    private String lastAnswer = "";
    private String status = "";
    private String message = "";
    private boolean foreground = true;
    private boolean speaking;

    VoicePresentation(Activity activity, VoicePanel panel) {
        context = activity; this.panel = panel; overlay = new VoiceOverlay(activity, activity::finish);
    }
    void foreground(boolean visible) {
        foreground = visible;
        if (visible) { overlay.close(); }
        else { overlay.show(status, message); overlay.speaking(speaking); }
    }
    Locale listening(boolean followUp) {
        Locale language = SpeechPreferences.locale(context, SpeechPreferences.INPUT);
        show(context.getString(R.string.listening_language, SpeechPreferences.label(context, language)),
                followUp ? lastAnswer : "");
        panel.listening(true);
        return language;
    }
    void listeningStopped() { panel.listening(false); }
    void playbackStarted() { speaking = true; panel.speaking(true); overlay.speaking(true); }
    void playbackStopped() { speaking = false; panel.speaking(false); overlay.speaking(false); }
    void thinking(String text) { show(context.getString(R.string.thinking), text); }
    void progress(boolean executingTool) {
        show(context.getString(executingTool ? R.string.executing_tool : R.string.thinking), message);
    }
    String answered(String answer) {
        lastAnswer = answer.isEmpty() ? context.getString(R.string.no_answer) : answer;
        show(context.getString(R.string.answer), lastAnswer);
        return lastAnswer;
    }
    void playbackFailed(String reason) {
        playbackStopped();
        show(context.getString(R.string.unavailable), lastAnswer + SECTION_BREAK + reason);
    }
    void finished() {
        show(context.getString(R.string.voice_finished), lastAnswer);
    }
    void failed(String message) {
        playbackStopped();
        show(context.getString(R.string.unavailable), message == null ? context.getString(R.string.try_again) : message);
    }
    private void show(String state, String text) {
        status = state; message = text; speaking = false;
        panel.show(state, text);
        if (!foreground) { overlay.show(state, text); }
    }
    void closeOverlay() { overlay.close(); }
}
