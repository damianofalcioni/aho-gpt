package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.tools.ToolPreferences;
import java.util.List;

final class ToolsSettingsDialog implements AutoCloseable {
    private final Activity activity;
    private final AssistantApplication app;
    private final ToolPreferences preferences;
    private final ToolDetailsDialog details;
    private AlertDialog dialog;
    private int generation;

    ToolsSettingsDialog(Activity activity) {
        this.activity = activity; app = (AssistantApplication) activity.getApplication();
        preferences = app.catalog().preferences(); details = new ToolDetailsDialog(activity);
        Switch toggle = activity.findViewById(R.id.tools_enabled);
        toggle.setChecked(preferences.enabled());
        toggle.setOnCheckedChangeListener((button, checked) -> preferences.enable(checked));
        activity.findViewById(R.id.tools_settings).setOnClickListener(view -> show());
    }
    private void show() {
        close();
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_tools, new FrameLayout(activity), false);
        dialog = new AlertDialog.Builder(activity).setTitle(R.string.tools).setView(content)
                .setPositiveButton(R.string.close, null).create();
        dialog.setOnDismissListener(ignored -> { generation++; details.close(); });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        int request = ++generation;
        app.worker().execute(() -> load(content, request));
    }
    private void load(View content, int request) {
        try {
            List<RecognizedTool> tools = RecognizedTool.load(activity, app.catalog());
            activity.runOnUiThread(() -> { if (current(request)) { loaded(content, tools); } });
        } catch (Exception exception) {
            activity.runOnUiThread(() -> {
                if (current(request)) { ((TextView) content.findViewById(R.id.tools_status)).setText(R.string.tools_load_failed); }
            });
        }
    }
    private boolean current(int request) {
        return request == generation && !activity.isFinishing() && !activity.isDestroyed();
    }
    private void loaded(View content, List<RecognizedTool> tools) {
        ToolListAdapter adapter = new ToolListAdapter(activity, preferences, tools, details::show);
        ListView list = content.findViewById(R.id.tool_list);
        list.setItemsCanFocus(true); list.setAdapter(adapter);
        TextView status = content.findViewById(R.id.tools_status);
        EditText search = content.findViewById(R.id.tool_search);
        adapter.filter(search.getText().toString());
        status.setText(activity.getString(R.string.tools_count, adapter.getCount()));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                adapter.filter(text.toString()); status.setText(activity.getString(R.string.tools_count, adapter.getCount()));
            }
            @Override public void afterTextChanged(Editable text) { }
        });
    }
    @Override public void close() {
        generation++; details.close();
        if (dialog != null) { dialog.dismiss(); dialog = null; }
    }
}
