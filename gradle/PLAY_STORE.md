# Google Play publishing

See [release distribution](../SPECIFICATION.md#release-distribution) for events, packages, credentials and commands. The workflow matches Vibro: signed release artifacts, stable production uploads, beta testing uploads, separate listing publication and stable F-Droid submission.

The Play Console apps aho.gpt and aho.gpt.wear need their initial setup, first upload, store listing, policy declarations and release permissions. PLAY_SERVICE_ACCOUNT_JSON is the complete JSON key in GitHub Actions, but a file path for local Gradle publishing. Do not reuse Vibro credentials without granting access to these apps.

The optional Gradle publisher defaults to an internal-track draft. Run ./gradlew -PplayPublishing :app:publishGplayHandheldReleaseBundle or :app:publishGplayWearReleaseBundle with the signing environment configured. It never increments versionCode. Use an unused, increasing code for every new Play upload. Promote an existing beta in Play Console instead of reuploading its code.
