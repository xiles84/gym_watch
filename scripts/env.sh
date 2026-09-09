#!/usr/bin/env bash
# Source this before any gradle or adb command:  source scripts/env.sh
# Nothing this project needs is on PATH by default; it all ships inside
# Android Studio. See docs/LESSONS.md #8.

_studio="/c/Program Files/Android/Android Studio"
_sdk="${LOCALAPPDATA:-$HOME/AppData/Local}/Android/Sdk"

[ -d "$_studio/jbr" ] || echo "warn: Android Studio JBR not found at $_studio/jbr"
[ -d "$_sdk" ]        || echo "warn: Android SDK not found at $_sdk"

export JAVA_HOME="$_studio/jbr"
export ANDROID_HOME="$_sdk"
export ANDROID_SDK_ROOT="$_sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

echo "java  : $(java -version 2>&1 | head -1)"
echo "adb   : $(adb version 2>/dev/null | head -1)"
echo "sdk   : $ANDROID_HOME"
