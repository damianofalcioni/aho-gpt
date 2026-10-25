package aho.gpt.android.logging;

import android.app.Activity;
import android.app.Application;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

/** Lifecycle and failure metadata only. Never inspect intents, view text or exception messages. */
public final class AppDiagnostics implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "App";
    private AppDiagnostics() { }
    public static void install(Application app) {
        AppLogger.init(app);
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            try { AppLogger.anomaly(app, TAG, "Uncaught exception", failure); }
            finally { terminate(previous, thread, failure); }
        });
        previousExit(app);
        app.registerActivityLifecycleCallbacks(new AppDiagnostics());
        AppLogger.i(TAG, "Application started");
    }
    private static void terminate(Thread.UncaughtExceptionHandler previous, Thread thread, Throwable failure) {
        if (previous != null) { previous.uncaughtException(thread, failure); }
        else { Process.killProcess(Process.myPid()); System.exit(10); }
    }
    private static void previousExit(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_exit_diagnostics", Context.MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 30) {
            int pid = prefs.getInt("pid", 0);
            if (pid > 0) { reportExit(context, pid, prefs.getLong("started", 0)); }
        }
        prefs.edit().putInt("pid", Process.myPid()).putLong("started", System.currentTimeMillis()).apply();
    }
    private static void reportExit(Context context, int pid, long started) {
        if (Build.VERSION.SDK_INT < 30) { return; }
        try {
            ActivityManager manager = context.getSystemService(ActivityManager.class);
            if (manager == null) { return; }
            for (ApplicationExitInfo exit : manager.getHistoricalProcessExitReasons(context.getPackageName(), pid, 10)) {
                if (exit.getTimestamp() >= started && anomalous(exit.getReason())) {
                    AppLogger.anomaly(context, TAG, "Previous process exit reason=" + exit.getReason()
                            + " status=" + exit.getStatus() + " timestamp=" + exit.getTimestamp(), null);
                    return;
                }
            }
        } catch (RuntimeException ignored) {
            // Exit history is best effort on OEM builds.
        }
    }
    private static boolean anomalous(int reason) {
        return reason == 2 || reason == 4 || reason == 5 || reason == 6 || reason == 7 || reason == 9;
    }
    private void activity(Activity activity, String state) { AppLogger.i(TAG, activity.getClass().getSimpleName() + state); }
    @Override public void onActivityCreated(Activity activity, Bundle state) { activity(activity, " created"); }
    @Override public void onActivityStarted(Activity activity) { activity(activity, " started"); }
    @Override public void onActivityResumed(Activity activity) { activity(activity, " resumed"); }
    @Override public void onActivityPaused(Activity activity) { activity(activity, " paused"); }
    @Override public void onActivityStopped(Activity activity) { activity(activity, " stopped"); }
    @Override public void onActivityDestroyed(Activity activity) { activity(activity, " destroyed"); }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { activity(activity, " saved instance state"); }
}
