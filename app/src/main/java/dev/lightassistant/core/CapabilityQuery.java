package dev.lightassistant.core;

import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

/** Structured constraints are combined with AND; an empty field leaves that constraint open. */
public final class CapabilityQuery {
    private final JSONObject arguments;
    private final String query;
    private final String app;

    public CapabilityQuery(JSONObject arguments) {
        this.arguments = arguments; query = value("query"); app = value("app");
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
        return app.isEmpty() || lower(metadata.app()).contains(app) || lower(metadata.packageName()).contains(app);
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
        int score = metadata.launcher() ? 5 : 0;
        if (app.equals(lower(metadata.app()))) { score += 100; }
        if (app.equals(lower(metadata.packageName()))) { score += 100; }
        if (query.equals(lower(metadata.app()))) { score += 50; }
        return score;
    }

    private String value(String field) { return lower(arguments.optString(field)).trim(); }
    private String lower(String value) { return value.toLowerCase(Locale.ROOT); }
}
