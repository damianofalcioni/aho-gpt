package aho.gpt.android.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;
import aho.gpt.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Recycles rows so large installed-app catalogs do not inflate an entire view tree. */
final class ToolListAdapter extends BaseAdapter {
    interface Details { void show(RecognizedTool tool); }
    private record Row(int heading, RecognizedTool tool) { }
    private final Activity activity;
    private final ScreenToolControls controls;
    private final Details details;
    private final List<RecognizedTool> all;
    private final List<Row> visible = new ArrayList<>();
    private int toolCount;

    ToolListAdapter(Activity activity, ScreenToolControls controls, List<RecognizedTool> tools, Details details) {
        this.activity = activity; this.controls = controls; this.all = tools; this.details = details;
    }
    void filter(String query) {
        String text = query.trim().toLowerCase(Locale.ROOT);
        visible.clear();
        toolCount = 0;
        addSection(text, false, R.string.tools);
        addSection(text, true, R.string.app_actions);
        notifyDataSetChanged();
    }
    private void addSection(String query, boolean appAction, int heading) {
        boolean addedHeading = false;
        for (RecognizedTool tool : all) {
            if (tool.appAction() == appAction && tool.searchable().contains(query)) {
                if (!addedHeading) { visible.add(new Row(heading, null)); addedHeading = true; }
                visible.add(new Row(0, tool));
                toolCount++;
            }
        }
    }
    int toolCount() { return toolCount; }
    @Override public int getCount() { return visible.size(); }
    @Override public RecognizedTool getItem(int position) { return visible.get(position).tool(); }
    @Override public long getItemId(int position) { return position; }
    @Override public int getViewTypeCount() { return 2; }
    @Override public int getItemViewType(int position) { return getItem(position) == null ? 0 : 1; }
    @Override public boolean areAllItemsEnabled() { return false; }
    @Override public boolean isEnabled(int position) { return getItem(position) != null; }
    @Override public View getView(int position, View recycled, ViewGroup parent) {
        if (getItem(position) == null) {
            TextView heading = recycled == null
                    ? (TextView) activity.getLayoutInflater().inflate(R.layout.row_tool_heading, parent, false)
                    : (TextView) recycled;
            heading.setText(visible.get(position).heading());
            return heading;
        }
        View row = recycled == null ? activity.getLayoutInflater().inflate(R.layout.row_tool, parent, false) : recycled;
        RecognizedTool tool = getItem(position);
        ((TextView) row.findViewById(R.id.tool_name)).setText(tool.name());
        ((TextView) row.findViewById(R.id.tool_summary)).setText(controls.available(tool)
                ? tool.summary() : activity.getString(R.string.tool_needs_accessibility));
        bindDetails(row, tool);
        Switch toggle = row.findViewById(R.id.tool_enabled);
        controls.bind(toggle, tool);
        return row;
    }
    private void bindDetails(View row, RecognizedTool tool) {
        View more = row.findViewById(R.id.tool_details);
        more.setVisibility(tool.appAction() ? View.VISIBLE : View.GONE);
        more.setContentDescription(activity.getString(R.string.tool_details_for, tool.name(), tool.summary()));
        more.setOnClickListener(tool.appAction() ? view -> details.show(tool) : null);
        View info = row.findViewById(R.id.tool_info);
        info.setVisibility(tool.appAction() ? View.GONE : View.VISIBLE);
        info.setContentDescription(activity.getString(R.string.setting_information_for, tool.name()));
        info.setOnClickListener(tool.appAction() ? null : view -> details.show(tool));
    }
}
