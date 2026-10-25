# Aho! GPT

A voice assistant for Android phones, tablets, TV and Wear OS. Tap the microphone, speak a request and hear an answer. Conversations are temporary; no chat history is saved.

## Get started

1. Install the `handheld` APK on Android 6 or newer, or the `wear` APK on Wear OS (Android 8 or newer).
2. Open the app and tap the logo for **About & settings**.
3. Choose **Continue with ChatGPT**, allow plan usage, then **Choose model**.
4. Return to the home screen, tap the microphone and allow microphone access.

Finish sign-in on the same device. If the sign-in page does not work, choose **Open in browser**. Wear OS requires a browser installed on the watch; signing in through your phone is not supported.

The home screen shows your account and model names. Tap either to change its settings. To connect a different account, clear the app’s data in Android settings and set it up again.

## Speaking and listening

Aho! GPT is guided to give short, clear answers in simple sentences. Ask for more detail when you need it. It follows the language and style of your latest request: formal, casual or using natural slang to match you. You can also ask for a different language or tone.

After each answer, listening starts automatically for up to eight seconds while the voice panel is foreground. Just speak to reply; you do not need to press the microphone again. This also works with voice playback turned off. After silence, the panel shows **Finished** and closes after three seconds. Errors also close after three seconds. Tap the microphone before it closes if you want to start again.

Tap **×** or use Android Back to close and cancel pending work. Actions already sent to another app cannot be undone by closing the panel. There is no wake word or background listening.

In **About & settings → Languages**, use each gear to choose a language and test it:

- **TTS** reads answers aloud. You can also choose an installed voice. Turning it off displays answers as text; you can still reply by voice.
- **STT** recognizes your speech. Turning it off stops microphone use.

If speech is unavailable, use the gear to check Android speech settings or install language data. Choosing a playback language does not translate answers.

## Apps and shortcuts

You can ask Aho! GPT to open apps and use available app actions. Enable **Screen access & actions** if you want it to read the screen and operate visible controls. Android permissions and each app determine what is possible; some actions may fail.

For tasks inside an app, Aho! GPT is guided to try an app action first, then open the app directly before using screen controls for the remaining steps.

Use **Refresh app actions** after installing or updating apps. Home-screen website shortcuts require screen access and a visible, accessible icon.

In **Device & permissions → Tools**, use the gear to search recognized device tools and app actions. Each entry has an enable switch and a **⋮** button showing its description, intent declarations or parameters, and limitations. The main **Tools** switch turns all tools off while keeping your individual choices. Disabled items are hidden from model tools and discovery results and cannot be dispatched by the assistant. Changing choices discards pending model responses and starts the next request with fresh context. These choices do not revoke Android permissions or undo actions already sent.

Enable **Allow voice panel over other apps** to keep the panel and close button visible while tools run in other apps. Screen access also provides this overlay. Without either permission, Android hides the panel when another app opens. Tool results return to the model before it continues. There is no app-imposed tool-round, call-count or turn-duration limit; account limits and Android restrictions still apply.

Where supported, **Default assistant settings** lets you select Aho! GPT for your device’s assistant shortcut. For a TV remote, configure a separate button mapper with:

```text
Package: aho.gpt (release) or aho.gpt.debug (debug)
Activity: aho.gpt.android.ui.VoiceActivity
```

## Logs

In **About & settings → Advanced**, turn on **Logs** to save diagnostics in `Download/AhoGPT/logs`. Allow storage access if asked; denying it keeps logging off. Use its gear to open or share files, or delete the app’s logs. Deletion asks for confirmation.

Logging is off by default. Crashes may still create a log when storage is accessible. Logs include device details, request timing, speech errors and tool status, but no credentials, spoken requests, answers or screen text. Files survive uninstall. **Advanced → Use external storage** saves to a supported SD card instead. Missing storage or write failures turn logging off; enable it again after fixing access.

## Privacy and account limits

Requests, temporary context and selected app-action results are sent to OpenAI. With screen access enabled, visible text may also be sent, excluding password fields. Speech providers may process audio or answer text online. Provider data policies apply.

Models and usage limits depend on your ChatGPT account. Review them with **Manage ChatGPT usage**. This is an unofficial app.

Build requirements and validation commands are in [SPECIFICATION.md](SPECIFICATION.md#validation).
