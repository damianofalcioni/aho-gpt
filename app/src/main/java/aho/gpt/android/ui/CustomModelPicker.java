package aho.gpt.android.ui;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.net.CustomHttp;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.ServerEndpoint;
import aho.gpt.custom.ServerModels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Dialog-local discovery never changes saved settings or shares a voice connection. */
final class CustomModelPicker implements AutoCloseable {
    interface Selection { void accept(String model); }
    private final Activity activity;
    private final Spinner spinner;
    private final TextView status;
    private final EditText endpoint;
    private final EditText token;
    private final View refresh;
    private final CustomHttp http = new CustomHttp();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final TextWatcher watcher;
    private RequestScope active;
    private Future<?> work;
    private String source;
    private String model;
    private final Selection selection;

    CustomModelPicker(Activity activity, View content, Selection selection) {
        this.selection = selection;
        this.activity = activity;
        spinner = content.findViewById(R.id.custom_model); status = content.findViewById(R.id.custom_model_status);
        endpoint = content.findViewById(R.id.custom_endpoint); token = content.findViewById(R.id.custom_token);
        ProviderSettings settings = new ProviderSettings(activity);
        source = settings.endpoint(); model = settings.customModel();
        watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { changed(); }
            @Override public void afterTextChanged(Editable text) { }
        };
        refresh = content.findViewById(R.id.custom_models_refresh);
        refresh.setEnabled(false); refresh.setOnClickListener(view -> refresh());
        bind(Collections.emptyList(), false);
    }
    void start() {
        endpoint.addTextChangedListener(watcher); token.addTextChangedListener(watcher);
        refresh.setEnabled(true); refresh();
    }
    String model() { return model; }
    private void changed() {
        stop();
        if (!endpoint.getText().toString().trim().equals(source)) { model = ""; }
        bind(Collections.emptyList(), false); status.setText(R.string.custom_models_refresh);
    }
    private void refresh() {
        stop();
        String base;
        try { base = ServerEndpoint.normalize(endpoint.getText().toString()); }
        catch (IllegalArgumentException exception) { endpoint.setError(activity.getString(R.string.custom_endpoint_invalid)); return; }
        if (!base.equals(source)) { model = ""; }
        source = base; String secret = token.getText().toString();
        RequestScope scope = new RequestScope(); active = scope;
        spinner.setEnabled(false); status.setText(R.string.loading_models);
        work = worker.submit(() -> request(scope, base, secret));
    }
    private void request(RequestScope scope, String base, String secret) {
        try {
            List<String> ids = ServerModels.read(http.json(base + "/models", secret, null, scope));
            activity.runOnUiThread(() -> { if (current(scope)) { loaded(ids); } });
        } catch (Exception exception) {
            activity.runOnUiThread(() -> { if (current(scope)) { bind(Collections.emptyList(), false); status.setText(R.string.models_load_failed); } });
        }
    }
    private boolean current(RequestScope scope) {
        return active == scope && !activity.isFinishing() && !activity.isDestroyed();
    }
    private void loaded(List<String> ids) {
        bind(ids, true);
        status.setText(ids.isEmpty() ? R.string.no_available_models : R.string.custom_models_ready);
    }
    private void bind(List<String> available, boolean verified) {
        List<String> ids = new ArrayList<>(available); ids.add(0, "");
        List<String> names = new ArrayList<>(ids); names.set(0, activity.getString(R.string.custom_model_first));
        if (!ids.contains(model)) {
            ids.add(model); names.add(verified ? activity.getString(R.string.saved_choice_unavailable, model) : model);
        }
        spinner.setEnabled(ids.size() > 1);
        SpinnerChoices.bind(activity, spinner, names, ids.indexOf(model), position -> {
            model = ids.get(position); selection.accept(model);
        });
    }
    private void stop() {
        if (active != null) { active.cancel(); http.cancel(active); active = null; }
        if (work != null) { work.cancel(true); work = null; }
    }
    @Override public void close() {
        endpoint.removeTextChangedListener(watcher); token.removeTextChangedListener(watcher);
        stop(); worker.shutdownNow(); spinner.setOnItemSelectedListener(null); refresh.setEnabled(false);
    }
}
