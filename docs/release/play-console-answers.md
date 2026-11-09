# Play Console answers

Package: `ai.devdogs.blurp`
Name: Blurp
Store title: Blurp: Fold Frost
Account: organization developer account (no 12-tester closed-test wait)
Privacy policy URL: https://devdogs0.github.io/blurp/release/privacy-policy.html (GitHub Pages, from `docs/` on `main`)
Contact email: support@devdogs.ai
Website: https://github.com/devdogs0/blurp

## Accessibility declaration

Play Console → App content → Accessibility API. Google's rules: https://support.google.com/googleplay/android-developer/answer/10964491

- Is this an accessibility tool? **No.** `isAccessibilityTool` is left unset, so it is false: Blurp isn't made to help people with disabilities.
- Why does the app use the Accessibility API? **App functionality** only.
- Do you collect and/or share personal or sensitive data using the accessibility capabilities? **No.**
- Video link: **required.** Record it on an internal-testing install with `demo-video-script.md`, upload it to YouTube as Unlisted and paste the link. Send the form again whenever the use changes.

What the service does:

- It draws a see-through frost (an accessibility overlay, `TYPE_ACCESSIBILITY_OVERLAY`) over the half of the screen that moves while the phone folds. Nothing else.
- It reads nothing: `canRetrieveWindowContent` is false and `onAccessibilityEvent` ignores every event. It never acts for the user: no gestures, clicks or global actions.
- Its windows never take a touch (`FLAG_NOT_TOUCHABLE`, `FLAG_NOT_FOCUSABLE`). One of them stays on screen at 1 px and fully transparent, never drawn, because One UI's process freezer otherwise freezes the service a few minutes after Blurp was last opened, and the frost would then arrive after the fold.
- It draws nothing until the in-app disclosure is accepted, even when someone switches it on straight from Settings (`OnboardingFlow.frostShown`).
- It has no internet permission, so nothing can leave the phone.

Why no narrower API can do it (Google asks for one wherever it would work):

- **Display over other apps** (`SYSTEM_ALERT_WINDOW`, `TYPE_APPLICATION_OVERLAY`): those windows sit below the lock screen and the status bar, so the frost would be missing from the lock screen, one of the places it is for.
- **An activity shown over the lock screen** (`showWhenLocked`): it becomes the app in front, so whatever the person is using is paused on every fold.
- An accessibility overlay is the one window an app can draw above the lock screen and every app without taking over the screen or its input.

In-app disclosure: shown alone on Blurp's first screen, before Blurp ever sends anyone to Settings. Continue stays off until the box is ticked. The text must match the app:

  Title: Blurp

  Body: Blurp frosts the half of your screen that moves while you fold, in every app and on the lock screen. Only Android's Accessibility service lets an app lay a layer over the lock screen and every other app, so Blurp needs it.

  Blurp uses it only to draw that frost. It doesn't read what's on your screen or what you type, and it doesn't collect or share any data. It has no internet access.

  Checkbox: I understand and allow Blurp to use the Accessibility service

  Button: Continue

Declining: leaving without the tick (Back or Home) changes nothing, and the disclosure comes back the next time Blurp opens.

## Release order

1. **Internal testing first.** Build the bundle with `./gradlew :app:bundleRelease` (`app/build/outputs/bundle/release/app-release.aab`), upload it to Internal testing, add yourself as a tester and install Blurp from Play's link on the Fold8.
2. Check setup on that install: the disclosure, Open Settings, the switch, Allow, the first fold, "You're all set". Play installs aren't held back like a downloaded APK, so "App was denied access" must not appear.
3. Record the demo video on that install and complete the Accessibility declaration with its link.
4. Then release to production.

## Data safety

- Does your app collect or share any of the required user data types? **No.**
- Blurp has no internet permission (the manifest strips it), no ads, no analytics and no account. Nothing leaves the phone.

## Advertising ID

- Does your app use advertising ID? **No.** (The manifest strips `com.google.android.gms.permission.AD_ID`.)

## Ads, price and audience

- Contains ads: **No**
- Price: **Free**, no in-app purchases
- Target audience: **13+**
- Designed for children: **No**
- Content rating: complete the questionnaire; expected Everyone / PEGI 3 equivalent. No violence, sexual content, or user-generated content.

## Device catalog

1. Manifest already requires `android.hardware.sensor.hinge_angle`.
2. In Play Console → Device catalog: exclude all devices, then include every regional model of:
   - **Galaxy Z Fold7**: **SM-F966\***
   - **Galaxy Z Fold8**: **SM-F971\***
   - **Galaxy Z Fold8 Ultra**: **SM-F976\***
3. Do not include Flip, Fold6 or older, or tri-fold until tested.
4. Only the Fold8 has been tested on a real phone. The Fold7 and Ultra are covered by unit tests on their screen sizes; check them on Samsung Remote Test Lab before launch if one is free there.

## App signing

devdogs.ai/apps signs Blurp with the DEVDOGS release key (the one `keystore.properties` points to; certificate SHA-256 `35:DA:54:1B:3B:FB:C3:85:55:F0:79:87:F3:6C:27:8C:43:B5:48:E5:16:5D:AE:99:96:2C:44:5F:33:D1:77:3E`). Play has to sign with the same key, or a copy installed from the site can't take Play's updates, nor the other way round.

- For a new app, Play makes its own signing key. Before the first release reaches open testing or production, open Play app signing, choose **Change key** → **Provide a copy of your app signing key**, and follow Play's instructions to upload this key. After a release reaches those tracks, the key can only be upgraded, not swapped back.
- The same key signs the bundle as the upload key. Google recommends a separate upload key, but this key already signs the site's builds on this Mac, so a second one adds little.
- Back up the `.jks` and its password (the password is only in the git-ignored `keystore.properties`). Google can't reset a signing key you manage, and losing it ends updates.
- This repo never contains the `.jks`.
