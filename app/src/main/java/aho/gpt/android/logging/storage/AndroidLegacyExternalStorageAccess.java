package aho.gpt.android.logging.storage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public final class AndroidLegacyExternalStorageAccess {
    private AndroidLegacyExternalStorageAccess() { }
    public static boolean isRuntimeReadPermissionRelevant() { return Build.VERSION.SDK_INT <= 28; }
    public static boolean needsOutputPermission(Context context) {
        return isRuntimeReadPermissionRelevant()
                && (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);
    }
    public static void requestOutputPermission(Activity activity, int request) {
        activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, request);
    }
}
