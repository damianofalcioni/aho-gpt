<p align="center">
  <img src="artwork/Aho-GPT.png" alt="Aho! GPT logo" width="160" height="160">
</p>

# Aho! GPT

A lightweight voice assistant for Android phones, tablets, TV and Wear OS. Connect ChatGPT or a custom AI server to ask questions and act across apps by speaking.

## Main features

- **Voice conversations:** spoken answers and automatic follow-up listening, with temporary context and no saved chat history.
- **App actions:** open apps and use their available actions. Optional accessibility access lets the assistant read screen text and operate visible controls.
- **Your choice of tools:** enable or disable individual device functions and app actions. Android permissions and app restrictions still apply.
- **AI providers:** choose ChatGPT or an OpenAI-compatible server, such as llama.cpp, and select a detected model.
- **Language and speech settings:** English or Italian app text, recognition language, installed voices and speech tests. Choose custom speech models and voices, or direct voice through Realtime.
- **Device integration:** support for the system assistant shortcut, external TV button mappers and a voice panel over other apps where permitted.
- **Permissions and logs:** direct Android access settings and optional logs that exclude credentials, requests, answers and screen text.

The app uses a simple native interface, with no chat screen, wake word or persistent background listening. Answers are guided to be concise and follow the language and tone of your latest request.

## Privacy and availability

Requests, temporary context and selected tool results go to your selected AI provider. With screen access enabled, visible text may also be sent, excluding password fields. HTTP custom servers receive data without encryption. Speech providers may process audio or answer text online.

Models and usage limits depend on your provider. Custom servers need model listing and either Chat Completions or Realtime; tools need a compatible model. Device actions depend on available apps, permissions and accessible controls; completion is not guaranteed. This is an unofficial app.

[Homepage](https://damianofalcioni.github.io/aho-gpt/) · [Changelog](docs/CHANGELOG/index.html) · [Privacy policy](docs/privacy-policy/index.html) · [Terms of service](docs/terms-of-service/index.html)

Technical details, limitations and build instructions are in [SPECIFICATION.md](SPECIFICATION.md).

Licensed under the [MIT License](LICENSE).
