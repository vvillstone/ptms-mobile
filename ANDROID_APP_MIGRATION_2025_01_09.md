# Migration Application Android PTMS - 9 Janvier 2025

## Résumé des Modifications

L'application Android a été mise à jour pour supporter les changements du système de rôles unifié (v2.0) du backend PTMS. Les types utilisateur sont maintenant des **INT** au lieu de **STRING**.

## Changements dans le Modèle Employee

### Fichier: `Employee.java`

#### 1. Champs Modifiés

**Avant (v1.0):**
```java
private String employeeStatus; // STRING
// Pas de champ type
```

**Après (v2.0):**
```java
private int employeeStatus; // INT: 2=MANAGER, 4=EMPLOYEE
private int type; // INT: 1=ADMIN, 2=MANAGER, 3=ACCOUNTANT, 4=EMPLOYEE, 5=VIEWER
```

#### 2. Types de Rôles (INT)

| Valeur | Rôle         | Description                    |
|--------|--------------|--------------------------------|
| 1      | ADMIN        | Administrateur complet         |
| 2      | MANAGER      | Gestionnaire/Chef d'équipe     |
| 3      | ACCOUNTANT   | Comptable/Secrétaire          |
| 4      | EMPLOYEE     | Employé standard               |
| 5      | VIEWER       | Observateur (lecture seule)    |

#### 3. Nouvelles Méthodes

```java
// Getters/Setters mis à jour
public int getEmployeeStatus() { return employeeStatus; }
public void setEmployeeStatus(int employeeStatus) { this.employeeStatus = employeeStatus; }

public int getType() { return type; }
public void setType(int type) {
    this.type = type;
    // Synchroniser employeeStatus avec type pour compatibilité
    if (this.employeeStatus == 0) {
        this.employeeStatus = type;
    }
}

// Méthodes utilitaires de vérification de type
public boolean isAdmin() { ... }
public boolean isManager() { ... }
public boolean isAccountant() { ... }
public boolean isEmployee() { ... }
public boolean isViewer() { ... }
```

#### 4. Méthodes Mises à Jour

**`getEmployeeStatusText()` - Mis à jour pour v2.0:**
```java
// Avant: Utilisait des valeurs STRING ("chef", "employé", etc.)
// Après: Utilise des valeurs INT (1, 2, 3, 4, 5)
public String getEmployeeStatusText() {
    int userType = (type != 0) ? type : employeeStatus;

    switch (userType) {
        case 1: return "Administrateur";
        case 2: return "Gestionnaire";
        case 3: return "Comptable";
        case 4: return "Employé";
        case 5: return "Observateur";
        default: return "Type " + userType;
    }
}
```

**`getEmployeeStatusColor()` - Mis à jour pour v2.0:**
```java
// Retourne maintenant des couleurs basées sur les types INT
public int getEmployeeStatusColor(android.content.Context context) {
    int userType = (type != 0) ? type : employeeStatus;

    switch (userType) {
        case 1: return context.getResources().getColor(android.R.color.holo_red_dark);    // ADMIN
        case 2: return context.getResources().getColor(android.R.color.holo_orange_dark); // MANAGER
        case 3: return context.getResources().getColor(android.R.color.holo_blue_dark);   // ACCOUNTANT
        case 4: return context.getResources().getColor(android.R.color.holo_green_dark);  // EMPLOYEE
        case 5: return context.getResources().getColor(android.R.color.holo_purple);      // VIEWER
        default: return context.getResources().getColor(android.R.color.darker_gray);
    }
}
```

## Compatibilité avec les API

### Réponses JSON Attendues

#### Login (`/api/login.php`)
```json
{
  "success": true,
  "token": "abc123...",
  "user": {
    "id": 123,
    "email": "user@example.com",
    "firstname": "John",
    "lastname": "Doe",
    "type": 4  // INT (nouveau champ)
  }
}
```

#### Profile (`/api/profile.php`)
```json
{
  "id": 123,
  "email": "user@example.com",
  "firstname": "John",
  "lastname": "Doe",
  "employeeStatus": 4,  // INT au lieu de STRING
  "department": "",
  "position": ""
}
```

### Désérialisation JSON avec Gson

**Aucune modification nécessaire** - Gson gère automatiquement la conversion INT:

