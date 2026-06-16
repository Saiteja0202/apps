# FriendsChat

A WhatsApp-style group + 1-on-1 chat app for a friends circle. Internet-based,
real-time, with photo / video / file sharing. Kotlin + Jetpack Compose, backed by
Firebase (Auth + Firestore + Storage + Cloud Messaging).

## Status
- ✅ App builds: `app/build/outputs/apk/debug/app-debug.apk` (also copied to `../FriendsChat.apk`)
- ⚠️ Needs your Firebase project to function — see **[SETUP.md](SETUP.md)**.

## Features
- Email + password sign up / login (Firebase Auth)
- Real-time 1-on-1 direct messages and group chats (Cloud Firestore live listeners)
- Send **text, photos, videos, and any file** (Firebase Storage)
- Chat list with last-message preview and timestamps
- User directory to start new chats / build groups
- FCM wiring + optional Cloud Function for background push notifications

## Rebuild the APK after editing `firebase.xml`
From a terminal with the toolchain env set (same machine this was built on):

```powershell
$root = "C:\Users\2414342\android-build"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:ANDROID_HOME = "$root\sdk"
$env:GRADLE_USER_HOME = "$root\gradle-home"
Set-Location "C:\Users\2414342\Downloads\Apps\FriendsChat"
& "$root\gradle\gradle-8.9\bin\gradle.bat" assembleDebug --no-daemon --console=plain
```

The fresh APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

## Install on a phone
Copy the APK to the phone and open it (allow "install unknown apps"), or via USB:

```powershell
& "C:\Users\2414342\android-build\sdk\platform-tools\adb.exe" install -r FriendsChat.apk
```

## Notes
- Debug-signed build (fine for sharing with friends; not for the Play Store as-is).
- "FriendsChat" is a placeholder name — easy to change in `res/values/strings.xml`
  (display name) and `applicationId` in `app/build.gradle.kts` (note: changing the
  package means re-registering the Android app in Firebase).
