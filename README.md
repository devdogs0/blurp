# Blurp

Blurp frosts the half of a Samsung Galaxy Z Fold's screen that is swinging while you fold or unfold the phone: on the home screen, in other apps and on the lock screen. When the phone opens, the inner screen lights up frosted and clears as it settles; when it closes, the cover screen does the same.

Free, with no ads, no internet permission and nothing collected.

## Phones

- Galaxy Z Fold7, Z Fold8 and Z Fold8 Ultra, on Android 17 (One UI 9).
- Only the Fold8 has been tested on a real phone. The Fold7 and Ultra are covered by unit tests on their screen sizes, so reports from those phones are welcome.

## How it works

- An accessibility service draws overlay windows above everything, the lock screen included, and blurs what is under them with One UI's window blur. It cannot read what is on the screen (`canRetrieveWindowContent="false"`).
- The hinge angle sensor drives the frost on the inner screen while it moves. When the screens switch, the new screen lights up frosted and clears on a timer.
- `HingeController.kt` runs the fold phases and the reveal timing; `DuoFrostModel.kt` sets the frost's strength and shape; `FrostOverlay.kt` owns the windows.

One UI 9 does four things the code works around:

1. It stops drawing frames for background apps after about 4 seconds, so `FrameClock` runs on its own thread.
2. Samsung's process freezer freezes Blurp a few minutes after it was last opened, so a 1 px invisible anchor window keeps it running.
3. During the fold animation it hides overlays and fades them back in after the new screen lights. Blurp's windows opt out with `SEM_EXTENSION_FLAG2_SKIP_ASYNC_ROTATION`.
4. The new screen lights 0.14 to 0.36 s after the switch, so the reveal holds for 0.24 s (`RevealTimingTest`).

Screen recordings show black for about 0.6 s during One UI's fold animation while the real screen shows the app and the frost. Judge the frost by eye on the phone, not from a recording.

## Build

Use Android Studio, or the command line with JDK 17 or 21 (this Gradle version doesn't run on JDK 26):

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open Blurp and follow its setup; it sends you to Settings › Accessibility to switch Blurp on.

For a release build, copy `keystore.properties.example` to `keystore.properties` and fill it in. Never commit it. Without it, release builds are signed with the debug key.

## Tests

- Unit tests: `./gradlew :app:testDebugUnitTest`
- Device checks, with a Fold on adb and Blurp's service on: `tools/check-fold-fade.sh`, `tools/check-background-frost.sh`, `tools/check-not-frozen.sh`
- Debug builds hold the inner frost at a hinge angle with `adb shell am broadcast -a ai.devdogs.blurp.PREVIEW --ef angle 90`. Send it without the angle to go back to the hinge.

## Contributing

Issues and pull requests are welcome.

- Say which phone and One UI version you tested on. Changes to the fold timing or the frost's look need a check on a real Fold.
- Keep the unit tests passing and add a test for the behaviour you change.
- No network access, ads, analytics or tracking. The manifest strips the internet permission on purpose.

## Privacy

Blurp collects nothing. See [docs/release/privacy-policy.html](docs/release/privacy-policy.html).

## Licence

Blurp is free software under the GNU General Public License v3.0; see [LICENSE](LICENSE). The licence covers the code, not the Blurp name or icon: forks you publish need their own name and icon.

Copyright © 2026 DEVDOGS
