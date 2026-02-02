import os
import subprocess
import shutil
import time

# Chemin du projet
project_dir = r"C:\Devs\web\appAndroid"
app_build_dir = os.path.join(project_dir, "app", "build")
debug_apk_dir = os.path.join(app_build_dir, "outputs", "apk", "debug")

print("=== Fix and Build Script ===\n")

# 1. Arrêter les daemons Gradle
print("[1/5] Arrêt des daemons Gradle...")
os.chdir(project_dir)
subprocess.run(["gradlew.bat", "--stop"], shell=True, capture_output=True)
time.sleep(2)

# 2. Supprimer le dossier build
print("[2/5] Suppression du dossier build...")
if os.path.exists(app_build_dir):
    try:
        shutil.rmtree(app_build_dir)
        print("  ✓ Dossier build supprimé")
    except Exception as e:
        print(f"  ⚠ Erreur suppression: {e}")
        # Essayer avec rmdir Windows
        subprocess.run(["cmd", "/c", "rmdir", "/s", "/q", app_build_dir], shell=True)

time.sleep(1)

# 3. Créer le dossier debug propre
print("[3/5] Création du dossier debug...")
os.makedirs(debug_apk_dir, exist_ok=True)

# 4. Compiler
print("[4/5] Compilation de l'APK...")
result = subprocess.run(
    ["gradlew.bat", "assembleDebug", "--console=plain"],
    cwd=project_dir,
    capture_output=True,
    text=True,
    timeout=600
)

print("\n--- Sortie compilation ---")
print(result.stdout[-2000:] if len(result.stdout) > 2000 else result.stdout)

if result.returncode == 0:
    print("\n✅ BUILD SUCCESSFUL!")

    # 5. Vérifier l'APK
    print("[5/5] Vérification de l'APK...")
    apk_file = os.path.join(debug_apk_dir, "app-debug.apk")
    if os.path.exists(apk_file):
        size = os.path.getsize(apk_file) / (1024 * 1024)
        print(f"  ✓ APK trouvé: {apk_file}")
        print(f"  ✓ Taille: {size:.2f} MB")
    else:
        print("  ✗ APK non trouvé!")
        # Lister les fichiers
        if os.path.exists(debug_apk_dir):
            files = os.listdir(debug_apk_dir)
            print(f"  Fichiers dans debug/: {files}")
else:
    print(f"\n❌ BUILD FAILED (code {result.returncode})")
    print("\n--- Erreurs ---")
    print(result.stderr[-1000:] if len(result.stderr) > 1000 else result.stderr)
