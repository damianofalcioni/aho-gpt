package aho.gpt.android.tools;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.core.CapabilityIndex;
import aho.gpt.core.CapabilityMetadata;
import aho.gpt.core.CapabilityQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public final class IntentCatalog {
    private final Context context;
    private final Map<String, IntentCapability> capabilities = new LinkedHashMap<>();
    private final ToolPreferences preferences;
    private int scanned;
    private int unreadable;

    public IntentCatalog(Context context) { this.context = context; preferences = new ToolPreferences(context); }

    public ToolPreferences preferences() { return preferences; }

    public synchronized void scan() {
        AppLogger.d("Catalog", "Scan started");
        Map<String, IntentCapability> found = new LinkedHashMap<>();
        PackageManager manager = context.getPackageManager();
        PackageManifestScanner scanner = new PackageManifestScanner(manager, context.getPackageName());
        scanned = 0; unreadable = 0;
        for (PackageInfo info : installed(manager)) {
            if (info.packageName.equals(context.getPackageName())) { continue; }
            scanned++;
            try { for (IntentCapability cap : scanner.scan(info)) { found.put(cap.id, cap); } }
            catch (Exception exception) { unreadable(exception); }
        }
        for (IntentCapability cap : new ResolvedActivityScanner(manager, context.getPackageName()).scan()) {
            found.put(cap.id, cap);
        }
        capabilities.clear(); capabilities.putAll(found);
        AppLogger.i("Catalog", "Scan complete packages=" + scanned + " unreadable=" + unreadable + " actions=" + found.size());
    }
    private void unreadable(Exception exception) {
        unreadable++;
        if (unreadable == 1) { AppLogger.w("Catalog", "Manifest unreadable; first failure", exception); }
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
        return search(new JSONObject().put("query", query).put("offset", offset));
    }

    public synchronized JSONObject search(JSONObject arguments) throws Exception {
        CapabilityQuery query = new CapabilityQuery(arguments);
        CapabilityIndex index = enabledIndex();
        JSONObject result;
        switch (arguments.optString("mode", "search")) {
            case "overview": result = index.overview(query); break;
            case "search": result = page(index.search(query, arguments.optInt("offset"))); break;
            default: throw new IllegalArgumentException("Discovery mode must be overview or search");
        }
        return result.put("packages_scanned", scanned).put("unreadable_manifests", unreadable);
    }

    private CapabilityIndex enabledIndex() {
        Map<String, CapabilityMetadata> metadata = new LinkedHashMap<>();
        for (IntentCapability cap : capabilities.values()) {
            metadata.put(cap.id, cap.metadata());
        }
        return new CapabilityIndex(metadata, id -> preferences.allows(capabilities.get(id).preferenceKey()));
    }

    public synchronized List<IntentCapability> entries() { return new ArrayList<>(capabilities.values()); }

    private JSONObject page(CapabilityIndex.Page matches) throws Exception {
        JSONArray page = new JSONArray();
        for (String id : matches.ids()) { page.put(capabilities.get(id).describe()); }
        return new JSONObject().put("capabilities", page).put("total", matches.total()).put("next_offset", matches.nextOffset());
    }

    public synchronized IntentCapability get(String id) {
        IntentCapability cap = capabilities.get(id);
        if (cap == null) { throw new IllegalArgumentException("Unknown or stale capability. Search the catalog again."); }
        return cap;
    }
    public synchronized int size() { return capabilities.size(); }
}
