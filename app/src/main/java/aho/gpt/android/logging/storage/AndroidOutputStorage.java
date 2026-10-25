package aho.gpt.android.logging.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

/** Uses only the selected Downloads destination; missing access never redirects output. */
public final class AndroidOutputStorage {

    private AndroidOutputStorage() { }

    public static AndroidOutputDestination requested(Context context) throws IOException {
        if (!AppOutputStorageSettings.useSdCard(context)) {
            return phone(context);
        }
        AndroidOutputStorageVolumes.Card card = AndroidOutputStorageVolumes.find(context,
                AppOutputStorageSettings.cardId(context));
        if (card == null) {
            throw new IOException("SD card is unavailable");
        }
        return card(context, card, AndroidOutputStorageAccess.existingGrant(context, card));
    }

    public static AndroidOutputDestination card(Context context, AndroidOutputStorageVolumes.Card card,
                                                String grant) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AndroidOutputDestination.downloads(card.mediaStoreName, card.root.getAbsolutePath());
        }
        if (grant == null) {
            throw new IOException("SD card folder access is required");
        }
        Uri target = AndroidDocumentAccess.buildExternalStorageTreeUri(
                card.id + ":" + AndroidOutputDestination.LOG_PATH);
        if (!AndroidOutputFolderRecovery.hasGrant(context, target)) {
            throw new IOException("SD card folder permission is missing");
        }
        return AndroidOutputDestination.documents(target, card.root.getAbsolutePath());
    }

    private static AndroidOutputDestination phone(Context context) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AndroidOutputDestination.downloads(MediaStore.VOLUME_EXTERNAL_PRIMARY,
                    Environment.getExternalStorageDirectory().getAbsolutePath());
        }
        if (AndroidLegacyExternalStorageAccess.needsOutputPermission(context)) {
            throw new IOException("Downloads permission is required");
        }
        return AndroidOutputDestination.file(new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "AhoGPT/logs"));
    }

    public static AndroidOutputDestination current(Context context) {
        String id = AppOutputStorageSettings.cardId(context);
        String root = AppOutputStorageSettings.useSdCard(context) ? "/storage/" + id
                : Environment.getExternalStorageDirectory().getAbsolutePath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AndroidOutputDestination.downloads(AppOutputStorageSettings.useSdCard(context)
                    ? id.toLowerCase(Locale.ROOT) : MediaStore.VOLUME_EXTERNAL_PRIMARY, root);
        }
        return AppOutputStorageSettings.useSdCard(context)
                ? AndroidOutputDestination.documents(AndroidDocumentAccess.buildExternalStorageTreeUri(
                        id + ":" + AndroidOutputDestination.LOG_PATH), root)
                : AndroidOutputDestination.file(new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "AhoGPT/logs"));
    }

    public static AndroidOutputFile create(Context context, String mime, String name) throws IOException {
        AndroidOutputDestination target = requested(context);
        try {
            return AndroidOutputFile.createWritable(context, target, mime, name);
        } catch (IOException | RuntimeException e) {
            throw new IOException("Selected output storage is unavailable", e);
        }
    }

    public static boolean hasAccess(Context context) {
        try { requested(context); return true; }
        catch (IOException | RuntimeException exception) { return false; }
    }

    public static void verify(Context context, AndroidOutputDestination target) throws IOException {
        AndroidOutputFile probe = AndroidOutputFile.create(context, target, "text/plain", ".aho-gpt-write-check.txt");
        try (OutputStream stream = probe.open()) {
            stream.write(0);
        } finally {
            probe.removeQuietly();
        }
    }

}
