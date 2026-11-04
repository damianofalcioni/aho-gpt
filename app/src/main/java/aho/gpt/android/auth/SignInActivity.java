package aho.gpt.android.auth;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import aho.gpt.R;
import aho.gpt.android.AssistantApplication;
import aho.gpt.android.logging.AppLogger;
import java.io.IOException;

public final class SignInActivity extends Activity {
    private static final String TAG = "Auth";
    private volatile LoopbackCallback callback;
    private WebView web;
    private TextView status;
    private Uri authorization;
    private volatile boolean destroyed;
    private boolean usage;

    @android.annotation.SuppressLint("WearRecents") // This temporary account task must not replace the app's launcher task.
    public static void open(Activity activity, boolean usage) {
        activity.startActivity(new Intent(activity, SignInActivity.class).putExtra("usage", usage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        usage = getIntent().getBooleanExtra("usage", false);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setFitsSystemWindows(true);
        int padding = getResources().getDimensionPixelSize(R.dimen.page_padding);
        layout.setPadding(padding, padding, padding, padding);
        status = new TextView(this); status.setText(usage ? R.string.manage_usage : R.string.signing_in); layout.addView(status);
        Button browser = new Button(this); browser.setText(R.string.open_browser);
        browser.setOnClickListener(view -> openBrowser()); layout.addView(browser);
        setContentView(layout);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH)) { createWebView(layout); }
        if (usage) { showAuthorization(Uri.parse("https://chatgpt.com/settings/usage")); return; }
        AssistantApplication app = (AssistantApplication) getApplication();
        app.authWorker().execute(() -> begin(app));
    }

    @android.annotation.SuppressLint("SetJavaScriptEnabled") // OAuth pages need JavaScript. No native bridge or local-file access.
    private void createWebView(LinearLayout layout) {
        try {
            web = new WebView(this);
            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setDomStorageEnabled(true);
            web.getSettings().setAllowFileAccess(false);
            web.getSettings().setAllowContentAccess(false);
            CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
            web.setWebViewClient(new WebViewClient() {
                @SuppressWarnings("deprecation") // Android 6 delivers this navigation callback.
                @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return !allowed(Uri.parse(url));
                }
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return !allowed(request.getUrl());
                }
            });
            layout.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        } catch (RuntimeException exception) {
            AppLogger.w(TAG, "WebView unavailable", exception); status.setText(R.string.browser_required);
        }
    }

    private boolean allowed(Uri uri) {
        if ("https".equals(uri.getScheme())) { return true; }
        return !usage && "http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost()) && "/auth/callback".equals(uri.getPath());
    }

    private void begin(AssistantApplication app) {
        AppLogger.i(TAG, "Sign-in started");
        try (LoopbackCallback listener = new LoopbackCallback()) {
            callback = listener;
            OAuthAttempt attempt = new OAuthAttempt(listener.redirect(), app.auth().client());
            Uri uri = app.auth().authorization(attempt);
            runOnUiThread(() -> showAuthorization(uri));
            app.auth().complete(attempt, listener.await(attempt.state,
                    getString(R.string.sign_in_received), getString(R.string.sign_in_invalid_callback)));
            AppLogger.i(TAG, "Sign-in completed");
            runOnUiThread(() -> { if (!destroyed) { finish(); } });
        } catch (Exception exception) {
            AppLogger.e(TAG, "Sign-in stopped", exception);
            runOnUiThread(() -> { if (!destroyed) { status.setText(exception.getMessage()); } });
        }
    }

    private void showAuthorization(Uri uri) {
        if (destroyed) { closeListener(); return; }
        authorization = uri;
        if (web != null) { web.loadUrl(uri.toString()); } else { openBrowser(); }
    }

    @android.annotation.SuppressLint("WearRecents") // Keep the external browser alive when this temporary task is removed.
    private void openBrowser() {
        if (authorization == null) { return; }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, authorization).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            AppLogger.i(TAG, "Browser opened");
        }
        catch (android.content.ActivityNotFoundException exception) {
            AppLogger.w(TAG, "Browser unavailable", exception); status.setText(R.string.browser_required);
        }
    }

    private void closeListener() {
        if (callback == null) { return; }
        try { callback.close(); } catch (IOException exception) { status.setText(R.string.sign_in_cancelled); }
    }
    @Override public void finish() {
        if (isTaskRoot()) { super.finishAndRemoveTask(); } else { super.finish(); }
    }
    @Override public void onDestroy() {
        destroyed = true; closeListener();
        if (web != null) { web.destroy(); }
        super.onDestroy();
    }
}
