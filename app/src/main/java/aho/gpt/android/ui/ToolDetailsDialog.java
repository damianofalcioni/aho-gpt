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
        new Field("app", R.string.tool_field_app), new Field("component", R.string.tool_field_component),
        new Field("kind", R.string.tool_field_kind), new Field("action", R.string.tool_field_action),
        new Field("categories", R.string.tool_field_categories),
        new Field("schemes", R.string.tool_field_schemes), new Field("mime_types", R.string.tool_field_mime),
        new Field("authorities", R.string.tool_field_authorities), new Field("paths", R.string.tool_field_paths)
    };
    private static final Field[] ASSIGNED = {
        new Field("category", R.string.tool_field_category), new Field("id", R.string.tool_field_id)
    };
    private final Activity activity;
    private AlertDialog dialog;

    ToolDetailsDialog(Activity activity) { this.activity = activity; }
    void show(RecognizedTool tool) {
        close();
        if (!tool.appAction()) { information(tool); return; }
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_tool_details, new FrameLayout(activity), false);
        fields(content.findViewById(R.id.tool_detail_fields), FIELDS, tool.details());
        fields(content.findViewById(R.id.tool_assigned_fields), ASSIGNED, tool.details());
        dialog = new AlertDialog.Builder(activity).setTitle(tool.name()).setView(content)
                .setPositiveButton(R.string.close, null).show();
    }
    private void fields(LinearLayout parent, Field[] fields, JSONObject details) {
        for (Field field : fields) {
            if (details.has(field.key())) { field(parent, field.label(), details.opt(field.key())); }
        }
    }
    private void information(RecognizedTool tool) {
        int message = switch (tool.key()) {
            case "find_intents" -> R.string.tool_find_intents_info;
            case "invoke_intent" -> R.string.tool_invoke_intent_info;
            case "read_screen" -> R.string.tool_read_screen_info;
            default -> R.string.tool_ui_action_info;
        };
        dialog = new AlertDialog.Builder(activity).setTitle(tool.name()).setMessage(message)
                .setPositiveButton(R.string.close, null).show();
    }
    private void field(LinearLayout parent, int label, Object value) {
        View row = activity.getLayoutInflater().inflate(R.layout.row_tool_detail, parent, false);
        ((TextView) row.findViewById(R.id.tool_field_label)).setText(label);
        ((TextView) row.findViewById(R.id.tool_field_value)).setText(display(value));
        parent.addView(row);
    }
    private String display(Object value) {
        if (!(value instanceof JSONArray array)) { return structured(value); }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            if (i > 0) { text.append('\n'); }
            text.append(structured(array.opt(i)));
        }
        return text.toString();
    }
    private String structured(Object value) {
        try {
            if (value instanceof JSONObject object) { return object.toString(2); }
            return String.valueOf(value);
        } catch (JSONException exception) { return String.valueOf(value); }
    }
    @Override public void close() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
