package dev.lightassistant.android.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import dev.lightassistant.R;

/** One transient answer surface; never a transcript or chat composer. */
public final class VoicePanel extends LinearLayout {
    private final TextView status;
    private final TextView answer;
    private final Button speak;

    public VoicePanel(Context context) { this(context, () -> { }, () -> { }); }

    public VoicePanel(Context context, Runnable onSpeak, Runnable onClose) {
        super(context);
        setOrientation(VERTICAL); setPadding(dp(24), dp(20), dp(24), dp(20));
        setBackgroundResource(R.drawable.bg_panel);
        setElevation(dp(12));
        boolean watch = context.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        LinearLayout body = content(watch);
        status = text(14); status.setText(R.string.ready); body.addView(status);
        answer = text(20); answer.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        addAnswer(body, watch);
        speak = button(R.string.speak, onSpeak); body.addView(speak);
        body.addView(button(R.string.close, onClose));
    }
    private LinearLayout content(boolean watch) {
        if (!watch) { return this; }
        setPadding(dp(30), dp(30), dp(30), dp(30));
        ScrollView scroll = new ScrollView(getContext());
        LinearLayout body = new LinearLayout(getContext()); body.setOrientation(VERTICAL);
        scroll.addView(body); addView(scroll, new LayoutParams(-1, -1));
        return body;
    }
    private void addAnswer(LinearLayout body, boolean watch) {
        if (watch) { body.addView(answer); return; }
        ScrollView scroll = new ScrollView(getContext()); scroll.addView(answer);
        body.addView(scroll, new LayoutParams(-1, 0, 1));
    }
    public void show(String state, String message) {
        status.setText(state); answer.setText(message); speak.setVisibility(View.VISIBLE);
    }
    private Button button(int label, Runnable action) {
        Button button = new Button(getContext()); button.setText(label);
        LayoutParams params = new LayoutParams(-1, -2); params.topMargin = dp(8);
        button.setLayoutParams(params);
        button.setOnClickListener(view -> action.run()); return button;
    }
    private TextView text(int size) {
        TextView text = new TextView(getContext());
        text.setTextSize(size); text.setGravity(Gravity.CENTER); return text;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
