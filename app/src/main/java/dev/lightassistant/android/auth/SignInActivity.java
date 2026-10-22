package dev.lightassistant.android.auth;

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
import dev.lightassistant.R;
import dev.lightassistant.android.AssistantApplication;
import java.io.IOException;

public final class SignInActivity extends Activity {
    private volatile LoopbackCallback callback;
    private WebView web;
    private TextView status;
    private Uri authorization;
    private volatile boolean destroyed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        status = new TextView(this); status.setText(R.string.signing_in); layout.addView(status);
        Button browser = new Button(this); browser.setText(R.string.open_browser);
        browser.setOnClickListener(view -> openBrowser()); layout.addView(browser);
        setContentView(layout);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH)) { createWebView(layout); }
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
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return !allowed(request.getUrl());
                }
            });
            layout.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        } catch (RuntimeException exception) { status.setText(R.string.browser_required); }
    }

    private boolean allowed(Uri uri) {
        if ("https".equals(uri.getScheme())) { return true; }
        return "127.0.0.1".equals(uri.getHost()) && "/auth/callback".equals(uri.getPath());
    }

    private void begin(AssistantApplication app) {
        try (LoopbackCallback listener = new LoopbackCallback()) {
            callback = listener;
            OAuthAttempt attempt = new OAuthAttempt(listener.redirect(), app.auth().client());
            Uri uri = app.auth().authorization(attempt);
            runOnUiThread(() -> showAuthorization(uri));
            app.auth().complete(attempt, listener.await(attempt.state));
            runOnUiThread(() -> { if (!destroyed) { finish(); } });
        } catch (Exception exception) {
            runOnUiThread(() -> { if (!destroyed) { status.setText(exception.getMessage()); } });
        }
    }

    private void showAuthorization(Uri uri) {
        if (destroyed) { closeListener(); return; }
        authorization = uri;
        if (web != null) { web.loadUrl(uri.toString()); } else { openBrowser(); }
    }

    private void openBrowser() {
        if (authorization == null) { return; }
        try { startActivity(new Intent(Intent.ACTION_VIEW, authorization)); }
        catch (android.content.ActivityNotFoundException exception) { status.setText(R.string.browser_required); }
    }

    private void closeListener() {
        if (callback == null) { return; }
        try { callback.close(); } catch (IOException exception) { status.setText(R.string.sign_in_cancelled); }
    }
    @Override public void onDestroy() {
        destroyed = true; closeListener();
        if (web != null) { web.destroy(); }
        super.onDestroy();
    }
}
