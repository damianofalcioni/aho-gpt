# Light Assistant specification

## Product

Provide a lightweight Android, Android TV and Wear OS AI harness. Use Java, native Android APIs, no runtime dependencies, and the reference project's focused architecture and zero-violation complexity checks. The UI is a Speak button and transient answer overlay with setup controls; no chat screen.

## Voice and continuity

Use platform STT and TTS. Prefer on-device STT if available; clearly report missing providers. User invocation starts a microphone session. After answer playback, listen for up to eight seconds while the voice panel is foreground. A follow-up beginning in that interval retains context; otherwise start a fresh conversation. Keep history in memory, bound its size, and clear it after expiry, failure or lifecycle destruction. No wake-word or persistent background listener.

## OpenAI

Use only OpenAI models discovered from the authorized account. Authenticate using the documented Sign in with ChatGPT public-client flow, PKCE, state, nonce, signature validation, protected token storage and serialized rotating refresh. Request plan usage explicitly, enforce granted scope, and keep API keys out of the app. Provide the requested integrated WebView where supported, with a same-device system-browser fallback. Wear OS has no WebView; current sign-in requires a browser on the watch. Use public Responses with non-persistent streaming requests and explicit history. Only completed responses may trigger tools.

## Device capabilities

Scan all visible installed applications for accessible exported manifest intent filters and expose a searchable local catalog to the model. Support explicit activities, services and broadcasts, subject to Android permissions and execution restrictions. Do not claim extras' semantics or inaccessible capabilities can be inferred. Catalog metadata remains local until a search tool returns selected entries. Rescan explicitly and at process start.

When the app's accessibility service is enabled, include screen-reading and requested UI action tools. Exclude password nodes; treat other screen text and app metadata as untrusted input. No passive event retention or key interception. Mutating tools carry out the active spoken request and return actual dispatch results; unclear targets must be clarified by voice.

## Form factors

One Android application module with shared sources and `handheld` (phone/TV) and `wear` flavors. Native layouts must work with touch, D-pad and round screens. Expose `dev.lightassistant.android.ui.VoiceActivity` and voice-command intents for a separate TV button mapper. Optional assistant-role integration may be offered. Answer overlays can use an enabled accessibility service or a separately granted overlay permission; voice works without either.

## Validation

Use PMD with the inherited reference limits, plain JVM unit tests for protocol/session safety, lint for both variants, and debug/release builds with release shrinking. Explicitly distinguish static/build verification from physical-device integration testing. Maintain zero runtime dependencies.
