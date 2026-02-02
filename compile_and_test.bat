@echo off
echo ========================================
echo   COMPILATION ET TEST PTMS ANDROID
echo ========================================
echo.

echo 1. Nettoyage du projet...
call gradlew clean
if %errorlevel% neq 0 (
    echo ❌ Erreur lors du nettoyage
    pause
    exit /b 1
)
echo ✅ Nettoyage terminé

echo.
echo 2. Synchronisation des dépendances...
call gradlew build --refresh-dependencies
if %errorlevel% neq 0 (
    echo ❌ Erreur lors de la synchronisation
    pause
    exit /b 1
)
echo ✅ Synchronisation terminée

echo.
echo 3. Compilation du projet...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ❌ Erreur lors de la compilation
    pause
    exit /b 1
)
echo ✅ Compilation terminée

echo.
echo 4. Vérification des fichiers générés...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✅ APK généré: app\build\outputs\apk\debug\app-debug.apk
    dir "app\build\outputs\apk\debug\app-debug.apk"
) else (
    echo ❌ APK non trouvé
    pause
    exit /b 1
)

echo.
echo 5. Vérification de la taille de l'APK...
for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo Taille: %%~zA bytes

echo.
echo ========================================
echo   COMPILATION TERMINÉE AVEC SUCCÈS !
echo ========================================
echo.
echo 📱 APK généré: app\build\outputs\apk\debug\app-debug.apk
echo.
echo 📋 Prochaines étapes:
echo    1. Installer l'APK sur un appareil/émulateur
echo    2. Tester la fonctionnalité hors ligne
echo    3. Vérifier la synchronisation automatique
echo.
pause
