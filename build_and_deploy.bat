@echo off
REM ============================================================
REM PTMS Mobile - Script de compilation et déploiement
REM ============================================================
REM Ce script compile l'APK et le copie vers le serveur
REM
REM Prérequis:
REM - Android Studio installé avec le SDK
REM - Variables d'environnement ANDROID_HOME configurée
REM ============================================================

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║         PTMS Mobile - Compilation et Déploiement              ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM Vérifier que gradlew.bat existe
if not exist "%~dp0gradlew.bat" (
    echo [ERREUR] gradlew.bat non trouvé!
    pause
    exit /b 1
)

echo [1/3] Nettoyage du projet...
call "%~dp0gradlew.bat" clean
if %ERRORLEVEL% neq 0 (
    echo [ERREUR] Echec du nettoyage
    pause
    exit /b 1
)

echo.
echo [2/3] Compilation de l'APK debug...
call "%~dp0gradlew.bat" assembleDebug
if %ERRORLEVEL% neq 0 (
    echo [ERREUR] Echec de la compilation
    pause
    exit /b 1
)

echo.
echo [3/3] Recherche de l'APK généré...
for /f "delims=" %%i in ('dir /b /s "%~dp0app\build\outputs\apk\debug\*.apk" 2^>nul') do (
    set "APK_PATH=%%i"
    set "APK_NAME=%%~nxi"
)

if not defined APK_PATH (
    echo [ERREUR] APK non trouvé!
    pause
    exit /b 1
)

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║  ✅ COMPILATION RÉUSSIE                                       ║
echo ╠═══════════════════════════════════════════════════════════════╣
echo ║  📦 APK: %APK_NAME%
echo ║  📂 Chemin: %APK_PATH%
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM Demander si on veut copier vers le serveur
set /p DEPLOY="Copier vers le serveur (uploads/apk) ? [O/N]: "
if /i "%DEPLOY%"=="O" (
    set "DEST_DIR=C:\Devs\web\uploads\apk"
    if not exist "%DEST_DIR%" mkdir "%DEST_DIR%"
    copy "%APK_PATH%" "%DEST_DIR%\"
    echo.
    echo ✅ APK copié vers %DEST_DIR%
)

echo.
echo Terminé!
pause
