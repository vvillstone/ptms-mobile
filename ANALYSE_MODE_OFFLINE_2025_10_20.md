# Analyse - Mode Offline et Problèmes de Déconnexion

**Date:** 20 Octobre 2025
**Problèmes Identifiés:**
1. Mode offline/online non mis à jour partout
2. Impossible de se reconnecter après déconnexion en mode offline

---

## 🔍 Question 1: Mode Offline/Online mis à jour partout?

### Détection du Mode Offline/Online

**Mécanisme actuel:**
- `NetworkUtils.isOnline(context)` - Vérifie la connectivité réseau via `ConnectivityManager`
- `OfflineSyncManager.isOnline()` - Wrapper qui utilise `ConnectivityManager`
- Détection **en temps réel** selon l'état du réseau

**Fichiers utilisant la détection:**
- `AllNotesActivity.java` - `syncManager.isOnline()`
- `LoadingActivity.java` - Probablement NetworkUtils
- `TimeEntryActivity.java` - Vérification réseau
- `DashboardActivity.java` - État de connexion
- Et **23 autres fichiers**

### ⚠️ Problème: Pas de Mise à Jour Automatique

**Comportement actuel:**
- Chaque Activity vérifie `isOnline()` **quand elle se charge**
- **PAS de mise à jour automatique** si le réseau change pendant que l'activité est affichée
- **PAS de broadcast receiver** écoutant les changements de réseau

**Exemple:**
```java
// Dans AllNotesActivity
private void loadNotes() {
    if (syncManager.isOnline()) {
        // Charge depuis serveur
    } else {
        // Charge depuis cache
    }
}
```

Ce code vérifie l'état **UNE SEULE FOIS** au chargement. Si le réseau change après, l'UI n'est pas mise à jour.

### ✅ Solution Recommandée

**Implémenter un BroadcastReceiver global:**

```java
public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isOnline = NetworkUtils.isOnline(context);

        // Notifier toutes les activités ouvertes
        EventBus.getDefault().post(new NetworkStatusChangedEvent(isOnline));

        // Si connexion restaurée, lancer la synch automatique
        if (isOnline) {
            OfflineSyncManager syncManager = new OfflineSyncManager(context);
            syncManager.syncPendingData(null);
        }
    }
}
```

**Enregistrer dans chaque Activity:**
```java
@Override
protected void onResume() {
    super.onResume();
    EventBus.getDefault().register(this);
}

@Override
protected void onPause() {
    super.onPause();
    EventBus.getDefault().unregister(this);
}

@Subscribe(threadMode = ThreadMode.MAIN)
public void onNetworkStatusChanged(NetworkStatusChangedEvent event) {
    if (event.isOnline) {
        // Mise à jour UI → Mode Online
        Toast.makeText(this, "Connexion rétablie", Toast.LENGTH_SHORT).show();
        loadData(); // Recharger depuis serveur
    } else {
        // Mise à jour UI → Mode Offline
        Toast.makeText(this, "Mode hors ligne", Toast.LENGTH_SHORT).show();
    }
}
```

---

## 🔍 Question 2: Pourquoi impossible de se reconnecter après déconnexion en offline?

### Problème Identifié

**Cause racine:** La méthode de déconnexion efface **TOUT** y compris les credentials offline.

### Analyse du Code

#### 1. ProfileActivity.java (ligne 316-325)
```java
private void logout() {
    // ❌ PROBLÈME: Efface TOUT
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear(); // ← Supprime AUSSI les credentials offline!
    editor.apply();

    // Redirection vers login
    startActivity(new Intent(this, MainActivity.class));
    finish();
}
```

#### 2. DashboardActivity.java (ligne 250-259)
```java
private void logout() {
    // ❌ PROBLÈME: Efface TOUT
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear(); // ← Supprime AUSSI les credentials offline!
    editor.apply();

    // Redirection vers login
    startActivity(new Intent(this, MainActivity.class));
    finish();
}
```

### Ce qui est Supprimé par `editor.clear()`

**Données perdues:**
- ✅ `auth_token` - Token d'authentification (normal)
- ❌ `offline_email` - Email pour login offline (PROBLÈME!)
- ❌ `offline_password_hash` - Hash du mot de passe offline (PROBLÈME!)
- ❌ `offline_login_enabled` - Flag mode offline (PROBLÈME!)
- ❌ `user_id` - ID utilisateur (peut être problématique)
- ❌ `user_email` - Email utilisateur (peut être problématique)
- ❌ `user_name` - Nom utilisateur (peut être problématique)

