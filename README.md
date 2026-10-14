# App Lock

App Lock is an Android app that puts a biometric or PIN screen in front of the apps you choose. An accessibility service watches which app comes to the foreground and opens the lock screen when it is a blocked one. This repository holds the whole app, written in Kotlin with Jetpack Compose; the interface is in Portuguese.

## What it does

- **App list.** Search the installed launcher apps and mark the ones to block. Blocking an essential system app (such as Settings or the home screen) asks for confirmation first.
- **Unlock.** The lock screen asks for the device biometrics through `BiometricPrompt` (`BIOMETRIC_WEAK`) and falls back to a 4 to 8 digit PIN. The PIN is stored as a salted PBKDF2-HMAC-SHA256 hash, encrypted with an AES-GCM key from the Android Keystore.
- **Setup.** A diagnostic screen walks through the accessibility service, the overlay permission (the reliable path on Android 12+), the PIN, battery optimization and manufacturer specific settings.
- **Protection of the app itself.** Optionally, opening App Lock and changing its settings also requires biometrics or the PIN.
- **Master switch and temporary lock.** A switch turns all blocking on or off. A temporary lock keeps blocking on for 30 or 60 minutes even with the switch off, and an alarm ends it (rescheduled after a reboot).
- **Automation.** `AutomationActivity` accepts `applock://action/{enable,disable,toggle,lock,restore}` (with `?minutes=` for `lock`) and explicit intents `com.archieapps.applock.action.ENABLE`, `DISABLE`, `TOGGLE`, `LOCK` (extra `minutes`) and `RESTORE`, so launchers and automation apps can drive it. Launcher shortcuts use the same URIs. When enabled, actions that weaken the protection (disable, restore, toggle off) ask for authentication first.

Nothing on screen is read or sent anywhere: the accessibility service only looks at which package is in the foreground.

## Requirements

- Android **7.1+** (minSdk 25), targetSdk 36
- JDK **21** (Gradle daemon toolchain)
- Gradle **9.5.0** (wrapper), Android Gradle Plugin **9.3.0**, Kotlin **2.3.21**
- Android SDK with platform 36, set through `local.properties` or `ANDROID_HOME`

## Building

Build a debug APK, or install it on a connected device:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

After installing, open the app and follow the diagnostic screen: blocking only works once the accessibility service is enabled in the system settings.
