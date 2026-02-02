@echo off
echo ========================================
echo   INSTALLATION ET TEST PTMS ANDROID
echo ========================================
echo.

echo 1. Vérification de l'APK...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✅ APK trouvé: app-debug.apk
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo    Taille: %%~zA bytes
) else (
    echo ❌ APK non trouvé. Compilez d'abord avec: gradlew assembleDebug
    pause
    exit /b 1
)

echo.
echo 2. Vérification des appareils connectés...
adb devices
if %errorlevel% neq 0 (
    echo ❌ ADB non trouvé. Assurez-vous qu'Android SDK est installé
    pause
    exit /b 1
)

echo.
echo 3. Installation de l'APK...
echo    Désinstallation de l'ancienne version (si elle existe)...
adb uninstall com.ptms.mobile
echo    Installation de la nouvelle version...
adb install app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo ❌ Échec de l'installation
    pause
    exit /b 1
)
echo ✅ Installation réussie

echo.
echo 4. Lancement de l'application...
adb shell am start -n com.ptms.mobile/.activities.MainActivity
if %errorlevel% neq 0 (
    echo ❌ Échec du lancement
    pause
    exit /b 1
)
echo ✅ Application lancée

echo.
echo ========================================
echo   INSTALLATION TERMINÉE AVEC SUCCÈS !
echo ========================================
echo.
echo 📱 Application installée et lancée
echo 📋 Prochaines étapes de test:
echo.
echo    TEST 1 - Fonctionnement de base:
echo    ✅ Vérifier que l'écran de connexion s'affiche
echo    ✅ Se connecter avec des identifiants valides
echo    ✅ Vérifier l'accès au dashboard
echo.
echo    TEST 2 - Fonctionnalité hors ligne:
echo    ✅ Aller dans "Saisie d'heures"
echo    ✅ Vérifier l'indicateur "Connecté"
echo    ✅ Couper le WiFi/données mobiles
echo    ✅ Vérifier l'indicateur "Hors ligne"
echo    ✅ Saisir des heures et vérifier la sauvegarde locale
echo.
echo    TEST 3 - Synchronisation:
echo    ✅ Remettre le WiFi/données mobiles
echo    ✅ Vérifier la synchronisation automatique
echo    ✅ Vérifier les notifications de statut
echo.
echo    TEST 4 - Service arrière-plan:
echo    ✅ Fermer l'application
echo    ✅ Vérifier que la synchronisation continue
echo    ✅ Rouvrir l'application et vérifier les résultats
echo.
echo 📖 Guide détaillé: test_offline_functionality.md
echo.
pause
