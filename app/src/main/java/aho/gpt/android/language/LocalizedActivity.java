package aho.gpt.android.language;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import aho.gpt.android.ui.AppTheme;

/** Applies display preferences before inflation and when returning from settings. */
public abstract class LocalizedActivity extends Activity {
    private String appliedLanguage;
    private boolean appliedLightTheme;

    @Override protected void attachBaseContext(Context base) {
        appliedLanguage = AppLanguage.effective(base);
        super.attachBaseContext(AppLanguage.wrap(base));
    }
    @Override protected void onCreate(Bundle state) {
        appliedLightTheme = AppTheme.apply(this);
        super.onCreate(state);
    }
    @Override protected void onResume() { super.onResume(); refreshDisplay(); }
    public final void refreshDisplay() {
        boolean changed = appliedLightTheme != AppTheme.isLight(this) || !appliedLanguage.equals(AppLanguage.effective(this));
        if (!isFinishing() && changed) { recreate(); }
    }
}
