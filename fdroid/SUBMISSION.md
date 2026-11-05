# F-Droid submission

Stable GitHub releases call fdroid-ready.yml and fdroid-submit.yml for handheld and Wear independently. Each app gets its own fdroiddata branch and merge request. Beta tags are excluded.

Create a public GitLab fdroiddata fork with CI enabled. Set GITLAB_NAMESPACE and GITLAB_TOKEN (api and write_repository access) as GitHub Actions secrets. The workflow renders an immutable release commit and the verified APK signer, runs fdroid rewritemeta and lint, pushes the metadata, waits for successful fork CI, then creates or reuses a merge request. Closed requests are not duplicated.

Signing secrets and public APK attachments are required. Repository metadata deliberately uses a signer placeholder; the submission job replaces it from the verified APK. It checks the public APK package, version and certificate before submitting.

Both stores use aho.gpt for handheld and aho.gpt.wear for Wear. Updates between stores require matching app signing certificates and compatible version codes. Keep the Play app signing certificate consistent with the published APK certificate; a Play upload key alone does not establish that match.

Retry Submit F-Droid Metadata manually with the published stable tag and device. The default fork branches are aho.gpt-handheld and aho.gpt-wear. Submission does not guarantee acceptance or publication. F-Droid must review the app, classify any anti-features and reproduce its APK.
