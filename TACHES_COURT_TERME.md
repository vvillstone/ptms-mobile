# 📋 TÂCHES COURT TERME - PTMS Mobile Android

**Date:** 22 Octobre 2025
**Priorité:** Amélioration avant production
**Durée estimée:** 1-2 semaines

---

## 🎯 OBJECTIF

Résoudre les problèmes mineurs identifiés lors de l'audit avant déploiement en production.

**Status actuel:** 8.5/10
**Status cible:** 9.5/10

---

## 📝 LISTE DES TÂCHES

### 🔴 PRIORITÉ CRITIQUE

#### 1. Build Release et Tests

**Objectif:** Générer et tester l'APK de production

**Fichiers concernés:**
- `build.gradle` (app)
- `local.properties` (signing config)

**Tâches:**
```bash
# 1. Build release APK
cd appAndroid
.\gradlew.bat assembleRelease

# 2. Vérifier APK généré
# Emplacement: app/build/outputs/apk/release/
# Fichier: PTMS-Mobile-v2.0-release-*.apk

# 3. Installer sur device de test
adb install -r app/build/outputs/apk/release/PTMS-Mobile-v2.0-release-*.apk

# 4. Tests manuels complets
```

**Tests à effectuer:**
- [ ] Login online
- [ ] Login offline (après auth initiale)
- [ ] Saisie heures offline → sync auto
- [ ] Création notes texte/audio
- [ ] Chat (envoi messages)
- [ ] Reports (affichage jour/semaine/mois)
- [ ] Timer service
- [ ] Rotation écran
- [ ] Permissions (audio, notifications)
- [ ] Déconnexion/reconnexion

**Durée estimée:** 2-3 heures

**Validation:**
- [ ] APK release fonctionne sur Android 7, 10, 12, 14
- [ ] Toutes les fonctionnalités testées OK
- [ ] Performance satisfaisante
- [ ] Pas de crash

---

### 🟠 PRIORITÉ ÉLEVÉE

#### 2. Nettoyer Fichiers Dupliqués

**Objectif:** Supprimer les fichiers en double pour éviter confusion

**Problème identifié:**
```
appAndroid/app/src/main/java/com/ptms/mobile/database/
├── OfflineDatabaseHelper.java
└── OfflineDatabaseHelper_FIXED.java  ⚠️ DOUBLON
```

**Étapes:**

**A. Identifier la version active**
```bash
cd appAndroid/app/src/main/java/com/ptms/mobile

# Rechercher quelle version est importée
grep -r "import.*OfflineDatabaseHelper" . | grep -v ".class"
```

**B. Vérifier les différences**
```bash
cd database
# Comparer les 2 fichiers
diff OfflineDatabaseHelper.java OfflineDatabaseHelper_FIXED.java
```

**C. Décider quelle version garder**
- Si `_FIXED` est plus récent et corrige des bugs → garder `_FIXED`
- Supprimer l'autre version

**D. Mettre à jour les imports**
```bash
# Si on garde _FIXED, renommer en version principale
mv OfflineDatabaseHelper_FIXED.java OfflineDatabaseHelper.java

# Ou supprimer directement la version obsolète
rm OfflineDatabaseHelper.java  # ou _FIXED selon le cas
```

**E. Vérifier compilation**
```bash
cd appAndroid
.\gradlew.bat assembleDebug
```

**Durée estimée:** 30 minutes

**Validation:**
- [ ] Un seul fichier OfflineDatabaseHelper existe
- [ ] Compilation réussie
- [ ] Mode offline fonctionne toujours

---

#### 3. Créer Icône ic_timer

**Objectif:** Ajouter l'icône manquante pour le service Timer

**Fichier concerné:**
- `TimerService.java:338`

**TODO actuel:**
```java
.setSmallIcon(R.drawable.ic_timer) // TODO: Créer cette icône
```

**Étapes:**

**A. Créer l'icône XML**

**Fichier:** `app/src/main/res/drawable/ic_timer.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M15,1H9v2h6V1zM11,14h2V8h-2v6zm8.03-6.61l1.42-1.42c-0.43-0.51-0.9-0.99-1.41-1.41l-1.42,1.42C16.07,4.74,14.12,4,12,4c-4.97,0-9,4.03-9,9s4.02,9,9,9s9-4.03,9-9c0-2.12-0.74-4.07-1.97-5.61zM12,20c-3.87,0-7-3.13-7-7s3.13-7,7-7s7,3.13,7,7s-3.13,7-7,7z"/>
</vector>
```

**B. Vérifier l'icône**
```bash
# Compiler pour vérifier
.\gradlew.bat assembleDebug
```

