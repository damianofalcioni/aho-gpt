package aho.gpt.android.speech;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.speech.RecognitionService;
import java.util.List;

/** Provider inventory must exclude services that cannot acquire microphone audio. */
final class RecognitionProviders {
    interface MicrophonePermission { boolean granted(String name); }
    private RecognitionProviders() { }

    static ComponentName select(Context context) {
        PackageManager packages = context.getPackageManager();
        return select(packages.queryIntentServices(new Intent(RecognitionService.SERVICE_INTERFACE), 0),
                name -> packages.checkPermission(Manifest.permission.RECORD_AUDIO, name) == PackageManager.PERMISSION_GRANTED);
    }
    static ComponentName select(List<ResolveInfo> candidates, MicrophonePermission microphoneGranted) {
        for (ResolveInfo candidate : candidates) {
            ServiceInfo service = candidate.serviceInfo;
            if (enabled(service) && microphoneGranted.granted(service.packageName)) {
                return new ComponentName(service.packageName, service.name);
            }
        }
        return null;
    }
    private static boolean enabled(ServiceInfo service) {
        return service != null && service.enabled && service.exported
                && service.applicationInfo != null && service.applicationInfo.enabled;
    }
}
