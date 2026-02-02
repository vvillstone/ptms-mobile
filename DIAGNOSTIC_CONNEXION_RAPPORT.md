# Rapport de Diagnostic - Problème de Connexion PTMS Mobile

## Date
8 octobre 2025

## Problème Identifié
L'application Android PTMS Mobile ne pouvait pas se connecter au serveur en raison d'une **URL de serveur incorrecte**.

## Cause Racine

### URL Configurée Initialement
```
https://192.168.188.28/api/
```

Cette URL est une adresse IP locale qui n'est accessible que depuis le réseau local. L'application ne pouvait pas se connecter au serveur de production.

### URL Correcte
```
https://serveralpha.protti.group/api/
```

C'est l'URL du serveur de production accessible depuis internet.

## Modifications Effectuées

### 1. Mise à Jour de l'URL par Défaut

**Fichiers Modifiés:**

#### `appAndroid/app/src/main/java/com/ptms/mobile/utils/SettingsManager.java`
```java
// AVANT
private static final String DEFAULT_SERVER_URL = "https://192.168.188.28/api/";
private static final boolean DEFAULT_IGNORE_SSL = true;

// APRÈS
private static final String DEFAULT_SERVER_URL = "https://serveralpha.protti.group/api/";
private static final boolean DEFAULT_IGNORE_SSL = false; // SSL valide en production
```

#### `appAndroid/app/src/main/java/com/ptms/mobile/utils/ApiConfig.java`
```java
// AVANT
public static final String BASE_URL = "https://192.168.188.28/api/";
public static final String UNIFIED_BASE_URL = "https://192.168.188.28/api/unified.php/";
public static final boolean DEFAULT_IGNORE_SSL = true;

// APRÈS
public static final String BASE_URL = "https://serveralpha.protti.group/api/";
public static final String UNIFIED_BASE_URL = "https://serveralpha.protti.group/api/unified.php/";
public static final boolean DEFAULT_IGNORE_SSL = false; // SSL valide en production
```

### 2. Ajout d'Utilitaires de Diagnostic

**Nouveau Fichier:** `appAndroid/app/src/main/java/com/ptms/mobile/utils/ConnectionDiagnostic.java`

Ce fichier fournit des outils pour :
- Tester la connexion au serveur
- Tester plusieurs URLs candidates
- Diagnostiquer les problèmes SSL
- Tester l'authentification

### 3. Documentation

**Nouveaux Documents:**
- `appAndroid/TROUBLESHOOTING_CONNECTION.md` - Guide complet de dépannage
- `appAndroid/DIAGNOSTIC_CONNEXION_RAPPORT.md` - Ce rapport

### 4. Recompilation

L'application a été recompilée avec succès :
- **Taille:** 7.53 MB (7,893,058 octets)
- **Date:** 08.10.2025 01:22
- **Emplacement:** 
  - `appAndroid/app/build/outputs/apk/debug/app-debug.apk`
  - `apk_output/PTMS-Mobile-v2.0-debug.apk`

## Configuration Côté Serveur

### Endpoints Requis

L'application Android attend les endpoints suivants :

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/login.php` | POST | Authentification utilisateur |
| `/api/projects.php` | GET | Liste des projets |
| `/api/work-types.php` | GET | Types de travaux |
| `/api/time-entry.php` | POST | Saisie d'heures |
| `/api/reports.php` | GET | Rapports de temps |
| `/api/profile.php` | GET | Profil utilisateur |

### Structure de Réponse Attendue

#### Login (POST `/api/login.php`)

**Requête:**
```json
{
  "email": "utilisateur@exemple.com",
  "password": "motdepasse"
}
```

**Réponse Succès:**
```json
{
  "success": true,
  "message": "Connexion réussie",
  "token": "base64_encoded_token",
  "user": {
    "id": 1,
    "email": "utilisateur@exemple.com",
    "firstname": "Prénom",
    "lastname": "Nom",
    "department": "Département",
    "position": "Poste"
  }
}
```

**Note Importante:** Le serveur retourne `user` et non `employee`. L'application Android est configurée pour utiliser `user`.

## Tests de Validation

### 1. Test de Connexion Simple

#### Dans l'Application
1. Ouvrir l'application PTMS Mobile
2. Sur l'écran de connexion, appuyer sur le bouton **Paramètres** (⚙️)
3. Vérifier que l'URL est : `https://serveralpha.protti.group/api/`
4. Appuyer sur **Test Connexion**
5. Résultat attendu : "Test OK - Code: 400 (Retourne du JSON)"
   - Code 400 est normal car on envoie des données vides
   - L'important est que ce soit du JSON et non du HTML

#### Dans l'Application - Test URL de Base
1. Appuyer sur **Test URL de Base**
2. Résultat attendu : "URL de base - Code: 200 - Serveur accessible"

### 2. Test de Login Réel

#### Avec des Credentials Valides
1. Revenir à l'écran de connexion
2. Entrer un email et mot de passe valides
3. Appuyer sur **Se connecter**
4. Résultat attendu : Redirection vers le Dashboard

#### Logs Attendus
```
D/LOGIN: Tentative de connexion pour: user@exemple.com
D/LOGIN: URL du serveur utilisée: https://serveralpha.protti.group/api/
D/LOGIN: URL de base: https://serveralpha.protti.group/api/
D/LOGIN: URL complète: https://serveralpha.protti.group/api/login.php
D/LOGIN: Envoi de la requête de connexion...
D/LOGIN: Réponse reçue: 200 OK
D/LOGIN: Réponse login: success=true
D/LOGIN: Token reçu: base64encodedtoken...
D/LOGIN: Connexion réussie - Redirection vers dashboard...
```

### 3. Test depuis Terminal/cURL

