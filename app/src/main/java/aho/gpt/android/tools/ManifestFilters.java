package aho.gpt.android.tools;

import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import org.xmlpull.v1.XmlPullParser;

/** Reads compiled manifest XML through public AssetManager APIs; no hidden API reflection. */
final class ManifestFilters {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private ManifestFilters() { }

    static IntentFilter read(XmlResourceParser xml) throws Exception {
        IntentFilter filter = new IntentFilter();
        int depth = xml.getDepth();
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.getEventType() == XmlPullParser.END_TAG && xml.getDepth() == depth) { return filter; }
            if (xml.getEventType() == XmlPullParser.START_TAG) { add(xml, filter); }
        }
        return filter;
    }

    private static void add(XmlResourceParser xml, IntentFilter filter) throws Exception {
        String name = xml.getAttributeValue(ANDROID, "name");
        switch (xml.getName()) {
            case "action": if (name != null) { filter.addAction(name); } break;
            case "category": if (name != null) { filter.addCategory(name); } break;
            case "data": data(xml, filter); break;
            default: break;
        }
    }

    private static void data(XmlResourceParser xml, IntentFilter filter) throws Exception {
        String scheme = xml.getAttributeValue(ANDROID, "scheme");
        String mime = xml.getAttributeValue(ANDROID, "mimeType");
        String host = xml.getAttributeValue(ANDROID, "host");
        if (scheme != null) { filter.addDataScheme(scheme); }
        if (mime != null) { filter.addDataType(mime); }
        if (host != null) { filter.addDataAuthority(host, xml.getAttributeValue(ANDROID, "port")); }
        path(xml, filter, "path", android.os.PatternMatcher.PATTERN_LITERAL);
        path(xml, filter, "pathPrefix", android.os.PatternMatcher.PATTERN_PREFIX);
        path(xml, filter, "pathPattern", android.os.PatternMatcher.PATTERN_SIMPLE_GLOB);
    }

    private static void path(XmlResourceParser xml, IntentFilter filter, String key, int type) {
        String value = xml.getAttributeValue(ANDROID, key);
        if (value != null) { filter.addDataPath(value, type); }
    }
}
