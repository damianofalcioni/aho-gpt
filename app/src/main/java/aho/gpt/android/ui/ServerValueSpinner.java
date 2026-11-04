package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Spinner;
import aho.gpt.R;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.SpeechCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Discovery never drops a saved value; Custom permits unlisted provider identifiers. */
final class ServerValueSpinner implements AutoCloseable {
    private final Activity activity;
    private final Spinner spinner;
    private final int title;
    private final Runnable changed;
    private List<String> available = Collections.emptyList();
    private String value;
    private AlertDialog entry;
    ServerValueSpinner(Activity activity, Spinner spinner, int title, String value, Runnable changed) {
        this.activity = activity; this.spinner = spinner; this.title = title; this.value = value; this.changed = changed;
        choices(Collections.emptyList());
    }
    String value() { return value; }
    void choices(List<String> choices) {
        available = SpeechCatalog.identifiers(choices);
        List<String> ids = new ArrayList<>(available);
        if (!ids.contains(value)) { ids.add(0, value); }
        List<String> names = new ArrayList<>(ids); names.add(activity.getString(R.string.custom_speech_custom_value));
        SpinnerChoices.bind(activity, spinner, names, ids.indexOf(value), position -> {
            if (position == ids.size()) { choices(available); custom(); }
            else { value = ids.get(position); changed.run(); }
        });
    }
    AlertDialog custom() {
        EditText input = new EditText(activity);
        input.setId(android.R.id.edit);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256)}); input.setText(value);
        input.setContentDescription(activity.getString(title)); input.setSaveEnabled(false);
        entry = new AlertDialog.Builder(activity).setTitle(title).setView(input)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create();
        entry.show(); entry.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> accept(input)); return entry;
    }
    private void accept(EditText input) {
        try { value = SpeechApiConfig.identifier(input.getText().toString()); }
        catch (IllegalArgumentException exception) { input.setError(activity.getString(R.string.custom_speech_value_invalid)); return; }
        choices(available); changed.run(); entry.dismiss();
    }
    @Override public void close() {
        spinner.setOnItemSelectedListener(null);
        if (entry != null) { entry.dismiss(); entry = null; }
    }
}
