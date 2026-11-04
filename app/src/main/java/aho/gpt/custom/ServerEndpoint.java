package aho.gpt.custom;

import java.net.URI;

/** A user supplied API base; credentials belong in the separate token field. */
public final class ServerEndpoint {
    public static final String DEFAULT = "https://api.openai.com/v1";
    private ServerEndpoint() { }
    public static String normalize(String value) {
        URI uri = URI.create(value.trim());
        validate(uri);
        String base = uri.toString();
        while (base.endsWith("/")) { base = base.substring(0, base.length() - 1); }
        return base;
    }
    private static void validate(URI uri) {
        String scheme = uri.getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) { throw new IllegalArgumentException("HTTP required"); }
        validateHost(uri);
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) { throw new IllegalArgumentException("Invalid API base"); }
    }
    private static void validateHost(URI uri) {
        if (uri.getHost() == null || uri.getRawUserInfo() != null) { throw new IllegalArgumentException("Invalid host"); }
        if (uri.getPort() == 0 || uri.getPort() > 65535) { throw new IllegalArgumentException("Invalid port"); }
    }
}