**Résultat:**
1. Utilisateur se déconnecte
2. `editor.clear()` supprime les credentials offline
3. InitialAuthManager perd aussi ses données
4. Impossible de se reconnecter en mode offline

### ✅ Solution: Utiliser AuthenticationManager.logout()

**AuthenticationManager a déjà la bonne implémentation:**

```java
/**
 * Déconnecte l'utilisateur (TOUTES les sources)
 * NE supprime PAS les credentials offline ni l'auth initiale
 */
public void logout() {
    Log.d(TAG, "🚪 Déconnexion utilisateur");

    // Supprimer la session active
    sessionManager.logoutUser();

    // ✅ Supprimer SEULEMENT le token (garder user_id, user_name, credentials offline)
    SharedPreferences.Editor editor = prefs.edit();
    editor.remove("auth_token"); // ← Supprime SEULEMENT le token
    editor.commit();

    Log.d(TAG, "✓ Déconnexion complète (credentials offline préservés)");
}
```

**Avantages:**
- ✅ Supprime le token d'authentification (déconnexion effective)
- ✅ Préserve les credentials offline
- ✅ Préserve l'auth initiale
- ✅ Permet de se reconnecter en mode offline

---

## 📋 Données Stockées pour le Mode Offline

### SharedPreferences: `ptms_prefs`

**Authentification:**
- `auth_token` - Token JWT (supprimé à la déconnexion)
- `offline_email` - Email pour login offline (préservé)
- `offline_password_hash` - Hash SHA-256 du mot de passe (préservé)
- `offline_login_enabled` - Flag mode offline activé (préservé)

**Utilisateur:**
- `user_id` - ID utilisateur (préservé pour offline)
- `user_email` - Email utilisateur (préservé)
- `user_name` - Nom complet (préservé)
- `user_type` - Type utilisateur (1=admin, 4=employee)

### SharedPreferences: `initial_auth_prefs`

**Auth Initiale:**
- `has_initial_auth` - Flag authentification initiale effectuée
- `auth_date` - Date de la première authentification
- `user_email` - Email de l'utilisateur
- `data_cache_date` - Date du dernier téléchargement de données
- `projects_count` - Nombre de projets en cache
- `work_types_count` - Nombre de types de travail en cache

### SharedPreferences: `PTMSSession` (SessionManager)

**Session Active:**
- `is_logged_in` - Flag utilisateur connecté
- `token` - Token d'authentification (même que auth_token)
- `user_id` - ID utilisateur
- `user_email` - Email utilisateur
- `user_name` - Nom utilisateur
- `session_cookie` - Cookie de session PHP

---

## 🔧 Corrections à Apporter

### 1. ProfileActivity.java

**AVANT (ligne 316-325):**
```java
private void logout() {
    // Effacer les données de session
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear(); // ❌ PROBLÈME
    editor.apply();

    // Rediriger vers la page de connexion
    startActivity(new Intent(this, MainActivity.class));
    finish();
}
```

