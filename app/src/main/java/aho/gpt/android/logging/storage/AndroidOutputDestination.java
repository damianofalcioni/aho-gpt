package aho.gpt.android.logging.storage;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Build;

import java.io.File;
import java.io.IOException;

import aho.gpt.android.logging.storage.AppOutputFolderSettings.Kind;

/** A snapshot used by saving, browsing and deletion; deletion never follows a later fallback. */
public final class AndroidOutputDestination {
    public final File directory;
    public final Uri tree;
    public final String volume;
    public final String relativePath;
    public final String label;

    private AndroidOutputDestination(File directory, Uri tree, String volume, String relativePath, String label) {
        this.directory = directory;
        this.tree = tree;
        this.volume = volume;
        this.relativePath = relativePath;
        this.label = label;
    }

    static AndroidOutputDestination file(File directory) {
        return new AndroidOutputDestination(directory, null, null, null,
                directory.getAbsolutePath().replace(File.separatorChar, '/'));
    }

    static AndroidOutputDestination downloads(String volume, String root, Kind kind) {
        String relative = "Download/AhoGPT/" + subfolder(kind) + "/";
        return new AndroidOutputDestination(null, null, volume, relative,
                root + "/" + relative.substring(0, relative.length() - 1));
    }

    static AndroidOutputDestination documents(Uri tree, String root, Kind kind) {
        return new AndroidOutputDestination(null, tree, null, null,
                root + "/Download/AhoGPT/" + subfolder(kind));
    }

    public static String subfolder(Kind kind) {
        return switch (kind) {
            case LOGS -> "logs";
        };
    }

    public String token() {
        if (directory != null) {
            return "file|" + directory.getAbsolutePath();
        }
        return tree == null ? "downloads|" + volume + "|" + relativePath : "tree|" + tree;
    }

    static AndroidOutputDestination fromToken(String token) throws IOException {
        String[] parts = token.split("\\|", 3);
        return switch (parts[0]) {
            case "file" -> file(new File(parts[1]));
            case "tree" -> new AndroidOutputDestination(null, Uri.parse(parts[1]), null, null, parts[1]);
            case "downloads" -> new AndroidOutputDestination(null, null, parts[1], parts[2], parts[2]);
            default -> throw new IOException("Unknown output destination");
        };
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
