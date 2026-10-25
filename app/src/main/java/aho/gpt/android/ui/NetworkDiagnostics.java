package aho.gpt.android.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.widget.Toast;
import aho.gpt.R;

/** Uses Android's connection assessment without sending a diagnostic request. */
final class NetworkDiagnostics implements AutoCloseable {
    private final Activity activity;
    private final DiagnosticRow row;

    NetworkDiagnostics(Activity activity) {
        this.activity = activity;
        row = new DiagnosticRow(activity, R.id.diagnostic_access, R.string.diagnostic_internet, this::settings);
    }
    void refresh() {
        int status = status();
        int color = status == R.string.diagnostic_validated ? R.color.diagnostic_ok : R.color.diagnostic_warning;
        if (status == R.string.diagnostic_offline) { color = R.color.diagnostic_error; }
        row.update(status, color, activity.getString(R.string.diagnostic_internet_help));
    }
    private int status() {
        ConnectivityManager manager = activity.getSystemService(ConnectivityManager.class);
        if (manager == null) { return R.string.diagnostic_unverified; }
        if (manager.getActiveNetwork() == null) { return R.string.diagnostic_offline; }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
        if (capabilities == null) { return R.string.diagnostic_unverified; }
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
            return R.string.diagnostic_network_sign_in;
        }
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ? R.string.diagnostic_validated : R.string.diagnostic_unverified;
    }
    private void settings() {
        try { activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); }
        catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(activity, R.string.setting_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
    @Override public void close() { row.close(); }
}
