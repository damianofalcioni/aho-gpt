package aho.gpt.android.logging.storage;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import aho.gpt.R;
import android.app.Activity;

/** Hosts system file picking in its own task without creating an in-app file browser. */
public final class AndroidOutputBrowserActivity extends Activity {
    static final int REQUEST_OPEN_FILE = 4013;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) {
            try {
                startActivityForResult(AndroidOutputStorageBrowser.pickerIntent(getIntent()),
                        REQUEST_OPEN_FILE);
            } catch (ActivityNotFoundException | SecurityException e) {
                unavailable();
                onActivityResult(REQUEST_OPEN_FILE, RESULT_CANCELED, null);
            }
        }
    }

    @Override
    @android.annotation.SuppressLint("WearRecents") // Viewers must survive removal of this temporary picker task.
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_FILE) {
            return;
        }
        try {
            if (resultCode == RESULT_OK && data != null) {
                Intent action = selectionIntent(data);
                if (action != null) {
                    startActivity(action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            unavailable();
        } finally {
            // This activity owns a separate picker task; remove its card from Recents too.
            finishAndRemoveTask();
        }
    }

    private Intent selectionIntent(Intent data) {
        ArrayList<Uri> files = selectedUris(data);
        if (files.isEmpty()) {
            return null;
        }
        ClipData clip = ClipData.newRawUri("", files.get(0));
        for (int i = 1; i < files.size(); i++) {
            clip.addItem(new ClipData.Item(files.get(i)));
        }
        Intent action;
        if (files.size() == 1) {
            action = new Intent(Intent.ACTION_VIEW).setDataAndType(files.get(0),
                    sharedMimeType(files));
        } else {
            action = new Intent(Intent.ACTION_SEND_MULTIPLE).setType(sharedMimeType(files))
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        }
        action.setClipData(clip);
        action.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // The chooser carries every URI grant before this host releases its picker grant.
        return files.size() == 1 ? AndroidFileActionChooser.create(this, action,
                getString(R.string.logs), null) : Intent.createChooser(action, null);
    }

    private static ArrayList<Uri> selectedUris(Intent data) {
        LinkedHashSet<Uri> files = new LinkedHashSet<>();
        if (data.getData() != null) {
            files.add(data.getData());
        }
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) {
                    files.add(uri);
                }
            }
        }
        return new ArrayList<>(files);
    }

    private String sharedMimeType(ArrayList<Uri> files) {
        String common = getContentResolver().getType(files.get(0));
        if (common == null) {
            return "*/*";
        }
        for (int i = 1; i < files.size(); i++) {
            if (!common.equals(getContentResolver().getType(files.get(i)))) {
                return "*/*";
            }
        }
        return common;
    }

    private void unavailable() {
        Toast.makeText(this, R.string.log_browser_unavailable, Toast.LENGTH_LONG).show();
    }
}
