package aho.gpt.android.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
import aho.gpt.R;

/** Opens public Android settings without an intermediate app dialog. */
final class PermissionSettings {
    private PermissionSettings() { }

    static void app(Activity activity) {
        open(activity, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + activity.getPackageName())));
    }
    static void open(Activity activity, Intent intent) {
        try { activity.startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(activity, R.string.setting_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
}