**APRÈS:**
```java
private void logout() {
    // ✅ Utiliser AuthenticationManager pour déconnexion propre
    AuthenticationManager authManager = AuthenticationManager.getInstance(this);
    authManager.logout(); // Préserve les credentials offline

    // Rediriger vers la page de connexion
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

### 2. DashboardActivity.java

**AVANT (ligne 250-259):**
```java
private void logout() {
    // Effacer les données de session
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear(); // ❌ PROBLÈME
    editor.apply();

    // Rediriger vers la page de connexion
    startActivity(new Intent(this, MainActivity.class));
    finish();
}
```

**APRÈS:**
```java
private void logout() {
    // ✅ Utiliser AuthenticationManager pour déconnexion propre
    AuthenticationManager authManager = AuthenticationManager.getInstance(this);
    authManager.logout(); // Préserve les credentials offline

    // Rediriger vers la page de connexion
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

---

## 🎯 Comportement Attendu Après Correction

### Scénario 1: Déconnexion en Mode Online

```
1. Utilisateur connecté en ligne
2. Clic "Se déconnecter"
3. AuthenticationManager.logout() appelé
   → Supprime SEULEMENT auth_token
   → Préserve offline_email, offline_password_hash
   → Préserve has_initial_auth
4. Redirection vers LoginActivity
5. Utilisateur peut SE RECONNECTER en ligne OU offline
```

### Scénario 2: Déconnexion en Mode Offline

```
1. Utilisateur connecté en mode offline
2. Réseau désactivé (WiFi + données)
3. Clic "Se déconnecter"
4. AuthenticationManager.logout() appelé
   → Supprime SEULEMENT auth_token
   → Préserve offline_email, offline_password_hash
   → Préserve has_initial_auth
5. Redirection vers LoginActivity
6. ✅ Utilisateur peut SE RECONNECTER en mode offline
   (car credentials et auth initiale préservés)
```

### Scénario 3: Réinitialisation Complète (Debug)

```
// Pour les tests ou le debug
AuthenticationManager authManager = AuthenticationManager.getInstance(this);
authManager.fullReset(); // Supprime TOUT y compris credentials offline

→ Utilisateur doit refaire une auth initiale en ligne
```

---

## 📊 Comparaison des Méthodes de Déconnexion

| Méthode | Supprime Token | Préserve Credentials Offline | Préserve Auth Initiale | Use Case |
|---------|----------------|------------------------------|------------------------|----------|
| `prefs.edit().clear()` | ✅ | ❌ | ❌ | ❌ NE PAS UTILISER |
| `SessionManager.logoutUser()` | ✅ | ❌ | ❌ | ❌ NE PAS UTILISER |
| `AuthenticationManager.logout()` | ✅ | ✅ | ✅ | ✅ RECOMMANDÉ |
| `AuthenticationManager.fullReset()` | ✅ | ❌ | ❌ | Debug/Tests seulement |

---

## ⚠️ Avertissements

### 1. Ne PAS utiliser `editor.clear()` pour la déconnexion
```java
// ❌ MAUVAIS
SharedPreferences.Editor editor = prefs.edit();
editor.clear(); // Supprime TOUT y compris mode offline
editor.apply();
```

### 2. Ne PAS utiliser `SessionManager.logoutUser()` directement
```java
// ❌ MAUVAIS
SessionManager sessionManager = new SessionManager(this);
sessionManager.logoutUser(); // Fait un clear() total
```

### 3. TOUJOURS utiliser `AuthenticationManager.logout()`
```java
// ✅ BON
AuthenticationManager authManager = AuthenticationManager.getInstance(this);
authManager.logout(); // Déconnexion propre
```

---

## 🧪 Tests à Effectuer Après Correction

### Test 1: Déconnexion/Reconnexion Online
1. ✅ Se connecter en ligne
2. ✅ Se déconnecter
3. ✅ Se reconnecter en ligne avec mêmes identifiants
4. ✅ Vérifier que ça fonctionne

### Test 2: Déconnexion/Reconnexion Offline
1. ✅ Se connecter en ligne (auth initiale)
2. ✅ Se déconnecter
3. ✅ Désactiver le réseau
4. ✅ Se reconnecter avec mêmes identifiants
5. ✅ **ATTENDU:** Connexion offline réussie

### Test 3: Déconnexion Offline puis Reconnexion Online
1. ✅ Se connecter en ligne
2. ✅ Désactiver le réseau
3. ✅ Se déconnecter (en mode offline)
4. ✅ Réactiver le réseau
5. ✅ Se reconnecter en ligne
6. ✅ Vérifier que ça fonctionne

### Test 4: Changement de Réseau Pendant Utilisation
1. ✅ Ouvrir l'app en ligne
2. ✅ Désactiver le réseau
3. ✅ **ATTENDU:** Message "Mode hors ligne" (si BroadcastReceiver implémenté)
4. ✅ Réactiver le réseau
5. ✅ **ATTENDU:** Message "Connexion rétablie" + sync auto

---

## 📝 Résumé

### Problème 1: Mode Offline/Online pas mis à jour partout
**Cause:** Pas de BroadcastReceiver écoutant les changements de réseau
**Solution:** Implémenter NetworkChangeReceiver avec EventBus

### Problème 2: Impossible de se reconnecter après déconnexion offline
**Cause:** `editor.clear()` supprime les credentials offline
**Solution:** Utiliser `AuthenticationManager.logout()` au lieu de `clear()`

---

**Auteur:** Claude Code
**Date:** 20 Octobre 2025
**Version:** PTMS v2.0
