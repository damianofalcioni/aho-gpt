package aho.gpt.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import aho.gpt.android.net.ProviderSettings;

/** Prompt only after a user enables or saves the custom provider. */
final class CustomNetworkAccess {
    private CustomNetworkAccess() { }
    static void request(Activity activity) {
        if (Build.VERSION.SDK_INT < 37 || !new ProviderSettings(activity).customEnabled()) { return; }
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.ACCESS_LOCAL_NETWORK}, 42);
        }
    }
}
