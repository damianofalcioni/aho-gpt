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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Mutually exclusive draft speech routes; only successful checks can enable them. */
final class CustomSpeechToggle implements AutoCloseable {
    private final Activity activity;
    private final Switch speech;
    private final TextView speechLabel;
    private final TextView realtimeLabel;
    private final Switch realtime;
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
        speech = content.findViewById(R.id.custom_speech_apis); realtime = content.findViewById(R.id.custom_realtime_api);
        speechLabel = content.findViewById(R.id.custom_speech_label);
        realtimeLabel = content.findViewById(R.id.custom_realtime_label);
        endpoint = content.findViewById(R.id.custom_endpoint); token = content.findViewById(R.id.custom_token);
        model = new ProviderSettings(activity).customModel();
        configuration = new ProviderSettings(activity).speechConfig();
        speech.setEnabled(false); realtime.setEnabled(false);
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
        if (mode == AudioMode.REALTIME || pending == AudioMode.REALTIME) { invalidate(); }
    }
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
        boolean supported;
        try { probe.check(requested, base, secret, selected, config, scope); scope.check(); supported = true; }
        catch (Exception exception) { supported = false; }
        boolean result = supported;
        activity.runOnUiThread(() -> finish(scope, requested, result));
    }
    private void finish(RequestScope scope, AudioMode requested, boolean supported) {
        if (active != scope) { return; }
        active = null; work = null; pending = AudioMode.PLATFORM;
        mode = supported ? requested : AudioMode.PLATFORM; render();
        if (!supported) { unavailable(requested); }
    }
    private void render() {
        speech.setOnCheckedChangeListener(null); realtime.setOnCheckedChangeListener(null);
        speech.setChecked(mode == AudioMode.SPEECH); realtime.setChecked(mode == AudioMode.REALTIME);
        speech.setEnabled(!checking()); realtime.setEnabled(!checking());
        speechLabel.setText(pending == AudioMode.SPEECH ? R.string.custom_speech_checking : R.string.custom_speech_apis);
        realtimeLabel.setText(pending == AudioMode.REALTIME ? R.string.custom_realtime_checking : R.string.custom_realtime_api);
        speech.setOnCheckedChangeListener((button, checked) -> select(AudioMode.SPEECH, checked));
        realtime.setOnCheckedChangeListener((button, checked) -> select(AudioMode.REALTIME, checked));
    }
    private void unavailable(AudioMode requested) {
        if (toast != null) { toast.cancel(); }
        int message = requested == AudioMode.REALTIME ? R.string.custom_realtime_unavailable : R.string.custom_speech_unavailable;
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
        speech.setOnCheckedChangeListener(null); realtime.setOnCheckedChangeListener(null);
    }
}
