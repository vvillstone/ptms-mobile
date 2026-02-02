@echo off
echo ========================================
echo COMPILATION PTMS ANDROID v2.0
echo ========================================
echo.

cd /d "%~dp0"

echo Repertoire de travail: %CD%
echo.

echo [1/3] Nettoyage...
call gradlew.bat clean --no-daemon
echo.

echo [2/3] Compilation Debug APK...
call gradlew.bat assembleDebug --no-daemon --stacktrace
echo.

echo [3/3] Verification APK...
if exist "app\build\outputs\apk\debug\*.apk" (
    echo.
    echo ========================================
    echo COMPILATION REUSSIE!
    echo ========================================
    echo.
    dir /B app\build\outputs\apk\debug\*.apk
    echo.
) else (
    echo.
    echo ========================================
    echo ERREUR: APK non trouve!
    echo ========================================
    echo.
)

pause
