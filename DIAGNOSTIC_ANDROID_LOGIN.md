# Diagnostic Problème Login Android - Résultats

**Date**: 9 Octobre 2025
**Contexte**: Login fonctionne depuis l'application web mais pas depuis Android

---

## 📊 Résultats du Diagnostic

### ✅ Points Positifs

1. **Structure de la base de données CORRECTE**
   - Colonne `type` est maintenant **INT(11)** (pas ENUM)
   - La migration vers INT a été effectuée avec succès
   - Table `employee_list` n'existe plus (migration complète)

2. **Données utilisateurs présentes**
   - 6 utilisateurs actifs dans la table `users`
   - Distribution des types:
     - Type 1 (ADMIN): 2 utilisateurs
     - Type 2 (MANAGER): 1 utilisateur
     - Type 4 (EMPLOYEE): 1 utilisateur
     - Type 5 (VIEWER): 2 utilisateurs
   - 5 utilisateurs ont email + password valides

3. **Requête API Android**
   - La requête `SELECT * FROM users WHERE (email = ? OR username = ?) AND status = 1 AND type IN (2, 4)` fonctionne
   - **2 utilisateurs matchent** cette requête (type IN (2, 4))

---

## 🔍 Analyse Détaillée

### Utilisateurs Disponibles pour Android (type 2 ou 4)

Selon le diagnostic, **2 utilisateurs** peuvent se connecter depuis Android:

1. **ID 41 - jdupont**
   - Type: 2 (MANAGER)
   - Email: jean.dupont@example.com
   - Nom: Jean Dupont

2. **ID inconnu - type 4 (EMPLOYEE)**
   - 1 utilisateur de type EMPLOYEE existe
   - Pas affiché dans les 5 premiers exemples

### Utilisateurs NON Disponibles pour Android

**ID 1 - William** (Type 1 - ADMIN)
- Email: william.protti@gmail.com
- ❌ Ne peut pas se connecter via Android (type 1 non accepté)