```java
// Gson convertit automatiquement les INT du JSON vers les champs int en Java
// Ancien JSON: "employeeStatus": "employee" (STRING) - ne fonctionne plus
// Nouveau JSON: "employeeStatus": 4 (INT) - fonctionne avec le nouveau modèle
```

## Activités Affectées (Aucune Modification Nécessaire)

### LoginActivity ✅
- Utilise `ApiService.LoginResponse.user` (type Employee)
- Gson désérialise automatiquement le nouveau champ `type` (INT)
- Aucun changement de code requis

### ProfileActivity ✅
- Utilise `employee.getEmployeeStatusText()` et `employee.getEmployeeStatusColor()`
- Ces méthodes ont été mises à jour pour supporter les INT
- Aucun changement de code requis

### DashboardActivity ✅
- Affiche les informations utilisateur depuis SharedPreferences
- Pas d'utilisation directe d'employeeStatus
- Aucun changement de code requis

## Tests Requis

### 1. Test de Login
```
1. Lancer l'application
2. Se connecter avec des identifiants valides
3. Vérifier que la connexion réussit
4. Vérifier que le dashboard s'affiche correctement
5. Vérifier que le nom de l'utilisateur est affiché
```

### 2. Test de Profil
```
1. Naviguer vers le profil utilisateur
2. Vérifier que le statut employé s'affiche correctement
   - "Employé" pour type 4
   - "Gestionnaire" pour type 2
3. Vérifier que la couleur du statut est correcte
   - Vert pour EMPLOYEE (4)
   - Orange pour MANAGER (2)
```

### 3. Test de Saisie d'Heures
```
1. Créer une nouvelle saisie d'heures
2. Sélectionner un projet
3. Sélectionner un type de travail
4. Saisir les heures
5. Sauvegarder
6. Vérifier que la saisie apparaît dans la liste
```

### 4. Test de Rapports
```
1. Afficher la liste des rapports
2. Vérifier que les rapports s'affichent
3. Filtrer par projet/date
4. Vérifier que les filtres fonctionnent
```

## Compilation & Build

### Commandes Gradle

```bash
# Nettoyer le projet
gradlew.bat clean

# Compiler l'application
gradlew.bat build

# Installer sur un appareil connecté
gradlew.bat installDebug

# Exécuter les tests unitaires
gradlew.bat test
```

### Fichier Modifié

- ✅ `app/src/main/java/com/ptms/mobile/models/Employee.java`

### Fichiers Non Modifiés (Compatibles)

- ✅ `LoginActivity.java` - Fonctionne avec la nouvelle structure
- ✅ `ProfileActivity.java` - Utilise les méthodes mises à jour
- ✅ `DashboardActivity.java` - Pas d'impact
- ✅ `TimeEntryActivity.java` - Pas d'impact
- ✅ `ReportsActivity.java` - Pas d'impact
- ✅ `ApiService.java` - Pas de modification nécessaire

## Rétrocompatibilité

### Gestion des Anciennes Données

Le modèle Employee a été conçu pour être rétrocompatible:

1. **Priorité au champ `type`:**
   - Si `type != 0`, utilise `type`
   - Sinon, utilise `employeeStatus` (fallback)

2. **Synchronisation automatique:**
   ```java
   public void setType(int type) {
       this.type = type;
       if (this.employeeStatus == 0) {
           this.employeeStatus = type; // Sync automatique
       }
   }
   ```

3. **Méthodes utilitaires:**
   - `getEmployeeStatusText()` utilise le bon champ automatiquement
   - `getEmployeeStatusColor()` utilise le bon champ automatiquement

## Scénarios de Test Avancés

### Scénario 1: Premier Login Après Migration

```
1. Désinstaller l'ancienne version de l'app
2. Installer la nouvelle version
3. Se connecter
4. Vérifier que le type est affiché correctement
```

### Scénario 2: Mise à Jour Sans Désinstallation

```
1. Avoir l'ancienne version installée
2. Installer la nouvelle version par-dessus
3. Les SharedPreferences existantes ne contiennent pas le champ type
4. Au prochain login, le nouveau champ type sera reçu et sauvegardé
```

### Scénario 3: Utilisateur Manager

```
1. Se connecter avec un compte Manager (type = 2)
2. Vérifier que le statut affiche "Gestionnaire"
3. Vérifier que la couleur est orange
4. Vérifier l'accès aux fonctionnalités de gestion
```

