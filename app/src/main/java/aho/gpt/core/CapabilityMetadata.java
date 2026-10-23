package aho.gpt.core;

import java.util.List;
import java.util.Locale;

/** Searchable public intent metadata, independent of Android framework classes. */
public record CapabilityMetadata(String app, String packageName, String component, String kind,
        String action, boolean launcher, List<String> schemes, List<String> mimeTypes) {
    public CapabilityCategory category() { return CapabilityCategory.classify(this); }

    public String searchableText() {
        CapabilityCategory group = category();
        return (app + " " + packageName + " " + component + " " + kind + " " + action + " "
                + schemes + " " + mimeTypes + " " + group.id + " " + group.description).toLowerCase(Locale.ROOT);
    }
}
