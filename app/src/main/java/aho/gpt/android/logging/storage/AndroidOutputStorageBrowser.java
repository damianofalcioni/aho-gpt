package aho.gpt.android.logging.storage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.io.File;
import java.io.IOException;

public final class AndroidOutputStorageBrowser {
    private AndroidOutputStorageBrowser() { }
    public static void prepare(Context context, AndroidOutputDestination destination) throws IOException {
        if (destination.directory != null) { destination.ensureDirectory(); }
        else if (destination.tree != null) { destination.ensureTree(context); }
        else { AndroidOutputStorage.verify(context, destination); }
    }
    @android.annotation.SuppressLint("WearRecents") // The temporary picker task must leave the main app task reachable.
    public static void open(Activity activity, AndroidOutputDestination destination) {
        activity.startActivity(new Intent(activity, AndroidOutputBrowserActivity.class)
                .setData(folderUri(activity, destination))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
    static Intent pickerIntent(Intent launch) {
        return AndroidDocumentAccess.openDocumentIntent(launch.getData()).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    }
    private static Uri folderUri(Context context, AndroidOutputDestination destination) {
        if (destination.tree != null) { return AndroidOutputFolderRecovery.accessUri(context, destination.tree); }
        String path = destination.label.replace(File.separatorChar, '/');
        int start = path.indexOf("/Download/");
        boolean primary = destination.directory != null || (Build.VERSION.SDK_INT >= 29
                && MediaStore.VOLUME_EXTERNAL_PRIMARY.equals(destination.volume));
        String id = primary ? "primary" : new File(path.substring(0, start)).getName();
        return AndroidDocumentAccess.buildExternalStorageDocumentUri(id + ":" + path.substring(start + 1));
    }
}
