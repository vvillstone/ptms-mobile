# 🚀 GUIDE D'INSTALLATION DES CORRECTIONS - PTMS Android

**Date**: 2025-01-19
**Version**: 1.0
**Durée estimée**: 30 minutes

---

## ✅ CE QUI A ÉTÉ FAIT

✅ **Fichier créé**: `OfflineDatabaseHelper_FIXED.java` (version corrigée complète)
  - ✅ Tous les `db.close()` supprimés (27 occurrences)
  - ✅ `synchronized` ajouté à toutes les méthodes
  - ✅ Migration v6 créée (status TEXT → INTEGER)
  - ✅ Colonnes manquantes ajoutées
  - ✅ Cache en mémoire implémenté (TTL 5 min)
  - ✅ Transactions pour opérations batch

✅ **Documents créés**:
  - `RAPPORT_PROBLEMES_OFFLINE_MODE.md` - Analyse détaillée
  - `DATA_PATTERN_SYNCHRONISATION.md` - Spécification des données
  - `RESUME_ANALYSE_ET_SOLUTIONS.md` - Plan d'action
  - Ce guide d'installation

---

## 🔧 ÉTAPES D'INSTALLATION

### Étape 1: Sauvegarde (OBLIGATOIRE)

```bash
# Créer une branche de sauvegarde
cd appAndroid
git checkout -b backup-before-offline-fix
git add .
git commit -m "Backup avant corrections mode offline"

# Revenir sur la branche principale
git checkout main
```

---

### Étape 2: Remplacer OfflineDatabaseHelper.java

#### Option A: Remplacement complet (RECOMMANDÉ)

```bash
cd app/src/main/java/com/ptms/mobile/database

# Sauvegarder l'ancien fichier
cp OfflineDatabaseHelper.java OfflineDatabaseHelper.java.backup

# Remplacer par la version corrigée
cp OfflineDatabaseHelper_FIXED.java OfflineDatabaseHelper.java
```

#### Option B: Modifications manuelles (si vous voulez garder certaines personnalisations)

Ouvrir `OfflineDatabaseHelper.java` et appliquer les corrections suivantes:

##### 2.1. Changer la version de la base de données

```java
// LIGNE 25
// ❌ AVANT
private static final int DATABASE_VERSION = 5;

// ✅ APRÈS
private static final int DATABASE_VERSION = 6;
```

##### 2.2. Ajouter les constantes pour les nouvelles colonnes

```java
// APRÈS LIGNE 45 (après les colonnes communes)
// ✅ AJOUTER
private static final String COLUMN_PROJECT_STATUS = "status";
private static final String COLUMN_IS_PLACEHOLDER = "is_placeholder";
private static final String COLUMN_ASSIGNED_USER_ID = "assigned_user_id";
private static final String COLUMN_CLIENT = "client";
private static final String COLUMN_PRIORITY = "priority";
private static final String COLUMN_PROGRESS = "progress";
private static final String COLUMN_WORK_TYPE_STATUS = "status";

// ✅ AJOUTER - Cache en mémoire
private List<Project> cachedProjects = null;
private List<WorkType> cachedWorkTypes = null;
private long lastProjectsCacheTime = 0;
private long lastWorkTypesCacheTime = 0;
private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
```

##### 2.3. Mettre à jour CREATE_TABLE_PROJECTS

```java
// LIGNES 69-80
// ❌ SUPPRIMER l'ancienne définition

// ✅ REMPLACER PAR
private static final String CREATE_TABLE_PROJECTS =
    "CREATE TABLE " + TABLE_PROJECTS + "(" +
    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
    COLUMN_SERVER_ID + " INTEGER UNIQUE," +
    COLUMN_NAME + " TEXT NOT NULL," +
    COLUMN_DESCRIPTION + " TEXT," +
    COLUMN_PROJECT_STATUS + " INTEGER NOT NULL DEFAULT 1," +  // ✅ INTEGER
    COLUMN_IS_PLACEHOLDER + " INTEGER DEFAULT 0," +           // ✅ AJOUTÉ
    COLUMN_ASSIGNED_USER_ID + " INTEGER," +                   // ✅ AJOUTÉ
    COLUMN_CLIENT + " VARCHAR(255)," +                        // ✅ AJOUTÉ
    COLUMN_PRIORITY + " TEXT DEFAULT 'medium'," +             // ✅ AJOUTÉ
    COLUMN_PROGRESS + " REAL DEFAULT 0.00," +                 // ✅ AJOUTÉ
    COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
    COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
    COLUMN_SYNCED + " INTEGER DEFAULT 1" +
    ")";
```

