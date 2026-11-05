package aho.gpt.core;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Constraints use AND; names within apps use OR. Empty scalar fields leave constraints open. */
public final class CapabilityQuery {
    private final JSONObject arguments;
    private final String query;
    private final String app;
    private final List<String> alternatives;

    public CapabilityQuery(JSONObject arguments) {
        this.arguments = arguments; query = value("query"); app = value("app");
        alternatives = alternatives(arguments.optJSONArray("apps"));
        CapabilityCategory.validate(value("category"));
    }

    public boolean matches(CapabilityMetadata metadata) {
        return identity(metadata) && contract(metadata) && words(metadata.searchableText());
    }

    private boolean identity(CapabilityMetadata metadata) {
        return app(metadata) && exact("package", metadata.packageName()) && exact("kind", metadata.kind())
                && exact("category", metadata.category().id);
    }

    private boolean app(CapabilityMetadata metadata) {
        return (app.isEmpty() || named(metadata, app)) && alternative(metadata);
    }

    private boolean alternative(CapabilityMetadata metadata) {
        if (alternatives.isEmpty()) { return true; }
        for (String name : alternatives) { if (named(metadata, name)) { return true; } }
        return false;
    }

    private boolean named(CapabilityMetadata metadata, String name) {
        return lower(metadata.app()).contains(name) || lower(metadata.packageName()).contains(name);
    }

    private List<String> alternatives(JSONArray names) {
        List<String> result = new ArrayList<>();
        if (names == null) { return result; }
        for (int i = 0; i < names.length(); i++) {
            Object name = names.opt(i);
            if (!(name instanceof String text) || text.trim().isEmpty()) { throw new IllegalArgumentException("App name is blank or invalid"); }
            result.add(lower((String) name).trim());
        }
        return result;
    }

    private boolean contract(CapabilityMetadata metadata) {
        return exact("action", metadata.action()) && scheme(metadata.schemes())
                && MimeTypes.matches(metadata.mimeTypes(), value("mime_type"));
    }

    private boolean exact(String field, String candidate) {
        String requested = value(field);
        return requested.isEmpty() || requested.equals(lower(candidate));
    }

    private boolean scheme(List<String> candidates) {
        String requested = value("scheme");
        if (requested.isEmpty()) { return true; }
        for (String candidate : candidates) { if (requested.equals(lower(candidate))) { return true; } }
        return false;
    }

    private boolean words(String haystack) {
        if (query.isEmpty()) { return true; }
        for (String word : query.split("\\s+")) { if (!haystack.contains(word)) { return false; } }
        return true;
    }

    public int score(CapabilityMetadata metadata) {
        int relevance = appScore(metadata, app);
        for (String name : alternatives) { relevance = Math.max(relevance, appScore(metadata, name)); }
        int score = relevance + (metadata.launcher() ? 5 : 0);
        if (query.equals(lower(metadata.app()))) { score += 50; }
        return score;
    }

    private int appScore(CapabilityMetadata metadata, String name) {
        int score = name.equals(lower(metadata.app())) ? 100 : 0;
        return score + (name.equals(lower(metadata.packageName())) ? 100 : 0);
    }

    private String value(String field) { return lower(arguments.optString(field)).trim(); }
    private String lower(String value) { return value.toLowerCase(Locale.ROOT); }
}
