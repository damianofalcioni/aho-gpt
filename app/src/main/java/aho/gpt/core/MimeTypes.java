package aho.gpt.core;

import java.util.List;
import java.util.Locale;

/** Matches MIME metadata for discovery only; Android still validates execution against the real filter. */
final class MimeTypes {
    private MimeTypes() { }

    static boolean matches(List<String> candidates, String requested) {
        if (requested.isEmpty()) { return true; }
        for (String candidate : candidates) {
            if (matches(candidate.toLowerCase(Locale.ROOT), requested)) { return true; }
        }
        return false;
    }

    private static boolean matches(String candidate, String requested) {
        String[] declared = candidate.split("/", -1);
        String[] wanted = requested.split("/", -1);
        if (declared.length != 2 || wanted.length != 2) { return false; }
        return part(declared[0], wanted[0]) && part(declared[1], wanted[1]);
    }

    private static boolean part(String declared, String wanted) {
        return "*".equals(declared) || "*".equals(wanted) || declared.equals(wanted);
    }
}
