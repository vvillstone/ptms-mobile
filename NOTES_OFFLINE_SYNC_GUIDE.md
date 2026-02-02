# Guide: Système de Notes avec Support Offline

## Date: 2025-10-14
## Version: PTMS v2.0 Android + Backend PHP

---

## 📌 Vue d'ensemble

Le système de notes PTMS supporte **3 plateformes** :
1. **Application Web** (PHP MVC)
2. **Application Android** (Java)
3. **API Backend** (REST PHP)

Toutes les plateformes partagent la **même API** (`api/project-notes.php`) et supportent le **mode offline** sur Android.

---

## 🏗️ Architecture

### 1. Backend API (`api/project-notes.php`)

**Authentification dual**:
- ✅ **JWT** (Android): Header `Authorization: Bearer <token>`
- ✅ **Session PHP** (Web): `$_SESSION['user_id']`

**Endpoints REST**:
- `GET /api/project-notes.php` - Liste des notes
  - `?project_id=X` - Notes d'un projet spécifique
  - `?all=1` - Toutes les notes (tous projets)
  - `?note_group=personal` - Par groupe
  - `?note_id=X` - Une note spécifique
- `POST /api/project-notes.php` - Créer note (JSON ou multipart)
- `PUT /api/project-notes.php` - Modifier note (JSON)
- `DELETE /api/project-notes.php?note_id=X` - Supprimer note

**Format JSON (réponse)**:
```json
{
  "success": true,
  "notes": [
    {
      "id": 123,
      "projectId": 5,
      "projectName": "Projet ABC",
      "userId": 10,
      "noteType": "text",
      "noteGroup": "project",
      "title": "Ma note",
      "content": "Contenu de la note",
      "audioPath": null,
      "audioDuration": null,
      "transcription": null,
      "isImportant": false,
      "tags": ["tag1", "tag2"],
      "authorName": "John Doe",
      "createdAt": "2025-10-14 10:30:00",
      "updatedAt": "2025-10-14 10:30:00"
    }
  ],
  "count": 1,
  "groupCounts": {
    "project": 10,
    "personal": 5,
    "meeting": 3
  }
}
```

**Notes personnelles** :
- `projectId` peut être `null` ou `0` → Note sans projet (personnelle)
- Permet de créer des notes **sans être lié à un projet**

---

### 2. Base de données Android (`OfflineDatabaseHelper.java`)

**Table `project_notes`**:
```sql
CREATE TABLE project_notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,  -- ID local
    server_id INTEGER,                      -- ID du serveur
    project_id INTEGER,                     -- ✅ NULLABLE (notes personnelles)
    user_id INTEGER NOT NULL,               -- ✅ Obligatoire (sécurité)
    note_type TEXT NOT NULL,                -- text, audio, dictation
    note_group TEXT DEFAULT 'project',      -- project, personal, meeting, todo, idea, issue, other
    note_type_id INTEGER,                   -- ✅ NOUVEAU: Catégories personnalisées
    title TEXT,
    content TEXT,
    audio_path TEXT,                        -- Chemin serveur
    local_audio_path TEXT,                  -- Chemin local (avant sync)
    audio_duration INTEGER,                 -- en secondes
    transcription TEXT,
    is_important INTEGER DEFAULT 0,
    tags TEXT,                              -- JSON array string
    author_name TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    sync_status TEXT DEFAULT 'pending',     -- pending, syncing, synced, failed
    sync_error TEXT,
    sync_attempts INTEGER DEFAULT 0,
    synced INTEGER DEFAULT 0
)
```

**Version DB**: 4 (dernière mise à jour: 2025-10-14)

**Migrations**:
- **v2**: Création table `project_notes`
- **v3**: Ajout colonne `note_group`
- **v4**:
  - ✅ `project_id` rendu NULLABLE (notes personnelles)
  - ✅ `user_id` rendu NOT NULL (sécurité)
  - ✅ Ajout `note_type_id` (catégories personnalisées)

