# Light Assistant specification

## Product

Provide a lightweight Android, Android TV and Wear OS AI harness. Use Java, native Android APIs, no runtime dependencies, and the reference project's focused architecture and zero-violation complexity checks. Use vibe-navigator's black background, white iconography, rounded dark surfaces and outlined controls throughout the native UI. The main screen contains only a tappable app logo, which opens About & settings, and a large microphone/Speak button. Keep account, model, usage, permission, assistant, tool-rescan and sign-out controls on the separate About & settings page, alongside app information and help. Answers use a transient voice panel and overlay; no chat screen. Provide visible D-pad focus and layouts sized for phone, TV and watch screens.

## Voice and continuity

Use platform STT and TTS. Prefer on-device STT if available; clearly report missing providers. User invocation starts a microphone session. After answer playback, listen for up to eight seconds while the voice panel is foreground. A follow-up beginning in that interval retains context; otherwise start a fresh conversation. Keep history in memory, bound its size, and clear it after expiry, failure or lifecycle destruction. No wake-word or persistent background listener.

Provide a Speech languages entry in About & settings with independently saved STT and TTS language choices, each defaulting to the device language. Pass the chosen BCP 47 locale to recognition and playback. Show the requested recognition language by name and tag throughout acquisition, including follow-ups. Query ready STT languages from the same recognizer used for listening on Android 13 and later; query available TTS languages from the default playback engine. Capability queries must not open the microphone or download models. If a provider cannot report support, clearly label device-locale choices as unverified. Do not advertise downloadable STT languages as ready. Report missing or unsupported language data, preserve a visible answer on playback failure, and do not open follow-up listening after failed playback. TTS language changes the playback voice, not answer translation.

## OpenAI

Use only OpenAI models discovered from the authorized account. Authenticate using the documented Sign in with ChatGPT public-client flow, PKCE, state, nonce, signature validation, protected token storage and serialized rotating refresh. Request plan usage explicitly, enforce granted scope, and keep API keys out of the app. Provide the requested integrated WebView where supported, with a same-device system-browser fallback. Wear OS has no WebView; current sign-in requires a browser on the watch. Use public Responses with non-persistent streaming requests and explicit history. Only completed responses may trigger tools.

## Device capabilities

Scan all visible installed applications for accessible exported manifest intent filters and expose a searchable local catalog to the model. Support explicit activities, services and broadcasts, subject to Android permissions and execution restrictions. Do not claim extras' semantics or inaccessible capabilities can be inferred. Catalog metadata remains local until a search tool returns selected entries. Rescan explicitly and at process start.

Prioritize discovered app launchers in catalog search results and identify them explicitly. Opening an app uses its discovered exported activity, MAIN action, phone or TV launcher category and launcher task flags. Invoke the launcher directly without first hiding the voice activity or navigating Home. Return dispatch failures without claiming verified completion; Android background-launch restrictions still apply.

Supplement manifest parsing with public package-resolver queries for phone/TV launchers and general HTTP/HTTPS handlers, including split APK activities. Do not claim complete discovery of other split-manifest filters. Expose two device-tool functions, or four when accessibility is available, with at most 25 selected catalog entries per search page; keep the full inventory local.

When the app's accessibility service is enabled, include screen-reading and requested UI action tools. Exclude password nodes; treat other screen text and app metadata as untrusted input. No passive event retention or key interception. Mutating tools carry out the active spoken request and return actual dispatch results; unclear targets must be clarified by voice.

## Form factors

One Android application module with shared sources and `handheld` (phone/TV) and `wear` flavors. Native layouts must work with touch, D-pad and round screens. Expose `dev.lightassistant.android.ui.VoiceActivity` and voice-command intents for a separate TV button mapper. Optional assistant-role integration may be offered. Answer overlays can use an enabled accessibility service or a separately granted overlay permission; voice works without either.

## Validation

Use PMD with the inherited reference limits, plain JVM unit tests for protocol/session safety, lint for both variants, and debug/release builds with release shrinking. Explicitly distinguish static/build verification from physical-device integration testing. Maintain zero runtime dependencies.
