# 🔐 Guide de Configuration Keystore Production - PTMS Mobile

**Date**: 2025-10-23
**Version**: 2.0.1
**Objectif**: Créer et configurer le keystore pour signer l'APK de production

---

## 📋 Vue d'Ensemble

Le keystore de production est **CRITIQUE** pour le déploiement d'applications Android. Il permet de:
- ✅ Signer l'APK avec un certificat unique
- ✅ Prouver l'authenticité de l'application
- ✅ Permettre les mises à jour de l'application
- ✅ Publier sur Google Play Store (si applicable)

**⚠️ IMPORTANT**:
- Le keystore doit être **conservé en lieu sûr** (perte = impossibilité de mettre à jour l'app)
- Le mot de passe doit être **fort et sécurisé**
- **NE JAMAIS** commiter le keystore dans Git

---

## 🔧 Étape 1: Vérifier Java JDK

Le keystore est généré avec l'outil `keytool` fourni avec le JDK.

### Vérifier l'installation JDK

```bash
# Vérifier version Java
java -version

# Vérifier keytool
keytool -help
```

**Attendu**: JDK 17+ (compatible avec Android Gradle 8.13)

**Si keytool non trouvé**:
- Windows: Ajouter `C:\Program Files\Java\jdk-17\bin` au PATH
- Linux/Mac: Installer OpenJDK 17

---

## 🔑 Étape 2: Créer le Keystore Production

### Commande de Génération

```bash
# Se placer dans le dossier appAndroid
cd C:\Devs\web\appAndroid

# Générer le keystore
keytool -genkey -v -keystore ptms-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ptms-mobile
```

### Paramètres Expliqués

| Paramètre | Valeur | Description |
|-----------|--------|-------------|
| `-genkey -v` | - | Générer une paire de clés (verbose) |
| `-keystore` | `ptms-release-key.jks` | Nom du fichier keystore |
| `-keyalg` | `RSA` | Algorithme de chiffrement |
| `-keysize` | `2048` | Taille de la clé (2048 bits = sécurisé) |
| `-validity` | `10000` | Validité en jours (~27 ans) |
| `-alias` | `ptms-mobile` | Alias de la clé (identifiant) |

### Informations à Fournir

Lors de l'exécution, keytool demandera:

```
Enter keystore password: [MOT_DE_PASSE_FORT]
Re-enter new password: [CONFIRMER]

What is your first and last name?
  [Unknown]:  PROTTI Sarl

What is the name of your organizational unit?
  [Unknown]:  Development

What is the name of your organization?
  [Unknown]:  PROTTI Sarl

What is the name of your City or Locality?
  [Unknown]:  [Votre ville]

What is the name of your State or Province?
  [Unknown]:  [Canton]

What is the two-letter country code for this unit?
  [Unknown]:  CH

Is CN=PROTTI Sarl, OU=Development, O=PROTTI Sarl, L=[Ville], ST=[Canton], C=CH correct?
  [no]:  yes

Enter key password for <ptms-mobile>
	(RETURN if same as keystore password): [RETURN ou autre mot de passe]
```

**Recommandations**:
- **Mot de passe keystore**: Minimum 12 caractères, mélange majuscules/minuscules/chiffres/symboles
- **Mot de passe clé**: Utiliser le MÊME mot de passe que le keystore (appuyer sur RETURN)
- **Informations organisation**: Utiliser les vraies informations de PROTTI Sàrl

### Résultat Attendu

```
Generating 2,048 bit RSA key pair and self-signed certificate (SHA256withRSA) with a validity of 10,000 days
	for: CN=PROTTI Sarl, OU=Development, O=PROTTI Sarl, L=[Ville], ST=[Canton], C=CH
[Storing ptms-release-key.jks]
```

**Fichier créé**: `C:\Devs\web\appAndroid\ptms-release-key.jks`

---

## 📝 Étape 3: Documenter les Informations du Keystore

**⚠️ CRITIQUE**: Conserver ces informations dans un endroit SÉCURISÉ (gestionnaire de mots de passe, coffre-fort numérique).

### Informations à Conserver

```
========================================
PTMS Mobile - Keystore Production
========================================

Fichier: ptms-release-key.jks
Localisation: C:\Devs\web\appAndroid\
Alias: ptms-mobile

Mot de passe keystore: [VOTRE_MOT_DE_PASSE]
Mot de passe clé: [MÊME_MOT_DE_PASSE]

Organisation: PROTTI Sarl
Unité: Development
Ville: [Votre ville]
Canton: [Votre canton]
Pays: CH

Algorithme: RSA 2048 bits
Validité: 10000 jours (expire en ~2052)
Créé le: 2025-10-23

========================================
SAUVEGARDES
========================================

Sauvegarde 1: [Localisation sécurisée 1]
Sauvegarde 2: [Localisation sécurisée 2]
Sauvegarde 3: [Cloud sécurisé]

========================================
```

**Sauvegarder dans**:
- Gestionnaire de mots de passe (1Password, LastPass, Bitwarden)
- Document chiffré
- **NE PAS** enregistrer en clair sur le disque

---

## 💾 Étape 4: Sauvegarder le Keystore

### Créer des Sauvegardes

**Sauvegarde 1 - Locale sécurisée**:
```bash
# Copier vers un dossier sécurisé hors du projet
copy ptms-release-key.jks C:\Secure\Backups\PTMS\ptms-release-key.jks
```

**Sauvegarde 2 - Cloud chiffré**:
- Utiliser un service cloud sécurisé (OneDrive, Dropbox avec chiffrement)
- Placer dans un dossier chiffré

**Sauvegarde 3 - Support physique**:
- USB cryptée
- Disque dur externe sécurisé

**⚠️ NE JAMAIS**:
- Commiter le keystore dans Git
- Envoyer par email
- Stocker en clair sur serveur web
- Partager le mot de passe en clair

---

## 🔒 Étape 5: Configurer build.gradle

### 5.1 Créer keystore.properties (Sécurisé)

```bash
# Dans appAndroid/
echo. > keystore.properties
```

**Contenu de `keystore.properties`**:
```properties
storeFile=ptms-release-key.jks
storePassword=VOTRE_MOT_DE_PASSE_KEYSTORE
keyAlias=ptms-mobile
keyPassword=VOTRE_MOT_DE_PASSE_CLE
```

**⚠️ IMPORTANT**: Ajouter à `.gitignore`:
```bash
# Dans appAndroid/.gitignore
keystore.properties
*.jks
*.keystore
```

### 5.2 Modifier build.gradle (app)

**Localisation**: `appAndroid/app/build.gradle`

**Ajouter AVANT `android {`** (ligne ~1-10):
```gradle
// Charger les propriétés du keystore
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}
```

**Ajouter DANS `android {` APRÈS `buildTypes {`**:
```gradle
android {
    // ... autres configurations ...

    signingConfigs {
        release {
            if (keystorePropertiesFile.exists()) {
                storeFile file(keystoreProperties['storeFile'])
                storePassword keystoreProperties['storePassword']
                keyAlias keystoreProperties['keyAlias']
                keyPassword keystoreProperties['keyPassword']
            }
        }
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release  // ← AJOUTER CETTE LIGNE
        }
        debug {
            minifyEnabled false
        }
    }
}
```

---

## 🏗️ Étape 6: Build APK Release Signé

### 6.1 Clean Build

```bash
cd C:\Devs\web\appAndroid

# Nettoyer
.\gradlew.bat clean

# Build release
.\gradlew.bat assembleRelease
```

### 6.2 Vérifier le Build

**Localisation de l'APK**:
```
appAndroid/app/build/outputs/apk/release/app-release.apk
```

**Vérifier la signature**:
```bash
# Vérifier que l'APK est signé
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

**Résultat attendu**:
```
jar verified.
```

**Voir les détails du certificat**:
```bash
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

**Attendu**:
```
Owner: CN=PROTTI Sarl, OU=Development, O=PROTTI Sarl, L=[Ville], ST=[Canton], C=CH
Issuer: CN=PROTTI Sarl, OU=Development, O=PROTTI Sarl, L=[Ville], ST=[Canton], C=CH
Serial number: [numéro]
Valid from: Thu Oct 23 ... 2025 until: ...
Certificate fingerprints:
	 SHA1: [empreinte SHA1]
	 SHA256: [empreinte SHA256]
```

---

## ✅ Étape 7: Tests Post-Signature

### 7.1 Installer sur Device/Émulateur

```bash
# Désinstaller version debug si présente
adb uninstall com.ptms.mobile

# Installer version release
adb install app/build/outputs/apk/release/app-release.apk
```

### 7.2 Tests Critiques

**Test 1: Application Lance**:
- [ ] App s'ouvre sans crash
- [ ] Écran de login visible

**Test 2: Login Fonctionne**:
- [ ] Login réussi
- [ ] Dashboard chargé
- [ ] Token sauvegardé

**Test 3: Fonctionnalités Principales**:
- [ ] Création de note
- [ ] Édition de note (sans doublon)
- [ ] Chat affiche noms utilisateurs
- [ ] Saisie heures offline

**Test 4: ProGuard/R8 OK**:
- [ ] Pas d'erreurs de méthodes manquantes
- [ ] Navigation fonctionne
- [ ] API calls réussis

### 7.3 Comparer Taille APK

```bash
# Debug APK
dir app\build\outputs\apk\debug\app-debug.apk

# Release APK
dir app\build\outputs\apk\release\app-release.apk
```

**Attendu**:
- Debug: ~5.2 MB
- Release: ~4.9 MB (minifié avec ProGuard/R8)

---

## 📦 Étape 8: Copier APK vers Uploads

```bash
# Créer dossier si nécessaire
if not exist "C:\Devs\web\uploads\apk\" mkdir "C:\Devs\web\uploads\apk\"

# Copier avec timestamp
copy app\build\outputs\apk\release\app-release.apk "C:\Devs\web\uploads\apk\PTMS-Mobile-v2.0.1-release-%date:~-4,4%%date:~-7,2%%date:~-10,2%.apk"
```

---

## 🚨 Dépannage

### Erreur: keytool command not found

**Solution**:
```bash
# Windows - Trouver keytool
where keytool

# Si non trouvé, ajouter au PATH
set PATH=%PATH%;C:\Program Files\Java\jdk-17\bin
```

### Erreur: keystore was tampered with, or password was incorrect

**Cause**: Mauvais mot de passe

**Solution**:
- Vérifier le mot de passe dans vos notes
- Si perdu, **impossible de récupérer** → créer nouveau keystore (mais ne pourra pas mettre à jour app existante)

### Erreur: Gradle signing failed

**Vérifications**:
1. Le fichier `keystore.properties` existe bien
2. Le chemin vers `ptms-release-key.jks` est correct
3. Les mots de passe sont corrects (sans espaces)
4. Le fichier `.jks` n'est pas corrompu

### Erreur: jarsigner not found

**Solution**:
```bash
# Même dossier que keytool
set PATH=%PATH%;C:\Program Files\Java\jdk-17\bin
```

---

## 📋 Checklist Finale

### Avant Production

- [ ] Keystore créé avec validité 10000 jours
- [ ] Mot de passe fort et documenté
- [ ] 3 sauvegardes du keystore (locale, cloud, physique)
- [ ] `keystore.properties` créé et configuré
- [ ] `keystore.properties` et `*.jks` dans `.gitignore`
- [ ] `build.gradle` configuré avec `signingConfig`
- [ ] Build release réussi
- [ ] APK signé vérifié avec `jarsigner -verify`
- [ ] Certificat vérifié avec `keytool -printcert`
- [ ] Tests fonctionnels sur APK release OK
- [ ] Taille APK réduite (~4.9 MB)
- [ ] APK copié vers `uploads/apk/`

### Sécurité

- [ ] Keystore **non committé** dans Git
- [ ] `keystore.properties` **non committé** dans Git
- [ ] Mot de passe stocké dans gestionnaire sécurisé
- [ ] Sauvegardes testées (restauration possible)
- [ ] Accès au keystore restreint (permissions fichier)

---

## 🔐 Bonnes Pratiques

### DO ✅

1. **Sauvegarder le keystore dans 3+ endroits sécurisés**
2. **Utiliser un mot de passe fort** (12+ caractères, complexe)
3. **Documenter toutes les informations** (alias, mots de passe, dates)
4. **Tester la restauration des sauvegardes**
5. **Restreindre l'accès** au keystore (permissions, chiffrement)
6. **Utiliser le même keystore** pour toutes les versions futures
7. **Vérifier la signature** après chaque build

### DON'T ❌

1. **NE JAMAIS** commiter le keystore dans Git
2. **NE JAMAIS** partager le mot de passe en clair
3. **NE JAMAIS** utiliser un mot de passe faible
4. **NE JAMAIS** créer un nouveau keystore pour une mise à jour (impossible de publier)
5. **NE JAMAIS** stocker sur serveur web accessible
6. **NE JAMAIS** envoyer par email non chiffré
7. **NE JAMAIS** oublier de sauvegarder (perte = catastrophe)

---

## 📞 Support & Ressources

### Documentation Android

- [Signing Your App](https://developer.android.com/studio/publish/app-signing)
- [Generate Upload Key](https://developer.android.com/studio/publish/app-signing#generate-key)
- [Keytool Documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)

### En Cas de Problème

**Perte du Keystore**:
- ⚠️ **Impossible de mettre à jour l'app** si déjà publiée
- Solution: Créer nouveau keystore et publier comme nouvelle app (perd utilisateurs)

**Oubli du Mot de Passe**:
- ⚠️ **Impossible de récupérer** - aucune backdoor
- Solution: Même problème que perte du keystore

**Keystore Corrompu**:
- Restaurer depuis sauvegarde
- Si aucune sauvegarde: même problème que perte

**⚠️ C'EST POURQUOI LES SAUVEGARDES SONT CRITIQUES ⚠️**

---

## 📊 Résumé Visuel

```
┌─────────────────────────────────────────────────────────┐
│         PROCESSUS DE SIGNATURE PRODUCTION               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. [Créer Keystore]                                    │
│       ↓                                                 │
│  2. [Documenter Infos] → [Gestionnaire MdP]            │
│       ↓                                                 │
│  3. [Sauvegarder × 3] → [Locale, Cloud, USB]           │
│       ↓                                                 │
│  4. [Configurer build.gradle] + [keystore.properties]  │
│       ↓                                                 │
│  5. [gradlew assembleRelease]                           │
│       ↓                                                 │
│  6. [Vérifier Signature] → jarsigner -verify            │
│       ↓                                                 │
│  7. [Tests Fonctionnels] → APK release                  │
│       ↓                                                 │
│  8. [Copier vers uploads/apk/]                          │
│       ↓                                                 │
│  ✅ [PRÊT POUR PRODUCTION]                              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

**Créé le**: 2025-10-23
**Version**: 1.0
**Pour**: PTMS Mobile v2.0.1
**Status**: Guide complet
**Prochaine étape**: Exécuter les commandes pour créer le keystore

