# Light Assistant

A small, voice-first Android AI harness in pure Java. One Speak button starts a request; answers appear in a transient panel and use Android text-to-speech. There is no chat screen, transcript database, bundled speech model, or runtime library dependency.

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

1. Open Light Assistant and choose **Continue with ChatGPT**. Approve ChatGPT plan usage, then choose an account-specific OpenAI model. No API key, client secret, private ChatGPT endpoint, or alternative model provider is used.
2. Grant the microphone permission when first speaking. Android's installed `SpeechRecognizer` and `TextToSpeech` handle speech; on-device recognition is preferred when available. Otherwise the system recognizer may use its provider's network service.
3. Optionally enable the app's accessibility service after reviewing the disclosure. It adds visible-screen reading, click, text, scroll, back, home, and recents tools. It never intercepts keys or records screen events in the background.
4. Optionally grant **Allow answers over other apps**. The enabled accessibility service can also display an answer overlay. Without either permission, answers remain in the voice activity and are still spoken.

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

The optional platform assistant service is provided for devices exposing default-assistant settings. TV key routing is handled by your external mapper.

## Conversation continuity

The microphone opens only after user invocation, or briefly after a spoken answer while the voice panel remains visible. After TTS finishes, an **eight-second follow-up window** retains the context. Starting speech inside that window continues the exchange; silence, Close, an error, app process death, or activity recreation clears it. Initial recognition has a fifteen-second timeout; speech in progress has a twenty-second maximum. There is no always-listening wake word or background microphone service.

History is held only in memory and replayed to Responses with `store:false` and `stream:true`. It is not stored as a ChatGPT conversation. The next new request clears expired history. Complete tool exchanges remain together; a size bound starts a fresh session on the next user turn. Ordinary OpenAI service data policies still apply to transmitted requests.

## Installed apps as tools

On process start and explicit rescan, the app enumerates installed packages and reads their compiled manifests through public Android resource APIs. It catalogs accessible exported **activity, activity-alias, service, and broadcast-receiver intent filters**, including custom actions. The model searches the local catalog through a function, then invokes an exact discovered capability ID. Only selected catalog results go to OpenAI, rather than the entire inventory on every turn.

Intent and accessibility actions run within the active voice request. Ambiguous actions are clarified through spoken follow-up. Dispatch failures are returned to the model; a dispatched intent is never reported as verified completion. No action runs from a partial or failed model response, and tool rounds are bounded. Close cancels pending inference.

Android does not publish a complete semantic schema for app extras, hidden/non-exported capabilities, signature-protected components, dynamic receivers, or every deep-link constraint. Discovery cannot make those callable. The scanner reports unreadable manifests, cannot bypass platform access controls, and does not invent extra semantics. String extras are supported; file grants, arbitrary parcelables, nested intents, and file/content URIs are excluded. Background service restrictions still apply. Rescan after installing or updating apps; stale capability IDs fail closed.

`QUERY_ALL_PACKAGES` is necessary for the requested broad catalog. Store distribution requires an eligibility review for broad package visibility and accessibility functionality; this repository does not claim Play approval.

## Security and architecture

- `core/`: monotonic session policy and memory-only conversation state.
- `openai/`: Responses/SSE parsing, function schemas, bounded tool loop; plain JVM tests.
- `android/auth/`: OAuth PKCE/state/nonce, signed ID-token verification, serialized refresh and Android Keystore AES-GCM atomic credential storage. App OAuth tokens are excluded from backups and never passed to the WebView.
- `android/tools/`: manifest inventory, searchable capability catalog and explicit intent dispatch.
- `android/access/`: opt-in UI inspection and actions, with password nodes excluded and no passive event retention.
- `android/speech/`: lifecycle-owned platform STT/TTS.
- `android/ui/`, `android/assist/`: native buttons, transient answer surfaces, optional assistant service.

App and screen text are treated as untrusted data in model instructions. Tool execution is tied to the active voice request and Android permission boundaries. Password filtering does not make other screen content non-sensitive: enabling screen tools permits requested visible text to be sent to OpenAI.

## Verification

```powershell
.\gradlew.bat complexityCheck testHandheldDebugUnitTest testWearDebugUnitTest lintHandheldDebug lintWearDebug assembleHandheldDebug assembleWearDebug assembleHandheldRelease assembleWearRelease
```

PMD inherits the reference project's zero-violation baseline: cognitive complexity below 10, method cyclomatic complexity below 8, class complexity below 40, NPath below 80, method NCSS below 60, and class NCSS below 400. No runtime dependency is added by these checks. CI runs the same validation.

Tests cover follow-up expiry, context reset, tool result replay, tool loop bounds, namespace isolation, truncated SSE and late inference failures. Real-device OAuth, speech providers, accessibility interactions, TV mapper behavior, watch layouts and overlays require device testing. No live inference or account access is performed by the build.

## Official implementation references

- [OpenAI registration and sign-in](https://developers.openai.com/siwc/token-sharing-open-source/sign-in)
- [OpenAI models and inference](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference)
- [OpenAI preview request limitations](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations)
- [OpenAI account sessions and token refresh](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions)
- [Android WebView availability, including Wear OS](https://developer.android.com/develop/ui/views/layout/webapps/managing-webview)
- [Android accessibility services](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Android package visibility](https://developer.android.com/training/package-visibility)
