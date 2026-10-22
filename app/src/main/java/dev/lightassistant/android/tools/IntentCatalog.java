package dev.lightassistant.android.tools;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public final class IntentCatalog {
    private final Context context;
    private final Map<String, IntentCapability> capabilities = new LinkedHashMap<>();
    private int scanned;
    private int unreadable;

    public IntentCatalog(Context context) { this.context = context; }

    public synchronized void scan() {
        Map<String, IntentCapability> found = new LinkedHashMap<>();
        PackageManager manager = context.getPackageManager();
        PackageManifestScanner scanner = new PackageManifestScanner(manager, context.getPackageName());
        scanned = 0; unreadable = 0;
        for (PackageInfo info : installed(manager)) {
            if (info.packageName.equals(context.getPackageName())) { continue; }
            scanned++;
            try { for (IntentCapability cap : scanner.scan(info)) { found.put(cap.id, cap); } }
            catch (Exception exception) { unreadable++; }
        }
        for (IntentCapability cap : new ResolvedActivityScanner(manager, context.getPackageName()).scan()) {
            found.put(cap.id, cap);
        }
        capabilities.clear(); capabilities.putAll(found);
    }

    private List<PackageInfo> installed(PackageManager manager) {
        int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            return manager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags));
        }
        return legacyPackages(manager, flags);
    }
    @SuppressWarnings("deprecation") // Required for Android 6–12.
    private List<PackageInfo> legacyPackages(PackageManager manager, int flags) { return manager.getInstalledPackages(flags); }

    public synchronized JSONObject search(String query, int offset) throws Exception {
        String needle = query.toLowerCase(Locale.ROOT);
        List<IntentCapability> matches = new ArrayList<>();
        for (IntentCapability cap : capabilities.values()) {
            String haystack = cap.label + " " + cap.component + " " + cap.action;
            if (haystack.toLowerCase(Locale.ROOT).contains(needle)) { matches.add(cap); }
        }
        Collections.sort(matches, (first, second) -> Boolean.compare(
                first.launcherCategory().isEmpty(), second.launcherCategory().isEmpty()));
        JSONArray page = new JSONArray();
        int start = Math.max(0, offset);
        int end = Math.min(matches.size(), start + 25);
        for (int i = start; i < end; i++) { page.put(matches.get(i).describe()); }
        return new JSONObject().put("capabilities", page).put("total", matches.size())
                .put("next_offset", end < matches.size() ? end : -1)
                .put("packages_scanned", scanned).put("unreadable_manifests", unreadable);
    }

    public synchronized IntentCapability get(String id) {
        IntentCapability cap = capabilities.get(id);
        if (cap == null) { throw new IllegalArgumentException("Unknown or stale capability. Search the catalog again."); }
        return cap;
    }
    public synchronized int size() { return capabilities.size(); }
}
