package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import aho.gpt.R;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

final class ToolDetailsDialog implements AutoCloseable {
    private record Field(String key, int label) { }
    private static final Field[] FIELDS = {
        new Field("name", R.string.tool_field_name), new Field("app", R.string.tool_field_app),
        new Field("type", R.string.tool_field_type),
        new Field("description", R.string.tool_field_description), new Field("usage", R.string.tool_field_usage),
        new Field("availability", R.string.tool_field_availability), new Field("component", R.string.tool_field_component),
        new Field("kind", R.string.tool_field_kind), new Field("action", R.string.tool_field_action),
        new Field("category", R.string.tool_field_category), new Field("opens_app", R.string.tool_field_launcher),
        new Field("semantics_known", R.string.tool_field_semantics), new Field("categories", R.string.tool_field_categories),
        new Field("schemes", R.string.tool_field_schemes), new Field("mime_types", R.string.tool_field_mime),
        new Field("authorities", R.string.tool_field_authorities), new Field("paths", R.string.tool_field_paths),
        new Field("parameters", R.string.tool_field_parameters), new Field("strict", R.string.tool_field_strict),
        new Field("id", R.string.tool_field_id), new Field("note", R.string.tool_field_note)
    };
    private final Activity activity;
    private AlertDialog dialog;

    ToolDetailsDialog(Activity activity) { this.activity = activity; }
    void show(RecognizedTool tool) {
        close();
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_tool_details, new FrameLayout(activity), false);
        LinearLayout fields = content.findViewById(R.id.tool_detail_fields);
        for (Field field : FIELDS) {
            if (tool.details().has(field.key())) { field(fields, field.label(), tool.details().opt(field.key())); }
        }
        dialog = new AlertDialog.Builder(activity).setTitle(tool.name()).setView(content)
                .setPositiveButton(R.string.close, null).show();
    }
    private void field(LinearLayout parent, int label, Object value) {
        View row = activity.getLayoutInflater().inflate(R.layout.row_tool_detail, parent, false);
        ((TextView) row.findViewById(R.id.tool_field_label)).setText(label);
        ((TextView) row.findViewById(R.id.tool_field_value)).setText(display(value));
        parent.addView(row);
    }
    private String display(Object value) {
        if (Boolean.TRUE.equals(value)) { return activity.getString(R.string.tool_yes); }
        if (Boolean.FALSE.equals(value)) { return activity.getString(R.string.tool_no); }
        if (value instanceof JSONArray array && array.length() == 0) { return activity.getString(R.string.tool_none); }
        return structured(value);
    }
    private String structured(Object value) {
        try {
            if (value instanceof JSONArray array) { return array.toString(2); }
            if (value instanceof JSONObject object) { return object.toString(2); }
            return String.valueOf(value);
        } catch (JSONException exception) { return String.valueOf(value); }
    }
    @Override public void close() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
