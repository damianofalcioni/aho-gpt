# Aho! GPT

A small, voice-first Android AI harness in pure Java. One microphone button starts a request; answers appear in a transient panel and optionally use Android text-to-speech. There is no chat screen, transcript database, bundled speech model, or runtime library dependency.

The home screen shows the app logo, a central account/model/discovered-intent count summary, and a large icon-only microphone button. Tap the logo or a status row to open **About & settings**. The microphone also redirects there when account or model setup is missing. Settings starts with app name, version, description, project links, copyright and adapted credits, followed by divided Languages, Account & model, Device & permissions, and help sections. It uses Android system navigation without an in-page Back button. The native theme follows vibe-navigator's black background, white icons, rounded dark controls and outlined surfaces, with visible D-pad focus and phone, TV and watch sizing. About links and active switches use the same inherited native Material accent colors as the reference app. Settings also match its regular headings, compact row spacing, 16dp phone inset, 44dp gear and 40dp info controls, muted secondary text and colored source/issue icons. Account and device actions have adjacent help buttons. Ready speech-provider messages stay in the info dialogs; only checking/unavailable status adds text below a speech row. The TV home screen places its summary and microphone side by side.

The home-screen logo uses white lines on its button's dark surface, with cutouts matching the normal and focused button colors. The launcher icon and TV banner retain the approved black-on-white Aho!/GPT calling-person artwork. All artwork uses native paths with no bundled PNG or runtime tracing library. Android 8 and later use an adaptive launcher icon with a white background and inset artwork to accommodate launcher masks; Android 6–7 use a square legacy icon. The home screen gives the logo greater prominence and uses a smaller microphone circle with a spoken accessibility label. Logo/button diameters are 112/160dp on phones, 128/216dp on TV and 64/88dp on watches.

