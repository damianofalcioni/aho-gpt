package aho.gpt.android.ui;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import aho.gpt.R;
import aho.gpt.android.net.CustomAudioProbe;
import aho.gpt.android.net.ProviderSettings;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.AudioMode;
import aho.gpt.custom.ServerEndpoint;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.live.LiveSetupException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.EnumMap;
import java.util.Map;

/** Mutually exclusive draft speech routes; only successful checks can enable them. */
final class CustomSpeechToggle implements AutoCloseable {
    private final Activity activity;
    private record Row(Switch control, TextView label, int title, int checking, int unavailable) { }
    private final Map<AudioMode, Row> rows = new EnumMap<>(AudioMode.class);
    private final EditText endpoint;
    private final EditText token;
    private final CustomAudioProbe probe = new CustomAudioProbe();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final TextWatcher watcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence text, int start, int before, int count) { invalidate(); }
        @Override public void afterTextChanged(Editable text) { }
    };
    private RequestScope active;
    private Future<?> work;
    private Toast toast;
    private AudioMode mode = AudioMode.PLATFORM;
    private AudioMode pending = AudioMode.PLATFORM;
    private String model;
    private SpeechApiConfig configuration;
    CustomSpeechToggle(Activity activity, View content) {
        this.activity = activity;
        rows.put(AudioMode.SPEECH, new Row(content.findViewById(R.id.custom_speech_apis), content.findViewById(R.id.custom_speech_label),
                R.string.custom_speech_apis, R.string.custom_speech_checking, R.string.custom_speech_unavailable));
        rows.put(AudioMode.REALTIME, new Row(content.findViewById(R.id.custom_realtime_api), content.findViewById(R.id.custom_realtime_label),
                R.string.custom_realtime_api, R.string.custom_realtime_checking, R.string.custom_realtime_unavailable));
        rows.put(AudioMode.LIVE, new Row(content.findViewById(R.id.custom_live_api), content.findViewById(R.id.custom_live_label),
                R.string.custom_live_api, R.string.custom_live_checking, R.string.custom_live_unavailable));
        endpoint = content.findViewById(R.id.custom_endpoint); token = content.findViewById(R.id.custom_token);
        model = new ProviderSettings(activity).customModel();
        configuration = new ProviderSettings(activity).speechConfig();
        for (Row row : rows.values()) { row.control().setEnabled(false); }
    }
    void start() {
        mode = new ProviderSettings(activity).audioMode(); render();
        endpoint.addTextChangedListener(watcher); token.addTextChangedListener(watcher);
    }
    AudioMode mode() { return mode; }
    boolean checking() { return active != null; }
    SpeechApiConfig configuration() { return configuration; }
    void configure(SpeechApiConfig value) {
        boolean recheck = mode.requiresRecheck(configuration, value) || pending.requiresRecheck(configuration, value);
        configuration = value;
        if (recheck) { invalidate(); }
    }
    void modelChanged(String value) {
        if (value.equals(model)) { return; }
        model = value;
        if (usesModel(mode) || usesModel(pending)) { invalidate(); }
    }
    private boolean usesModel(AudioMode value) { return value == AudioMode.REALTIME || value == AudioMode.LIVE; }
    private void select(AudioMode requested, boolean checked) {
        if (!checked) { mode = AudioMode.PLATFORM; render(); return; }
        stop(); mode = AudioMode.PLATFORM; render();
        String base;
        try { base = ServerEndpoint.normalize(endpoint.getText().toString()); }
        catch (IllegalArgumentException exception) { unavailable(requested); return; }
        String secret = token.getText().toString(); String selected = model;
        SpeechApiConfig config = configuration;
        RequestScope scope = new RequestScope(); active = scope; pending = requested; render();
        work = worker.submit(() -> check(scope, requested, base, secret, selected, config));
    }
    private void check(RequestScope scope, AudioMode requested, String base, String secret, String selected, SpeechApiConfig config) {
        int outcome;
        try { probe.check(requested, base, secret, selected, config, scope); scope.check(); outcome = 0; }
        catch (Exception exception) { outcome = exception instanceof LiveSetupException ? R.string.custom_live_setup_rejected : -1; }
        int result = outcome;
        activity.runOnUiThread(() -> finish(scope, requested, result));
    }
    private void finish(RequestScope scope, AudioMode requested, int result) {
        if (active != scope) { return; }
        active = null; work = null; pending = AudioMode.PLATFORM;
        mode = result == 0 ? requested : AudioMode.PLATFORM; render();
        if (result != 0) { unavailable(requested, result); }
    }
    private void render() {
        for (var entry : rows.entrySet()) {
            AudioMode value = entry.getKey(); Row row = entry.getValue();
            row.control().setOnCheckedChangeListener(null);
            row.control().setChecked(mode == value); row.control().setEnabled(!checking());
            row.label().setText(pending == value ? row.checking() : row.title());
            row.control().setOnCheckedChangeListener((button, checked) -> select(value, checked));
        }
    }
    private void unavailable(AudioMode requested) {
        unavailable(requested, -1);
    }
    private void unavailable(AudioMode requested, int reason) {
        if (toast != null) { toast.cancel(); }
        int message = reason > 0 ? reason : rows.get(requested).unavailable();
        toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT); toast.show();
    }
    private void invalidate() { stop(); mode = AudioMode.PLATFORM; render(); }
    void stop() {
        if (active != null) { active.cancel(); probe.cancel(active); active = null; }
        if (work != null) { work.cancel(true); work = null; }
        pending = AudioMode.PLATFORM; render();
        if (toast != null) { toast.cancel(); toast = null; }
    }
    @Override public void close() {
        stop(); worker.shutdownNow();
        endpoint.removeTextChangedListener(watcher); token.removeTextChangedListener(watcher);
        for (Row row : rows.values()) { row.control().setOnCheckedChangeListener(null); }
    }
}
