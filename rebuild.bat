@echo off
cd /d C:\devs\web\appAndroid
echo Nettoyage...
call gradlew.bat clean
echo Compilation...
call gradlew.bat assembleDebug
echo.
echo Compilation terminee!
dir app\build\outputs\apk\debug\app-debug.apk
