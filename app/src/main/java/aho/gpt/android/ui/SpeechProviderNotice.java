package aho.gpt.android.ui;

import android.app.Activity;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.speech.SpeechAvailability;

/** A toast for a refused STT enable attempt, never for settings refresh. */
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
        close();
        toast = Toast.makeText(activity, R.string.speech_google_disabled_notice, Toast.LENGTH_SHORT);
        toast.show();
        return true;
    }
    @Override public void close() {
        if (toast != null) { toast.cancel(); toast = null; }
    }
}
