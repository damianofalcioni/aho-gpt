package aho.gpt.android.ui;

import android.app.Activity;
import android.widget.Spinner;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.logging.AppLogger;
import aho.gpt.openai.AccountModels;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/** Model loading belongs to the open settings dialog only. */
final class AccountModelPicker {
    private final Activity activity;
    private final AssistantApplication app;
    private final Spinner spinner;
    private final Runnable changed;
    private int generation;

    AccountModelPicker(Activity activity, Spinner spinner, Runnable changed) {
        this.activity = activity; this.spinner = spinner; this.changed = changed;
        app = (AssistantApplication) activity.getApplication();
    }
    void load(boolean connected) {
        int request = ++generation;
        placeholder(connected ? R.string.loading_models : R.string.log_in_first);
        if (!connected) { return; }
        app.worker().execute(() -> request(request));
    }
    private void request(int request) {
        try {
            List<AccountModels.Model> models = AccountModels.read(app.http().json(
                    "https://api.openai.com/v1/models", app.auth().accessToken(), null, ""));
            activity.runOnUiThread(() -> { if (current(request)) { loaded(models); } });
        } catch (Exception exception) {
            AppLogger.w("Settings", "Model load failed", exception);
            activity.runOnUiThread(() -> { if (current(request)) { placeholder(R.string.models_load_failed); } });
        }
    }
    private boolean current(int request) {
        return generation == request && !activity.isFinishing() && !activity.isDestroyed();
    }
    private void placeholder(int label) {
        spinner.setEnabled(false);
        SpinnerChoices.bind(activity, spinner, Collections.singletonList(activity.getString(label)), 0, ignored -> { });
    }
    private void loaded(List<AccountModels.Model> models) {
        if (models.isEmpty()) { placeholder(R.string.no_available_models); return; }
        List<String> names = new ArrayList<>(); List<String> slugs = new ArrayList<>();
        names.add(activity.getString(R.string.choose_model)); slugs.add("");
        for (AccountModels.Model model : models) { names.add(model.name()); slugs.add(model.slug()); }
        int selected = slugs.indexOf(app.model());
        if (selected < 0) {
            names.set(0, activity.getString(R.string.saved_choice_unavailable, app.model())); selected = 0;
        }
        spinner.setEnabled(true);
        SpinnerChoices.bind(activity, spinner, names, selected, position -> {
            if (position > 0) { app.model(slugs.get(position)); changed.run(); }
            else { spinner.setSelection(Math.max(0, slugs.indexOf(app.model()))); }
        });
    }
    void stop() { generation++; spinner.setOnItemSelectedListener(null); }
}
