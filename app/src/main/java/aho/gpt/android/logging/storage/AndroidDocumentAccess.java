package aho.gpt.android.logging.storage;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

/** Public document-provider routes used by log storage and browsing. */
final class AndroidDocumentAccess {
    private static final String AUTHORITY = "com.android.externalstorage.documents";
    private AndroidDocumentAccess() { }
    static boolean isExternalStorageDocument(Uri uri) { return AUTHORITY.equals(uri.getAuthority()); }
    static Uri buildExternalStorageTreeUri(String id) { return DocumentsContract.buildTreeDocumentUri(AUTHORITY, id); }
    static Uri buildExternalStorageDocumentUri(String id) { return DocumentsContract.buildDocumentUri(AUTHORITY, id); }
    static Intent openDocumentTreeIntent(Uri initial) {
        return initial(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), initial);
    }
    static Intent openDocumentIntent(Uri initial) {
        return initial(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), initial);
    }
    @android.annotation.SuppressLint("ObsoleteSdkInt") // Shared handheld code supports Android 6–7.
    private static Intent initial(Intent intent, Uri initial) {
        if (Build.VERSION.SDK_INT >= 26) { intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial); }
        return intent;
    }
}
