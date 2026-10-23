package aho.gpt.android.tools;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

/** Supplements manifest parsing with filters Android actually resolves, including split APK activities. */
final class ResolvedActivityScanner {
    private final PackageManager packages;
    private final String callerPackage;

    ResolvedActivityScanner(PackageManager packages, String callerPackage) {
        this.packages = packages; this.callerPackage = callerPackage;
    }

    List<IntentCapability> scan() {
        List<IntentCapability> result = new ArrayList<>();
        add(result, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER));
        add(result, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER));
        add(result, web("https"));
        add(result, web("http"));
        return result;
    }

    private Intent web(String scheme) {
        // Resolver queries only: no request is sent to this reserved domain.
        return new Intent(Intent.ACTION_VIEW, Uri.parse(scheme + "://example.invalid"))
                .addCategory(Intent.CATEGORY_BROWSABLE);
    }

    private void add(List<IntentCapability> result, Intent query) {
        for (ResolveInfo resolved : query(query)) {
            if (!usable(resolved)) { continue; }
            ActivityInfo info = resolved.activityInfo;
            result.add(new IntentCapability(info.applicationInfo.loadLabel(packages).toString(), "activity",
                    new ComponentName(info.packageName, info.name), query.getAction(), resolved.filter));
        }
    }

    private List<ResolveInfo> query(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            return packages.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.GET_RESOLVED_FILTER));
        }
        return legacyQuery(intent);
    }

    @SuppressWarnings("deprecation") // Required for Android 6–12.
    private List<ResolveInfo> legacyQuery(Intent intent) {
        return packages.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
    }

    private boolean usable(ResolveInfo resolved) {
        if (resolved.filter == null || resolved.activityInfo == null) { return false; }
        ActivityInfo info = resolved.activityInfo;
        if (!accessible(info)) { return false; }
        return info.permission == null || packages.checkPermission(info.permission, callerPackage) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean accessible(ActivityInfo info) {
        return info.exported && info.enabled && info.applicationInfo.enabled && !callerPackage.equals(info.packageName);
    }
}
