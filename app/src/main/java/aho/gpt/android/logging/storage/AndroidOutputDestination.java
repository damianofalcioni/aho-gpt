package aho.gpt.android.logging.storage;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/** A snapshot used by saving, browsing and deletion; deletion never follows a later fallback. */
public final class AndroidOutputDestination {
    static final String LOG_PATH = "Download/AhoGPT/logs";
    public final File directory;
    public final Uri tree;
    public final String volume;
    public final String relativePath;
    public final String label;
    private final String documentId;

    private AndroidOutputDestination(File directory, Uri tree, String volume, String relativePath,
                                     String label, String documentId) {
        this.directory = directory;
        this.tree = tree;
        this.volume = volume;
        this.relativePath = relativePath;
        this.label = label;
        this.documentId = documentId;
    }

    static AndroidOutputDestination file(File directory) {
        return new AndroidOutputDestination(directory, null, null, null,
                directory.getAbsolutePath().replace(File.separatorChar, '/'), "primary:" + LOG_PATH);
    }

    static AndroidOutputDestination downloads(String volume, String root) {
        String storageId = MediaStore.VOLUME_EXTERNAL_PRIMARY.equals(volume) ? "primary" : new File(root).getName();
        return new AndroidOutputDestination(null, null, volume, LOG_PATH + "/",
                root + "/" + LOG_PATH, storageId + ":" + LOG_PATH);
    }

    static AndroidOutputDestination documents(Uri tree, String root) {
        return new AndroidOutputDestination(null, tree, null, null,
                root + "/" + LOG_PATH, null);
    }

    Uri folderUri(Context context) {
        return tree == null ? AndroidDocumentAccess.buildExternalStorageDocumentUri(documentId)
                : AndroidOutputFolderRecovery.accessUri(context, tree);
    }

    public boolean sameLocation(AndroidOutputDestination other) {
        return Objects.equals(directory, other.directory) && Objects.equals(tree, other.tree)
                && Objects.equals(volume, other.volume) && Objects.equals(relativePath, other.relativePath);
    }

    Uri collection() throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new IOException("MediaStore Downloads requires Android 10");
        }
        return MediaStore.Downloads.getContentUri(volume);
    }

    Uri ensureTree(Context context) throws IOException {
        return AndroidOutputFolderAccess.ensureFolder(context, tree);
    }

    File ensureDirectory() throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Output folder is unavailable");
        }
        return directory;
    }
}