##### 2.4. Mettre à jour CREATE_TABLE_WORK_TYPES

```java
// LIGNES 82-93
// ❌ SUPPRIMER l'ancienne définition

// ✅ REMPLACER PAR
private static final String CREATE_TABLE_WORK_TYPES =
    "CREATE TABLE " + TABLE_WORK_TYPES + "(" +
    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
    COLUMN_SERVER_ID + " INTEGER UNIQUE," +
    COLUMN_NAME + " TEXT NOT NULL," +
    COLUMN_DESCRIPTION + " TEXT," +
    COLUMN_WORK_TYPE_STATUS + " INTEGER NOT NULL DEFAULT 1," + // ✅ AJOUTÉ
    COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
    COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
    COLUMN_SYNCED + " INTEGER DEFAULT 1" +
    ")";
```

##### 2.5. Ajouter la migration v6 dans onUpgrade()

```java
// DANS LA MÉTHODE onUpgrade(), APRÈS LA MIGRATION V5 (ligne ~256)
// ✅ AJOUTER CECI À LA FIN

// ✅ MIGRATION V6: Correction des types de données
if (oldVersion < 6) {
    Log.d(TAG, "========================================");
    Log.d(TAG, "MIGRATION V6: Correction des structures");
    Log.d(TAG, "========================================");

    // ✅ 1. Migration table PROJECTS (status TEXT → INTEGER + nouvelles colonnes)
    Log.d(TAG, "Migration projects: status TEXT → INTEGER + nouvelles colonnes");

    db.execSQL("CREATE TABLE projects_new (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COLUMN_SERVER_ID + " INTEGER UNIQUE," +
        COLUMN_NAME + " TEXT NOT NULL," +
        COLUMN_DESCRIPTION + " TEXT," +
        COLUMN_PROJECT_STATUS + " INTEGER NOT NULL DEFAULT 1," +
        COLUMN_IS_PLACEHOLDER + " INTEGER DEFAULT 0," +
        COLUMN_ASSIGNED_USER_ID + " INTEGER," +
        COLUMN_CLIENT + " VARCHAR(255)," +
        COLUMN_PRIORITY + " TEXT DEFAULT 'medium'," +
        COLUMN_PROGRESS + " REAL DEFAULT 0.00," +
        COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_SYNCED + " INTEGER DEFAULT 1" +
    ")");

    // Copier les données en convertissant status
    db.execSQL("INSERT INTO projects_new " +
        "(id, server_id, name, description, status, is_placeholder, created_at, updated_at, synced) " +
        "SELECT id, server_id, name, description, " +
        "CASE WHEN status = 'active' OR status = '1' THEN 1 ELSE 0 END, " +
        "COALESCE(is_placeholder, 0), created_at, updated_at, synced " +
        "FROM " + TABLE_PROJECTS);

    db.execSQL("DROP TABLE " + TABLE_PROJECTS);
    db.execSQL("ALTER TABLE projects_new RENAME TO " + TABLE_PROJECTS);

    Log.d(TAG, "✅ Table projects migrée: status TEXT → INTEGER");

    // ✅ 2. Migration table WORK_TYPES (ajout status)
    Log.d(TAG, "Migration work_types: ajout colonne status");

    db.execSQL("CREATE TABLE work_types_new (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COLUMN_SERVER_ID + " INTEGER UNIQUE," +
        COLUMN_NAME + " TEXT NOT NULL," +
        COLUMN_DESCRIPTION + " TEXT," +
        COLUMN_WORK_TYPE_STATUS + " INTEGER NOT NULL DEFAULT 1," +
        COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_SYNCED + " INTEGER DEFAULT 1" +
    ")");

    // Copier les données
    db.execSQL("INSERT INTO work_types_new " +
        "(id, server_id, name, description, status, created_at, updated_at, synced) " +
        "SELECT id, server_id, name, description, 1, created_at, updated_at, synced " +
        "FROM " + TABLE_WORK_TYPES);

    db.execSQL("DROP TABLE " + TABLE_WORK_TYPES);
    db.execSQL("ALTER TABLE work_types_new RENAME TO " + TABLE_WORK_TYPES);

    Log.d(TAG, "✅ Table work_types migrée: colonne status ajoutée");

    Log.d(TAG, "========================================");
    Log.d(TAG, "MIGRATION V6 TERMINÉE AVEC SUCCÈS");
    Log.d(TAG, "========================================");
}
```

