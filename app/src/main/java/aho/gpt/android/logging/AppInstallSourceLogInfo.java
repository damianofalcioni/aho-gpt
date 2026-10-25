package aho.gpt.android.logging;

import android.content.Context;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.os.Build;

final class AppInstallSourceLogInfo {
    private static final String UNKNOWN = "unknown";
    private static final String SEPARATOR = "; ";

    private AppInstallSourceLogInfo() {
    }

    static void append(StringBuilder out, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            appendInstallSourceInfo(out, context);
            return;
        }
        append(out, "appInstallerPackage", legacyInstallerPackage(context));
    }

    private static void appendInstallSourceInfo(StringBuilder out, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { return; }
        try {
            InstallSourceInfo installSourceInfo = context.getPackageManager()
                    .getInstallSourceInfo(context.getPackageName());
            append(out, "appInstallingPackage", installSourceInfo.getInstallingPackageName());
            append(out, "appInitiatingPackage", installSourceInfo.getInitiatingPackageName());
            append(out, "appOriginatingPackage", installSourceInfo.getOriginatingPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            append(out, "appInstallingPackage", UNKNOWN);
            append(out, "appInitiatingPackage", UNKNOWN);
            append(out, "appOriginatingPackage", UNKNOWN);
        }
    }

    @SuppressWarnings("deprecation")
    private static String legacyInstallerPackage(Context context) {
        String installerPackage = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return installerPackage == null ? UNKNOWN : installerPackage;
    }

    private static void append(StringBuilder out, String key, String value) {
        out.append(SEPARATOR)
                .append(key)
                .append("=")
                .append(safe(value));
    }

    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }
}
