package aho.gpt.android.ui;

import android.os.Handler;
import android.os.Looper;
import aho.gpt.android.logging.AppLogger;

/** Keeps panel dismissal independent of recognition timeouts and foreground changes. */
final class VoiceDismissal {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable finish;

    VoiceDismissal(Runnable finish) { this.finish = finish; }

    void schedule() {
        cancel();
        AppLogger.d("Voice", "Dismissal scheduled delayMs=3000");
        main.postDelayed(() -> { AppLogger.i("Voice", "Automatic dismissal"); finish.run(); }, 3_000);
    }

    void cancel() { main.removeCallbacksAndMessages(null); }
}
