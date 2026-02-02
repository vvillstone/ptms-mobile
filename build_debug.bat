@echo off
echo Building Android Debug APK...
call gradlew.bat clean assembleDebug --no-daemon --stacktrace > build_output.txt 2>&1
echo Build complete. Check build_output.txt for details.
