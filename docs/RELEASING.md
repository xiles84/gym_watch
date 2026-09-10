# Releasing

How to cut a signed release build and put it on a watch.

## What is secret, and why

Two files are **gitignored and must stay that way**:

| File | Contains |
|---|---|
| `gym-watch-release.jks` | the release signing key |
| `local.properties` | the SDK path **and the keystore passwords** |

Together they are the app's identity. Anyone holding both can build an app that
Android accepts as an update to this one. They are not in git and must never be:
a key pushed to a remote is compromised even if the commit is later deleted,
because remotes cache and mirrors index.

Back them up somewhere private (a password manager or an encrypted drive).
**If the keystore is lost it cannot be regenerated** — a new key produces what
Android treats as a different app, so the old one must be uninstalled and its
data is gone.

## Creating the keystore (already done once; here for a rebuild)

```bash
keytool -genkeypair \
  -keystore gym-watch-release.jks \
  -alias gym-watch \
  -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=Gym Watch, OU=Personal, O=Gym Watch, L=Unknown, ST=Unknown, C=BR"
```

`keytool` prompts for a store password and a key password; this project uses the
same value for both. It lives in Android Studio's JDK:
`C:/Program Files/Android/Android Studio/jbr/bin/keytool`.

Then add four lines to `local.properties`:

```properties
releaseStoreFile=gym-watch-release.jks
releaseStorePassword=<the password you chose>
releaseKeyAlias=gym-watch
releaseKeyPassword=<the same password>
```

`app/build.gradle.kts` reads these. If they are missing, the release build
silently falls back to the **debug** keystore so the variant still assembles —
fine for sideloading, useless for anything else.

Verify what you created:

```bash
keytool -list -v -keystore gym-watch-release.jks -alias gym-watch
```

## Building

```bash
source scripts/env.sh
./gradlew :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk` — around **3 MB**,
versus ~35 MB for debug, because the release build runs R8 with resource
shrinking. Keep rules live in `app/proguard-rules.pro`.

Always smoke-test a release build on the watch. R8 breaks things debug builds
never hit, and the Health Services data classes are the most likely casualty.

## Installing

A release APK and a debug APK are signed with different keys, and Android treats
those as different apps. Switching between them needs an uninstall first, which
**erases the counter, favourites and rest length**:

```bash
adb -s <watch> uninstall com.gymwatch
adb -s <watch> install app/build/outputs/apk/release/app-release.apk
```

Installing a newer release *over an older release* needs no uninstall:

```bash
adb -s <watch> install -r app/build/outputs/apk/release/app-release.apk
```

## Versioning

`versionCode` and `versionName` are in `app/build.gradle.kts`. Bump both, commit,
then tag:

```bash
git tag -a v0.1.0 -m "Gym Watch 0.1.0"
git push origin main --tags
```

`versionCode` must increase for every build you install over another, or the
install is rejected.

## Rotating the password

Change it in the keystore, then update the two lines in `local.properties`:

```bash
keytool -storepasswd -keystore gym-watch-release.jks
keytool -keypasswd -keystore gym-watch-release.jks -alias gym-watch
```

The signing *key* is unchanged, so already-installed apps keep updating fine.
