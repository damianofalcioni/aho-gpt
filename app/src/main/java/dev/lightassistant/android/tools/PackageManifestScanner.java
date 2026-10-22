package dev.lightassistant.android.tools;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;

final class PackageManifestScanner {
    private final PackageManager packages;
    private final String callerPackage;
    PackageManifestScanner(PackageManager packages, String callerPackage) {
        this.packages = packages; this.callerPackage = callerPackage;
    }

    List<IntentCapability> scan(PackageInfo info) throws Exception {
        Map<String, ComponentInfo> components = new HashMap<>();
        add(components, info.activities); add(components, info.services); add(components, info.receivers);
        List<IntentCapability> result = new ArrayList<>();
        String label = info.applicationInfo.loadLabel(packages).toString();
        try (XmlResourceParser xml = packages.getResourcesForApplication(info.applicationInfo)
                .getAssets().openXmlResourceParser("AndroidManifest.xml")) {
            read(xml, info.packageName, label, components, result);
        }
        return result;
    }

    private void read(XmlResourceParser xml, String pkg, String label,
            Map<String, ComponentInfo> components, List<IntentCapability> output) throws Exception {
        Declaration current = null;
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            switch (xml.getEventType()) {
                case XmlPullParser.START_TAG: current = start(xml, pkg, label, components, output, current); break;
                case XmlPullParser.END_TAG: if (isComponent(xml.getName())) { current = null; } break;
                default: break;
            }
        }
    }

    private Declaration start(XmlResourceParser xml, String pkg, String label,
            Map<String, ComponentInfo> components, List<IntentCapability> output, Declaration current) throws Exception {
        String tag = xml.getName();
        if (isComponent(tag)) {
            String name = xml.getAttributeValue("http://schemas.android.com/apk/res/android", "name");
            return new Declaration(components.get(qualified(pkg, name)), kind(tag));
        }
        if ("intent-filter".equals(tag) && current != null && usable(current.info)) {
            emit(output, label, current.kind, current.info, ManifestFilters.read(xml));
        }
        return current;
    }
    private record Declaration(ComponentInfo info, String kind) { }

    private void emit(List<IntentCapability> output, String label, String kind,
            ComponentInfo component, IntentFilter filter) {
        for (int i = 0; i < filter.countActions(); i++) {
            output.add(new IntentCapability(label, kind, new ComponentName(component.packageName,
                    component.name), filter.getAction(i), filter));
        }
    }

    private boolean usable(ComponentInfo info) {
        if (info == null || !info.exported || !info.enabled || !info.applicationInfo.enabled) { return false; }
        String permission = permission(info);
        return permission == null || packages.checkPermission(permission, callerPackage) == PackageManager.PERMISSION_GRANTED;
    }

    private String permission(ComponentInfo info) {
        if (info instanceof android.content.pm.ActivityInfo activity) { return activity.permission; }
        if (info instanceof android.content.pm.ServiceInfo service) { return service.permission; }
        return null;
    }
    private void add(Map<String, ComponentInfo> map, ComponentInfo[] values) {
        if (values != null) { for (ComponentInfo value : values) { map.put(value.name, value); } }
    }
    private String qualified(String pkg, String name) {
        if (name == null) { return ""; }
        if (name.startsWith(".")) { return pkg + name; }
        return name.contains(".") ? name : pkg + "." + name;
    }
    private boolean isComponent(String tag) {
        return "activity".equals(tag) || "activity-alias".equals(tag) || "service".equals(tag) || "receiver".equals(tag);
    }
    private String kind(String tag) {
        if ("receiver".equals(tag)) { return "broadcast"; }
        return "service".equals(tag) ? "service" : "activity";
    }
}
