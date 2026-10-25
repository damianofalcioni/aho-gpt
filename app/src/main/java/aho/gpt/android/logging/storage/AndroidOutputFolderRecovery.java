package aho.gpt.android.logging.storage;

import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.IOException;

/** Missing external-storage folders can be recreated only beneath a still-granted ancestor. */
final class AndroidOutputFolderRecovery {
    private AndroidOutputFolderRecovery() {
    }

    static Uri accessUri(Context context, Uri selected) {
        Uri parent = ancestorGrant(context, selected);
        return parent == null ? selected : DocumentsContract.buildDocumentUriUsingTree(parent,
                DocumentsContract.getTreeDocumentId(selected));
    }

    static boolean hasGrant(Context context, Uri selected) {
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.isReadPermission() && permission.isWritePermission()
                    && permission.getUri().equals(selected)) {
                return true;
            }
        }
        return ancestorGrant(context, selected) != null;
    }

    private static Uri ancestorGrant(Context context, Uri selected) {
        if (!AndroidDocumentAccess.isExternalStorageDocument(selected)) {
            return null;
        }
        String target = DocumentsContract.getTreeDocumentId(selected);
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            Uri candidate = permission.getUri();
            if (permission.isReadPermission() && permission.isWritePermission()
                    && AndroidDocumentAccess.isExternalStorageDocument(candidate)
                    && relativePath(DocumentsContract.getTreeDocumentId(candidate), target) != null) {
                return candidate;
            }
        }
        return null;
    }

    static Uri restore(Context context, Uri selected) throws IOException {
        Uri parent = ancestorGrant(context, selected);
        if (parent == null) {
            throw new IOException("Missing folder has no granted parent");
        }
        String parentId = DocumentsContract.getTreeDocumentId(parent);
        Uri current = DocumentsContract.buildDocumentUriUsingTree(parent, parentId);
        String relative = relativePath(parentId, DocumentsContract.getTreeDocumentId(selected));
        for (String name : relative.split("/")) {
            current = childDirectory(context, parent, current, name);
        }
        return current;
    }

    static String relativePath(String parentId, String targetId) {
        String prefix = parentId.endsWith(":") ? parentId : parentId + "/";
        if (!targetId.startsWith(prefix)) {
            return null;
        }
        String relative = targetId.substring(prefix.length());
        for (String segment : relative.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return null;
            }
        }
        return relative;
    }

    private static Uri childDirectory(Context context, Uri tree, Uri parent, String name) throws IOException {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent));
        try (Cursor cursor = AndroidWritableDocumentTree.query(context, children, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE})) {
            if (cursor == null) {
                throw new IOException("Parent folder is unavailable");
            }
            while (cursor.moveToNext()) {
                if (name.equals(cursor.getString(1))) {
                    return existingDirectory(tree, cursor);
                }
            }
        }
        Uri created = DocumentsContract.createDocument(context.getContentResolver(), parent,
                DocumentsContract.Document.MIME_TYPE_DIR, name);
        if (created == null) {
            throw new IOException("Could not recreate output folder");
        }
        return created;
    }

    private static Uri existingDirectory(Uri tree, Cursor cursor) throws IOException {
        if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))) {
            throw new IOException("Output folder path contains a file");
        }
        return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0));
    }

    static boolean writableDirectory(Context context, Uri folder) {
        try (Cursor cursor = AndroidWritableDocumentTree.query(context, documentUri(folder), new String[]{
                DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_FLAGS})) {
            return cursor != null && cursor.moveToFirst()
                    && DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(0))
                    && (cursor.getInt(1) & DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Uri documentUri(Uri folder) {
        return DocumentsContract.buildDocumentUriUsingTree(folder, AndroidWritableDocumentTree.directoryId(folder));
    }

}