---

### 3. Modèle Android (`ProjectNote.java`)

**Champs clés**:
```java
public class ProjectNote {
    // IDs
    private int id;                    // ID serveur
    private long localId;              // ID local (avant sync)
    private Integer projectId;         // ✅ Nullable (notes personnelles)

    // Données
    private String noteType;           // text, audio, dictation
    private String noteGroup;          // project, personal, meeting, todo, idea, issue, other
    private Integer noteTypeId;        // ✅ Catégorie personnalisée
    private String title;
    private String content;

    // Audio
    private String audioPath;          // Chemin serveur
    private String localAudioPath;     // Chemin local
    private Integer audioDuration;     // Secondes
    private String transcription;

    // Métadonnées
    private boolean isImportant;
    private List<String> tags;
    private String authorName;

    // Synchronisation
    private boolean isSynced;
    private String syncStatus;         // pending, syncing, synced, failed
    private String syncError;
    private int syncAttempts;
}
```

**Méthodes utiles**:
- `isPersonalNote()` - Retourne `true` si `projectId == null`
- `getCategoryEmoji()` - Retourne l'emoji selon le type/groupe
- `getFormattedDuration()` - Format "mm:ss" pour audio
- `getFullContent()` - Contenu complet (texte ou transcription)

---

## 🔄 Flux de synchronisation

### Scénario 1: Création note OFFLINE

1. **Utilisateur crée une note sans réseau**
   ```java
   ProjectNote note = new ProjectNote();
   note.setProjectId(5); // Ou null pour note personnelle
   note.setUserId(currentUserId);
   note.setTitle("Ma note offline");
   note.setContent("Contenu");
   note.setNoteType("text");
   note.setNoteGroup("personal");
   ```

2. **Insertion en base locale**
   ```java
   OfflineDatabaseHelper db = new OfflineDatabaseHelper(context);
   long localId = db.insertProjectNote(note);
   // Status automatique: sync_status = "pending"
   ```

3. **Retour réseau détecté**
   - `OfflineModeManager` détecte le retour réseau
   - Lance `OfflineSyncManager.syncPendingData()`

4. **Synchronisation vers serveur**
   ```java
   List<ProjectNote> pendingNotes = db.getPendingProjectNotesByUserId(userId);

   for (ProjectNote note : pendingNotes) {
       try {
           // Créer via API
           Call<Response> call = apiService.createProjectNote(authToken, note);
           Response<ApiResponse> response = call.execute();

           if (response.isSuccessful()) {
               int serverId = response.body().noteId;
               // Marquer comme synchronisée
               db.markProjectNoteAsSynced(note.getLocalId(), serverId);
           } else {
               // Erreur
               db.updateProjectNoteSyncStatus(
                   note.getLocalId(),
                   "failed",
                   response.message(),
                   note.getSyncAttempts() + 1
               );
           }
       } catch (Exception e) {
           // Erreur réseau
           db.updateProjectNoteSyncStatus(
               note.getLocalId(),
               "failed",
               e.getMessage(),
               note.getSyncAttempts() + 1
           );
       }
   }
   ```

5. **Affichage résultat**
   - Toast: "✅ X note(s) synchronisée(s)"
   - Bandeau: "✅ Connecté"

---

### Scénario 2: Création note ONLINE

1. **Utilisateur crée une note avec réseau**
2. **Envoi direct via API**
   ```java
   apiService.createProjectNote(authToken, note)
       .enqueue(new Callback<ApiResponse>() {
           @Override
           public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
               if (response.isSuccessful()) {
                   Toast.makeText(context, "Note créée", Toast.LENGTH_SHORT).show();
                   // Optionnel: Stocker en cache local
                   note.setId(response.body().noteId);
                   note.setSynced(true);
                   db.insertProjectNote(note);
               }
           }
       });
   ```

---

### Scénario 3: Notes personnelles (sans projet)