**C. Tester la notification**
```bash
# Installer APK
adb install -r uploads/apk/*.apk

# Lancer l'app et démarrer le timer
# Vérifier que l'icône s'affiche dans la notification
```

**Durée estimée:** 15 minutes

**Validation:**
- [ ] Fichier `ic_timer.xml` créé
- [ ] Compilation réussie
- [ ] Icône visible dans notification Timer

---

#### 4. Implémenter Édition de Notes

**Objectif:** Permettre modification des notes existantes

**Fichier concerné:**
- `NoteViewerActivity.java:279-280`

**TODO actuel:**
```java
// TODO: Ouvrir CreateNoteUnifiedActivity en mode édition
Toast.makeText(this, "Édition non implémentée (TODO)", Toast.LENGTH_SHORT).show();
```

**Étapes:**

**A. Modifier CreateNoteUnifiedActivity**

**Fichier:** `CreateNoteUnifiedActivity.java`

```java
// Ajouter mode édition
public static final String EXTRA_NOTE_ID = "note_id";
public static final String EXTRA_EDIT_MODE = "edit_mode";

private boolean isEditMode = false;
private int editNoteId = -1;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Vérifier si mode édition
    isEditMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
    editNoteId = getIntent().getIntExtra(EXTRA_NOTE_ID, -1);

    if (isEditMode && editNoteId > 0) {
        loadExistingNote(editNoteId);
    }
}

private void loadExistingNote(int noteId) {
    // Charger note depuis DB
    OfflineDatabaseHelper dbHelper = new OfflineDatabaseHelper(this);
    ProjectNote note = dbHelper.getProjectNoteById(noteId);

    if (note != null) {
        // Pré-remplir les champs
        etTitle.setText(note.getTitle());
        etContent.setText(note.getContent());
        // ... autres champs

        // Changer titre activité
        setTitle("Modifier la note");
    }
}

private void saveNote() {
    if (isEditMode) {
        // Mode édition: UPDATE
        ProjectNote note = buildNoteFromForm();
        note.setId(editNoteId);

        OfflineDatabaseHelper dbHelper = new OfflineDatabaseHelper(this);
        dbHelper.updateProjectNote(note);

        Toast.makeText(this, "Note modifiée", Toast.LENGTH_SHORT).show();
    } else {
        // Mode création: INSERT (code existant)
        // ...
    }
}
```

**B. Modifier OfflineDatabaseHelper**

Ajouter la méthode si manquante:

```java
public ProjectNote getProjectNoteById(int noteId) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.query(
        TABLE_PROJECT_NOTES,
        null,
        "id = ?",
        new String[]{String.valueOf(noteId)},
        null, null, null
    );

    ProjectNote note = null;
    if (cursor.moveToFirst()) {
        note = cursorToProjectNote(cursor);
    }

    cursor.close();
    return note;
}

public void updateProjectNote(ProjectNote note) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();

    values.put("title", note.getTitle());
    values.put("content", note.getContent());
    values.put("type", note.getType());
    values.put("category", note.getCategory());
    values.put("audio_path", note.getAudioPath());
    values.put("is_synced", 0); // Marquer comme non synchronisé
    values.put("updated_at", getCurrentTimestamp());

    db.update(TABLE_PROJECT_NOTES, values, "id = ?",
        new String[]{String.valueOf(note.getId())});
}
```

**C. Modifier NoteViewerActivity**

```java
private void editNote() {
    // Ouvrir CreateNoteUnifiedActivity en mode édition
    Intent intent = new Intent(this, CreateNoteUnifiedActivity.class);
    intent.putExtra(CreateNoteUnifiedActivity.EXTRA_EDIT_MODE, true);
    intent.putExtra(CreateNoteUnifiedActivity.EXTRA_NOTE_ID, currentNoteId);
    startActivityForResult(intent, REQUEST_EDIT_NOTE);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_EDIT_NOTE && resultCode == RESULT_OK) {
        // Recharger la note modifiée
        loadNoteDetails(currentNoteId);
        Toast.makeText(this, "Note mise à jour", Toast.LENGTH_SHORT).show();
    }
}
```

**Durée estimée:** 2-3 heures

**Validation:**
- [ ] Bouton "Modifier" fonctionne
- [ ] Formulaire pré-rempli avec données existantes
- [ ] Sauvegarde met à jour la note (pas de duplication)
- [ ] Note marquée comme "non synchronisée"
- [ ] Sync automatique met à jour sur serveur

---

### 🟡 PRIORITÉ MOYENNE

#### 5. Afficher Vrai Nom Utilisateur dans Chat

**Objectif:** Remplacer "Utilisateur #123" par le vrai nom

