# Battery: before and after sleep-between-folds

Device: Galaxy Z Fold8 SM-F971U1, plugged in, screen kept on (`stay_on_while_plugged_in=7`), phone flat and untouched for 10 minutes. App uid `u0a366`. `dumpsys batterystats` reports 0 time on battery because the phone was charging; CPU mAh is still attributed per uid.

## Baseline (`ec6587a`, pre-battery)

10 minutes starting 2026-11-08 03:02:28.

- UID `u0a366` estimated power: **0.0842 mAh** (all CPU, background).
- Hinge: `hinge_angle` **Wakeup**, sampling 20 ms, batching 0 ms.
- Gyro: `lsm6dsv_0 Gyroscope` Non-wakeup, sampling 20 ms, batching **0 ms**.
- Both stayed registered the whole window.

This phone has no non-wakeup `TYPE_HINGE_ANGLE`. `getDefaultSensor(TYPE_HINGE_ANGLE, false)` falls back to the wakeup hinge (`wakeUp=true`).

## After (current code)

10 minutes starting 2026-11-08 03:13:24.

- UID `u0a366` estimated power: **0.00826 mAh** (all CPU, background). About **10×** less than baseline at rest.
- Hinge: still wakeup 20 ms / batch 0 (no non-wakeup hinge exists).
- Gyro at rest: sampling 20 ms, batching **800 ms**.
- `ACTION_SCREEN_OFF` unregisters hinge and gyro; `ACTION_SCREEN_ON` re-registers and re-primes.

## Device check of the frost

Preview at 90° still frosts the swinging half on the home screen and clears fully at 180°. Sensors unregister on screen off and return on screen on.

Physical 3× fold/reopen/cover-open suite was not run here (cannot fold the device from adb). Gyro-batching reopen remains the risk to watch on a real fold.

## 2026-11-08 afternoon: One UI 9 background behavior, and the frame loop

Two One UI 9 behaviors stopped the frost whenever Blurp's own screen wasn't open. Both are fixed; both have a device check in `tools/`.

1. **Frames.** Samsung's `Choreographer.doFrame` skips `CALLBACK_ANIMATION` (every `postFrameCallback`) on an app's main thread once the app has been in the background 4 s with no visible window (logcat: `SYSPERF_ACTIVE_APP_BBA_ENABLE : stop animation in background states`). The frost's frames now come from a Choreographer on its own thread (`FrameClock`), which One UI never flags. Check: `tools/check-background-frost.sh`.
2. **Process freezing.** Samsung's Freecess froze the whole process about 4 minutes after Blurp was last opened (`FZ : ai.devdogs.blurp ... reason: Bg`), accessibility service or not; hinge events then waited up to 2.6 s, past the fold. Freecess skips apps with a window on screen (`FREEZE_SKIP_FLOATING_WINDOW`, result 36), so `FrostOverlay` keeps a 1 px window with alpha 0 on screen. SurfaceFlinger leaves alpha-0 layers out of composition (checked in `dumpsys SurfaceFlinger`'s HWC layer list), so it is never drawn. Check: `tools/check-not-frozen.sh`.

Battery fixes in the same pass: the controller had kept running frames about 2.2 s after every fold to ease an unused value (`motion`); the uncommitted edits that had re-laid out a window every frame and kept frames running 20 s after every open were dropped.

At rest, screen on, phone untouched for 2 minutes (14:41-14:43, process CPU from `/proc`): **1.6 ms of CPU per second** on the main thread (gyro batches every 0.8 s, about 1.5 wakeups/s), **0** wakeups on the vsync and render threads. Screen off: no sensors registered.
