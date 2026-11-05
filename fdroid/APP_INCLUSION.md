## App

Aho! GPT is a native voice assistant for Android, TV and Wear OS. It supports temporary voice follow-ups, app actions and optional accessibility tools. It has no runtime libraries or saved conversation store. This request covers only {{APP_ID}} ({{DEVICE}}).

Release: [{{RELEASE_REF}}](https://github.com/damianofalcioni/aho-gpt/releases/tag/{{RELEASE_REF}}), version {{VERSION_NAME}} ({{VERSION_CODE}}).

The MIT-licensed source and shared Fastlane listing are in the upstream repository. The recipe builds the matching fdroid device flavor. Binaries and AllowedAPKSigningKeys retain the upstream signing identity; F-Droid must reproduce the APK before publication.

Requests and selected tool results go to the configured AI provider. ChatGPT access depends on its service; a custom OpenAI-compatible provider can be selected. Speech providers may process audio online. Optional screen access can send visible text, excluding passwords. The app is not an offline assistant. Inclusion and any required anti-feature declarations remain subject to F-Droid review.

[Successful fork pipeline]({{PIPELINE_URL}}). This automated check does not establish physical-device behavior, live OAuth compatibility or F-Droid acceptance.
