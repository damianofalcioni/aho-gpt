package aho.gpt.android.logging.storage;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;

import java.io.File;
import java.util.Locale;

/** Stable removable slots only: getExternalMediaDirs excludes transient USB devices. */
public final class AndroidOutputStorageVolumes {
    private AndroidOutputStorageVolumes() { }

    // Unlike arbitrary storage volumes, these stable slots exclude transient USB devices.
    @SuppressWarnings("deprecation")
    public static Card find(Context context, String id) {
        File[] dirs = context.getExternalMediaDirs();
        if (dirs == null) {
            return null;
        }
        for (File dir : dirs) {
            Card card = card(context, dir);
            if (card != null && (id.isEmpty() || id.equals(card.id))) {
                return card;
            }
        }
        return null;
    }

    private static Card card(Context context, File dir) {
        if (dir == null || !Environment.isExternalStorageRemovable(dir)
                || !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(dir))) {
            return null;
        }
        File root = dir.getParentFile().getParentFile().getParentFile();
        String id = root.getName();
        String volume = mediaStoreName(context, dir, id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volume == null) {
            return null;
        }
        return new Card(id, root, volume);
    }

    private static String mediaStoreName(Context context, File dir, String id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StorageManager manager = context.getSystemService(StorageManager.class);
            StorageVolume volume = manager == null ? null : manager.getStorageVolume(dir);
            return volume == null ? null : volume.getMediaStoreVolumeName();
        }
        String name = id.toLowerCase(Locale.ROOT);
        return MediaStore.getExternalVolumeNames(context).contains(name) ? name : null;
    }

    public static final class Card {
        public final String id;
        public final File root;
        public final String mediaStoreName;

        Card(String id, File root, String mediaStoreName) {
            this.id = id;
            this.root = root;
            this.mediaStoreName = mediaStoreName;
        }
    }
}
