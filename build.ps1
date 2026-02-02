Set-Location "C:\devs\web\appAndroid"
Write-Host "=== D\u00e9but de la compilation ===" -ForegroundColor Green
Write-Host "Nettoyage..." -ForegroundColor Yellow
.\gradlew.bat clean | Out-Null
Write-Host "Compilation..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug

if (Test-Path "app\build\outputs\apk\debug\app-debug.apk") {
    $apk = Get-Item "app\build\outputs\apk\debug\app-debug.apk"
    Write-Host "`n=== R\u00e9sultat ===" -ForegroundColor Green
    Write-Host "Fichier: $($apk.FullName)"
    Write-Host "Taille: $([math]::Round($apk.Length/1MB,2)) MB"
    Write-Host "Date: $($apk.LastWriteTime)"
    Write-Host "`n[OK] Compilation r\u00e9ussie!" -ForegroundColor Green
} else {
    Write-Host "`n[ERREUR] APK non trouv\u00e9!" -ForegroundColor Red
}
