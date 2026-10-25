package aho.gpt.android.logging.storage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/** Offers sharing apps alongside viewers, retaining temporary access to the file. */
public final class AndroidFileActionChooser {
    private AndroidFileActionChooser() { }

    public static Intent create(Context context, Intent view, String title, String fileLabel) {
        Intent send = new Intent(Intent.ACTION_SEND).setType(view.getType())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(view.getClipData());
        send.putExtra(Intent.EXTRA_STREAM, view.getData());
        send.putExtra(Intent.EXTRA_TITLE, fileLabel);
        Intent chooser = Intent.createChooser(send, title);
        List<Intent> targets = viewTargets(context, view);
        if (!targets.isEmpty()) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.toArray(new Parcelable[0]));
        }
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        chooser.setClipData(send.getClipData());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            chooser.putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false);
        }
        return chooser;
    }

    private static List<Intent> viewTargets(Context context, Intent view) {
        List<Intent> targets = new ArrayList<>();
        for (ResolveInfo handler : queryViewHandlers(context, view)) {
            ActivityInfo info = handler.activityInfo;
            if (info != null && info.packageName != null && info.name != null
                    && !context.getPackageName().equals(info.packageName)) {
                targets.add(new Intent(view).setComponent(new ComponentName(info.packageName, info.name)));
            }
        }
        return targets;
    }

    private static List<ResolveInfo> queryViewHandlers(Context context, Intent view) {
        PackageManager manager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return manager.queryIntentActivities(view,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY));
        }
        return manager.queryIntentActivities(view, PackageManager.MATCH_DEFAULT_ONLY);
    }
}
