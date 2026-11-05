package aho.gpt.android.ui;

import android.app.Activity;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.speech.SpeechAvailability;

/** Explains refused speech enable attempts, never settings refresh. */
final class SpeechProviderNotice implements AutoCloseable {
    interface GoogleState { boolean disabled(); }
    private final Activity activity;
    private final GoogleState googleState;
    private Toast toast;

    SpeechProviderNotice(Activity activity) {
        this(activity, () -> SpeechAvailability.googleAppDisabled(activity));
    }
    SpeechProviderNotice(Activity activity, GoogleState googleState) {
        this.activity = activity; this.googleState = googleState;
    }
    boolean showIfNeeded(boolean available) {
        if (available || !googleState.disabled()) { return false; }
        show(R.string.speech_google_disabled_notice);
        return true;
    }
    void show(int message) {
        close();
        toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT);
        toast.show();
    }
    @Override public void close() {
        if (toast != null) { toast.cancel(); toast = null; }
    }
}
