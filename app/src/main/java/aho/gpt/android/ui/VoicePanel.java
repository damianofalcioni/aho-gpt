package aho.gpt.android.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import aho.gpt.R;

/** One compact transient answer surface; never a transcript or chat composer. */
public final class VoicePanel extends LinearLayout {
    private final TextView status;
    private final TextView answer;
    private final VoicePanelBackground surface;

    public VoicePanel(Context context) { this(context, () -> { }, () -> { }); }

    public VoicePanel(Context context, Runnable onSpeak, Runnable onClose) {
        super(context);
        setOrientation(HORIZONTAL); setGravity(Gravity.CENTER_VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.voice_panel_padding);
        setPadding(padding, padding, padding, padding);
        surface = new VoicePanelBackground(this);
        setElevation(dp(12));
        LinearLayout text = new LinearLayout(context); text.setOrientation(VERTICAL);
        status = text(12);
        status.setTextColor(context.getColor(R.color.app_text_secondary)); text.addView(status);
        answer = text(18); text.addView(answer);
        addText(text);
        addView(controls(onSpeak, onClose));
    }
    private void addText(LinearLayout text) {
        ScrollView scroll = new ScrollView(getContext()); scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setVerticalFadingEdgeEnabled(true); scroll.setFadingEdgeLength(dp(16));
        scroll.addView(text, new ScrollView.LayoutParams(-1, -2));
        LayoutParams params = new LayoutParams(0, -1, 1); params.setMarginEnd(dp(12));
        addView(scroll, params);
    }
    private LinearLayout controls(Runnable onSpeak, Runnable onClose) {
        LinearLayout controls = new LinearLayout(getContext()); controls.setOrientation(VERTICAL);
        controls.addView(microphone(onSpeak), new LayoutParams(dp(48), dp(48)));
        Button close = new Button(getContext()); close.setText(R.string.close_symbol);
        close.setTextSize(26); close.setIncludeFontPadding(false);
        close.setContentDescription(getContext().getString(R.string.close));
        close.setBackgroundResource(R.drawable.bg_speak); close.setBackgroundTintList(null);
        close.setPadding(0, 0, 0, 0); close.setMinHeight(0); close.setMinimumHeight(0);
        close.setOnClickListener(view -> onClose.run());
        LayoutParams params = new LayoutParams(dp(48), dp(48)); params.topMargin = dp(4);
        controls.addView(close, params); return controls;
    }
    public void show(String state, String message) {
        status.setText(state); answer.setText(message);
        surface.listening(false); surface.speaking(false);
    }
    public void listening(boolean active) { surface.listening(active); }
    public void speaking(boolean active) { surface.speaking(active); }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow(); updateVisibility();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility); updateVisibility();
    }
    @Override protected void onVisibilityChanged(View changed, int visibility) {
        super.onVisibilityChanged(changed, visibility); updateVisibility();
    }
    private void updateVisibility() {
        if (surface != null) { surface.visible(isShown() && getWindowVisibility() == View.VISIBLE && isAttachedToWindow()); }
    }
    @Override protected void onDetachedFromWindow() {
        surface.visible(false); super.onDetachedFromWindow();
    }
    private ImageButton microphone(Runnable action) {
        ImageButton button = new ImageButton(getContext());
        button.setImageResource(R.drawable.ic_mic); button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setContentDescription(getContext().getString(R.string.speak));
        button.setBackgroundResource(R.drawable.bg_speak); button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setOnClickListener(view -> action.run()); return button;
    }
    private TextView text(int size) {
        TextView text = new TextView(getContext());
        text.setTextSize(size); text.setGravity(Gravity.START); return text;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