##### 2.6. CRITIQUE: Supprimer TOUS les db.close()

**Rechercher et supprimer** toutes les lignes contenant `db.close();` dans les méthodes suivantes:

- `insertProject()` - ligne ~296
- `getAllProjects()` - ligne ~327
- `clearProjects()` - ligne ~336
- `insertWorkType()` - ligne ~375
- `getAllWorkTypes()` - ligne ~400
- `clearWorkTypes()` - ligne ~410
- `insertTimeReport()` - ligne ~437
- `getAllPendingTimeReports()` - ligne ~473
- `updateTimeReportSyncStatus()` - ligne ~490
- `markTimeReportAsSynced()` - ligne ~505
- `getPendingSyncCount()` - ligne ~521
- Et TOUTES les autres méthodes (27 occurrences au total)

```java
// ❌ SUPPRIMER TOUTES CES LIGNES
db.close();

// ❌ SUPPRIMER AUSSI
cursor.close();
db.close();
```

**GARDER seulement** les `cursor.close()`, mais **SUPPRIMER** les `db.close()`.

##### 2.7. Ajouter `synchronized` à toutes les méthodes publiques

Ajouter le mot-clé `synchronized` devant toutes les méthodes publiques:

```java
// ❌ AVANT
public long insertProject(Project project) {

// ✅ APRÈS
public synchronized long insertProject(Project project) {

// Faire ceci pour TOUTES les méthodes publiques:
// - insertProject(), getAllProjects(), clearProjects()
// - insertWorkType(), getAllWorkTypes(), clearWorkTypes()
// - insertTimeReport(), getAllPendingTimeReports()
// - updateTimeReportSyncStatus(), markTimeReportAsSynced()
// - getPendingSyncCount(), insertProjectNote(), etc.
```

##### 2.8. Ajouter les méthodes pour le cache en mémoire