About links assume [damianofalcioni/aho-gpt](https://github.com/damianofalcioni/aho-gpt) and its GitHub Pages homepage, as requested; publication of those destinations has not been verified. Privacy and usage information open local dialogs.

## Devices and installation

The visible app name is **Aho! GPT** (from the Italian exclamation “Aho!”); the repository path is `aho-gpt`. Its base application ID and Java namespace are `aho.gpt`; debug builds add `.debug`, and Wear OS builds add `.wear` before that debug suffix. The visible-name update keeps those IDs and existing installation data. Android treats the earlier application-ID rename as a separate installation from its predecessor: sign in again, grant permissions, and reselect the default assistant/accessibility service as needed. Update external TV button mappers to the component below.

- **Android phones/tablets and Android TV:** `handheld` APK, Android 6 or newer. TV works with a D-pad and your separate button mapper.
- **Wear OS:** `wear` APK, API 26 or newer, with a watch manifest and the same implementation. Speech recognition and sign-in require providers available on the watch itself. Wear OS does not supply WebView; a compatible browser installed on the watch is required by the current OAuth fallback. Phone-mediated sign-in is not implemented.

Build with JDK 21 (Gradle/AGP), Java 17 source compatibility, Android SDK 37, and the supplied Gradle wrapper:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleHandheldDebug assembleWearDebug
```

Set your Android SDK path in an untracked `local.properties`, or `ANDROID_HOME`. This project uses the same Gradle 9.8.1 / AGP 9.4.1 and focused-component structure as `D:\PROJECTS\vibe-navigator`, with **zero runtime dependencies**. JUnit, desktop JSON and PMD are build/test tools only. Release builds shrink code and resources and require your own release signing configuration before distribution.

## Setup

1. Open Aho! GPT, tap the app logo or an account/model status row for **About & settings**, and choose **Continue with ChatGPT**. Approve ChatGPT plan usage, then choose an account-specific OpenAI model. Return to the main screen and tap the microphone. No API key, client secret, private ChatGPT endpoint, or alternative model provider is used.
2. Grant the microphone permission when first speaking. Android's installed `SpeechRecognizer` and `TextToSpeech` handle speech; on-device recognition is preferred when available. Otherwise the system recognizer may use its provider's network service.
3. Optionally enable the app's accessibility service after reviewing the disclosure. It adds visible-screen reading, click, text, scroll, back, home, and recents tools. It never intercepts keys or records screen events in the background.
4. Optionally grant **Allow answers over other apps**. The enabled accessibility service can also display an answer overlay. Without either permission, answers remain in the voice activity and are still spoken.

In **About & settings → Languages**, TTS and STT each have a single row with a gear, enable switch and info button. Each gear opens selection, **Test**, and a shortcut to Android's corresponding speech settings. Switches are disabled when their provider is unavailable or an installed Google app is disabled; devices without the Google app can use other installed providers. Provider loss preserves the saved enable preference. The gears remain usable for setup.

STT and TTS language choices are saved separately and default to **Device language**. TTS also offers installed voices from the default engine; selecting a voice updates its language, and selecting a language restores its default voice. The selected voice is used for answers. Explicit voice tests work even with automatic playback off. STT tests request microphone permission, show their result locally, and never invoke a model or tools. Tests stop on backgrounding or dismissal. Turning STT off prevents listening. Turning TTS off leaves answers visible and prevents automatic follow-up listening.

Listening shows the requested language name and BCP 47 tag for initial requests and follow-ups. The STT picker uses ready languages reported by the same recognition provider on Android 13 and later; if support cannot be queried, it explicitly marks device-locale choices as unverified. TTS lists installed voices and their languages reported by the default engine. Language availability depends on installed provider data and may require installation through Android settings. The app does not download speech models. Missing or unsupported TTS data or a missing selected voice leaves the answer visible, reports the problem, and prevents automatic follow-up listening. TTS voice selection does not translate answer text.

An embedded sign-in WebView is provided on devices that support it. OpenAI documents authorization in the system browser; providers may reject embedded authentication. **Open in browser** uses the same PKCE attempt and device-local callback. The app binds an ephemeral `127.0.0.1` listener before opening authorization. It expires after five minutes and closes on activity destruction. Do not open the callback flow on another device: its loopback address belongs to that other device.

The initial implementation keeps one account registration per installation. Sign-out clears credentials while retaining its issued client ID and stable host ID for reauthorization. To replace the registration with a different account, clear app data. Multi-account selection is not implemented.

## TV button mapper

Configure your mapper to launch:

```text
Debug package: aho.gpt.debug
Release package: aho.gpt
Activity: aho.gpt.android.ui.VoiceActivity
```

It also handles `android.intent.action.VOICE_COMMAND` and `android.intent.action.ASSIST`. Example:

```shell
adb shell am start -n aho.gpt.debug/aho.gpt.android.ui.VoiceActivity
```

On phones exposing default-assistant settings, open **About & settings → Default assistant settings** and select **Aho! GPT** under **Assist app**. The system assistant shortcut (usually holding Home on Android 8) opens the voice panel and starts listening after account/model setup and microphone permission. If necessary, find the picker under Android **Settings → Apps → Default apps → Assist & voice input**; labels and shortcuts depend on the device. This uses the exported `ACTION_ASSIST` activity and the installed speech provider. The app does not register a voice interaction or recognition service, provide a wake word, or support assistant invocation from the lock screen. Custom global key combinations require an external mapper or a device-provided shortcut. TV key routing is handled by your external mapper.

## Conversation continuity

The microphone opens only after user invocation, or briefly after a spoken answer while the voice panel remains visible. After TTS finishes, an **eight-second follow-up window** retains the context. Starting speech inside that window continues the exchange; silence, Close, an error, app process death, or activity recreation clears it. Initial recognition has a fifteen-second timeout; speech in progress has a twenty-second maximum. There is no always-listening wake word or background microphone service.

History is held only in memory and replayed to Responses with `store:false` and `stream:true`. It is not stored as a ChatGPT conversation. The next new request clears expired history. Complete tool exchanges remain together; a size bound starts a fresh session on the next user turn. Ordinary OpenAI service data policies still apply to transmitted requests.

## Installed apps as tools

On process start and explicit rescan, the app enumerates installed packages and reads their compiled manifests through public Android resource APIs. It catalogs accessible exported **activity, activity-alias, service, and broadcast-receiver intent filters**, including custom actions. The model searches the local catalog through a function, then invokes an exact discovered capability ID. Only selected catalog results go to OpenAI, rather than the entire inventory on every turn.

Intent and accessibility actions run within the active voice request. Ambiguous actions are clarified through spoken follow-up. Dispatch failures are returned to the model; a dispatched intent is never reported as verified completion. No action runs from a partial or failed model response, and tool rounds are bounded. Close cancels pending inference.

Each spoken turn has separate, cancellable execution authority lasting at most two minutes from recognition completion. The agent checks it after network responses and before tools; queued Android work checks again on the main thread. Closing the panel revokes that authority before cancelling inference. An action already dispatched to another app cannot be undone by cancellation. Follow-up turns receive new authority while retaining the existing short-lived conversation context.

Tool arguments are validated locally against the advertised names, types, enums, required fields and size limits. Parsing rejects duplicate JSON keys, trailing data and unsupported syntax. Intent filters and Android permissions still govern execution. Each turn permits at most eight model rounds and 64 distinct tool call IDs. A repeated ID with identical encoded arguments returns its cached receipt; reusing it for a different tool or arguments is refused. This does not deduplicate newly assigned call IDs: the model is instructed never to replay mutations under a new ID or retry an uncertain mutation, and to inspect fresh state or explain uncertainty instead.

Mutation receipts distinguish `refused` before dispatch, `rejected` by the platform, `accepted` for a dispatch acknowledgement and `unknown` when execution cannot be established, including timeouts. Receipts do not claim task completion inside another app. UI actions invalidate cached screen nodes, requiring a fresh screen read before another targeted action. Unexpected tool exceptions return fixed codes rather than raw exception text. These checks do not independently establish that a model-selected action matches the spoken request; task scope and ambiguity remain model-guided.

App searches list discovered launcher activities first and mark them with `opens_app`. Opening an app by intent uses its exact exported component, `ACTION_MAIN`, the discovered phone or TV launcher category, and launcher task flags. When a suitable launcher is found, the model is instructed to invoke that entry directly without minimizing the assistant or reading the screen first. Android still controls whether a launch is allowed; background launches may be restricted.

If targeted intent searches find no usable capability, or intent dispatch reports a failure, the model is instructed to try the enabled accessibility tools for the same clear spoken request. This fallback can locate and open the requested app through Home, its launcher or Recents, and navigate inside it. The model must read fresh screen state after navigation, use node IDs from the latest read and check the visible target after opening it. Ambiguous targets require a spoken clarification; platform permissions and inaccessible controls remain binding. If accessibility is unavailable or the fallback fails, the model reports the limitation. This is model guidance within the existing eight-round tool limit, not a deterministic fallback controller or a guarantee of successful UI navigation.

The local catalog can contain thousands of capabilities. Responses receives two tool functions, or four with accessibility enabled; catalog searches return at most 25 matching entries per page. The app does not send the whole catalog with each request. Android package-resolver queries supplement manifest parsing for phone/TV launchers and general HTTP/HTTPS handlers, including split APK installations. Other filters in split APK manifests may remain undiscovered.

PWAs installed as Android packages (WebAPKs) can appear in that catalog through their exported launchers and intent filters. A plain **Add to Home screen** website shortcut has no separate installed-app entry and is not scanned. With accessibility enabled, the model is instructed to open Home, read the launcher and click the requested visible shortcut even when the native app is absent or disabled. A request to open a website with an unambiguous URL can also use a discovered browser HTTP/HTTPS handler, searched without the website's app-name filter. A specifically requested shortcut is not replaced with a guessed URL. Launcher accessibility support and the bounded model loop still determine whether this succeeds; the app does not have privileged access to the launcher's shortcut database.

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
adb shell am instrument -w aho.gpt.debug.test/aho.gpt.android.tools.IntentLaunchTests
```

For Wear OS, use the corresponding wear APK paths and `aho.gpt.wear.debug.test` instrumentation package.

For an explicitly requested real-app launch, install the app and its test APK, then run `adb shell am instrument -w -e launch_package com.android.chrome aho.gpt.debug.test/aho.gpt.android.tools.IntentLaunchTests`. This opt-in check opens the assistant's home screen, scans the real catalog, selects the requested package's discovered launcher and invokes it through `DeviceTools`. Verify the foreground package separately. It does not use speech or contact OpenAI.

For an explicitly requested home-screen shortcut check, use `adb shell am instrument -r -w -e home_shortcut YouTube -e screen_marker youtube.com aho.gpt.debug.test/aho.gpt.android.tools.IntentLaunchTests`. The named shortcut must be on the current Home page, and its label must uniquely match a clickable node. Instrumentation restarts the app process; if accessibility disconnects, reconnect the already-enabled service in Android settings during the test's 30-second wait. The check uses `DeviceTools` to navigate Home, read the screen, click the matching shortcut and read fresh state for the requested marker, with the active package changed from Home to another external app. It outputs only counts, dispatch receipts and check results, with no screen text. A visible marker is limited evidence of the opened destination, not verification of page loading or any subsequent task.

Before the rename, on 22 October 2026, all eight instrumentation checks passed on a Huawei PRA-LX1 running Android 8 (API 26). The catalog contained 2,537 declarations; structured Chrome searches selected one app launcher and found two HTTPS handler entries. The opt-in Chrome check dispatched the selected launcher through `DeviceTools`; Android activity and window state then identified Chrome as the resumed and focused app. This verifies that device's discovery and catalog-to-launch path, without claiming an end-to-end spoken-request, model, OAuth, TV or Wear OS test.

Before the rename, on 23 October 2026, the updated debug app and all nine instrumentation checks passed on the same phone. Selecting the previous app name in the system **Assist app** picker persisted after reopening Settings. Holding Home opened `VoiceActivity`, focused its window, displayed the listening state and bound the installed Google speech recognition service. The microphone test was then closed. This verifies default-assistant selection and microphone startup on that device, without claiming a completed spoken request or new OAuth verification. Both variants also passed unit tests, complexity checks, lint and debug/release builds; inspection of both shrunk release APKs confirmed the voice activity remained present and the obsolete assistant services were absent.

After the rename on 23 October 2026, both variants passed all 34 JVM tests each, `complexityCheck`, debug lint, debug/release builds and instrumentation APK builds. Inspection of both shrunk release APKs confirmed the `aho-gpt` label, `aho.gpt` / `aho.gpt.wear` IDs, all six manifest entry point classes in DEX, and the exported voice activity with `ASSIST` and `VOICE_COMMAND` intent filters. The renamed app has not been device-tested or OAuth-tested.

For the native XML artwork update on 23 October 2026, both variants passed 54 JVM tests each, `complexityCheck`, debug lint and debug/release builds. The artwork uses shorter paths within lint's existing performance limit. Inspection of both shrunk release APKs confirmed the launcher icon, vector artwork and TV banner resources were retained, with no PNG resources packaged. Launcher appearance and the updated home-screen logo have not been verified on a device.

On 24 October 2026, the updated handheld debug app's opt-in shortcut check passed on the connected Huawei PRA-LX1 (Android 8/API 26). The local scan returned 38 WebAPK launcher declarations and no YouTube app launcher; the native YouTube package was disabled. Through `DeviceTools`, Home exposed a matching clickable website shortcut, its click returned `UI_ACTION_ACCEPTED`, the active package changed to an external app, and a fresh screen read contained the requested `youtube.com` marker. The enabled accessibility service had to be reconnected after the instrumentation process restart. All nine standard instrumentation checks also passed. Both variants passed 54 JVM tests each, `complexityCheck`, debug lint, debug/release builds with release shrinking and instrumentation APK builds. This verifies the shortcut tool path, without claiming an end-to-end spoken/model test or new OAuth verification.

For the Aho! GPT interface update on 24 October 2026, both variants passed 54 JVM tests each, `complexityCheck`, debug lint, debug/release builds with shrinking, and instrumentation APK builds. The shrunk release APKs retained the `Aho! GPT` application label and existing package IDs. The handheld debug APK was installed in place on the Huawei PRA-LX1 (Android 8/API 26). Checks covered the white home logo, enlarged icon-only microphone, account/model/tool summary, About-first settings, compact Languages rows, STT/TTS configuration dialogs, installed-voice picker, Android STT/TTS settings redirects, and the model-row shortcut scrolling to model selection. The default TTS preview returned its completion callback. Turning STT off blocked listening with an explanatory message; its original enabled preference was restored. The phone check also caught and resolved an Android 8 themed-drawable inflation failure. This update was not OAuth-tested or tested with a live model request, successful STT acquisition, disabled/missing providers, or physical TV/Wear OS devices.

The subsequent settings-style refinement was compared against screenshots of the installed ViBRo Navigator app on the same phone. The updated About links, active speech switches, regular headings, control sizes, section spacing, and account/device action-help rows were visually checked; the new model information button opened its expected local help dialog. Both variants again passed 54 JVM tests each, complexity checks, debug lint and debug/release builds with shrinking. No account, model, permission or speech preference was changed during this comparison.

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
