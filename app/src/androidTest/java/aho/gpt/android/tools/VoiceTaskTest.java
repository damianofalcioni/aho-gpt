package aho.gpt.android.tools;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import aho.gpt.android.speech.SpeechPreferences;
import aho.gpt.android.ui.MainActivity;
import aho.gpt.android.ui.VoiceActivity;

/** Opt-in device checks for real task removal; disables STT temporarily and restores its preference. */
final class VoiceTaskTest {
    private enum Exit { AUTOMATIC, CLOSE, BACK }
    private final Instrumentation instrumentation;
    private final Context context;

    VoiceTaskTest(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        context = instrumentation.getTargetContext();
    }
    void run() {
        SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String key = SpeechPreferences.INPUT + "_enabled";
        boolean existed = settings.contains(key);
        boolean enabled = settings.getBoolean(key, true);
        Bundle result = new Bundle();
        int status = Activity.RESULT_OK;
        try {
            require(settings.edit().putBoolean(key, false).commit(), "Could not disable STT for the test");
            checks();
            result.putString("stream", "Voice task tests run: 6, Failures: 0\n");
        } catch (Exception | AssertionError failure) {
            status = Activity.RESULT_CANCELED;
            result.putString("stream", failure.toString());
        } finally {
            SharedPreferences.Editor restore = settings.edit();
            if (existed) { restore.putBoolean(key, enabled); }
            else { restore.remove(key); }
            restore.commit();
        }
        instrumentation.finish(status, result);
    }
    private void checks() {
        Activity caller = instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            verify(caller, new Intent(context, VoiceActivity.class), false, Exit.AUTOMATIC);
            verify(caller, new Intent(context, VoiceActivity.class), true, Exit.AUTOMATIC);
            verify(caller, new Intent(Intent.ACTION_ASSIST).setPackage(context.getPackageName()), true, Exit.AUTOMATIC);
            verify(caller, new Intent(Intent.ACTION_VOICE_COMMAND).setPackage(context.getPackageName()), true, Exit.AUTOMATIC);
            verify(caller, new Intent(context, VoiceActivity.class), true, Exit.CLOSE);
            verify(caller, new Intent(context, VoiceActivity.class), true, Exit.BACK);
        } finally { instrumentation.runOnMainSync(caller::finish); }
    }
    private void verify(Activity caller, Intent intent, boolean external, Exit exit) {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(VoiceActivity.class.getName(), null, false);
        Activity voice;
        try {
            if (external) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else { instrumentation.runOnMainSync(() -> caller.startActivity(intent)); }
            voice = instrumentation.waitForMonitorWithTimeout(monitor, 5_000);
        } finally { instrumentation.removeMonitor(monitor); }
        require(voice != null, "Voice activity did not open");
        try {
            require(voice.getTaskId() != caller.getTaskId(), "Voice must not join the caller task");
            require(hasTask(VoiceActivity.class), "Voice task must exist before completion");
            switch (exit) {
                case CLOSE: instrumentation.runOnMainSync(voice::finish); break;
                case BACK: instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); break;
                default: break;
            }
            awaitRemoval();
            require(voice.isFinishing() || voice.isDestroyed(), "Voice activity must finish");
            require(!caller.isFinishing() && hasTask(MainActivity.class), "Caller task must survive voice completion");
        } finally { instrumentation.runOnMainSync(voice::finish); }
    }
    private void awaitRemoval() {
        long deadline = SystemClock.elapsedRealtime() + 6_000;
        while (hasTask(VoiceActivity.class) && SystemClock.elapsedRealtime() < deadline) { SystemClock.sleep(50); }
        require(!hasTask(VoiceActivity.class), "Completed voice task is still in the app task list");
    }
    private boolean hasTask(Class<? extends Activity> activity) {
        ComponentName component = new ComponentName(context, activity);
        for (ActivityManager.AppTask task : context.getSystemService(ActivityManager.class).getAppTasks()) {
            if (component.equals(task.getTaskInfo().baseIntent.getComponent())) { return true; }
        }
        return false;
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