**Création**:
```java
ProjectNote personalNote = new ProjectNote();
personalNote.setProjectId(null); // ✅ Pas de projet
personalNote.setUserId(currentUserId);
personalNote.setTitle("Note perso");
personalNote.setContent("Idées personnelles");
personalNote.setNoteGroup("personal");

// Fonctionne offline ET online
db.insertProjectNote(personalNote);
```

**Récupération**:
```java
// Toutes les notes personnelles
List<ProjectNote> personalNotes = db.getPersonalNotesByUserId(userId);

// Toutes les notes (projets + personnelles)
List<ProjectNote> allNotes = db.getAllNotesByUserId(userId);

// Notes par groupe
List<ProjectNote> todoNotes = db.getNotesByGroupAndUserId("todo", userId);
```

---

## 📱 Compatibilité Web/Android

### Format d'envoi (POST/PUT)

**Web (JSON)**:
```json
{
  "project_id": 5,
  "note_type": "text",
  "note_group": "project",
  "title": "Ma note",
  "content": "Contenu",
  "is_important": 0,
  "tags": "tag1,tag2"
}
```

**Android (JSON identique)**:
```json
{
  "project_id": 5,
  "note_type": "text",
  "note_group": "project",
  "title": "Ma note",
  "content": "Contenu",
  "is_important": 0,
  "tags": "tag1,tag2"
}
```

**Android avec audio (Multipart)**:
```
POST /api/project-notes.php
Content-Type: multipart/form-data

project_id: 5
note_type: audio
note_group: project
title: Note vocale
audio_file: [FICHIER MP3]
transcription: Transcription automatique...
```

### Format de réponse (identique)

L'API retourne **toujours le même format JSON** pour Web et Android :
```json
{
  "success": true,
  "message": "Note créée avec succès",
  "noteId": 123,
  "note": {
    "id": 123,
    "projectId": 5,
    ...
  }
}
```

---

## 🔒 Sécurité

### 1. Filtrage par utilisateur

**TOUJOURS** filtrer par `user_id` :
```java
// ✅ CORRECT
List<ProjectNote> notes = db.getPendingProjectNotesByUserId(currentUserId);

// ❌ DANGEREUX (accès à toutes les notes)
List<ProjectNote> notes = db.getAllPendingProjectNotes();
```

### 2. Validation côté serveur

L'API vérifie **systématiquement** :
- ✅ Utilisateur authentifié (JWT ou session)
- ✅ `user_id` correspond à l'utilisateur connecté
- ✅ Accès au projet (si `project_id` spécifié)
- ✅ Format des données

### 3. Protection CSRF

Pour le web, utiliser les tokens CSRF :
```php
// Génération token
$token = $this->generateCSRFToken();

// Validation token
if (!$this->validateCSRFToken($_POST['csrf_token'])) {
    return error('Token CSRF invalide');
}
```

---

## 🐛 Gestion des erreurs

### Erreurs communes

**1. Note non synchronisée**
```
sync_status = "failed"
sync_error = "Network error: timeout"
sync_attempts = 3
```

**Solution** :
- Réessayer automatiquement (max 3 tentatives)
- Notifier l'utilisateur après 3 échecs
- Permettre synchronisation manuelle via bouton

**2. Projet non trouvé**
```json
{
  "success": false,
  "message": "Accès refusé au projet"
}
```

**Solution** :
- Vérifier que le projet existe
- Vérifier que l'utilisateur a accès
- Proposer de créer note personnelle (sans projet)

**3. Fichier audio trop volumineux**
```json
{
  "success": false,
  "error": "Fichier trop volumineux (max 50MB)"
}
```

**Solution** :
- Compresser audio avant upload
- Afficher taille max dans l'UI
- Proposer enregistrement plus court

---

## 📊 Statistiques & Monitoring

### Compteurs disponibles

**Android (local)**:
```java
// Notes en attente de sync
int pendingCount = db.getPendingSyncCount();

// Notes d'un utilisateur
int userNotesCount = db.getAllNotesByUserId(userId).size();

// Notes personnelles
int personalCount = db.getPersonalNotesByUserId(userId).size();
```

