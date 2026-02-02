# Diagnostic - Doublons de Notes

**Date:** 20 Octobre 2025
**Problème:** Notes affichées en double dans l'extension Notes

---

## 🔍 Diagnostic

### Système de Synchronisation Actuel

**Flux de Création:**
1. **Online** → Note envoyée directement à l'API → PAS stockée localement
2. **Offline** → Note sauvegardée localement avec `status='pending'`

**Flux de Synchronisation:**
1. `syncPendingProjectNotes()` récupère les notes `status IN ('pending', 'failed')`
2. Envoie chaque note à l'API
3. Si succès → `markProjectNoteAsSynced(localId, serverId)` met à jour:
   - `server_id` = ID retourné par le serveur
   - `status` = 'synced'
   - `synced` = 1

**Flux d'Affichage:**
1. Charge toutes les notes depuis le serveur
2. Ajoute UNIQUEMENT les notes locales avec `id == 0` (pas de server_id)

---

## ❓ Questions à Vérifier

### 1. Le server_id est-il bien mis à jour après la sync?

**Vérification à faire:**
```sql
-- Dans la base SQLite locale
SELECT
    id as local_id,
    server_id,
    title,
    sync_status,
    synced
FROM project_notes
WHERE sync_status = 'synced';
```

**Attendu:**
- Toutes les notes avec `sync_status='synced'` devraient avoir un `server_id` NON NULL

**Si `server_id` est NULL:**
→ Le problème est dans `markProjectNoteAsSynced()` qui ne met pas à jour correctement

---

### 2. Y a-t-il d'autres endroits qui chargent toutes les notes locales?

**Fichiers à vérifier:**
- `NotesAgendaActivity.java`
- `ProjectNotesActivity.java`
- `NotesMenuActivity.java`
- `ProjectNotesListActivity.java`

**Rechercher:** Utilisation de `getAllNotesByUserId()` ou `getAllNotes()` sans filtrage par `server_id`

---

### 3. Les notes créées en online sont-elles sauvegardées localement par erreur?

**Vérifier dans `CreateNoteUnifiedActivity`:**
- Après `sendNoteToApi()` → success → `finish()`
- PAS de sauvegarde locale normalement
- Mais vérifier s'il y a un callback ou listener qui pourrait sauvegarder localement

---

## 🔧 Solutions Possibles

### Solution 1: Déduplication à l'affichage (Palliatif)

**Dans `AllNotesActivity.loadNotesFromServer()`:**

```java
// ✅ Ajouter toutes les notes du serveur
Map<Integer, ProjectNote> notesMap = new HashMap<>();
for (ProjectNote note : serverNotes) {
    notesMap.put(note.getId(), note);
}

// ✅ Ajouter SEULEMENT les notes non synchronisées (server_id = NULL)
List<ProjectNote> cachedNotes = syncManager.getPendingProjectNotes();
for (ProjectNote cachedNote : cachedNotes) {
    if (cachedNote.getId() == 0 || !notesMap.containsKey(cachedNote.getId())) {
        allNotes.add(cachedNote);
    }
}

// Convertir la map en liste
allNotes.addAll(notesMap.values());
```

---

### Solution 2: Nettoyer les notes synchronisées (Correct)

**Ajouter une méthode dans `OfflineDatabaseHelper`:**

```java
/**
 * Supprime les notes locales qui ont été synchronisées et qui existent sur le serveur
 * Garde uniquement les notes pending/failed
 */
public synchronized void cleanupSyncedNotes() {
    SQLiteDatabase db = this.getWritableDatabase();
    int deleted = db.delete(
        TABLE_PROJECT_NOTES,
        COLUMN_SYNC_STATUS + " = 'synced' AND " + COLUMN_SERVER_ID + " IS NOT NULL",
        null
    );
    Log.d(TAG, "Notes synchronisées nettoyées: " + deleted);
}
```

**Appeler après chaque sync réussie:**
```java
// Dans OfflineSyncManager.syncPendingProjectNotes()
if (syncedCount[0] > 0) {
    dbHelper.cleanupSyncedNotes();
}
```

---

### Solution 3: Charger UNIQUEMENT depuis le serveur en online (Recommandé)

**Dans `AllNotesActivity`:**

```java
private void loadNotes() {
    progressBar.setVisibility(View.VISIBLE);
    allNotes.clear();

    if (syncManager.isOnline()) {
        // Mode ONLINE: Charger UNIQUEMENT depuis le serveur
        // Les notes locales pending seront synchronisées automatiquement
        loadNotesFromServer();
    } else {
        // Mode OFFLINE: Charger depuis cache local
        loadNotesFromCache();
    }
}

private void loadNotesFromServer() {
    // Charger TOUTES les notes depuis le serveur (y compris celles créées offline et synchronisées)
    // Ne PAS ajouter les notes locales car elles devraient déjà être sur le serveur après sync
}

private void loadNotesFromCache() {
    // Charger TOUTES les notes locales (pour mode offline)
    int userId = sessionManager.getUserId();
    List<ProjectNote> cachedNotes = dbHelper.getAllNotesByUserId(userId);
    allNotes.addAll(cachedNotes);
}
```

---

## 📋 Plan d'Action

### Étape 1: Diagnostic

1. ✅ Lancer l'app et créer une note en offline
2. ✅ Synchroniser
3. ✅ Vérifier dans la base SQLite:
   ```sql
   SELECT * FROM project_notes WHERE title = 'Note Test';
   ```
4. ✅ Vérifier si `server_id` est rempli

### Étape 2: Vérifier les Doublons

1. ✅ Ouvrir AllNotesActivity
2. ✅ Compter le nombre de notes affichées
3. ✅ Vérifier les logs:
   ```
   AllNotesActivity: Notes chargées: X (serveur: Y, cache: Z)
   ```
4. ✅ Si X > Y et Z > 0 → Il y a des doublons

### Étape 3: Appliquer la Solution

- **Solution 1 (Rapide):** Déduplication à l'affichage
- **Solution 2 (Propre):** Nettoyer les notes synchronisées
- **Solution 3 (Idéal):** Ne charger QUE depuis le serveur en online

---

## ⚠️ Points d'Attention

1. **Synchronisation automatique** → S'assurer qu'elle fonctionne après création offline
2. **Mode offline** → Les notes doivent rester accessibles
3. **Conflits** → Si modification locale ET serveur → Gérer les conflits
4. **Performance** → Éviter de charger trop de notes en mémoire

---

## 📝 Notes Complémentaires

- Les notes créées ONLINE ne sont jamais dans la base locale
- Les notes créées OFFLINE sont dans la base locale jusqu'à sync
- Après sync, elles ont un `server_id` et `status='synced'`
- En mode ONLINE, on devrait charger UNIQUEMENT depuis le serveur
- En mode OFFLINE, on charge depuis la base locale

---

**Prochaine étape:** Tester et identifier laquelle des 3 solutions convient le mieux au besoin.
