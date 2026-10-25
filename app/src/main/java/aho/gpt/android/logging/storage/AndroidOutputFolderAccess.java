package aho.gpt.android.logging.storage;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;

final class AndroidOutputFolderAccess {
    private AndroidOutputFolderAccess() { }
    static Uri ensureFolder(Context context, Uri selected) throws IOException {
        if (!AndroidOutputFolderRecovery.hasGrant(context, selected)) {
            throw new IOException("Folder permission is missing");
        }
        Uri access = AndroidOutputFolderRecovery.accessUri(context, selected);
        if (AndroidOutputFolderRecovery.writableDirectory(context, access)) { return access; }
        Uri restored = AndroidOutputFolderRecovery.restore(context, selected);
        if (!AndroidOutputFolderRecovery.writableDirectory(context, restored)) {
            throw new IOException("Folder is unavailable");
        }
        return restored;
    }
}
