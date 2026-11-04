package aho.gpt.android.ui;

import android.app.Activity;
import android.widget.Switch;
import aho.gpt.R;
import aho.gpt.android.speech.SpeechAvailability;
import aho.gpt.android.speech.SpeechPreferences;

/** Unavailable STT stays off and muted, while taps can explain a refused enable attempt. */
final class SpeechInputToggle implements AutoCloseable {
    interface Availability { boolean available(); }
    private final Activity activity;
    private final Switch control;
    private final Availability availability;
    private final SpeechProviderNotice notice;

    SpeechInputToggle(Activity activity) {
        this(activity, activity.findViewById(R.id.stt_enabled), () -> SpeechAvailability.input(activity),
                new SpeechProviderNotice(activity));
    }
    SpeechInputToggle(Activity activity, Switch control, Availability availability, SpeechProviderNotice notice) {
        this.activity = activity; this.control = control; this.availability = availability; this.notice = notice;
    }
    void refresh(boolean available) {
        notice.close();
        control.setOnCheckedChangeListener(null);
        control.setEnabled(true); control.setAlpha(available ? 1f : 0.5f);
        control.setChecked(available && SpeechPreferences.enabled(activity, SpeechPreferences.INPUT));
        control.setOnCheckedChangeListener((button, checked) -> select(checked));
    }
    private void select(boolean checked) {
        if (checked && !availability.available()) {
            refresh(false); notice.showIfNeeded(false); return;
        }
        SpeechPreferences.enable(activity, SpeechPreferences.INPUT, checked);
    }
    @Override public void close() { notice.close(); }
}