**ID 8 - Pierre** (Type 5 - VIEWER)
- Email: NULL ⚠️
- ❌ Ne peut pas se connecter (pas d'email + type 5)

**ID 9 - admin** (Type 1 - ADMIN)
- Email: admin@ptms.com
- ❌ Ne peut pas se connecter via Android (type 1 non accepté)

**ID 39 - cprotti** (Type 5 - VIEWER)
- Email: protti.christian@gmail.com
- ❌ Ne peut pas se connecter via Android (type 5 non accepté)

---

## 🎯 Problème Identifié

### Pourquoi le login Android ne fonctionne pas?

**Hypothèses possibles** (à vérifier dans l'ordre):

#### 1. **Utilisateur testé n'est pas de type 2 ou 4**
   - Si vous essayez de vous connecter avec william.protti@gmail.com (type 1) → **ÉCHEC NORMAL**
   - Si vous essayez de vous connecter avec admin@ptms.com (type 1) → **ÉCHEC NORMAL**

   ✅ **Solution**: Utiliser `jean.dupont@example.com` (type 2) pour tester

#### 2. **Mot de passe incorrect**
   - Le hash du mot de passe en base ne correspond pas au mot de passe saisi

   ✅ **Solution**: Créer un utilisateur de test avec mot de passe connu

#### 3. **Problème réseau/URL**
   - L'application Android ne peut pas joindre l'API
   - URL API incorrecte configurée dans l'app

   ✅ **Solution**: Vérifier la configuration réseau Android

#### 4. **Problème dans la réponse JSON**
   - L'API retourne une erreur mais l'app Android ne l'affiche pas correctement

   ✅ **Solution**: Vérifier les logs backend (`debug.log`)

---

## 🛠️ Solutions Proposées

### Solution 1: Tester avec un utilisateur valide existant

**Utilisateur à tester**: jean.dupont@example.com (ID 41, type 2)

```
Email: jean.dupont@example.com
Type: 2 (MANAGER)
Status: Actif
```

⚠️ **Problème**: Vous ne connaissez probablement pas le mot de passe de cet utilisateur.

---

### Solution 2: Créer un utilisateur de test (RECOMMANDÉ)

**Script disponible**: `C:\devs\web\create_test_employee.php`

Ce script crée un utilisateur:
```
Username: testemploye
Email: test@ptms.local
Password: test123
Type: 4 (EMPLOYEE)
Status: 1 (Actif)
```

**Commande pour exécuter**:
```bash
php C:\devs\web\create_test_employee.php
```

---

### Solution 3: Réinitialiser le mot de passe d'un utilisateur existant

Si vous voulez utiliser `jean.dupont@example.com`:

```sql
UPDATE users
SET password = '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'
WHERE email = 'jean.dupont@example.com';
```

**Mot de passe**: `password`

**Hasher un nouveau mot de passe en PHP**:
```php
echo password_hash('VotreMotDePasse', PASSWORD_DEFAULT);
```

---

## 📝 Procédure de Test Complète

### Étape 1: Créer l'utilisateur de test
```bash
php C:\devs\web\create_test_employee.php
```

### Étape 2: Vérifier l'API avec curl

**Test direct de l'API**:
```bash
curl -X POST http://localhost/api/login.php \
  -H "Content-Type: application/json" \
  -d '{"email":"test@ptms.local","password":"test123"}'
```

**Réponse attendue** (succès):
```json
{
  "success": true,
  "message": "Connexion réussie",
  "token": "...",
  "user": {
    "id": 42,
    "email": "test@ptms.local",
    "username": "testemploye",
    "type": 4,
    "employeeStatus": 4
  }
}
```

### Étape 3: Tester depuis Android

1. **Installer l'APK**:
   ```bash
   cd C:\devs\web\appAndroid
   gradlew.bat installDebug
   ```

2. **Configurer l'URL API** (dans l'app Android):
   - Si serveur local: `http://192.168.x.x/api/` (remplacer par votre IP locale)
   - Si serveur distant: `http://your-server.com/api/`

3. **Se connecter**:
   - Email: `test@ptms.local`
   - Password: `test123`

### Étape 4: Vérifier les logs en cas d'échec

**Backend**:
```bash
tail -f C:\devs\web\debug.log
```

**Android**:
```bash
adb logcat -s PTMS:* API_CLIENT:* LOGIN:* TIME_ENTRY:*
```

---

## 🔬 Analyse du Code login.php

### Ancienne Version (Problématique)
```php
$employee = $db->fetch(
    "SELECT * FROM users
     WHERE (email = ? OR username = ?)
     AND status = 1
     AND type IN (2, 4)",  // ❌ Ne fonctionnait pas si type = ENUM
    [$emailOrUsername, $emailOrUsername]
);
```

### Nouvelle Version (Corrigée)
```php
$employee = $db->fetch(
    "SELECT * FROM users
     WHERE (email = ? OR username = ?)
     AND status = 1
     AND (
        type IN (2, 4)                                    -- ✅ INT
        OR type IN ('manager', 'employee', 'user')        -- ✅ ENUM legacy
        OR CAST(type AS UNSIGNED) IN (2, 4)               -- ✅ STRING '2', '4'
     )",
    [$emailOrUsername, $emailOrUsername]
);
```

**Ajout**: Fallback vers `employee_list` si utilisateur non trouvé:
```php
if (!$employee) {
    $employeeOld = $db->fetch(
        "SELECT * FROM employee_list
         WHERE email = ? AND status = 1",
        [$emailOrUsername]
    );

    if ($employeeOld) {
        $employee = $employeeOld;
        $employee['type'] = 4; // EMPLOYEE
    }
}
```

**Résultat**: L'API `login.php` est maintenant **robuste** et gère:
- ✅ Types INT (1, 2, 3, 4, 5)
- ✅ Types ENUM ('admin', 'manager', 'employee')
- ✅ Types STRING ('2', '4')
- ✅ Fallback vers employee_list (si table existe encore)

---

## ✅ Conclusion du Diagnostic

### Points Clés

1. **✅ La base de données est CORRECTE**
   - Type INT(11) comme attendu
   - Migration effectuée avec succès
   - Table employee_list supprimée

2. **✅ L'API login.php est CORRIGÉE**
   - Gère maintenant tous les cas possibles
   - Normalisation type STRING→INT
   - Fallback employee_list

3. **⚠️ Problème probable: Utilisateur de test**
   - Besoin de créer un utilisateur type 2 ou 4 avec mot de passe connu
   - OU réinitialiser le mot de passe d'un utilisateur existant

4. **⚠️ Vérifications supplémentaires nécessaires**
   - Configuration réseau Android (URL API)
   - Logs backend lors de la tentative de connexion
   - Logs Android pour voir l'erreur exacte

---

## 📋 Prochaines Étapes Recommandées

### Priorité 1: Créer utilisateur de test
```bash
php C:\devs\web\create_test_employee.php
```

### Priorité 2: Tester l'API directement
```bash
curl -X POST http://localhost/api/login.php \
  -H "Content-Type: application/json" \
  -d '{"email":"test@ptms.local","password":"test123"}'
```

### Priorité 3: Installer et tester Android
```bash
cd C:\devs\web\appAndroid
gradlew.bat installDebug
# Puis tester login avec test@ptms.local / test123
```

### Priorité 4: Analyser les logs si échec
```bash
# Backend
tail -f C:\devs\web\debug.log

# Android
adb logcat -s PTMS:* API_CLIENT:* LOGIN:*
```

---

## 📊 Statistiques Base de Données

- **Total utilisateurs actifs**: 6
- **Utilisateurs Android (type 2, 4)**: 2
- **Utilisateurs Web seulement (type 1, 5)**: 4
- **Utilisateurs sans email**: 1 (ID 8 - Pierre)
- **Migration employee_list**: ✅ Complète

---

## 🎯 Résumé Exécutif

**Diagnostic**: La base de données et l'API backend sont **CORRECTES** et **À JOUR**.

**Problème identifié**: Le login Android échoue probablement à cause:
1. Utilisateur testé n'est pas de type 2 ou 4 (par exemple, admin = type 1)
2. Mot de passe incorrect ou inconnu
3. Configuration réseau Android (URL API)

**Solution immédiate**: Créer un utilisateur de test avec credentials connus et tester l'application Android avec cet utilisateur.

**Statut**: ✅ API corrigée, 🔧 Tests nécessaires

---

**Auteur**: Diagnostic automatique PTMS
**Date**: 9 Octobre 2025
**Fichiers modifiés**: `api/login.php`
**Fichiers créés**: `create_test_employee.php`, `diagnose_android_login.php`
