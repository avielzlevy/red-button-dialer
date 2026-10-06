# Red Button

A minimal native Android app (Kotlin + Jetpack Compose):

- **Logo at the top**: tap it to open a modal and set the phone number. Closing the modal (Done, back, or tapping outside) saves it. Default: `0500000000`.
- **Red button in the middle**: asks for the call permission the first time, then calls the saved number. If permission is denied, it opens the dialer with the number filled in.

## Getting the APK

Every push to `main` builds the app in GitHub Actions and publishes it under **Releases** as `red-button-<build>.apk`. Open the latest release on your phone, download the APK and install it (allow installs from your browser when Android asks).

Builds are signed with a fixed debug key (`keystore/`), so a new build installs over the old one and keeps your saved number. That key is for testing only; a Play Store release needs its own private key.

## Changing the logo

Replace `app/src/main/res/drawable/ic_logo.xml` (or add `ic_logo.png` in its place and delete the XML).
