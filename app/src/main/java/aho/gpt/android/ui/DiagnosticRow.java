package aho.gpt.android.ui;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import aho.gpt.R;

/** Compact grant status with a direct Android settings action. */
final class DiagnosticRow {
    private final Activity activity;
    private final View row;
    private final TextView label;
    private final TextView status;
    private final GradientDrawable marker = new GradientDrawable();
    private int color;

    DiagnosticRow(Activity activity, int title, Runnable action) {
        this.activity = activity;
        LinearLayout parent = activity.findViewById(R.id.diagnostic_access);
        row = activity.getLayoutInflater().inflate(R.layout.row_diagnostic, parent, false);
        label = row.findViewById(R.id.diagnostic_label); label.setText(title);
        status = row.findViewById(R.id.diagnostic_status);
        marker.setShape(GradientDrawable.OVAL);
        row.findViewById(R.id.diagnostic_mark).setBackground(marker);
        row.setOnClickListener(view -> action.run()); parent.addView(row);
        checking();
    }
    void checking() { update(R.color.diagnostic_warning); }
    void state(boolean ready) { update(ready ? R.color.diagnostic_ok : R.color.diagnostic_error); }
    private void update(int color) {
        if (color == this.color) { return; }
        this.color = color;
        status.setText(color == R.color.diagnostic_ok ? R.string.diagnostic_ok : R.string.diagnostic_ko);
        int tint = activity.getColor(color);
        status.setTextColor(tint); marker.setColor(tint);
        row.setContentDescription(activity.getString(R.string.diagnostic_row, label.getText(), status.getText()));
    }
}
