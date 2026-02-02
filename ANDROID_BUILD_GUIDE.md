# 📱 Guide de Build Android - PTMS Mobile v2.0

**Date**: 2025-10-07
**Version**: 2.0
**Compatibilité**: Android 7.0+ (API 24+)

---

## 📋 Table des Matières

1. [Prérequis](#prérequis)
2. [Build Rapide](#build-rapide)
3. [Configuration](#configuration)
4. [Compatibilité avec la Migration](#compatibilité-avec-la-migration)
5. [Résolution des Problèmes](#résolution-des-problèmes)
6. [Installation sur Appareil](#installation-sur-appareil)

---

## 🔧 Prérequis

### Système Requis

- **Java JDK**: 17+ (recommandé: OpenJDK 17)
- **Android SDK**: API 24-34
- **Gradle**: 8.13+ (inclus via Gradle Wrapper)
- **Espace disque**: ~2 GB pour les dépendances

### Vérification Java

```bash
# Windows
java -version

# Linux/Mac
java --version
```

Si Java n'est pas installé:
- **Windows**: Télécharger [Oracle JDK 17](https://www.oracle.com/java/technologies/downloads/#java17)
- **Linux**: `sudo apt install openjdk-17-jdk`
- **Mac**: `brew install openjdk@17`

---

## ⚡ Build Rapide

### Sur Windows

```cmd
cd C:\Devs\web\appAndroid
build_apk.bat
```

### Sur Linux/Mac

```bash
cd /path/to/web/appAndroid
chmod +x build_apk.sh
./build_apk.sh
```

### Manuellement avec Gradle

```bash
# Windows
gradlew.bat clean assembleDebug

# Linux/Mac
./gradlew clean assembleDebug
```

---

## 📦 Fichiers de Build

Après le build, les APK se trouvent ici:

```
appAndroid/
├── app/build/outputs/apk/debug/
│   └── app-debug.apk              # APK debug signé
│
└── (copié vers)
    ../apk_output/
    └── PTMS-Mobile-v2.0-debug.apk  # APK renommé pour distribution
```

---

## ⚙️ Configuration

### Endpoints API

L'application utilise les endpoints suivants (définis dans `ApiService.java`):

#### Authentification
- `POST login.php` - Login employé

#### Données
- `GET projects.php` - Liste des projets
- `GET work-types.php` - Types de travail
- `POST time-entry.php` - Saisie d'heures
- `GET reports.php` - Rapports d'heures
- `GET profile.php` - Profil employé

#### Chat
- `GET chat-rooms.php` - Salles de chat
- `GET chat-messages.php` - Messages
- `POST chat-send.php` - Envoyer message
- `GET chat-users.php` - Liste utilisateurs

### Configuration Server URL

Par défaut, l'application se connecte à:
```
https://serveralpha.protti.group/api/
```

Pour changer l'URL:
1. Ouvrir l'application
2. Aller dans **Paramètres** > **Configuration serveur**
3. Modifier l'URL de base

---

## ✅ Compatibilité avec la Migration employee_list → users

### Ce qui a été mis à jour

✅ **API PHP** (`api/login.php`, `api/profile.php`):
- Utilise maintenant `SELECT * FROM users WHERE type IN ('employee', 'manager')`
- Mapping des champs:
  - `employee_code` → `code` ou `employee_code`
  - `date_created` → `date_added`
  - `employee_status` → `type`

✅ **Modèle Employee** (`Employee.java`):
- Champ `employeeStatus` compatible avec le nouveau `type` de users
- Supporte: 'admin', 'manager', 'employee', 'viewer'

✅ **Pas de changement nécessaire côté Android**:
- L'app utilise `employee_id` localement (SharedPreferences, base SQLite)
- Le serveur gère la conversion `employee_id` → `user_id` en interne

### Test de Compatibilité

1. **Login**:
   ```
   POST /api/login.php
   Body: {"email": "employee@example.com", "password": "password"}
   ```

2. **Profil**:
   ```
   GET /api/profile.php
   Header: Authorization: Bearer {token}
   ```

3. **Saisie d'heures**:
   ```
   POST /api/time-entry.php
   Body: {
     "project_id": 1,
     "work_type_id": 2,
     "report_date": "2025-10-07",
     "datetime_from": "08:00",
     "datetime_to": "17:00",
     "description": "Test"
   }
   ```

---

## 🐛 Résolution des Problèmes

### Erreur: "SDK location not found"

**Solution**: Créer `local.properties`:
```properties
sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

Ou sur Linux/Mac:
```properties
sdk.dir=/home/username/Android/Sdk
```

### Erreur: "Java version not compatible"

**Solution**: Vérifier la version Java et mettre à jour vers JDK 17+

```bash
# Windows - Définir JAVA_HOME
set JAVA_HOME=C:\Program Files\Java\jdk-17

# Linux/Mac
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### Erreur: "Gradle sync failed"

**Solution**: Nettoyer et reconstruire
```bash
gradlew clean --refresh-dependencies
gradlew assembleDebug
```

### Erreur: "Failed to connect to server"

**Vérifications**:
1. ✓ Le serveur web est accessible
2. ✓ SSL configuré correctement (ou ignoré dans les paramètres)
3. ✓ URL de base correcte dans l'app
4. ✓ Pare-feu/proxy autorise les connexions

### Build très lent

**Optimisations**:
1. Augmenter la mémoire Gradle:
   ```properties
   # gradle.properties
   org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m
   org.gradle.parallel=true
   org.gradle.caching=true
   ```

2. Utiliser le daemon Gradle:
   ```bash
   gradlew --daemon assembleDebug
   ```

---

## 📲 Installation sur Appareil

### Via USB (ADB)

1. **Activer le mode développeur** sur l'appareil Android
2. **Activer le débogage USB**
3. **Connecter l'appareil** via USB
4. **Installer l'APK**:

```bash
# Installer automatiquement
gradlew installDebug

# Ou manuellement avec adb
adb install app/build/outputs/apk/debug/app-debug.apk

# Forcer la réinstallation
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Via Fichier APK

1. **Copier l'APK** sur l'appareil (USB, email, cloud)
2. **Activer "Sources inconnues"** dans les paramètres Android
3. **Ouvrir le fichier APK** et confirmer l'installation

### Via Réseau Local

```bash
# Installer via Wi-Fi (si adb wireless activé)
adb connect 192.168.1.XXX:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔐 Build Release (Production)

### Configuration du Keystore

1. **Créer un keystore**:
```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ptms-release
```

2. **Créer `keystore.properties`**:
```properties
storeFile=release-key.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=ptms-release
keyPassword=YOUR_KEY_PASSWORD
```

3. **Build release**:
```bash
gradlew assembleRelease
```

4. **APK signé**:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 📊 Structure du Projet

```
appAndroid/
├── app/
│   ├── src/main/
│   │   ├── java/com/ptms/mobile/
│   │   │   ├── activities/         # Activités Android
│   │   │   ├── adapters/           # Adapters RecyclerView
│   │   │   ├── api/                # API Service (Retrofit)
│   │   │   ├── database/           # SQLite local
│   │   │   ├── models/             # Modèles de données
│   │   │   ├── utils/              # Utilitaires
│   │   │   └── MainActivity.java
│   │   ├── res/                    # Ressources (layouts, strings)
│   │   └── AndroidManifest.xml
│   └── build.gradle                # Configuration build app
├── gradle/
│   └── wrapper/                    # Gradle Wrapper
├── build.gradle                    # Configuration projet
├── settings.gradle                 # Paramètres projet
├── gradlew                         # Script Gradle (Linux/Mac)
├── gradlew.bat                     # Script Gradle (Windows)
├── build_apk.bat                   # Script build Windows
└── build_apk.sh                    # Script build Linux/Mac
```

---

## 📝 Notes de Version

### v2.0 (2025-10-07)

#### ✨ Nouveautés
- ✅ Compatibilité avec la migration `employee_list` → `users`
- ✅ Support des nouveaux types d'utilisateurs (admin, manager, employee, viewer)
- ✅ API endpoints mis à jour pour la table `users` unifiée

#### 🔧 Modifications Techniques
- API `login.php`: Requête sur `users` avec filtre `type IN ('employee', 'manager')`
- API `profile.php`: Retourne les données depuis `users`
- Modèle `Employee`: Compatible avec le champ `employeeStatus` (mappé sur `type`)

#### 🐛 Corrections
- Aucun changement requis dans le code Android
- L'app continue à fonctionner avec les anciens `employee_id` locaux
- Le serveur fait la conversion automatique

---

## 🆘 Support

### Logs de Debug

Activer les logs détaillés dans l'app:
1. Paramètres > Debug
2. Activer "Logs détaillés"
3. Consulter via `adb logcat`

```bash
# Filtrer les logs PTMS
adb logcat -s PTMS:* API_CLIENT:*
```

### Rapporter un Bug

Fichier de log: `/sdcard/Android/data/com.ptms.mobile/files/logs/`

Informations à fournir:
- Version Android
- Version de l'app
- Message d'erreur
- Étapes pour reproduire

---

## ✅ Checklist de Déploiement

- [ ] Build réussi sans erreur
- [ ] Tests de login effectués
- [ ] Saisie d'heures testée
- [ ] Chat fonctionnel
- [ ] API endpoints vérifiés
- [ ] SSL configuré (ou ignoré si dev)
- [ ] Version incrémentée dans `build.gradle`
- [ ] APK signé pour production (si release)

---

**Auteur**: Claude Code
**Contact**: Support PTMS
**License**: Propriétaire - PROTTI Sàrl
