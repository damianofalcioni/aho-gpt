package aho.gpt.android.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.tools.ToolPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Recycles rows so large installed-app catalogs do not inflate an entire view tree. */
final class ToolListAdapter extends BaseAdapter {
    interface Details { void show(RecognizedTool tool); }
    private final Activity activity;
    private final ToolPreferences preferences;
    private final Details details;
    private final List<RecognizedTool> all;
    private final List<RecognizedTool> visible = new ArrayList<>();

    ToolListAdapter(Activity activity, ToolPreferences preferences, List<RecognizedTool> tools, Details details) {
        this.activity = activity; this.preferences = preferences; this.all = tools; this.details = details;
        visible.addAll(tools);
    }
    void filter(String query) {
        String text = query.trim().toLowerCase(Locale.ROOT);
        visible.clear();
        for (RecognizedTool tool : all) {
            if (tool.searchable().contains(text)) { visible.add(tool); }
        }
        notifyDataSetChanged();
    }
    @Override public int getCount() { return visible.size(); }
    @Override public RecognizedTool getItem(int position) { return visible.get(position); }
    @Override public long getItemId(int position) { return position; }
    @Override public View getView(int position, View recycled, ViewGroup parent) {
        View row = recycled == null ? activity.getLayoutInflater().inflate(R.layout.row_tool, parent, false) : recycled;
        RecognizedTool tool = getItem(position);
        ((TextView) row.findViewById(R.id.tool_name)).setText(tool.name());
        ((TextView) row.findViewById(R.id.tool_summary)).setText(tool.summary());
        View more = row.findViewById(R.id.tool_details);
        more.setContentDescription(activity.getString(R.string.tool_details_for, tool.name(), tool.summary()));
        more.setOnClickListener(view -> details.show(tool));
        Switch toggle = row.findViewById(R.id.tool_enabled);
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(preferences.selected(tool.key()));
        toggle.setContentDescription(activity.getString(R.string.enable_tool, tool.name(), tool.summary()));
        toggle.setOnCheckedChangeListener((button, checked) -> preferences.select(tool.key(), checked));
        return row;
    }
}
