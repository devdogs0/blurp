# Demo video for the Accessibility declaration

Play's Accessibility API declaration needs a link to a short video. Google's rules (https://support.google.com/googleplay/android-developer/answer/10964491) say it must show, in order: the app launching, the full disclosure, the consent path up to the Accessibility switch, the decline path and how the disclosure comes back, and the feature itself, with subtitles where the screen doesn't explain it.

## Before recording

- Install Blurp from the **Play internal-testing link** on the Galaxy Z Fold8, not from devdogs.ai. A downloaded APK gets Android's extra "App was denied access" step; a Play install doesn't, and the video should show what Play users see.
- Film the phone with a second camera, at least for the folds. Screen recordings show black during One UI's fold animation, so the frost never appears in them.
- Clear notifications and personal content off the screens. Record on the inner screen.

## Shot list (about a minute)

1. **Launch (0–5 s).** Home screen; tap the Blurp icon.
2. **Disclosure (5–15 s).** Hold on the first screen until every line can be read. If any of it is off screen, scroll slowly.
3. **Decline (15–25 s).** Leave the box unticked: Continue stays off. Press Back and Blurp closes. Open Blurp again: the same disclosure is back. Subtitle: “Without consent, Blurp does nothing and asks again next time.”
4. **Consent (25–45 s).** Tick “I understand and allow Blurp to use the Accessibility service”, tap Continue, tap Open Settings, switch Blurp on, tap Allow on Android’s dialog, and go back to Blurp.
5. **The feature (45–60 s).** Close the phone halfway and open it: the half that moves frosts, then clears. Subtitle: “Blurp only draws this frost. It reads nothing on the screen.” Lock the phone, wake it and fold again: the frost covers the lock screen too. Subtitle: “Only the Accessibility service can draw over the lock screen.”

Upload it to YouTube as **Unlisted** and paste the link into the declaration. Record it again whenever the disclosure or the setup steps change.
