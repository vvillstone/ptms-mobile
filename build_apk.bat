@echo off
REM ====================================================
REM Script de Build Android APK - PTMS Mobile v2.0
REM ====================================================

echo.
echo ====================================================
echo   BUILD ANDROID APK - PTMS Mobile v2.0
echo ====================================================
echo.

REM Vérifier que nous sommes dans le bon répertoire
if not exist "gradlew.bat" (
    echo [ERREUR] gradlew.bat non trouve!
    echo Assurez-vous d'executer ce script depuis le repertoire appAndroid
    pause
    exit /b 1
)

echo [1/4] Nettoyage des builds precedents...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Le nettoyage a echoue!
    pause
    exit /b 1
)

echo.
echo [2/4] Build du projet Android...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Le build a echoue!
    pause
    exit /b 1
)

echo.
echo [3/4] Verification du fichier APK...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo [OK] APK genere avec succes!
    echo.
    echo Emplacement: app\build\outputs\apk\debug\app-debug.apk

    REM Obtenir la taille du fichier
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do (
        echo Taille: %%~zA octets
    )

    REM Obtenir la date de creation
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do (
        echo Date: %%~tA
    )
) else (
    echo [ERREUR] APK non trouve!
    pause
    exit /b 1
)

echo.
echo [4/4] Copie de l'APK vers le repertoire principal...
if not exist "..\apk_output" mkdir "..\apk_output"
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "..\apk_output\PTMS-Mobile-v2.0-debug.apk"
if %ERRORLEVEL% EQU 0 (
    echo [OK] APK copie vers: ..\apk_output\PTMS-Mobile-v2.0-debug.apk
)

echo.
echo ====================================================
echo   BUILD TERMINE AVEC SUCCES!
echo ====================================================
echo.
echo Fichiers generes:
echo   - app\build\outputs\apk\debug\app-debug.apk
echo   - ..\apk_output\PTMS-Mobile-v2.0-debug.apk
echo.
echo Vous pouvez maintenant installer l'APK sur votre appareil Android.
echo.
pause