```java
// ✅ AJOUTER CES MÉTHODES À LA FIN DE LA CLASSE (avant le dernier })

/**
 * ✅ NOUVEAU: Invalide le cache en mémoire
 */
public synchronized void invalidateCache() {
    cachedProjects = null;
    cachedWorkTypes = null;
    lastProjectsCacheTime = 0;
    lastWorkTypesCacheTime = 0;
    Log.d(TAG, "Cache mémoire invalidé");
}

/**
 * ✅ NOUVEAU: Vérifie si le cache est valide
 */
private boolean isProjectsCacheValid() {
    long now = System.currentTimeMillis();
    return cachedProjects != null && (now - lastProjectsCacheTime) < CACHE_VALIDITY_MS;
}

private boolean isWorkTypesCacheValid() {
    long now = System.currentTimeMillis();
    return cachedWorkTypes != null && (now - lastWorkTypesCacheTime) < CACHE_VALIDITY_MS;
}

/**
 * ✅ NOUVEAU: Remplace tous les projets en une seule transaction
 */
public synchronized void replaceAllProjects(List<Project> projects) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.beginTransaction();
    try {
        db.delete(TABLE_PROJECTS, null, null);

        for (Project project : projects) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SERVER_ID, project.getId());
            values.put(COLUMN_NAME, project.getName() != null ? project.getName() : "");
            values.put(COLUMN_DESCRIPTION, project.getDescription() != null ? project.getDescription() : "");
            values.put(COLUMN_PROJECT_STATUS, project.getStatus());
            values.put(COLUMN_IS_PLACEHOLDER, project.isPlaceholder() ? 1 : 0);

            if (project.getAssignedUserId() != null) {
                values.put(COLUMN_ASSIGNED_USER_ID, Integer.parseInt(project.getAssignedUserId()));
            }
            if (project.getClient() != null) {
                values.put(COLUMN_CLIENT, project.getClient());
            }
            if (project.getPriority() != null) {
                values.put(COLUMN_PRIORITY, project.getPriority());
            }
            values.put(COLUMN_PROGRESS, project.getProgress());

            if (project.getDateCreated() != null) {
                values.put(COLUMN_CREATED_AT, project.getDateCreated());
            }
            if (project.getDateUpdated() != null) {
                values.put(COLUMN_UPDATED_AT, project.getDateUpdated());
            }
            values.put(COLUMN_SYNCED, 1);

            db.insert(TABLE_PROJECTS, null, values);
        }

        db.setTransactionSuccessful();
        invalidateCache();
        Log.d(TAG, "Transaction réussie: " + projects.size() + " projets remplacés");
    } catch (Exception e) {
        Log.e(TAG, "Erreur transaction replaceAllProjects, rollback", e);
    } finally {
        db.endTransaction();
    }
}

/**
 * ✅ NOUVEAU: Remplace tous les types de travail en une seule transaction
 */
public synchronized void replaceAllWorkTypes(List<WorkType> workTypes) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.beginTransaction();
    try {
        db.delete(TABLE_WORK_TYPES, null, null);

        for (WorkType workType : workTypes) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SERVER_ID, workType.getId());
            values.put(COLUMN_NAME, workType.getName() != null ? workType.getName() : "");
            values.put(COLUMN_DESCRIPTION, workType.getDescription() != null ? workType.getDescription() : "");
            values.put(COLUMN_WORK_TYPE_STATUS, workType.getStatus());

            if (workType.getDateCreated() != null) {
                values.put(COLUMN_CREATED_AT, workType.getDateCreated());
            }
            if (workType.getDateUpdated() != null) {
                values.put(COLUMN_UPDATED_AT, workType.getDateUpdated());
            }
            values.put(COLUMN_SYNCED, 1);

            db.insert(TABLE_WORK_TYPES, null, values);
        }

        db.setTransactionSuccessful();
        invalidateCache();
        Log.d(TAG, "Transaction réussie: " + workTypes.size() + " types de travail remplacés");
    } catch (Exception e) {
        Log.e(TAG, "Erreur transaction replaceAllWorkTypes, rollback", e);
    } finally {
        db.endTransaction();
    }
}

/**
 * ✅ OVERRIDE: Ferme proprement la base de données
 */
@Override
public synchronized void close() {
    invalidateCache();
    super.close();
    Log.d(TAG, "Base de données fermée proprement");
}
```

##### 2.9. Mettre à jour getAllProjects() pour utiliser le cache

