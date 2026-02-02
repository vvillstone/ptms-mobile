# 🔨 COMMENT COMPILER PTMS ANDROID v2.0

## ⚠️ IMPORTANT : Recompilation Nécessaire

L'APK actuel date du **19/10/2025 à 02:04**.
Nos modifications ont été faites à **12:26-12:27** (aujourd'hui).

**L'APK actuel ne contient PAS les nouvelles fonctionnalités !**

### Fichiers Modifiés Récemment (4)

1. `BidirectionalSyncManager.java` - NOUVEAU (12:26)
2. `OfflineDatabaseHelper.java` - MODIFIÉ (12:27)
3. `AutoSyncService.java` - MODIFIÉ
4. `FloatingTimerWidgetManager.java` - MODIFIÉ

---

## 📋 MÉTHODE 1 : Script Batch Automatique (RECOMMANDÉ)

### Windows - Double-clic

1. Ouvrir l'explorateur Windows
2. Naviguer vers : `D:\ServeurWebNAS\SynologyDrive\appAndroid\`
3. Double-cliquer sur : **`compile_now.bat`**
4. Attendre la fin de la compilation
5. L'APK sera dans : `app\build\outputs\apk\debug\`

---

## 📋 MÉTHODE 2 : Ligne de Commande Windows

### PowerShell / CMD

```cmd
cd D:\ServeurWebNAS\SynologyDrive\appAndroid

REM Nettoyage
gradlew.bat clean --no-daemon

REM Compilation
gradlew.bat assembleDebug --no-daemon --stacktrace

REM Vérifier APK
dir app\build\outputs\apk\debug\*.apk
```

---

## 📋 MÉTHODE 3 : Android Studio

### Via IDE

1. Ouvrir Android Studio
2. File → Open → Sélectionner `D:\ServeurWebNAS\SynologyDrive\appAndroid\`
3. Attendre l'indexation du projet
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. Cliquer sur "locate" dans la notification
6. APK généré !

---

## ✅ Vérifier le Succès de la Compilation

### Fichier APK Attendu

```
app\build\outputs\apk\debug\PTMS-Mobile-v2.0-debug-YYYYMMDD-HHMM.apk
```

**Taille attendue** : ~8-10 MB

### Vérifier Date de Création

L'APK DOIT être créé **AUJOURD'HUI après 12:27** pour contenir les nouvelles fonctionnalités.

```cmd
dir /OD app\build\outputs\apk\debug\*.apk
```

La date doit être **postérieure à 12:27** !

---

## 🚨 EN CAS DE PROBLÈME

### Erreur : "JAVA_HOME not set"

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
gradlew.bat assembleDebug
```

### Erreur : "SDK location not found"

Créer `local.properties` :

```properties
sdk.dir=C\:\\Users\\VotreNom\\AppData\\Local\\Android\\Sdk
```

### Erreur : "Build failed"

Voir les logs :

```cmd
gradlew.bat assembleDebug --stacktrace --debug > build_log.txt 2>&1
```

Puis examiner `build_log.txt`

---

## 📦 APRÈS LA COMPILATION

### Copie Automatique

L'APK est automatiquement copié vers :
```
C:\Devs\web\uploads\apk\
```

### Installation sur Appareil

#### Via ADB

```cmd
adb devices
adb install -r app\build\outputs\apk\debug\PTMS-Mobile-v2.0-debug-*.apk
```

#### Via Copie Manuelle

1. Copier l'APK sur le téléphone
2. Ouvrir le fichier sur le téléphone
3. Autoriser "Sources inconnues" si demandé
4. Installer

---

## 🔍 VÉRIFIER QUE L'APK CONTIENT LES NOUVELLES FONCTIONNALITÉS

### Après Installation

1. Ouvrir l'app
2. Se connecter
3. Aller dans **Paramètres** ou **About**
4. Vérifier la version : **v2.0**
5. Vérifier la date de build : **19/10/2025 après 12:27**

### Logs de Synchronisation

```cmd
adb logcat -s BidirectionalSync AutoSyncService

# Si vous voyez ces logs, c'est OK :
# ✅ 🔄 Début synchronisation: FULL
# ✅ 📥 Téléchargement des projets...
# ✅ 📤 Upload de X rapports...
```

---

## 📊 CHECKLIST COMPLÈTE

- [ ] Script `compile_now.bat` créé
- [ ] Compilation lancée
- [ ] APK généré avec date > 12:27
- [ ] Taille APK ~8-10 MB
- [ ] APK copié vers `C:\Devs\web\uploads\apk\`
- [ ] APK installé sur appareil
- [ ] App démarre sans crash
- [ ] Logs de synchronisation visibles

---

## 🎯 RÉSUMÉ RAPIDE

```cmd
# COMPILATION RAPIDE (1 commande)

cd D:\ServeurWebNAS\SynologyDrive\appAndroid && gradlew.bat clean assembleDebug --no-daemon

# APK GÉNÉRÉ ICI :
# app\build\outputs\apk\debug\PTMS-Mobile-v2.0-debug-*.apk
```

---

**Dernière mise à jour** : 19/10/2025 12:38
**Statut** : Prêt à compiler
**Version cible** : v2.0 avec synchronisation bidirectionnelle
