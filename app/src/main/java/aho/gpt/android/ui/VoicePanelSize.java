package aho.gpt.android.ui;

import android.content.Context;
import aho.gpt.R;

/** Uses the same compact bounds in the activity and over other apps. */
final class VoicePanelSize {
    private VoicePanelSize() { }
    static int width(Context context) {
        return size(context, R.dimen.voice_panel_max_width, context.getResources().getConfiguration().screenWidthDp);
    }
    static int height(Context context) {
        return size(context, R.dimen.voice_panel_height, context.getResources().getConfiguration().screenHeightDp);
    }
    private static int size(Context context, int limit, int availableDp) {
        int margin = context.getResources().getDimensionPixelSize(R.dimen.voice_panel_margin);
        int available = Math.round(availableDp * context.getResources().getDisplayMetrics().density) - 2 * margin;
        return Math.min(context.getResources().getDimensionPixelSize(limit), available);
    }
}
