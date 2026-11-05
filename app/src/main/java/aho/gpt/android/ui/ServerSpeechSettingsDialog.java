package aho.gpt.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.net.CustomSpeechCatalog;
import aho.gpt.custom.ServerEndpoint;
import aho.gpt.custom.AudioMode;

/** Nested drafts commit only to the parent Custom dialog; discovery has no speech side effects. */
final class ServerSpeechSettingsDialog implements AutoCloseable {
    private final Activity activity;
    private final CustomSpeechToggle speech;
    private final AudioMode mode;
    private final EditText endpoint;
    private final EditText token;
    private final View button;
    private final CustomSpeechCatalog catalog = new CustomSpeechCatalog();
    private final TextWatcher watcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence text, int start, int before, int count) { dismiss(); }
        @Override public void afterTextChanged(Editable text) { }
    };
    private AlertDialog dialog;
    private View content;
    private ServerSpeechChoices pickers;
    private int generation;
    ServerSpeechSettingsDialog(Activity activity, View parent, CustomSpeechToggle speech, AudioMode mode) {
        this.activity = activity; this.speech = speech; this.mode = mode;
        endpoint = parent.findViewById(R.id.custom_endpoint); token = parent.findViewById(R.id.custom_token);
        button = parent.findViewById(ServerAudioOptions.of(mode).button()); button.setEnabled(false);
        button.setOnClickListener(view -> show());
        endpoint.addTextChangedListener(watcher); token.addTextChangedListener(watcher);
    }
    void ready() { button.setEnabled(true); }
    AlertDialog show() {
        dismiss();
        ServerAudioOptions options = ServerAudioOptions.of(mode);
        content = activity.getLayoutInflater().inflate(options.layout(), new FrameLayout(activity), false);
        pickers = new ServerSpeechChoices(activity, content, speech, mode);
        dialog = new AlertDialog.Builder(activity).setTitle(options.title()).setView(content)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, (window, which) -> pickers.apply()).create();
        dialog.setOnDismissListener(ignored -> stop());
        content.findViewById(R.id.server_speech_refresh).setOnClickListener(view -> refresh());
        dialog.show(); refresh(); return dialog;
    }
    private void refresh() {
        int request = ++generation;
        String base;
        try { base = ServerEndpoint.normalize(endpoint.getText().toString()); }
        catch (IllegalArgumentException exception) { status(R.string.custom_endpoint_invalid); return; }
        status(R.string.server_speech_loading);
        catalog.load(base, token.getText().toString(), mode != AudioMode.REALTIME, choices -> activity.runOnUiThread(() -> {
            if (request != generation || dialog == null || !dialog.isShowing()) { return; }
            pickers.loaded(choices); status(choices);
        }));
    }
    private void status(int message) { ((TextView) content.findViewById(R.id.server_speech_status)).setText(message); }
    private void status(CustomSpeechCatalog.Choices choices) {
        int models = choices.models() == null ? R.string.server_speech_models_missing : R.string.server_speech_models_listed;
        int voices = choices.voices() == null ? R.string.server_speech_voices_missing : R.string.server_speech_voices_listed;
        if (choices.openAi()) { voices = R.string.server_speech_openai_voices; }
        if (mode == AudioMode.REALTIME) { status(voices); return; }
        ((TextView) content.findViewById(R.id.server_speech_status)).setText(activity.getString(R.string.server_speech_choices_status,
                activity.getString(models), activity.getString(voices)));
    }
    void pause() {
        generation++; catalog.cancel();
        if (dialog != null && dialog.isShowing()) { status(R.string.server_speech_refresh_hint); }
    }
    private void stop() {
        generation++; catalog.cancel();
        if (pickers != null) { pickers.close(); }
    }
    private void dismiss() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
    @Override public void close() {
        dismiss(); catalog.close(); button.setOnClickListener(null);
        endpoint.removeTextChangedListener(watcher); token.removeTextChangedListener(watcher);
    }
}
