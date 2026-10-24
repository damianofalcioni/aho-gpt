package aho.gpt.android.ui;

import android.os.Handler;
import android.os.Looper;

/** Keeps panel dismissal independent of recognition timeouts and foreground changes. */
final class VoiceDismissal {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable finish;

    VoiceDismissal(Runnable finish) { this.finish = finish; }

    void schedule() {
        cancel();
        main.postDelayed(finish, 3_000);
    }

    void cancel() { main.removeCallbacksAndMessages(null); }
}
