package aho.gpt.android.ui;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.List;

/** Installs choices without treating the initial selection as a user edit. */
final class SpinnerChoices {
    interface Changed { void accept(int position); }
    private SpinnerChoices() { }
    static void bind(Context context, Spinner spinner, List<String> names, int selected, Changed changed) {
        spinner.setOnItemSelectedListener(null);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int previous = selected;
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != previous) { previous = position; changed.accept(position); }
            }
        });
    }
}
