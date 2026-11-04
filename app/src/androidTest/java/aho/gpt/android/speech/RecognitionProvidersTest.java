package aho.gpt.android.speech;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import java.util.Arrays;
import java.util.Collections;

/** Exercises real Android provider descriptors without opening the microphone or changing packages. */
public final class RecognitionProvidersTest {
    private final Context context;

    public RecognitionProvidersTest(Context context) { this.context = context; }
    public void run() {
        ResolveInfo automation = service("example.automation");
        ResolveInfo recognizer = service("example.recognizer");
        require(RecognitionProviders.select(Collections.singletonList(automation), name -> false) == null,
                "A service declaration without microphone permission must not enable STT");
        ComponentName selected = RecognitionProviders.select(Arrays.asList(automation, recognizer),
                name -> name.equals("example.recognizer"));
        require(selected != null && "example.recognizer".equals(selected.getPackageName()),
                "Skip the unusable declaration and select the working alternative");
        require(RecognitionProviders.select(Collections.emptyList(), name -> true) == null, "An empty inventory must disable STT");
        disabled(recognizer);
        require(SpeechAvailability.input(context) == (RecognitionProviders.select(context) != null),
                "Live availability must match the filtered inventory");
    }
    private void disabled(ResolveInfo candidate) {
        candidate.serviceInfo.enabled = false;
        require(RecognitionProviders.select(Collections.singletonList(candidate), name -> true) == null, "Skip disabled services");
        candidate.serviceInfo.enabled = true; candidate.serviceInfo.applicationInfo.enabled = false;
        require(RecognitionProviders.select(Collections.singletonList(candidate), name -> true) == null, "Skip disabled apps");
        candidate.serviceInfo.applicationInfo.enabled = true; candidate.serviceInfo.exported = false;
        require(RecognitionProviders.select(Collections.singletonList(candidate), name -> true) == null, "Skip private services");
    }
    private ResolveInfo service(String name) {
        ServiceInfo service = new ServiceInfo();
        service.packageName = name; service.name = name + ".RecognitionService";
        service.enabled = true; service.exported = true;
        service.applicationInfo = new ApplicationInfo(); service.applicationInfo.enabled = true;
        ResolveInfo result = new ResolveInfo(); result.serviceInfo = service;
        return result;
    }
    private void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
