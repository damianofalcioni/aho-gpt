package aho.gpt.android.language;

import android.app.Activity;
import android.content.Context;

/** Applies the display language before inflation and when returning from settings. */
public abstract class LocalizedActivity extends Activity {
    private String appliedLanguage;

    @Override protected void attachBaseContext(Context base) {
        appliedLanguage = AppLanguage.effective(base);
        super.attachBaseContext(AppLanguage.wrap(base));
    }
    @Override protected void onResume() { super.onResume(); refreshLanguage(); }
    public final void refreshLanguage() {
        if (!isFinishing() && !appliedLanguage.equals(AppLanguage.effective(this))) { recreate(); }
    }
}
