package aho.gpt.android.logging.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Writes documents using tree URIs, without assuming a filesystem path or overwriting old exports. */
public final class AndroidWritableDocumentTree {
    private AndroidWritableDocumentTree() {
    }

    public static Uri createFile(Context context, Uri tree,
                                 String mimeType, String name) throws IOException {
        String parentId = directoryId(tree);
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, parentId);
        String uniqueName = uniqueName(context, tree, parentId, name);
        Uri document = DocumentsContract.createDocument(context.getContentResolver(), parent, mimeType, uniqueName);
        if (document == null) {
            throw new IOException("Could not create output document");
        }
        return document;
    }

    private static String uniqueName(Context context, Uri tree,
                                     String parentId, String name) throws IOException {
        Set<String> names = new HashSet<>();
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        try (Cursor cursor = queryNames(context, children)) {
            if (cursor == null) {
                throw new IOException("Could not list output folder");
            }
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0));
            }
        }
        int index = 1;
        String candidate = name;
        int dot = name.lastIndexOf('.');
        while (names.contains(candidate)) {
            index++;
            candidate = dot > 0 ? name.substring(0, dot) + "-" + index + name.substring(dot)
                    : name + "-" + index;
        }
        return candidate;
    }

    private static Cursor queryNames(Context context, Uri children) {
        return query(context, children, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME});
    }

    @android.annotation.SuppressLint("ObsoleteSdkInt") // Shared handheld code supports Android 6–7.
    static Cursor query(Context context, Uri uri, String[] columns) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getContentResolver().query(uri, columns, Bundle.EMPTY, null);
        }
        return context.getContentResolver().query(uri, columns, null, null, null);
    }

    static String directoryId(Uri folder) {
        try {
            return DocumentsContract.getDocumentId(folder);
        } catch (IllegalArgumentException e) {
            return DocumentsContract.getTreeDocumentId(folder);
        }
    }

    public static void removeQuietly(Context context, Uri document) {
        if (document == null) {
            return;
        }
        try {
            DocumentsContract.deleteDocument(context.getContentResolver(), document);
        } catch (IOException | RuntimeException ignored) {
            // A disappearing provider can also prevent cleanup of its partial document.
        }
    }
}
