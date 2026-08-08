# BlueFind

A minimal Android app (Kotlin) that helps you find a lost phone using
classic Bluetooth — no internet, no Google account, no third-party
service. It works between two phones that both have BlueFind installed
and are already paired via Android's Bluetooth settings.

## How it works

1. On the phone that might get lost, tap **"Make This Phone Findable."**
   This starts a foreground service that opens a Bluetooth RFCOMM server
   socket and waits.
2. On your other phone (or a friend's), tap **"Find My Other Phone,"**
   pick the paired device from the list, and BlueFind opens a Bluetooth
   connection to it and sends a one-byte "ring" command.
3. The listening phone receives the command and shows a full-screen red
   alert (even over the lock screen) that plays a looping alarm sound and
   vibrates, until you tap **Stop Ringing**.

This only works over Bluetooth's normal range (roughly 10 meters/30 feet
line-of-sight), so it's meant for "it's somewhere in this room / this
house" situations, not GPS-style long-distance tracking.

## Requirements

- Android Studio (Koala or newer recommended)
- Two Android phones running Android 8.0 (API 26) or later, already
  paired to each other in **Settings > Bluetooth**
- Bluetooth turned on on both phones

## Building

### Option A — Android Studio (easiest for installing on your phone)

1. Open this folder in Android Studio and let Gradle sync (it will
   generate the missing `gradlew` wrapper files automatically the first
   time).
2. Build and install on both phones (`Run ▶`, or `./gradlew installDebug`
   with a phone connected via ADB, one at a time).
3. Grant the Bluetooth permission prompt on first launch on each phone.

### Option B — GitHub Actions (build an APK without installing anything)

This repo includes `.github/workflows/build-apk.yml`. Push this project
to a GitHub repository (or fork/upload it) and the workflow will:

1. Set up JDK 17 and the Android SDK.
2. Install Gradle and run `gradle assembleDebug`.
3. Upload the resulting `app-debug.apk` as a downloadable build artifact
   on the Actions run's summary page.

You can also trigger it manually from the **Actions** tab using
"Run workflow" (it listens for `workflow_dispatch`). Once it finishes,
download the `BlueFind-debug-apk` artifact, unzip it, and copy the
`.apk` onto your phone (or use `adb install app-debug.apk`) — you'll
need to allow "install from unknown sources" since it isn't signed for
the Play Store.

## Project layout

```
app/src/main/java/com/example/bluefind/
├── MainActivity.kt              # Home screen: permissions + the two buttons
├── BlueFindProtocol.kt          # Shared UUID / command byte / constants
├── service/FinderService.kt     # Foreground service: listens for connections
└── ui/
    ├── DeviceListActivity.kt    # Lists paired devices, sends the ring command
    └── RingActivity.kt          # Full-screen alarm + vibration + stop button
```

## Notes / things you may want to extend

- The RFCOMM connection is **insecure** (no pairing PIN re-prompt) for
  simplicity, since it only talks to devices you've already paired.
  For a hardened version, switch to `createRfcommSocketToServiceRecord`
  (secure) and consider adding a shared secret in the payload.
- `FinderService` currently accepts one connection at a time in a loop;
  that's enough for this use case but isn't a general-purpose server.
- There's no persistent "device nickname" list — it always reads Android's
  live paired-devices list. You could cache favorites in SharedPreferences.
- App icon is a simple placeholder vector; swap
  `res/drawable/ic_launcher_foreground.xml` for your own art.