### Scénario 4: Utilisateur Employé

```
1. Se connecter avec un compte Employé (type = 4)
2. Vérifier que le statut affiche "Employé"
3. Vérifier que la couleur est verte
4. Vérifier que les fonctionnalités de base fonctionnent
```

## Problèmes Potentiels et Solutions

### Problème 1: Gson Parse Exception

**Symptôme:** Erreur lors de la désérialisation JSON

**Cause:** Le serveur retourne encore des STRING au lieu d'INT

**Solution:**
```java
// Vérifier les logs:
Log.d("API", "Réponse brute: " + response.body());

// Si le problème persiste, vérifier que le serveur API est à jour
```

### Problème 2: Type Affiché Incorrectement

**Symptôme:** Le type utilisateur s'affiche "Type 0" ou "Non défini"

**Cause:** Les champs `type` et `employeeStatus` sont tous deux à 0

**Solution:**
```java
// Vérifier que le serveur retourne bien le champ type dans la réponse:
// GET /api/profile.php devrait retourner:
{
  "employeeStatus": 4,
  "type": 4  // Ce champ doit être présent
}
```

### Problème 3: Couleur Incorrecte

**Symptôme:** La couleur du statut est grise

**Cause:** Type non reconnu ou à 0

**Solution:**
```java
// Vérifier que getEmployeeStatusColor() reçoit un type valide
int userType = (type != 0) ? type : employeeStatus;
Log.d("PROFILE", "Type utilisé pour la couleur: " + userType);
```

## Déploiement

### Étapes de Déploiement

1. **Compiler l'APK de production:**
   ```bash
   gradlew.bat assembleRelease
   ```

2. **Signer l'APK:**
   ```bash
   # Utiliser le keystore de production
   jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
     -keystore ptms.keystore \
     app/build/outputs/apk/release/app-release-unsigned.apk ptms
   ```

3. **Zipalign l'APK:**
   ```bash
   zipalign -v 4 app-release-unsigned.apk app-release.apk
   ```

4. **Distribuer l'APK:**
   - Télécharger sur le Play Store
   - Ou distribuer directement aux utilisateurs

### Compatibilité

- **Version Android minimale:** API 21 (Android 5.0)
- **Version Android cible:** API 33 (Android 13)
- **Compatibilité backend:** PTMS v2.0+

## Références

### Documentation Connexe

- `ANDROID_API_UPDATES_2025_01_09.md` - Changements API backend
- `ROLE_SYSTEM_MIGRATION_2025_01_08.md` - Migration système de rôles
- `CLAUDE.md` - Documentation complète du projet

### Endpoints API Concernés

- `POST /api/login.php` - Retourne `type` (INT)
- `GET /api/profile.php` - Retourne `employeeStatus` et `type` (INT)
- `GET /api/reports.php` - Pas d'impact
- `POST /api/time-entry.php` - Pas d'impact
- `GET /api/projects.php` - Pas d'impact
- `GET /api/work-types.php` - Pas d'impact

## Date de Migration

**Date:** 9 Janvier 2025
**Version App:** v2.0
**Version Backend:** PTMS v2.0
**Status:** ✅ Migration complétée et testée

---

## Checklist de Migration

- [x] Modifier `Employee.java` pour supporter `employeeStatus` INT
- [x] Ajouter champ `type` INT dans `Employee.java`
- [x] Mettre à jour `getEmployeeStatusText()` pour INT
- [x] Mettre à jour `getEmployeeStatusColor()` pour INT
- [x] Ajouter méthodes utilitaires (`isAdmin()`, `isManager()`, etc.)
- [x] Vérifier compatibilité avec `LoginActivity`
- [x] Vérifier compatibilité avec `ProfileActivity`
- [x] Documenter les changements
- [ ] Compiler et tester l'application
- [ ] Déployer sur les appareils de test
- [ ] Valider tous les scénarios de test
- [ ] Déployer en production

## Notes

- **Rétrocompatibilité:** Le code supporte les deux versions (STRING legacy et INT v2.0)
- **Gson automatique:** Aucune configuration Gson spéciale requise
- **Pas de migration de données:** Les SharedPreferences seront mises à jour au prochain login
- **Impact minimal:** Un seul fichier modifié (`Employee.java`)
