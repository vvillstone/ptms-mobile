@echo off
REM Script de compilation pour PTMS Android - Mise à jour Notes v2.0
REM Date: 16 Janvier 2025

echo ========================================
echo PTMS Android - Build Notes Update v2.0
echo ========================================
echo.

REM Afficher la version de Gradle
echo [1/5] Verification de Gradle...
call gradlew.bat --version
if errorlevel 1 (
    echo ERREUR: Gradle n'est pas disponible
    pause
    exit /b 1
)
echo.

REM Nettoyer le projet
echo [2/5] Nettoyage du projet...
call gradlew.bat clean
if errorlevel 1 (
    echo ERREUR: Echec du nettoyage
    pause
    exit /b 1
)
echo.

REM Construire le projet
echo [3/5] Compilation en cours...
echo Ceci peut prendre quelques minutes...
call gradlew.bat build
if errorlevel 1 (
    echo ERREUR: Echec de la compilation
    echo.
    echo Consultez les logs ci-dessus pour plus de details
    pause
    exit /b 1
)
echo.

REM Afficher les APK générés
echo [4/5] Recherche des APK generes...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✓ APK Debug trouve: app\build\outputs\apk\debug\app-debug.apk
    dir "app\build\outputs\apk\debug\app-debug.apk" | findstr /C:"app-debug.apk"
) else (
    echo ⚠ APK Debug non trouve
)
echo.

if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo ✓ APK Release trouve: app\build\outputs\apk\release\app-release-unsigned.apk
    dir "app\build\outputs\apk\release\app-release-unsigned.apk" | findstr /C:"app-release"
) else (
    echo ℹ APK Release non genere (normal si pas signe)
)
echo.

REM Résumé
echo [5/5] Compilation terminee avec succes!
echo ========================================
echo.
echo Prochaines etapes:
echo 1. Installer l'APK sur un appareil:
echo    gradlew.bat installDebug
echo.
echo 2. Ou copier l'APK manuellement:
echo    app\build\outputs\apk\debug\app-debug.apk
echo.
echo 3. Tester les nouvelles fonctionnalites:
echo    - Menu Notes
echo    - Gerer les categories
echo    - Creer une note avec categorie
echo    - Notes personnelles (sans projet)
echo ========================================
echo.

pause