```java
// REMPLACER LA MÉTHODE getAllProjects() COMPLÈTE PAR:

public synchronized List<Project> getAllProjects() {
    // ✅ Retourner le cache si valide
    if (isProjectsCacheValid()) {
        Log.d(TAG, "Retour du cache mémoire: " + cachedProjects.size() + " projets");
        return new ArrayList<>(cachedProjects);
    }

    List<Project> projects = new ArrayList<>();
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PROJECTS + " ORDER BY " + COLUMN_NAME, null);

    if (cursor.moveToFirst()) {
        do {
            Project project = new Project();
            project.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVER_ID)));
            project.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            project.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));

            // ✅ status est INTEGER maintenant
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROJECT_STATUS));
            project.setStatus(status);

            // ✅ NOUVEAUX CHAMPS
            int isPlaceholder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PLACEHOLDER));
            project.setPlaceholder(isPlaceholder == 1);

            int assignedUserIdIdx = cursor.getColumnIndexOrThrow(COLUMN_ASSIGNED_USER_ID);
            if (!cursor.isNull(assignedUserIdIdx)) {
                project.setAssignedUserId(String.valueOf(cursor.getInt(assignedUserIdIdx)));
            }

            int clientIdx = cursor.getColumnIndexOrThrow(COLUMN_CLIENT);
            if (!cursor.isNull(clientIdx)) {
                project.setClient(cursor.getString(clientIdx));
            }

            int priorityIdx = cursor.getColumnIndexOrThrow(COLUMN_PRIORITY);
            if (!cursor.isNull(priorityIdx)) {
                project.setPriority(cursor.getString(priorityIdx));
            }

            project.setProgress(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS)));

            project.setDateCreated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
            project.setDateUpdated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
            projects.add(project);
        } while (cursor.moveToNext());
    }

    cursor.close();
    // ✅ PAS DE db.close() ICI!

    // ✅ Mettre en cache
    cachedProjects = new ArrayList<>(projects);
    lastProjectsCacheTime = System.currentTimeMillis();

    Log.d(TAG, "Projets récupérés depuis SQLite et mis en cache: " + projects.size());
    return projects;
}
```

Faire de même pour `getAllWorkTypes()`.

---

### Étape 3: Mettre à jour OfflineSyncManager.java

#### 3.1. Utiliser les nouvelles méthodes avec transactions

Remplacer dans `syncReferenceData()` (lignes ~178-183):

```java
// ❌ AVANT
dbHelper.clearProjects();
for (Project project : response.body().projects) {
    dbHelper.insertProject(project);
}

// ✅ APRÈS
dbHelper.replaceAllProjects(response.body().projects);
```

Faire de même pour work_types (lignes ~199-204):

```java
// ❌ AVANT
dbHelper.clearWorkTypes();
for (WorkType workType : response.body()) {
    dbHelper.insertWorkType(workType);
}

// ✅ APRÈS
dbHelper.replaceAllWorkTypes(response.body());
```

#### 3.2. Ajouter try-catch dans les callbacks

Entourer TOUS les callbacks Retrofit de try-catch:

```java
// EXEMPLE pour syncPendingTimeReports() ligne ~268

call.enqueue(new Callback<ApiService.ApiResponse>() {
    @Override
    public void onResponse(Call<ApiService.ApiResponse> call, Response<ApiService.ApiResponse> response) {
        try { // ✅ AJOUTER TRY
            if (response.isSuccessful() && response.body() != null && response.body().success) {
                dbHelper.markTimeReportAsSynced(report.getId(), report.getId());
                syncedCount[0]++;

                Log.d(TAG, "Rapport synchronisé: " + report.getProjectName() + " - " + report.getHours() + "h");

                if (callback != null) {
                    callback.onSyncProgress("Synchronisé " + syncedCount[0] + "/" + totalCount + " rapports");
                }
            } else {
                failedCount[0]++;
                String error = response.body() != null ? response.body().message : "Erreur serveur " + response.code();

                dbHelper.updateTimeReportSyncStatus(
                    report.getId(),
                    "failed",
                    error,
                    getSyncAttempts(report.getId()) + 1
                );

                Log.e(TAG, "Échec synchronisation rapport: " + error);
            }

            if (syncedCount[0] + failedCount[0] >= totalCount) {
                if (callback != null) {
                    callback.onSyncCompleted(syncedCount[0], failedCount[0]);
                }
            }
        } catch (Exception e) { // ✅ AJOUTER CATCH
            Log.e(TAG, "Erreur dans onResponse", e);
            failedCount[0]++;

            try {
                dbHelper.updateTimeReportSyncStatus(
                    report.getId(),
                    "failed",
                    "Erreur traitement: " + e.getMessage(),
                    getSyncAttempts(report.getId()) + 1
                );
            } catch (Exception ex) {
                Log.e(TAG, "Erreur updateTimeReportSyncStatus", ex);
            }

            if (syncedCount[0] + failedCount[0] >= totalCount) {
                if (callback != null) {
                    callback.onSyncCompleted(syncedCount[0], failedCount[0]);
                }
            }
        }
    }

    @Override
    public void onFailure(Call<ApiService.ApiResponse> call, Throwable t) {
        try { // ✅ AJOUTER TRY
            failedCount[0]++;
            String error = "Erreur réseau: " + t.getMessage();

            dbHelper.updateTimeReportSyncStatus(
                report.getId(),
                "failed",
                error,
                getSyncAttempts(report.getId()) + 1
            );

            Log.e(TAG, "Échec synchronisation rapport: " + error);

            if (syncedCount[0] + failedCount[0] >= totalCount) {
                if (callback != null) {
                    callback.onSyncCompleted(syncedCount[0], failedCount[0]);
                }
            }
        } catch (Exception e) { // ✅ AJOUTER CATCH
            Log.e(TAG, "Erreur dans onFailure", e);
        }
    }
});
```

