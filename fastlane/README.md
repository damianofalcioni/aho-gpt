# Store listing

metadata/android/ is the shared Fastlane Supply listing for Google Play and F-Droid. It contains English and Italian descriptions and English release notes. No Ruby or Fastlane executable is needed.

Images are in metadata/android/en-US/images/: logo.png holds the original artwork, icon.png is the 512×512 store icon, and phoneScreenshots/ is ready for real phone screenshots (2–8). Before publishing, add those screenshots and an opaque 1024×500 featureGraphic.jpg or featureGraphic.png. Do not copy Vibro artwork. The local verifier and F-Droid readiness job refuse incomplete listings.

logo-readme.png is the 160×160 rounded README variant, with transparent corners for light and dark backgrounds.

Run ./gradlew :app:preparePlayListing to validate and generate GPP resources. The Google Play Listing workflow does the same and saves a review artifact. Its publish option uploads both Play package listings and permits Google review submission.

For local uploads, set PLAY_SERVICE_ACCOUNT_JSON to a credential file path outside Git, or ANDROID_PUBLISHER_CREDENTIALS to JSON contents. Run ./gradlew -PplayPublishing -PplaySubmitForReview=true :app:publishPlayListing. This uploads listing text and images without building binaries. Keep signing and publishing credentials separate.

Text limits: title 30 characters, short description 80, full description 4000, release notes 500. Icon: 512×512 PNG, at most 1 MiB. Screenshots: 320–3840 pixels per side, longer side at most twice the shorter. Localized images fall back to en-US.

See [release details](../SPECIFICATION.md#release-distribution).
