# 📱 PTMS Mobile - Application Android

**Version**: 2.0
**Plateforme**: Android 7.0+ (API 24+)
**Architecture**: MVVM with Retrofit + OkHttp
**Langage**: Java + Kotlin

---

## 🚀 Build Rapide

### Windows
```cmd
build_apk.bat
```

### Linux/Mac
```bash
chmod +x build_apk.sh
./build_apk.sh
```

**Résultat**: `../apk_output/PTMS-Mobile-v2.0-debug.apk`

---

## ✅ Compatibilité Migration v2.0

Cette version est **100% compatible** avec la migration `employee_list` → `users`:

- ✅ API `login.php` utilise la table `users`
- ✅ API `profile.php` utilise la table `users`
- ✅ Mapping automatique `employeeStatus` → `type`
- ✅ Support des rôles: admin, manager, employee, viewer

**Aucune modification nécessaire dans l'application Android.**

---

## 📚 Documentation

Consultez [ANDROID_BUILD_GUIDE.md](./ANDROID_BUILD_GUIDE.md) pour:
- Installation détaillée
- Configuration
- Résolution de problèmes
- Build release

---

## 📦 Fonctionnalités

### Core Features
- ✅ **Authentification** - Login sécurisé avec JWT
- ✅ **Saisie d'heures** - Enregistrement en ligne/hors-ligne
- ✅ **Projets** - Liste et détails des projets assignés
- ✅ **Rapports** - Consultation des heures enregistrées
- ✅ **Profil** - Informations employé

### Advanced Features
- ✅ **Chat** - Messagerie en temps réel
- ✅ **Mode Hors-ligne** - SQLite local pour saisie sans connexion
- ✅ **Synchronisation** - Auto-sync des données hors-ligne
- ✅ **Notifications** - Alertes et rappels

## Structure du projet

```
AppAndroid/
├── app/
│   ├── src/main/
│   │   ├── java/com/ptms/mobile/
│   │   │   ├── activities/          # Activités Android
│   │   │   ├── adapters/           # Adaptateurs pour les listes
│   │   │   ├── api/                # Services API
│   │   │   ├── models/             # Modèles de données
│   │   │   ├── utils/              # Utilitaires
│   │   │   └── MainActivity.java   # Activité principale
│   │   ├── res/                    # Ressources (layouts, strings, etc.)
│   │   └── AndroidManifest.xml     # Manifeste Android
│   └── build.gradle                # Configuration Gradle
├── build.gradle                    # Configuration projet
└── settings.gradle                 # Paramètres Gradle
```

## Configuration

1. **Modifier l'URL du serveur** dans `ApiConfig.java`
2. **Compiler l'application** avec Android Studio
3. **Installer sur les appareils** des employés

## 🔗 Endpoints API

Base URL: `https://serveralpha.protti.group/api/`

### Authentification
- `POST /login.php` - Login employé

### Données
- `GET /projects.php` - Liste projets
- `GET /work-types.php` - Types de travail
- `POST /time-entry.php` - Saisie heures
- `GET /reports.php` - Rapports
- `GET /profile.php` - Profil

### Chat
- `GET /chat-rooms.php` - Salles
- `GET /chat-messages.php` - Messages
- `POST /chat-send.php` - Envoyer
- `GET /chat-users.php` - Utilisateurs

---

## 🔧 Prérequis

- **Java JDK 17+**
- **Android SDK** (API 24-34)
- **Gradle 8.13+** (inclus via wrapper)

---

## 🐛 Debug

```bash
# Logs en temps réel
adb logcat -s PTMS:* API_CLIENT:*

# Installer APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Clear app data
adb shell pm clear com.ptms.mobile
```

---

## 📝 Changelog

### v2.0.1 (2025-10-23) - Session d'améliorations
- ✅ **Édition de notes** : Mode UPDATE complet dans CreateNoteUnifiedActivity
- ✅ **Affichage noms utilisateurs** : Cache et récupération depuis l'API dans le chat
- ✅ **Tests unitaires** : 29 tests créés, corrigés et exécutés (Auth, Database, Sync)
  - ✅ Tests compilent à 100% (correction types et méthodes)
  - ✅ **24/29 tests passent (83%)** 🎉
  - ✅ **OfflineDatabaseHelper : 10/10 (100%)**
  - ✅ **OfflineSyncManager : 9/9 (100%)**
  - ⚠️ AuthenticationManager : 5/10 (50% - nécessite mocking SessionManager)
  - 📊 Rapports : `TESTS_RESULTS_2025_10_23.md`, `TESTS_CORRECTION_FINALE_2025_10_23.md`
- ✅ **Build release** : ProGuard/R8 optimisé, APK minifié (~4.9 MB)
- ✅ **Icône timer** : ic_timer.xml créé (Material Design)
- ✅ **Nettoyage** : Suppression fichiers dupliqués
- 🔧 Dépendances tests : Robolectric 4.10.3, Mockito 5.3.1
- 🔧 Ajout méthode `updateLastSyncTime()` dans OfflineSyncManager
- 🔧 Corrections tests : sync_status vs synced flag, Robolectric environment
- 📦 APK générés : Debug (5.2 MB) + Release (4.9 MB)
- 📚 Documentation : 4 documents de référence complets

### v2.0 (2025-10-07)
- ✅ Migration vers table `users` unifiée
- ✅ Support nouveaux types d'utilisateurs
- ✅ Compatibilité API v2.0
- 🔧 Scripts de build améliorés
- 📚 Documentation complète

### v1.0 (Legacy)
- Version initiale avec `employee_list`

---

## 🆘 Support

**Documentation**: Voir [ANDROID_BUILD_GUIDE.md](./ANDROID_BUILD_GUIDE.md)
**Issues**: Rapporter sur le serveur Git interne
**Contact**: Support PTMS - PROTTI Sàrl

---

## 📄 License

Propriétaire - PROTTI Sàrl © 2025

