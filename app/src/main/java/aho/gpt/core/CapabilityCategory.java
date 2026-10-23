package aho.gpt.core;

import java.util.ArrayList;
import java.util.List;

/** Descriptions refer to standard declared contracts, never inferred custom-app behavior. */
public enum CapabilityCategory {
    OPEN_APP("open_app", "Open an app's main screen.",
            "Leave data_uri, mime_type and string_extras empty."),
    OPEN_LINK("open_link", "Open a web URL or website link in a browser or matching app.",
            "Set data_uri to the requested HTTP/HTTPS URL. Honor the declared hosts and paths."),
    VIEW_CONTENT("view_content", "View data using a declared URI scheme or MIME type.",
            "Match data_uri and mime_type to the filter. File/content URIs and URI grants are unsupported.", "VIEW"),
    SHARE_CONTENT("share_content", "Receive shared text, images or other declared content types.",
            "ACTION_SEND text may use string_extras android.intent.extra.TEXT and mime_type text/plain. "
                    + "Image streams, multiple-item lists and URI grants are unsupported.", "SEND", "SEND_MULTIPLE"),
    CAPTURE_PHOTO("capture_photo", "Open a camera photo-capture activity.",
            "Returned photos and output URI grants are unsupported; dispatch does not prove capture.",
            "android.media.action.IMAGE_CAPTURE", "android.media.action.STILL_IMAGE_CAMERA"),
    CAPTURE_VIDEO("capture_video", "Open a camera video-capture activity.",
            "Returned videos and output URI grants are unsupported; dispatch does not prove recording.",
            "android.media.action.VIDEO_CAPTURE", "android.media.action.VIDEO_CAMERA"),
    DIAL("dial", "Open a phone dialer with a requested number.",
            "Set data_uri to a supported tel: URI. This does not place a call.", "DIAL"),
    SEND_MESSAGE("send_message", "Open a message or email composition activity for a declared URI scheme.",
            "Use the declared mailto:, sms: or other scheme. Dispatch does not prove a message was sent.", "SENDTO"),
    PICK_CONTENT("pick_content", "Open a content or document selection activity.",
            "Match the declared MIME type. Returning selected content to the assistant is unsupported.",
            "PICK", "GET_CONTENT", "OPEN_DOCUMENT"),
    EDIT_CONTENT("edit_content", "Open an activity that declares editing a supported content type.",
            "Match the filter. File/content URIs and URI grants are unsupported.", "EDIT"),
    CUSTOM("custom", "Other exported intent; its task meaning is unknown.",
            "Do not infer semantics from app labels or component names, or invent extras. Seek documented behavior.");

    public final String id;
    public final String description;
    public final String usage;
    private final List<String> actions;

    CapabilityCategory(String id, String description, String usage, String... actions) {
        this.id = id; this.description = description; this.usage = usage; this.actions = new ArrayList<>();
        for (String action : actions) {
            this.actions.add(action.contains(".") ? action : "android.intent.action." + action);
        }
    }

    public static CapabilityCategory classify(CapabilityMetadata metadata) {
        if (!"activity".equals(metadata.kind())) { return CUSTOM; }
        if (metadata.launcher()) { return OPEN_APP; }
        if (web(metadata)) { return OPEN_LINK; }
        return forAction(metadata.action());
    }

    private static boolean web(CapabilityMetadata metadata) {
        return "android.intent.action.VIEW".equals(metadata.action())
                && (metadata.schemes().contains("http") || metadata.schemes().contains("https"));
    }

    private static CapabilityCategory forAction(String action) {
        for (CapabilityCategory category : values()) {
            if (category.actions.contains(action)) { return category; }
        }
        return CUSTOM;
    }

    public static void validate(String id) {
        if (id.isEmpty()) { return; }
        for (CapabilityCategory category : values()) { if (category.id.equals(id)) { return; } }
        throw new IllegalArgumentException("Unknown capability category; request mode overview for available categories.");
    }
}
