package aho.gpt.android.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.widget.Switch;
import aho.gpt.R;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.android.speech.SpeechAvailability;
import aho.gpt.android.speech.SpeechPreferences;

/** Exercises enable-attempt toasts without changing saved choices or Google state. */
public final class SpeechProviderNoticeTest {
    private final Instrumentation instrumentation;

    public SpeechProviderNoticeTest(Instrumentation instrumentation) { this.instrumentation = instrumentation; }
    public void run() {
        var context = instrumentation.getTargetContext();
        Activity activity = instrumentation.startActivitySync(new Intent(context, SettingsActivity.class)
                .setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try { instrumentation.runOnMainSync(() -> check(activity)); }
        finally {
            instrumentation.runOnMainSync(activity::finish);
            instrumentation.waitForIdleSync();
        }
    }
    private void check(Activity activity) {
        boolean[] googleDisabled = {true};
        int[] googleChecks = {0};
        SpeechProviderNotice notice = new SpeechProviderNotice(activity, () -> { googleChecks[0]++; return googleDisabled[0]; });
        try {
            require(!notice.showIfNeeded(true), "An available alternative must prevent the Google warning");
            enableAttempt(activity, notice, googleChecks);
            googleDisabled[0] = false;
            require(!notice.showIfNeeded(false), "Enabled or absent Google must not show the disabled warning");
            googleDisabled[0] = true;
            require(notice.showIfNeeded(false), "A repeated enable attempt can show the toast again");
            switchState(activity);
        } finally { notice.close(); }
    }
    private void enableAttempt(Activity activity, SpeechProviderNotice notice, int[] googleChecks) {
        boolean saved = SpeechPreferences.enabled(activity, SpeechPreferences.INPUT);
        Switch control = new Switch(activity);
        SpeechInputToggle input = new SpeechInputToggle(activity, control, () -> false, notice);
        input.refresh(false, true);
        require(googleChecks[0] == 0, "Opening or refreshing settings must not request a toast");
        require(control.isEnabled() && !control.isChecked(), "Unavailable STT must remain off but accept an attempt");
        control.performClick();
        require(googleChecks[0] == 1 && !control.isChecked(), "Only an enable attempt may request the warning");
        require(SpeechPreferences.enabled(activity, SpeechPreferences.INPUT) == saved, "Refusal must preserve the saved choice");
        input.refresh(false, true);
        require(googleChecks[0] == 1, "Resuming settings must not repeat the warning");
        input.close();
    }
    private void switchState(Activity activity) {
        Switch toggle = activity.findViewById(R.id.stt_enabled);
        boolean platform = !new ProviderSettings(activity).serverAudio();
        boolean available = platform && SpeechAvailability.input(activity);
        require(toggle.isEnabled(), "STT must accept enable attempts for feedback");
        require(toggle.getAlpha() == (available ? 1f : 0.5f), "Unavailable STT must appear muted");
        require(toggle.isChecked() == (available && SpeechPreferences.enabled(activity, SpeechPreferences.INPUT)),
                "Provider checks must preserve the saved STT choice");
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