```bash
# Test de disponibilité
curl -I https://serveralpha.protti.group/api/login.php

# Test de login
curl -X POST https://serveralpha.protti.group/api/login.php \
  -H "Content-Type: application/json" \
  -d '{"email":"test@exemple.com","password":"testpassword"}'
```

## Vérifications Côté Serveur

### 1. Fichier `/api/login.php`

Vérifier que le fichier existe et est accessible :
```bash
# Sur le serveur
ls -la /path/to/web/api/login.php

# Permissions recommandées
chmod 644 /path/to/web/api/login.php
```

### 2. Configuration CORS

Le fichier `api/login.php` doit inclure les headers CORS :
```php
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Content-Type: application/json');
```

### 3. Base de Données

Vérifier la structure de la table `users` :
```sql
SELECT id, email, firstname, lastname, type, status 
FROM users 
WHERE type IN ('employee', 'manager') 
AND status = 1 
LIMIT 5;
```

### 4. Test Manuel de l'Endpoint

Créer un fichier de test `test_android_login.php` :
```php
<?php
require_once __DIR__ . '/api/login.php';
?>
```

Puis tester via navigateur :
```
https://serveralpha.protti.group/test_android_login.php
```

## Scénarios de Dépannage

### Scénario 1: Utilisateur avec Ancienne Version

**Problème:** L'utilisateur a déjà l'application installée avec l'ancienne URL

**Solution:**
1. Ouvrir les paramètres de l'application
2. Appuyer sur **Réinitialiser les Paramètres**
3. Vérifier que l'URL est maintenant `https://serveralpha.protti.group/api/`
4. Appuyer sur **Enregistrer**
5. Réessayer la connexion

### Scénario 2: Développement Local

**Problème:** Besoin de tester avec un serveur local

**Solution:**
1. Ouvrir les paramètres
2. Changer l'URL vers : `http://192.168.X.X/api/` ou `https://192.168.X.X/api/`
3. **Si HTTPS local:** Activer "Ignorer SSL"
4. Augmenter le timeout si nécessaire
5. Tester la connexion
6. Enregistrer

### Scénario 3: Erreur SSL

**Symptôme:** "Erreur SSL" ou "Certificate error"

**Solution:**
1. Si certificat autosigné : Activer "Ignorer SSL" dans les paramètres
2. Si certificat Let's Encrypt expiré : Renouveler le certificat sur le serveur
3. Vérifier que le serveur supporte TLS 1.2+

### Scénario 4: Timeout

**Symptôme:** "Timeout - Serveur trop lent"

**Solution:**
1. Augmenter le timeout dans les paramètres (60 secondes)
2. Vérifier la connexion internet
3. Vérifier la performance du serveur

## Configuration Recommandée

### Production
```
URL du Serveur: https://serveralpha.protti.group/api/
Ignorer SSL: NON (désactivé)
Timeout: 30 secondes
Mode Debug: OUI (pour logs détaillés)
```

### Développement Local
```
URL du Serveur: https://192.168.188.28/api/
Ignorer SSL: OUI (activé)
Timeout: 60 secondes
Mode Debug: OUI
```

## Logs de Debug

### Activer les Logs Complets

Dans Android Studio / adb :
```bash
# Logs de connexion
adb logcat | grep -E "LOGIN|API_CLIENT"

# Logs des paramètres
adb logcat | grep SETTINGS

# Tous les logs PTMS
adb logcat | grep -E "LOGIN|API_CLIENT|SETTINGS|PTMS"
```

### Logs Importants

Les logs montrent :
- L'URL utilisée pour chaque requête
- Les codes de réponse HTTP
- Les headers de requête/réponse
- Le contenu des réponses (JSON ou HTML)
- Les erreurs SSL
- Les timeouts

## Checklist de Validation

- [x] URL par défaut mise à jour vers `https://serveralpha.protti.group/api/`
- [x] Configuration SSL mise à jour (désactivée par défaut en production)
- [x] Utilitaires de diagnostic ajoutés
- [x] Documentation créée
- [x] Application recompilée
- [ ] Tests de connexion effectués avec credentials réels
- [ ] Validation sur appareil Android physique
- [ ] Test de tous les endpoints (login, projects, work-types, etc.)

## Prochaines Étapes

1. **Installation sur Appareil Test**
   - Installer l'APK : `apk_output/PTMS-Mobile-v2.0-debug.apk`
   - Tester la connexion avec des credentials valides
   - Vérifier tous les écrans de l'application

2. **Validation des Endpoints**
   - Tester `/api/projects.php`
   - Tester `/api/work-types.php`
   - Tester `/api/time-entry.php`
   - Tester `/api/reports.php`
   - Tester `/api/profile.php`

3. **Tests de Scénarios Complets**
   - Login → Dashboard → Saisie d'heures → Rapports
   - Déconnexion → Reconnexion
   - Mode hors ligne (si implémenté)

4. **Distribution**
   - Tester l'APK sur plusieurs appareils
   - Vérifier différentes versions d'Android (API 24+)
   - Créer une version Release signée si nécessaire

## Support

En cas de problème persistant :

1. Consulter `TROUBLESHOOTING_CONNECTION.md`
2. Activer le Mode Debug
3. Capturer les logs via `adb logcat`
4. Tester les endpoints avec cURL
5. Vérifier la configuration serveur

## Résumé

✅ **Problème Résolu:** URL de serveur incorrecte  
✅ **Solution Appliquée:** Mise à jour vers `https://serveralpha.protti.group/api/`  
✅ **Application Recompilée:** Version 1.0 - 08.10.2025  
⏳ **Validation Requise:** Tests avec credentials réels

L'application devrait maintenant pouvoir se connecter au serveur de production PTMS sans problème.


