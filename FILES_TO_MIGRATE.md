# 📋 FICHIERS À MIGRER VERS LE SYSTÈME UNIFIÉ

## ✅ FICHIERS DÉJÀ MIGRÉS

- ✅ `TimeEntryActivity.java` - Utilise BidirectionalSyncManager
- ✅ `BidirectionalSyncManager.java` - Manager unifié corrigé
- ✅ `OfflineDatabaseHelper.java` - Cache SQLite (pas de changement nécessaire)

---

## ⚠️ FICHIERS À MIGRER

### Priorité HAUTE (utilisent OfflineSyncManager)

1. **OfflineTimeEntryActivity.java**
   - Utilise : `OfflineSyncManager`, `OfflineDataManager`, `JsonSyncManager`
   - Action : Remplacer par `BidirectionalSyncManager`
   - Ligne : 24, 25, 26, 51-53

2. **ReportsActivity.java**
   - Utilise : `JsonSyncManager`
   - Action : Remplacer par `BidirectionalSyncManager.getPendingSyncCount()`
   - Ligne : 31, 87, 270-288

3. **ReportsEnhancedActivity.java**
   - Utilise : `JsonSyncManager`
   - Action : Remplacer par `BidirectionalSyncManager.getPendingSyncCount()` et `.syncFull()`
   - Ligne : 39, 127-129, 455-466, 600-627

### Priorité MOYENNE (utilisent OfflineSyncManager pour notes)

4. **AllNotesActivity.java**
   - Utilise : `OfflineSyncManager`
   - Action : Remplacer par `BidirectionalSyncManager` (si nécessaire pour sync notes)
   - Ligne : 26, 60, 109

5. **CreateNoteUnifiedActivity.java**
   - Utilise : `OfflineSyncManager`
   - Action : Remplacer par `BidirectionalSyncManager.saveProjectNote()` (à créer si besoin)
   - Ligne : 52, 113, 397

6. **NotesAgendaActivity.java**
   - Utilise : `OfflineSyncManager`
   - Action : Remplacer par `BidirectionalSyncManager`
   - Ligne : 33, 104, 275

7. **NotesDiagnosticActivity.java**
   - Utilise : `OfflineSyncManager`
   - Action : Remplacer par `BidirectionalSyncManager`
   - Ligne : 29, 97

8. **ProjectNotesActivity.java**
   - Utilise : `OfflineSyncManager`
   - Action : Remplacer par `BidirectionalSyncManager`
   - Ligne : 38

### Priorité BASSE (utilisent OfflineDataManager)

9. **ProjectNotesListActivity.java**
   - Utilise : `OfflineDataManager`
   - Action : Remplacer par `BidirectionalSyncManager.getProjects()`
   - Ligne : 23, 40, 70

10. **SyncFilesActivity.java**
    - Utilise : `JsonSyncManager` (probablement pour affichage)
    - Action : Adapter pour afficher les rapports pending depuis SQLite
    - Ligne : Vérifier le fichier

---

## 🗑️ FICHIERS À SUPPRIMER (après migration)

Une fois TOUS les fichiers ci-dessus migrés, vous pouvez supprimer :

1. **OfflineDataManager.java**
   - Chemin : `app/src/main/java/com/ptms/mobile/cache/OfflineDataManager.java`

2. **JsonSyncManager.java**
   - Chemin : `app/src/main/java/com/ptms/mobile/sync/JsonSyncManager.java`

3. **OfflineSyncManager.java**
   - Chemin : `app/src/main/java/com/ptms/mobile/sync/OfflineSyncManager.java`

---

## 📝 INSTRUCTIONS DE MIGRATION

### Pour les fichiers utilisant `JsonSyncManager` :

**AVANT** :
```java
import com.ptms.mobile.sync.JsonSyncManager;

private JsonSyncManager jsonSyncManager;

jsonSyncManager = new JsonSyncManager(this);
List<JsonSyncManager.SyncFileInfo> localFiles = jsonSyncManager.getAllFiles();
```

**APRÈS** :
```java
import com.ptms.mobile.sync.BidirectionalSyncManager;

private BidirectionalSyncManager syncManager;

syncManager = new BidirectionalSyncManager(this);
int pendingCount = syncManager.getPendingSyncCount();
```

### Pour les fichiers utilisant `OfflineSyncManager` :

**AVANT** :
```java
import com.ptms.mobile.sync.OfflineSyncManager;

private OfflineSyncManager syncManager;

syncManager = new OfflineSyncManager(this);
syncManager.syncPendingData(callback);
```

**APRÈS** :
```java
import com.ptms.mobile.sync.BidirectionalSyncManager;

private BidirectionalSyncManager syncManager;

syncManager = new BidirectionalSyncManager(this);
syncManager.syncFull(callback);
```

### Pour les fichiers utilisant `OfflineDataManager` :

**AVANT** :
```java
import com.ptms.mobile.cache.OfflineDataManager;

private OfflineDataManager offlineDataManager;

offlineDataManager = new OfflineDataManager(this);
List<Project> projects = offlineDataManager.loadProjectsFromCache();
```

**APRÈS** :
```java
import com.ptms.mobile.sync.BidirectionalSyncManager;

private BidirectionalSyncManager syncManager;

syncManager = new BidirectionalSyncManager(this);
List<Project> projects = syncManager.getProjects();
```

---

## ⚙️ COMPILATION

Pour vérifier que tout compile après migration :

```bash
cd C:\Devs\web\appAndroid
gradlew.bat assembleDebug
```

Si erreurs de compilation, vérifier que :
1. Tous les imports sont corrects
2. Aucune référence aux anciens managers
3. Les callbacks sont adaptés (SaveCallback au lieu de SyncCallback si nécessaire)

---

## 🧪 TESTS POST-MIGRATION

Pour chaque fichier migré, tester :

1. **Ouverture** : L'Activity s'ouvre sans crash
2. **Chargement** : Les données se chargent (online + offline)
3. **Sauvegarde** : Les données se sauvent (online + offline)
4. **Synchronisation** : La sync fonctionne

---

**Date** : 2025-01-19
**Status** : 10 fichiers à migrer restants
**Priorité** : HAUTE pour TimeEntry, MOYENNE pour Reports, BASSE pour Notes
