# AirTouch — manual testing checklist

Run these on the target Android 12 device after installing the debug APK.
Mark each item pass/fail and note anything unexpected.

## Prerequisites
- [ ] Debug APK installs without error.
- [ ] App launches to the dashboard.

## Permissions & services
- [ ] Camera permission request appears and grants.
- [ ] Overlay permission opens the system screen and grants.
- [ ] Accessibility service can be enabled in Settings → Accessibility.
- [ ] Dashboard statuses turn green ("Granted" / "Enabled") after returning to the app.
- [ ] Notification permission grants on Android 13+ (skipped on Android 12).

## Camera detection
- [ ] Tapping **Start AirTouch** shows the ongoing notification.
- [ ] The live preview appears and updates.
- [ ] Showing an open palm moves the state to **Pointer mode**.
- [ ] Removing the hand switches to **Tracking lost** and the cursor freezes.

## Pointer (open palm)
- [ ] Cursor follows the palm smoothly.
- [ ] Moving the hand right moves the cursor right (mirroring feels natural).
- [ ] Holding the hand still does not make the cursor jitter noticeably.
- [ ] Hovering over an icon does **not** trigger a tap.

## Scroll (index finger)
- [ ] Raising only the index finger switches to **Scroll mode**.
- [ ] Moving the index finger up scrolls the page down.
- [ ] Moving the index finger down scrolls the page up.
- [ ] A stationary finger causes no continuous scrolling.
- [ ] Switching back to an open palm returns to **Pointer mode**.

## Pinch (click)
- [ ] Pinching thumb + index performs exactly one tap.
- [ ] Keeping the pinch held does **not** repeat the tap.
- [ ] Releasing and pinching again performs another single tap.
- [ ] Rapid pinches respect the click cooldown.
- [ ] A tap lands at the cursor position.

## Fist (pause)
- [ ] Closing a fist immediately pauses all actions.
- [ ] Cursor/state shows a paused indicator.
- [ ] No taps or scrolls happen while paused.
- [ ] Showing an open palm resumes pointer mode.

## Emergency stop & lifecycle
- [ ] **Emergency Stop** stops the service and removes the cursor.
- [ ] The notification's *Emergency Stop* action also stops it.
- [ ] After stopping, no gesture produces any input.
- [ ] Backgrounding the app and returning shows correct state.
- [ ] Screen rotation does not crash or misplace the cursor.

## Settings
- [ ] Each slider changes behaviour and survives app restart.
- [ ] **Reset to defaults** restores values.
- [ ] Disabling a gesture makes that gesture inert.

## Resource use
- [ ] The app stays responsive on a low-end device.
- [ ] No runaway battery drain while running (foreground notification present).