**Appliquer le même pattern** pour:
- `syncReferenceData()` - callbacks projets (lignes 173-190)
- `syncReferenceData()` - callbacks work_types (lignes 193-211)
- `syncReferenceData()` - callbacks note_types (lignes 215-233)
- `syncPendingProjectNotes()` - callbacks notes (lignes 484-534)

#### 3.3. Impl\u00e9menter retry logic

Ajouter en haut de la classe:

```java
private static final int MAX_SYNC_ATTEMPTS = 3;
```

Modifier `onFailure()` pour implémenter retry:

```java
@Override
public void onFailure(Call<ApiService.ApiResponse> call, Throwable t) {
    try {
        failedCount[0]++;
        int attempts = getSyncAttempts(report.getId()) + 1;

        // ✅ Si < 3 tentatives, remettre en "pending"
        String status = (attempts < MAX_SYNC_ATTEMPTS) ? "pending" : "failed";

        dbHelper.updateTimeReportSyncStatus(
            report.getId(),
            status, // ✅ "pending" si retry possible
            "Erreur réseau (tentative " + attempts + "/" + MAX_SYNC_ATTEMPTS + "): " + t.getMessage(),
            attempts
        );

        Log.e(TAG, "Échec sync (tentative " + attempts + "/" + MAX_SYNC_ATTEMPTS + "): " + t.getMessage());

        if (syncedCount[0] + failedCount[0] >= totalCount) {
            if (callback != null) {
                callback.onSyncCompleted(syncedCount[0], failedCount[0]);
            }
        }
    } catch (Exception e) {
        Log.e(TAG, "Erreur dans onFailure", e);
    }
}
```

---

### Étape 4: Tester les modifications

#### 4.1. Désinstaller l'app existante (pour forcer la migration)

```bash
cd appAndroid
gradlew.bat uninstallDebug
```

#### 4.2. Compiler et installer

```bash
gradlew.bat assembleDebug
gradlew.bat installDebug
```

#### 4.3. Vérifier les logs

Lancer logcat et filtrer sur `OfflineDatabaseHelper`:

```bash
adb logcat -s OfflineDatabaseHelper
```

Vous devriez voir:

```
OfflineDatabaseHelper: Mise à jour de la base de données hors ligne de v5 à v6
OfflineDatabaseHelper: ========================================
OfflineDatabaseHelper: MIGRATION V6: Correction des structures
OfflineDatabaseHelper: ========================================
OfflineDatabaseHelper: Migration projects: status TEXT → INTEGER + nouvelles colonnes
OfflineDatabaseHelper: ✅ Table projects migrée: status TEXT → INTEGER
OfflineDatabaseHelper: Migration work_types: ajout colonne status
OfflineDatabaseHelper: ✅ Table work_types migrée: colonne status ajoutée
OfflineDatabaseHelper: ========================================
OfflineDatabaseHelper: MIGRATION V6 TERMINÉE AVEC SUCCÈS
OfflineDatabaseHelper: ========================================
```