**Fichier concerné:**
- `ChatActivityV2.java:398`

**TODO actuel:**
```java
"Utilisateur #" + senderId, // TODO: Récupérer le vrai nom
```

**Étapes:**

**A. Créer cache des noms utilisateurs**

```java
// Dans ChatActivityV2.java
private Map<Integer, String> userNamesCache = new HashMap<>();

private void loadUserNames() {
    // Charger depuis SharedPreferences ou API
    // Format: userId → userName
}

private String getUserName(int userId) {
    if (userNamesCache.containsKey(userId)) {
        return userNamesCache.get(userId);
    }

    // Charger depuis API si pas en cache
    loadUserNameFromApi(userId);

    return "Utilisateur #" + userId; // Fallback
}

private void loadUserNameFromApi(int userId) {
    // Appel API pour récupérer nom
    String token = sessionManager.getAuthToken();

    Call<Employee> call = apiService.getEmployeeById(token, userId);
    call.enqueue(new Callback<Employee>() {
        @Override
        public void onResponse(Call<Employee> call, Response<Employee> response) {
            if (response.isSuccessful() && response.body() != null) {
                Employee employee = response.body();
                userNamesCache.put(userId, employee.getFullName());

                // Rafraîchir l'affichage
                runOnUiThread(() -> chatAdapter.notifyDataSetChanged());
            }
        }

        @Override
        public void onFailure(Call<Employee> call, Throwable t) {
            Log.e(TAG, "Erreur chargement nom utilisateur", t);
        }
    });
}
```

**B. Utiliser dans l'adapter**

```java
// Remplacer
"Utilisateur #" + senderId

// Par
getUserName(senderId)
```

**C. Ajouter endpoint API si nécessaire**

Dans `ApiService.java`:
```java
@GET("employee/{id}")
Call<Employee> getEmployeeById(
    @Header("Authorization") String token,
    @Path("id") int userId
);
```

**Durée estimée:** 1-2 heures

**Validation:**
- [ ] Noms utilisateurs affichés dans chat
- [ ] Cache fonctionne (pas de requêtes répétées)
- [ ] Fallback "Utilisateur #X" si nom indisponible
- [ ] Performance OK (pas de lag)

---

#### 6. Ajouter Tests Unitaires

**Objectif:** Sécuriser le code avec des tests automatisés

**Fichiers à créer:**
```
app/src/test/java/com/ptms/mobile/
├── auth/
│   └── AuthenticationManagerTest.java
├── database/
│   └── OfflineDatabaseHelperTest.java
└── sync/
    └── OfflineSyncManagerTest.java
```

**A. Tests AuthenticationManager**

**Fichier:** `app/src/test/java/com/ptms/mobile/auth/AuthenticationManagerTest.java`

```java
package com.ptms.mobile.auth;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthenticationManagerTest {

    private AuthenticationManager authManager;

    @Before
    public void setUp() {
        // Setup mock context
        // authManager = AuthenticationManager.getInstance(mockContext);
    }

    @Test
    public void testPasswordHashing() {
        String password = "test123";
        String hash1 = authManager.hashPassword(password);
        String hash2 = authManager.hashPassword(password);

        // Même mot de passe → même hash
        assertEquals(hash1, hash2);

        // Hash doit être SHA-256 (64 caractères hex)
        assertEquals(64, hash1.length());
    }

    @Test
    public void testOfflineCredentialsValidation() {
        String email = "test@example.com";
        String password = "password123";

        // Sauvegarder credentials
        authManager.saveOfflineCredentials(email, password);

        // Valider credentials corrects
        assertTrue(authManager.validateOfflineCredentials(email, password));

        // Rejeter credentials incorrects
        assertFalse(authManager.validateOfflineCredentials(email, "wrongpassword"));
        assertFalse(authManager.validateOfflineCredentials("wrong@email.com", password));
    }

    @Test
    public void testLoginState() {
        // Test isLoggedIn() avec différents états
        assertFalse(authManager.isLoggedIn()); // Initial

        // Simuler login
        // authManager.saveLoginData(token, employee);
        // assertTrue(authManager.isLoggedIn());

        // Simuler logout
        // authManager.logout();
        // assertFalse(authManager.isLoggedIn());
    }
}
```

**B. Tests OfflineDatabaseHelper**