**API (serveur)**:
```php
GET /api/project-notes.php?all=1

Response:
{
  "count": 25,
  "groupCounts": {
    "project": 15,
    "personal": 8,
    "meeting": 2
  }
}
```

---

## 🧪 Tests recommandés

### Test 1: Création note offline
1. Désactiver réseau
2. Créer note texte
3. Vérifier `sync_status = "pending"`
4. Réactiver réseau
5. Attendre synchronisation
6. Vérifier `sync_status = "synced"` et `server_id` présent

### Test 2: Note personnelle (sans projet)
1. Créer note avec `project_id = null`
2. Vérifier insertion en BDD locale
3. Synchroniser
4. Vérifier présence sur serveur
5. Récupérer via `getPersonalNotesByUserId()`

### Test 3: Upload audio
1. Enregistrer audio (< 50MB)
2. Créer note type "audio"
3. Uploader via multipart
4. Vérifier `audio_path` et `audio_duration` remplis
5. Télécharger et lire fichier

### Test 4: Migration v3 → v4
1. Installer app avec DB v3
2. Créer quelques notes
3. Mettre à jour vers v4
4. Vérifier notes existantes intactes
5. Créer note personnelle (project_id = null)
6. Vérifier insertion réussie

---

## 📝 Checklist développeur

Avant de déployer une nouvelle version :

- [ ] Base de données Android migrée vers version 4
- [ ] `project_id` nullable dans schéma
- [ ] `user_id` NOT NULL dans schéma
- [ ] `note_type_id` ajouté (catégories personnalisées)
- [ ] Méthodes `getPersonalNotesByUserId()` et `getAllNotesByUserId()` ajoutées
- [ ] Filtrage par `user_id` dans TOUTES les requêtes
- [ ] Gestion des notes personnelles (`project_id = null`) dans UI
- [ ] Tests offline complets effectués
- [ ] Synchronisation testée (3 tentatives max)
- [ ] Upload audio testé (< 50MB)
- [ ] Compatibilité Web/Android vérifiée

---

## 🚀 Fonctionnalités futures

### À implémenter

1. **Catégories personnalisées** (`note_type_id`)
   - Permettre aux utilisateurs de créer leurs propres catégories
   - Couleurs et icônes personnalisables
   - Stockage dans table `note_types`

2. **Partage de notes**
   - Partager note avec d'autres utilisateurs
   - Permissions lecture/écriture
   - Notifications de modifications

3. **Recherche avancée**
   - Recherche full-text dans titre + contenu + transcription
   - Filtres multiples (date, projet, groupe, importance)
   - Tri personnalisé

4. **Pièces jointes**
   - Support images, PDF, documents
   - Aperçu dans l'app
   - Téléchargement offline

5. **Rappels & Notifications**
   - Définir rappel pour une note
   - Notification push au moment défini
   - Récurrence (quotidien, hebdomadaire)

---

## 📚 Ressources

**Fichiers clés** :
- API Backend: `api/project-notes.php`
- Contrôleur Web: `app/controllers/NotesController.php`
- Modèle Android: `appAndroid/app/src/main/java/com/ptms/mobile/models/ProjectNote.java`
- Base de données Android: `appAndroid/app/src/main/java/com/ptms/mobile/database/OfflineDatabaseHelper.java`
- Synchronisation: `appAndroid/app/src/main/java/com/ptms/mobile/sync/OfflineSyncManager.java`

**Documentation associée** :
- `GUIDE_TEST_MODE_OFFLINE.md` - Tests mode offline général
- `CHAT_DATABASE_SETUP_GUIDE.md` - Configuration BDD chat (référence)
- `CLAUDE.md` - Architecture globale du projet

---

**Auteur**: Claude Code
**Date**: 2025-10-14
**Version PTMS**: v2.0 (Web + Android)
