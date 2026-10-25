package aho.gpt.android.logging.storage;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import aho.gpt.logging.LogRecord;

final class AndroidOutputStorageCleaner {
    private AndroidOutputStorageCleaner() { }

    static void clear(Context context, AndroidOutputDestination destination) throws IOException {
        if (destination.tree != null) {
            AndroidOutputFolderCleaner.clearDocuments(context, destination.ensureTree(context));
        } else if (destination.directory != null) {
            clearFiles(destination.directory);
        } else {
            clearDownloads(context, destination);
        }
    }

    private static void clearFiles(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Could not list output files");
        }
        for (File file : files) {
            deleteFile(file);
        }
    }

    private static void deleteFile(File file) throws IOException {
        if (file.isFile() && LogRecord.isLogFile(file.getName()) && !file.delete()) {
            throw new IOException("Could not delete output file");
        }
    }

    private static void clearDownloads(Context context, AndroidOutputDestination destination)
            throws IOException {
        for (long id : downloadIds(context, destination)) {
            if (context.getContentResolver().delete(ContentUris.withAppendedId(destination.collection(), id),
                    null, null) != 1) {
                throw new IOException("Could not delete output download");
            }
        }
    }

    private static List<Long> downloadIds(Context context, AndroidOutputDestination destination)
            throws IOException {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(destination.collection(), new String[]{
                MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME},
                MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " + MediaStore.MediaColumns.OWNER_PACKAGE_NAME + "=?",
                new String[]{destination.relativePath, context.getPackageName()}, null)) {
            if (cursor == null) {
                throw new IOException("Could not list output downloads");
            }
            while (cursor.moveToNext()) {
                if (LogRecord.isLogFile(cursor.getString(1))) {
                    ids.add(cursor.getLong(0));
                }
            }
        }
        return ids;
    }
}