```java
package com.ptms.mobile.database;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class OfflineDatabaseHelperTest {

    private OfflineDatabaseHelper dbHelper;

    @Before
    public void setUp() {
        // Setup in-memory database pour tests
        // dbHelper = new OfflineDatabaseHelper(mockContext);
    }

    @Test
    public void testInsertAndRetrieveProject() {
        // Créer projet test
        Project project = new Project();
        project.setName("Test Project");
        project.setDescription("Description");

        // Insérer
        long id = dbHelper.insertProject(project);
        assertTrue(id > 0);

        // Récupérer
        Project retrieved = dbHelper.getProjectById((int)id);
        assertNotNull(retrieved);
        assertEquals("Test Project", retrieved.getName());
    }

    @Test
    public void testUnsyncedTimeReports() {
        // Insérer rapport non synchronisé
        TimeReport report = new TimeReport();
        report.setProjectId(1);
        report.setHours(8.0);
        report.setSynced(false);

        dbHelper.insertTimeReport(report);

        // Vérifier liste non synchronisés
        List<TimeReport> unsynced = dbHelper.getUnsyncedTimeReports();
        assertTrue(unsynced.size() > 0);

        // Marquer comme synchronisé
        dbHelper.markTimeReportAsSynced(report.getId());

        // Vérifier retiré de la liste
        unsynced = dbHelper.getUnsyncedTimeReports();
        assertEquals(0, unsynced.size());
    }
}
```

**C. Tests OfflineSyncManager**

```java
package com.ptms.mobile.sync;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class OfflineSyncManagerTest {

    private OfflineSyncManager syncManager;

    @Before
    public void setUp() {
        // syncManager = new OfflineSyncManager(mockContext);
    }

    @Test
    public void testSyncProjects() {
        // Tester sync projets
        // boolean success = syncManager.syncProjects();
        // assertTrue(success);
    }

    @Test
    public void testUploadBatch() {
        // Tester upload par lots
        // List<TimeReport> reports = createMockReports(5);
        // boolean success = syncManager.uploadBatch(reports);
        // assertTrue(success);
    }
}
```

**Durée estimée:** 4-6 heures

**Validation:**
- [ ] Tests compilent
- [ ] Tests passent: `.\gradlew.bat test`
- [ ] Coverage >50% pour classes testées

---

## 📊 RÉSUMÉ

### Tâches par Priorité

**🔴 CRITIQUE (3-4 heures):**
1. Build release APK + tests complets

**🟠 ÉLEVÉE (3-4 heures):**
2. Nettoyer fichiers dupliqués (30 min)
3. Créer icône ic_timer (15 min)
4. Implémenter édition notes (2-3h)

**🟡 MOYENNE (5-8 heures):**
5. Afficher vrais noms dans chat (1-2h)
6. Ajouter tests unitaires (4-6h)

**Total:** 11-16 heures (1-2 semaines)

---

## ✅ CHECKLIST COMPLÈTE

### Semaine 1

**Jour 1:**
- [ ] Build release APK
- [ ] Tests manuels complets (toutes fonctionnalités)
- [ ] Tests sur Android 7, 10, 12, 14

**Jour 2:**
- [ ] Nettoyer fichiers dupliqués
- [ ] Créer icône ic_timer
- [ ] Vérifier compilation

**Jour 3-4:**
- [ ] Implémenter édition notes
- [ ] Tests édition notes
- [ ] Vérifier sync

**Jour 5:**
- [ ] Afficher vrais noms dans chat
- [ ] Tests chat

### Semaine 2

**Jour 1-2:**
- [ ] Écrire tests unitaires (Auth)
- [ ] Écrire tests unitaires (Database)
- [ ] Écrire tests unitaires (Sync)

**Jour 3:**
- [ ] Exécuter tous les tests
- [ ] Corriger tests échoués
- [ ] Vérifier coverage

**Jour 4:**
- [ ] Build final release APK
- [ ] Tests de régression complets
- [ ] Documentation mises à jour

**Jour 5:**
- [ ] Review code complet
- [ ] Validation finale
- [ ] Prêt pour production ✅

---

## 🎯 RÉSULTAT ATTENDU

**Avant:** 8.5/10
**Après:** 9.5/10

**Améliorations:**
- ✅ APK release testé et validé
- ✅ Code nettoyé (pas de doublons)
- ✅ Tous les TODOs critiques résolus
- ✅ Tests unitaires ajoutés
- ✅ Application production-ready

---

## 📋 COMMANDES UTILES

```bash
# Build release
cd appAndroid
.\gradlew.bat assembleRelease

# Build debug
.\gradlew.bat assembleDebug

# Clean build
.\gradlew.bat clean assembleDebug

# Run tests
.\gradlew.bat test

# Install APK
adb install -r app/build/outputs/apk/release/*.apk

# Logs
adb logcat -s PTMS:*
```

---

**Créé:** 22 Octobre 2025
**Auteur:** Audit PTMS Mobile
**Durée totale:** 1-2 semaines (11-16 heures)
**Objectif:** Application production-ready à 9.5/10
