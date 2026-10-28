<p align="center">
  <img src="artwork/Aho-GPT.png" alt="Aho! GPT logo" width="160" height="160">
</p>

# Aho! GPT

A lightweight voice assistant for Android phones, tablets, TV and Wear OS. It connects OpenAI models to your device so you can ask questions and act across apps by speaking.

## Main features

- **Voice conversations:** spoken answers and automatic follow-up listening, with temporary context and no saved chat history.
- **App actions:** open apps and use their available actions. Optional accessibility access lets the assistant read screen text and operate visible controls.
- **Your choice of tools:** enable or disable individual device functions and app actions. Android permissions and app restrictions still apply.
- **Speech options:** separate recognition and playback languages, installed voices and optional spoken answers.
- **Device integration:** support for the system assistant shortcut, external TV button mappers and a voice panel over other apps where permitted.
- **Diagnostics:** local status checks and optional logs that exclude credentials, requests, answers and screen text.

The app uses a simple native interface, with no chat screen, wake word or persistent background listening. Answers are guided to be concise and follow the language and tone of your latest request.

## Privacy and availability

Requests, temporary context and selected tool results go to OpenAI. With screen access enabled, visible text may also be sent, excluding password fields. Speech providers may process audio or answer text online.

Models and usage limits depend on your authorized ChatGPT account. Device actions depend on available apps, permissions and accessible controls; completion is not guaranteed. This is an unofficial app.

Technical details, limitations and build instructions are in [SPECIFICATION.md](SPECIFICATION.md).

Licensed under the [MIT License](LICENSE).
