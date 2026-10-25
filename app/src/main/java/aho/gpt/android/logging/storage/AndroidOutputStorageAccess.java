package aho.gpt.android.logging.storage;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import java.io.IOException;

/** SD grants authorize the fixed Download/AhoGPT layout, never an arbitrary output folder. */
public final class AndroidOutputStorageAccess {
    private static final int FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

    private AndroidOutputStorageAccess() { }

    public static Intent picker(String id) {
        return AndroidDocumentAccess.openDocumentTreeIntent(
                AndroidDocumentAccess.buildExternalStorageDocumentUri(id + ":Download"))
                .addFlags(FLAGS | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
    }

    public static void accept(Context context, Intent data) throws IOException {
        Uri grant = data.getData();
        if (grant == null || (data.getFlags() & FLAGS) != FLAGS
                || (data.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) == 0) {
            throw new IOException("Persistent read/write folder access is required");
        }
        AndroidOutputStorageVolumes.Card card = validate(context, grant);
        context.getContentResolver().takePersistableUriPermission(grant, FLAGS);
        enable(context, card, grant.toString());
    }

    private static AndroidOutputStorageVolumes.Card validate(Context context, Uri grant) throws IOException {
        if (!AndroidDocumentAccess.isExternalStorageDocument(grant)) {
            throw new IOException("Select the SD card Downloads folder");
        }
        String id = DocumentsContract.getTreeDocumentId(grant);
        String[] parts = id.split(":", 2);
        AndroidOutputStorageVolumes.Card card = AndroidOutputStorageVolumes.find(context, parts[0]);
        if (card == null || parts.length != 2 || !isDownloadsAncestor(parts[1])) {
            throw new IOException("Select the SD card Downloads folder");
        }
        return card;
    }

    private static boolean isDownloadsAncestor(String path) {
        return path.isEmpty() || "Download".equals(path) || "Download/AhoGPT".equals(path);
    }

    public static boolean needsCardGrant(Context context, AndroidOutputStorageVolumes.Card card) {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && existingGrant(context, card) == null;
    }

    public static String existingGrant(Context context, AndroidOutputStorageVolumes.Card card) {
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.isReadPermission() && permission.isWritePermission()
                    && matchesCard(context, card, permission.getUri())) {
                return permission.getUri().toString();
            }
        }
        return null;
    }

    private static boolean matchesCard(Context context, AndroidOutputStorageVolumes.Card card, Uri uri) {
        try {
            return card.id.equals(validate(context, uri).id);
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    public static void enable(Context context, AndroidOutputStorageVolumes.Card card, String grant) throws IOException {
        AndroidOutputStorage.verify(context, AndroidOutputStorage.card(context, card, grant));
        AppOutputStorageSettings.select(context, card.id);
    }
}
