package aho.gpt.android.logging.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import aho.gpt.logging.LogRecord;

/** Deletes only this output's generated files, retaining the folder and its other contents. */
public final class AndroidOutputFolderCleaner {

    private AndroidOutputFolderCleaner() {
    }

    public static void clear(Context context, AndroidOutputDestination selected) throws IOException {
        AndroidOutputStorageCleaner.clear(context, selected);
    }

    static void clearDocuments(Context context, Uri folder) throws IOException {
        for (Uri document : outputDocuments(context, folder)) {
            if (!DocumentsContract.deleteDocument(context.getContentResolver(), document)) {
                throw new IOException("Could not delete output document");
            }
        }
    }

    private static List<Uri> outputDocuments(Context context, Uri folder) throws IOException {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(folder,
                AndroidWritableDocumentTree.directoryId(folder));
        List<Uri> result = new ArrayList<>();
        try (Cursor cursor = AndroidWritableDocumentTree.query(context, children, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE})) {
            if (cursor == null) {
                throw new IOException("Could not list output folder");
            }
            while (cursor.moveToNext()) {
                if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))
                        && LogRecord.isLogFile(cursor.getString(1))) {
                    result.add(DocumentsContract.buildDocumentUriUsingTree(folder, cursor.getString(0)));
                }
            }
        }
        return result;
    }

}
