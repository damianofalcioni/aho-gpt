package aho.gpt.android.logging.storage;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** One output stream, with pending-download publication and partial-write cleanup. */
public final class AndroidOutputFile {
    public final AndroidOutputDestination destination;
    public final Uri uri;
    private final Context context;
    private final File file;

    private AndroidOutputFile(Context context, AndroidOutputDestination destination, Uri uri, File file) {
        this.context = context.getApplicationContext();
        this.destination = destination;
        this.uri = uri;
        this.file = file;
    }

    static AndroidOutputFile create(Context context, AndroidOutputDestination destination,
                                    String mime, String name) throws IOException {
        if (destination.directory != null) {
            File file = uniqueFile(destination.ensureDirectory(), name);
            return new AndroidOutputFile(context, destination, Uri.fromFile(file), file);
        }
        Uri uri = destination.tree == null ? insertDownload(context, destination, mime, name)
                : AndroidWritableDocumentTree.createFile(context, destination.ensureTree(context), mime, name);
        return new AndroidOutputFile(context, destination, uri, null);
    }

    static AndroidOutputFile createWritable(Context context, AndroidOutputDestination target,
                                            String mime, String name) throws IOException {
        AndroidOutputFile file = create(context, target, mime, name);
        try (OutputStream ignored = file.open()) {
            return file;
        } catch (IOException | RuntimeException e) {
            file.removeQuietly();
            throw e;
        }
    }

    private static File uniqueFile(File dir, String name) throws IOException {
        int dot = name.lastIndexOf('.');
        int index = 1;
        File file = new File(dir, name);
        while (!file.createNewFile()) {
            index++;
            String unique = dot > 0 ? name.substring(0, dot) + "-" + index + name.substring(dot) : name + "-" + index;
            file = new File(dir, unique);
        }
        return file;
    }

    private static Uri insertDownload(Context context, AndroidOutputDestination destination,
                                      String mime, String name) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, destination.relativePath);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(destination.collection(), values);
        if (uri == null) {
            throw new IOException("Could not create download");
        }
        return uri;
    }

    public OutputStream open() throws IOException {
        OutputStream out = file == null ? context.getContentResolver().openOutputStream(uri, "wt")
                : new FileOutputStream(file);
        if (out == null) {
            throw new IOException("Could not write output file");
        }
        return out;
    }

    public InputStream read() throws IOException {
        InputStream in = file == null ? context.getContentResolver().openInputStream(uri) : new FileInputStream(file);
        if (in == null) {
            throw new IOException("Could not read output file");
        }
        return in;
    }

    public void publish() throws IOException {
        if (destination.volume != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (context.getContentResolver().update(uri, values, null, null) != 1) {
                throw new IOException("Could not publish output download");
            }
        }
    }

    public String path() {
        return file == null ? uri.toString() : file.getAbsolutePath();
    }

    public void removeQuietly() {
        try {
            if (file != null) {
                file.delete();
            } else if (destination.tree != null) {
                AndroidWritableDocumentTree.removeQuietly(context, uri);
            } else {
                context.getContentResolver().delete(uri, null, null);
            }
        } catch (RuntimeException ignored) {
            // Ejected storage can prevent removal of a partial output.
        }
    }
}
