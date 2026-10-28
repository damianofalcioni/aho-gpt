# Project guide

Read `SPECIFICATION.md` before changing product behavior. Use focused Java components, native platform UI, and no runtime dependencies. This project follows the architecture and maintainability baseline of the user's `vibe-navigator` repository without importing navigation-specific features or libraries.

- Keep Android framework APIs and lifecycle wiring under `aho.gpt.android`; keep model protocol and session policy independently JVM-testable.
- Keep all UI labels in `strings.xml`. Do not add a chat interface or a persistent conversation store.
- Always use short, plain descriptions in UI text, documentation and replies. Include only essential details.
- Keep README short and focused on the app's idea and main features. Avoid setup steps and detailed usage instructions; keep technical details in SPECIFICATION.md.
- Use only the documented public OpenAI OAuth/Responses path. Never embed a client secret, API key, stolen client ID or private ChatGPT backend endpoint.
- Keep issued OAuth registrations, host IDs, credentials and account identity consistent. Never log tokens, authorization URLs or screen contents.
- Keep the TV mapper entry point exported. Do not intercept microphone or remote keys in the accessibility service.
- Never execute partial streamed tool calls. Honor the scope of the spoken request, cancellation, platform permissions and inaccessible components.
- Fix PMD findings by simplifying code; do not weaken limits. Use narrowly scoped deprecation suppressions only for required compatibility callbacks or pre-minimum API alternatives.
- After changes run relevant unit tests, `complexityCheck`, both variant lints, and both builds. Test release shrinking when manifest entry points or reflection contracts change.
- Keep README and SPECIFICATION aligned with actual behavior and limitations. Do not call a build device-tested or OAuth-tested without that evidence.
