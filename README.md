# Light Assistant

A small, voice-first Android AI harness in pure Java. One Speak button starts a request; answers appear in a transient panel and use Android text-to-speech. There is no chat screen, transcript database, bundled speech model, or runtime library dependency.

The home screen shows only the app logo and a large microphone/Speak button. Tap the logo to open **About & settings**, where account connection, model selection, usage, device permissions, assistant settings, tool rescan and sign-out live. The page also includes app version information and help. The native theme follows vibe-navigator's black background, white icons, rounded dark controls and outlined surfaces, with visible D-pad focus and phone, TV and watch sizing.

## Devices and installation

- **Android phones/tablets and Android TV:** `handheld` APK, Android 6 or newer. TV works with a D-pad and your separate button mapper.
- **Wear OS:** `wear` APK, API 26 or newer, with a watch manifest and the same implementation. Speech recognition and sign-in require providers available on the watch itself. Wear OS does not supply WebView; a compatible browser installed on the watch is required by the current OAuth fallback. Phone-mediated sign-in is not implemented.

Build with JDK 21 (Gradle/AGP), Java 17 source compatibility, Android SDK 37, and the supplied Gradle wrapper:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleHandheldDebug assembleWearDebug
```

Set your Android SDK path in an untracked `local.properties`, or `ANDROID_HOME`. This project uses the same Gradle 9.8.1 / AGP 9.4.1 and focused-component structure as `D:\PROJECTS\vibe-navigator`, with **zero runtime dependencies**. JUnit, desktop JSON and PMD are build/test tools only. Release builds shrink code and resources and require your own release signing configuration before distribution.

## Setup

1. Open Light Assistant, tap the app logo for **About & settings**, and choose **Continue with ChatGPT**. Approve ChatGPT plan usage, then choose an account-specific OpenAI model. Return to the main screen and choose **Speak**. No API key, client secret, private ChatGPT endpoint, or alternative model provider is used.
2. Grant the microphone permission when first speaking. Android's installed `SpeechRecognizer` and `TextToSpeech` handle speech; on-device recognition is preferred when available. Otherwise the system recognizer may use its provider's network service.
3. Optionally enable the app's accessibility service after reviewing the disclosure. It adds visible-screen reading, click, text, scroll, back, home, and recents tools. It never intercepts keys or records screen events in the background.
4. Optionally grant **Allow answers over other apps**. The enabled accessibility service can also display an answer overlay. Without either permission, answers remain in the voice activity and are still spoken.

In **About & settings → Speech languages**, configure **Speech recognition (STT)** and **Voice playback (TTS)** separately. Each defaults to **Device language**. Listening shows the requested language name and BCP 47 tag for initial requests and follow-ups. The STT picker uses ready languages reported by the same recognition provider on Android 13 and later; the TTS picker uses the default engine’s available languages. If support cannot be queried, the page explicitly marks device-locale choices as unverified. Language availability depends on installed provider data and may require a download through Android settings. The app does not download speech models. Missing or unsupported TTS data leaves the answer visible, reports the problem, and prevents automatic follow-up listening. TTS language selects the voice language; it does not translate answer text.

An embedded sign-in WebView is provided on devices that support it. OpenAI documents authorization in the system browser; providers may reject embedded authentication. **Open in browser** uses the same PKCE attempt and device-local callback. The app binds an ephemeral `127.0.0.1` listener before opening authorization. It expires after five minutes and closes on activity destruction. Do not open the callback flow on another device: its loopback address belongs to that other device.

The initial implementation keeps one account registration per installation. Sign-out clears credentials while retaining its issued client ID and stable host ID for reauthorization. To replace the registration with a different account, clear app data. Multi-account selection is not implemented.

## TV button mapper

Configure your mapper to launch:

```text
Debug package: dev.lightassistant.debug
Release package: dev.lightassistant
Activity: dev.lightassistant.android.ui.VoiceActivity
```

It also handles `android.intent.action.VOICE_COMMAND` and `android.intent.action.ASSIST`. Example:

```shell
adb shell am start -n dev.lightassistant.debug/dev.lightassistant.android.ui.VoiceActivity
```

On phones exposing default-assistant settings, open **About & settings → Default assistant settings** and select **Light Assistant** under **Assist app**. The system assistant shortcut (usually holding Home on Android 8) opens the voice panel and starts listening after account/model setup and microphone permission. If necessary, find the picker under Android **Settings → Apps → Default apps → Assist & voice input**; labels and shortcuts depend on the device. This uses the exported `ACTION_ASSIST` activity and the installed speech provider. The app does not register a voice interaction or recognition service, provide a wake word, or support assistant invocation from the lock screen. Custom global key combinations require an external mapper or a device-provided shortcut. TV key routing is handled by your external mapper.

## Conversation continuity

The microphone opens only after user invocation, or briefly after a spoken answer while the voice panel remains visible. After TTS finishes, an **eight-second follow-up window** retains the context. Starting speech inside that window continues the exchange; silence, Close, an error, app process death, or activity recreation clears it. Initial recognition has a fifteen-second timeout; speech in progress has a twenty-second maximum. There is no always-listening wake word or background microphone service.

History is held only in memory and replayed to Responses with `store:false` and `stream:true`. It is not stored as a ChatGPT conversation. The next new request clears expired history. Complete tool exchanges remain together; a size bound starts a fresh session on the next user turn. Ordinary OpenAI service data policies still apply to transmitted requests.

## Installed apps as tools

On process start and explicit rescan, the app enumerates installed packages and reads their compiled manifests through public Android resource APIs. It catalogs accessible exported **activity, activity-alias, service, and broadcast-receiver intent filters**, including custom actions. The model searches the local catalog through a function, then invokes an exact discovered capability ID. Only selected catalog results go to OpenAI, rather than the entire inventory on every turn.

Intent and accessibility actions run within the active voice request. Ambiguous actions are clarified through spoken follow-up. Dispatch failures are returned to the model; a dispatched intent is never reported as verified completion. No action runs from a partial or failed model response, and tool rounds are bounded. Close cancels pending inference.

App searches list discovered launcher activities first and mark them with `opens_app`. Opening an app by intent uses its exact exported component, `ACTION_MAIN`, the discovered phone or TV launcher category, and launcher task flags. When a suitable launcher is found, the model is instructed to invoke that entry directly without minimizing the assistant or reading the screen first. Android still controls whether a launch is allowed; background launches may be restricted.

If targeted intent searches find no usable capability, or intent dispatch reports a failure, the model is instructed to try the enabled accessibility tools for the same clear spoken request. This fallback can locate and open the requested app through Home, its launcher or Recents, and navigate inside it. The model must read fresh screen state after navigation, use node IDs from the latest read and check the visible target after opening it. Ambiguous targets require a spoken clarification; platform permissions and inaccessible controls remain binding. If accessibility is unavailable or the fallback fails, the model reports the limitation. This is model guidance within the existing eight-round tool limit, not a deterministic fallback controller or a guarantee of successful UI navigation.

The local catalog can contain thousands of capabilities. Responses receives two tool functions, or four with accessibility enabled; catalog searches return at most 25 matching entries per page. The app does not send the whole catalog with each request. Android package-resolver queries supplement manifest parsing for phone/TV launchers and general HTTP/HTTPS handlers, including split APK installations. Other filters in split APK manifests may remain undiscovered.

The model can call `find_intents` with `{"mode":"overview"}` to learn which capability categories are present, with counts of declarations and distinct apps. The overview contains no app identities or capability IDs. Categories describe app launchers, web links, viewing content, sharing, photo/video camera entry points, dialing, message composition, content selection, editing and unknown/custom intents. Counts describe declared handlers, not successful task completion.

For a known goal, it can search directly, for example `{"category":"open_link","app":"Chrome","scheme":"https"}`. Optional filters include app-name/package substring (`app`), exact package (`package`), category, component kind, Android action, URI scheme and MIME type. Filters combine with AND; MIME filtering supports wildcards. Optional keyword searches match all words across app metadata, schemes, MIME types and the standard category description. Exact app matches rank ahead of substring matches, with launcher preference within equally relevant results. The model is instructed to narrow searches, inspect the overview or paginate before concluding a capability is missing. Existing `query`/`offset` searches remain supported.

Every result includes `category`, `description`, `usage` and `semantics_known`. Descriptions come from known Android contracts, not from guesses about app names. Custom actions and non-activity intents retain unknown semantics. Usage notes identify executor limitations: text sharing can use the documented string extra, while image streams, URI grants and returned camera/document results are unsupported. Search descriptions do not expand execution permissions or claim those missing features exist.

Android does not publish a complete semantic schema for app extras, hidden/non-exported capabilities, signature-protected components, dynamic receivers, or every deep-link constraint. Discovery cannot make those callable. The scanner reports unreadable manifests, cannot bypass platform access controls, and does not invent extra semantics. String extras are supported; file grants, arbitrary parcelables, nested intents, and file/content URIs are excluded. Background service restrictions still apply. Rescan after installing or updating apps; stale capability IDs fail closed.

`QUERY_ALL_PACKAGES` is necessary for the requested broad catalog. Store distribution requires an eligibility review for broad package visibility and accessibility functionality; this repository does not claim Play approval.

## Security and architecture

- `core/`: monotonic session policy, memory-only conversation state, and JVM-testable capability classification, filtering, ranking and overview counts.
- `openai/`: Responses/SSE parsing, function schemas, bounded tool loop; plain JVM tests.
- `android/auth/`: OAuth PKCE/state/nonce, signed ID-token verification, serialized refresh and Android Keystore AES-GCM atomic credential storage. App OAuth tokens are excluded from backups and never passed to the WebView.
- `android/tools/`: manifest inventory, searchable capability catalog and explicit intent dispatch.
- `android/access/`: opt-in UI inspection and actions, with password nodes excluded and no passive event retention.
- `android/speech/`: lifecycle-owned platform STT/TTS.
- `android/ui/`: native buttons, transient answer surfaces, exported voice-command and default-assistant activity.

App and screen text are treated as untrusted data in model instructions. Tool execution is tied to the active voice request and Android permission boundaries. Password filtering does not make other screen content non-sensitive: enabling screen tools permits requested visible text to be sent to OpenAI.

## Verification

```powershell
.\gradlew.bat complexityCheck testHandheldDebugUnitTest testWearDebugUnitTest lintHandheldDebug lintWearDebug assembleHandheldDebug assembleWearDebug assembleHandheldRelease assembleWearRelease
```

PMD inherits the reference project's zero-violation baseline: cognitive complexity below 10, method cyclomatic complexity below 8, class complexity below 40, NPath below 80, method NCSS below 60, and class NCSS below 400. No runtime dependency is added by these checks. CI runs the same validation.

Tests cover follow-up expiry, context reset, tool result replay, tool loop bounds, namespace isolation, truncated SSE, late inference failures, language catalog normalization, capability classification, structured filtering, wildcard MIME types, ranking, bounded pagination and overview privacy. Real-device OAuth, speech providers, accessibility interactions, TV mapper behavior, watch layouts and overlays require device testing. No live inference or account access is performed by the build.

The dependency-free `IntentLaunchTests` instrumentation checks phone and TV launcher categories and task flags, ordinary activities, deep-link preservation, invalid arguments, launch rejection, Android MIME metadata normalization, real-catalog discovery through `DeviceTools`, and exported assistant/voice-command resolution without a shadowing voice interaction service. Build them with `assembleHandheldDebugAndroidTest assembleWearDebugAndroidTest`. For an existing handheld installation, run the following commands to update the app and test APK in place, then execute the checks directly. The current Gradle connected-test installer refused to replace the existing app during device verification. Dispatch-construction checks use a recording activity; they do not prove that Chrome opens on a particular device.

```powershell
adb install -r app/build/outputs/apk/handheld/debug/app-handheld-debug.apk
adb install -r app/build/outputs/apk/androidTest/handheld/debug/app-handheld-debug-androidTest.apk
adb shell am instrument -w dev.lightassistant.debug.test/dev.lightassistant.android.tools.IntentLaunchTests
```

For Wear OS, use the corresponding wear APK paths and `dev.lightassistant.wear.debug.test` instrumentation package.

For an explicitly requested real-app launch, install the app and its test APK, then run `adb shell am instrument -w -e launch_package com.android.chrome dev.lightassistant.debug.test/dev.lightassistant.android.tools.IntentLaunchTests`. This opt-in check opens the assistant's home screen, scans the real catalog, selects the requested package's discovered launcher and invokes it through `DeviceTools`. Verify the foreground package separately. It does not use speech or contact OpenAI.

On 22 October 2026, all eight instrumentation checks passed on a Huawei PRA-LX1 running Android 8 (API 26). The catalog contained 2,537 declarations; structured Chrome searches selected one app launcher and found two HTTPS handler entries. The opt-in Chrome check dispatched the selected launcher through `DeviceTools`; Android activity and window state then identified Chrome as the resumed and focused app. This verifies that device's discovery and catalog-to-launch path, without claiming an end-to-end spoken-request, model, OAuth, TV or Wear OS test.

On 23 October 2026, the updated debug app and all nine instrumentation checks passed on the same phone. Selecting Light Assistant in the system **Assist app** picker persisted after reopening Settings. Holding Home opened `VoiceActivity`, focused its window, displayed the listening state and bound the installed Google speech recognition service. The microphone test was then closed. This verifies default-assistant selection and microphone startup on that device, without claiming a completed spoken request or new OAuth verification. Both variants also passed unit tests, complexity checks, lint and debug/release builds; inspection of both shrunk release APKs confirmed the voice activity remained present and the obsolete assistant services were absent.

## Official implementation references

- [OpenAI registration and sign-in](https://developers.openai.com/siwc/token-sharing-open-source/sign-in)
- [OpenAI models and inference](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference)
- [OpenAI preview request limitations](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations)
- [OpenAI account sessions and token refresh](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions)
- [Android WebView availability, including Wear OS](https://developer.android.com/develop/ui/views/layout/webapps/managing-webview)
- [Android speech recognition language support](https://developer.android.com/reference/android/speech/RecognitionSupport)
- [Android text-to-speech languages](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
- [Android accessibility services](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Android package visibility](https://developer.android.com/training/package-visibility)
- [Android standard intent contracts](https://developer.android.com/guide/components/intents-common)
