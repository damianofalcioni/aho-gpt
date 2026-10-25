package aho.gpt.android.logging;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import aho.gpt.BuildConfig;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

final class AppLogSessionInfo {
    private AppLogSessionInfo() { }
    static String formatDestination(Context context, String destination) {
        StringBuilder out = new StringBuilder("Log session system info");
        append(out, "androidVersion", Build.VERSION.RELEASE);
        append(out, "androidSdk", Build.VERSION.SDK_INT);
        append(out, "androidSecurityPatch", Build.VERSION.SECURITY_PATCH);
        append(out, "appVersion", BuildConfig.VERSION_NAME);
        append(out, "versionCode", BuildConfig.VERSION_CODE);
        append(out, "applicationId", BuildConfig.APPLICATION_ID);
        append(out, "flavor", BuildConfig.FLAVOR);
        append(out, "buildType", BuildConfig.BUILD_TYPE);
        append(out, "targetSdk", context.getApplicationInfo().targetSdkVersion);
        append(out, "minSdk", minSdk(context));
        AppInstallSourceLogInfo.append(out, context);
        device(out, context);
        append(out, "logFile", destination);
        return out.toString();
    }
    private static void device(StringBuilder out, Context context) {
        append(out, "manufacturer", Build.MANUFACTURER);
        append(out, "brand", Build.BRAND);
        append(out, "model", Build.MODEL);
        append(out, "device", Build.DEVICE);
        append(out, "product", Build.PRODUCT);
        append(out, "hardware", Build.HARDWARE);
        append(out, "supportedAbis", Arrays.toString(Build.SUPPORTED_ABIS));
        append(out, "locale", Locale.getDefault().toLanguageTag());
        append(out, "timeZone", TimeZone.getDefault().getID());
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        append(out, "screen", metrics.widthPixels + "x" + metrics.heightPixels);
        append(out, "densityDpi", metrics.densityDpi);
    }
    private static void append(StringBuilder out, String key, Object value) {
        out.append("; ").append(key).append('=').append(value);
    }
    @android.annotation.SuppressLint("ObsoleteSdkInt") // Shared handheld code supports Android 6.
    private static int minSdk(Context context) {
        if (Build.VERSION.SDK_INT >= 24) { return context.getApplicationInfo().minSdkVersion; }
        return 23;
    }
}