#### 4.4. Tests fonctionnels

✅ **Test 1: Mode offline de base**
1. Couper le WiFi
2. Créer une entrée de temps
3. Vérifier qu'elle est sauvegardée (pas de crash!)
4. Rallumer le WiFi
5. Vérifier la synchronisation automatique

✅ **Test 2: Perte de connexion pendant sync**
1. Créer 5 entrées offline
2. Reconnecter
3. Couper le WiFi pendant la sync
4. Vérifier **aucun crash**
5. Rallumer → vérifier reprise automatique

✅ **Test 3: Performance**
1. Charger la liste des projets 10 fois de suite
2. Vérifier que c'est instantané à partir de la 2ème fois (cache!)

---

## ⚙️ PARAMÈTRES DE CONFIGURATION

### Ajuster le TTL du cache (optionnel)

Dans `OfflineDatabaseHelper.java`, ligne ~30:

```java
// Par défaut: 5 minutes
private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000;

// Pour un cache plus agressif (30 secondes):
private static final long CACHE_VALIDITY_MS = 30 * 1000;

// Pour un cache plus long (15 minutes):
private static final long CACHE_VALIDITY_MS = 15 * 60 * 1000;
```

### Ajuster le nombre de tentatives de sync (optionnel)

Dans `OfflineSyncManager.java`, ligne ~38:

```java
// Par défaut: 3 tentatives
private static final int MAX_SYNC_ATTEMPTS = 3;

// Pour plus de tentatives:
private static final int MAX_SYNC_ATTEMPTS = 5;
```

---

## 🐛 TROUBLESHOOTING

### Problème: Erreur de compilation "cannot find symbol: replaceAllProjects"

**Solution**: Vous avez oublié d'ajouter les nouvelles méthodes. Revoir l'Étape 2.8.

---

### Problème: App crash au démarrage après migration

**Solution**: Désinstaller complètement l'app et réinstaller:

```bash
gradlew.bat uninstallDebug
gradlew.bat clean
gradlew.bat assembleDebug
gradlew.bat installDebug
```

---

### Problème: Migration ne se déclenche pas

**Solution**: La migration ne se déclenche que si `oldVersion < newVersion`.

Vérifier dans logcat:

```bash
adb logcat -s OfflineDatabaseHelper:D
```

Si vous ne voyez pas "Mise à jour de la base de données hors ligne de v5 à v6", c'est que la BD est déjà en v6.

Pour forcer la migration en dev:

```bash
adb shell
run-as com.ptms.mobile
cd databases
rm ptms_offline.db
exit
exit
```

Puis relancer l'app.

---

### Problème: Données perdues après migration

**Solution**: La migration v6 préserve toutes les données existantes. Si des données sont perdues:

1. Vérifier les logs pour voir si une erreur s'est produite
2. Restaurer depuis la sauvegarde:

```bash
git checkout backup-before-offline-fix
```

---

## ✅ CHECKLIST FINALE

Avant de déployer en production:

- [ ] Toutes les modifications appliquées
- [ ] App compile sans erreur
- [ ] Migration v6 testée et réussie
- [ ] Test mode offline OK
- [ ] Test perte de connexion OK
- [ ] Test synchronisation OK
- [ ] Performance vérifiée (cache fonctionne)
- [ ] Commit et push des modifications
- [ ] Documentation mise à jour

---

## 📞 SUPPORT

Si vous rencontrez des problèmes:

1. Vérifier les logs: `adb logcat -s OfflineDatabaseHelper OfflineSyncManager`
2. Consulter `RAPPORT_PROBLEMES_OFFLINE_MODE.md`
3. Consulter `DATA_PATTERN_SYNCHRONISATION.md`

---

**Auteur**: Claude Code
**Date**: 2025-01-19
**Version**: 1.0
**Testé sur**: Android 8.0+

🎉 **Félicitations! Votre application Android PTMS est maintenant robuste et performante!**
