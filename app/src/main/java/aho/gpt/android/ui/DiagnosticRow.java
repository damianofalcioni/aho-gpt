package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import aho.gpt.R;

/** Shared compact, focusable status row. Text conveys status without relying on color. */
final class DiagnosticRow implements AutoCloseable {
    private final Activity activity;
    private final View row;
    private final TextView label;
    private final TextView status;
    private final GradientDrawable marker = new GradientDrawable();
    private AlertDialog dialog;
    private String detail = "";
    private int message;
    private int color;

    DiagnosticRow(Activity activity, int container, int title, Runnable action) {
        this.activity = activity;
        LinearLayout parent = activity.findViewById(container);
        row = activity.getLayoutInflater().inflate(R.layout.row_diagnostic, parent, false);
        label = row.findViewById(R.id.diagnostic_label); label.setText(title);
        status = row.findViewById(R.id.diagnostic_status);
        marker.setShape(GradientDrawable.OVAL);
        row.findViewById(R.id.diagnostic_mark).setBackground(marker);
        row.setOnClickListener(view -> show(action)); parent.addView(row);
        update(R.string.diagnostic_checking, R.color.diagnostic_warning, "");
    }

    void update(int message, int color, String detail) {
        this.detail = activity.getString(R.string.diagnostic_detail, activity.getString(message), detail);
        if (message == this.message && color == this.color) { return; }
        this.message = message; this.color = color;
        status.setText(color == R.color.diagnostic_ok ? R.string.diagnostic_ok : R.string.diagnostic_ko);
        int tint = activity.getColor(color);
        status.setTextColor(tint); marker.setColor(tint);
        row.setContentDescription(activity.getString(R.string.diagnostic_row, label.getText(), status.getText()));
    }
    void state(boolean ready, int yes, int no, String detail) {
        update(ready ? yes : no, ready ? R.color.diagnostic_ok : R.color.diagnostic_error, detail);
    }
    private void show(Runnable action) {
        close();
        dialog = new AlertDialog.Builder(activity).setTitle(label.getText()).setMessage(detail)
                .setPositiveButton(R.string.open_settings, (ignored, which) -> action.run())
                .setNegativeButton(R.string.close, null).show();
    }
    @Override public void close() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
