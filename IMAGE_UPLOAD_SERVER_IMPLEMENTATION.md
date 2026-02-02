# 📸 IMPLÉMENTATION SERVER-SIDE - Upload Images Notes

**Date**: 2025-10-24
**Status**: ✅ Code complété - Migration DB à exécuter

---

## 🎯 Vue d'ensemble

Implémentation côté serveur PHP pour recevoir les images uploadées depuis l'application Android et les stocker dans la base de données.

---

## 📦 Fichiers modifiés

### 1. **api/project-notes.php** (endpoint principal)

**Modifications apportées:**

#### a) Ajout gestion upload image dans `handlePost()` (ligne ~246-257)
```php
// Gérer l'upload du fichier image
$imagePath = null;
if (isset($_FILES['image_file']) && $_FILES['image_file']['error'] === UPLOAD_ERR_OK) {
    $uploadResult = handleImageUpload($_FILES['image_file'], $projectId, $userId);
    if ($uploadResult['success']) {
        $imagePath = $uploadResult['path'];
    } else {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Erreur upload image: ' . $uploadResult['error']]);
        return;
    }
}
```

#### b) Mise à jour SQL INSERT pour inclure image_path (ligne ~302-311)
```php
$sql = "INSERT INTO project_notes (
    project_id, user_id, note_type, note_type_id, note_group, title, content,
    audio_path, audio_duration, image_path, transcription, is_important, tags, created_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

$result = $db->query($sql, [
    $projectId, $userId, $noteType, $noteTypeId, $noteGroup, $title, $content,
    $audioPath, $audioDuration, $imagePath ?? null, $transcription, $isImportant, $tags
]);
```

#### c) Nouvelle fonction `handleImageUpload()` (ligne ~543-595)
```php
function handleImageUpload($file, $projectId, $userId) {
    try {
        // Vérifier le type de fichier
        $allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
        $fileType = strtolower($file['type']);

        if (!in_array($fileType, $allowedTypes)) {
            return ['success' => false, 'error' => 'Type de fichier image non autorisé'];
        }

        // Vérifier la taille (max 10MB - les images devraient être compressées côté Android)
        $maxSize = 10 * 1024 * 1024;
        if ($file['size'] > $maxSize) {
            return ['success' => false, 'error' => 'Fichier trop volumineux (max 10MB)'];
        }

        // Créer le répertoire de destination
        $folderName = $projectId ? $projectId : 'personal';
        $uploadDir = __DIR__ . '/../uploads/image_notes/' . $folderName . '/';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }

        // Générer un nom de fichier unique
        $extension = pathinfo($file['name'], PATHINFO_EXTENSION);
        if (empty($extension)) {
            $extension = 'jpg';
        }
        $filename = 'img_' . $userId . '_' . time() . '_' . uniqid() . '.' . strtolower($extension);
        $uploadPath = $uploadDir . $filename;

        // Déplacer le fichier uploadé
        if (!move_uploaded_file($file['tmp_name'], $uploadPath)) {
            return ['success' => false, 'error' => 'Erreur lors de la sauvegarde du fichier'];
        }

        // Retourner le chemin relatif
        $relativePath = 'uploads/image_notes/' . $folderName . '/' . $filename;

        return [
            'success' => true,
            'path' => $relativePath
        ];

    } catch (Exception $e) {
        error_log("Erreur upload image: " . $e->getMessage());
        return ['success' => false, 'error' => 'Erreur serveur lors de l\'upload'];
    }
}
```

#### d) Ajout imagePath dans `formatNote()` (ligne ~632)
```php
return [
    'id' => (int)$note['id'],
    'projectId' => (int)$note['project_id'],
    'projectName' => $note['project_name'] ?? null,
    'userId' => (int)$note['user_id'],
    'noteType' => $note['note_type'],
    'noteGroup' => $note['note_group'] ?? 'project',
    'title' => $note['title'],
    'content' => $note['content'],
    'audioPath' => $note['audio_path'],
    'audioDuration' => $note['audio_duration'] ? (int)$note['audio_duration'] : null,
    'imagePath' => $note['image_path'] ?? null,  // ✅ AJOUTÉ
    'transcription' => $note['transcription'],
    'isImportant' => (bool)$note['is_important'],
    'tags' => $note['tags'] ? explode(',', $note['tags']) : [],
    'authorName' => $authorName,
    'createdAt' => $note['created_at'],
    'updatedAt' => $note['updated_at']
];
```

#### e) Suppression image dans `handleDelete()` (ligne ~469-475)
```php
// Supprimer le fichier image si présent
if (!empty($note['image_path'])) {
    $imageFile = __DIR__ . '/../' . $note['image_path'];
    if (file_exists($imageFile)) {
        unlink($imageFile);
    }
}
```

---

## 🗄️ Migration Base de Données

### Fichier créé: `database/migrations/2025_10_24_0001_add_image_support_to_project_notes.sql`

```sql
-- Ajouter la colonne image_path si elle n'existe pas
SET @col_exists = (SELECT COUNT(*)
                   FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'project_notes'
                   AND COLUMN_NAME = 'image_path');

SET @sql_add_col = IF(@col_exists = 0,
  'ALTER TABLE `project_notes` ADD COLUMN `image_path` VARCHAR(500) DEFAULT NULL COMMENT \'Chemin fichier image\' AFTER `audio_duration`',
  'SELECT 1');
PREPARE stmt FROM @sql_add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Vérifier la structure finale
SELECT 'Migration terminée: image_path ajoutée' AS Status;
DESCRIBE `project_notes`;
```

### ⚠️ À EXÉCUTER QUAND MYSQL EST DISPONIBLE:

```bash
mysql -u root -p ptms_db < database/migrations/2025_10_24_0001_add_image_support_to_project_notes.sql
```

Ou via l'interface web MySQL/phpMyAdmin.

---

## 📁 Structure de stockage

Les images seront stockées dans:
```
uploads/
  └── image_notes/
      ├── personal/               # Notes personnelles (sans projet)
      │   ├── img_1_1729732800_abc123.jpg
      │   └── img_2_1729732900_def456.jpg
      └── {project_id}/           # Notes par projet
          ├── img_1_1729733000_ghi789.jpg
          └── img_3_1729733100_jkl012.jpg
```

**Format des noms de fichiers:**
- Préfixe: `img_`
- User ID: `{userId}_`
- Timestamp: `{time()}_`
- Unique ID: `{uniqid()}`
- Extension: `.jpg|.png|.gif|.webp`

**Exemple:** `img_5_1729732800_6718a3c0f12d8.jpg`

---

## 🔧 Validation côté serveur

### Types de fichiers acceptés:
- `image/jpeg`
- `image/jpg`
- `image/png`
- `image/gif`
- `image/webp`

### Taille maximale:
- **10 MB** (les images sont compressées côté Android à max 2MB normalement)

### Sécurité:
- Vérification du type MIME
- Génération de noms uniques (évite écrasement)
- Création automatique des dossiers avec permissions 0755
- Suppression automatique lors de la suppression de la note

---

## 🔄 Flux de données

### Upload (Android → Server):

1. **Android** envoie requête `POST` multipart à `/api/project-notes.php`
   - Headers: `Authorization: Bearer {token}`
   - Body multipart:
     - `project_id` (optionnel)
     - `note_type`, `note_type_id`, `note_group`
     - `title`, `content`, `transcription`
     - `is_important`, `tags`
     - `audio_file` (optionnel, Part)
     - **`image_file`** (optionnel, Part) ← NOUVEAU

2. **Server PHP** traite la requête:
   - Authentification JWT/Session
   - Validation des champs
   - Upload audio (si présent)
   - **Upload image (si présent)** ← NOUVEAU
   - Insertion en DB avec `image_path`
   - Retour JSON avec note créée

3. **Android** reçoit la réponse:
   - `success: true`
   - `noteId: 123`
   - `note: { ... imagePath: "uploads/image_notes/5/img_1_xxx.jpg" }`

### Récupération (Server → Android):

1. **Android** demande les notes: `GET /api/project-notes.php?all=1`

2. **Server** retourne les notes avec `imagePath`:
```json
{
  "success": true,
  "notes": [
    {
      "id": 123,
      "title": "Ma note avec image",
      "imagePath": "uploads/image_notes/5/img_1_1729732800_abc123.jpg",
      ...
    }
  ]
}
```

3. **Android** affiche l'image:
   - URL complète: `https://server.com/uploads/image_notes/5/img_1_xxx.jpg`
   - Chargement via Glide/Picasso

---

## ✅ Checklist implémentation

- [x] Ajout paramètre `image_file` dans `handlePost()`
- [x] Fonction `handleImageUpload()` créée
- [x] Validation type MIME et taille
- [x] Génération noms de fichiers uniques
- [x] Création dossiers automatique
- [x] Mise à jour SQL INSERT avec `image_path`
- [x] Ajout `imagePath` dans `formatNote()`
- [x] Suppression image dans `handleDelete()`
- [x] Migration SQL créée
- [ ] Migration SQL exécutée (⚠️ **À FAIRE quand MySQL disponible**)
- [ ] Test upload depuis Android
- [ ] Test récupération et affichage

---

## 🧪 Tests à effectuer

### 1. Test migration SQL:
```sql
-- Vérifier que la colonne existe
DESCRIBE project_notes;

-- Devrait afficher:
-- image_path | varchar(500) | YES | | NULL |
```

### 2. Test upload:
- Depuis l'app Android, créer une note avec image
- Vérifier que le fichier est créé dans `uploads/image_notes/`
- Vérifier que `image_path` est sauvegardé en DB

### 3. Test récupération:
- Récupérer les notes via API
- Vérifier que `imagePath` est présent dans la réponse JSON

### 4. Test suppression:
- Supprimer une note avec image
- Vérifier que le fichier physique est supprimé
- Vérifier que l'enregistrement DB est supprimé

---

## 🔍 Dépannage

### Problème: "Permission denied" lors de l'upload

**Solution:**
```bash
# Sur Linux/Mac:
chmod 755 uploads/
chmod -R 755 uploads/image_notes/

# Sur Windows: Vérifier les permissions du dossier uploads
```

### Problème: "Type de fichier non autorisé"

**Cause:** Type MIME incorrect envoyé par Android

**Solution:** Vérifier que PhotoManager envoie le bon MIME type:
```java
RequestBody imageBody = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
```

### Problème: "Fichier trop volumineux"

**Cause:** Image > 10MB

**Solution:** Vérifier la compression côté Android (déjà implémentée, max 1920x1080 à 85%)

---

## 📊 Statistiques implémentation

**Lignes de code ajoutées:**
- `api/project-notes.php`: ~80 lignes
- Migration SQL: ~20 lignes

**Fonctionnalités:**
- ✅ Upload multipart
- ✅ Validation MIME type
- ✅ Validation taille
- ✅ Génération noms uniques
- ✅ Stockage organisé par projet
- ✅ Suppression automatique
- ✅ Support notes personnelles

**Compatibilité:**
- ✅ Android (multipart)
- ✅ Web (JSON - à implémenter si besoin)

---

## 🔗 Fichiers liés

**Android:**
- `NoteEditorActivity.java` - Interface et upload
- `ApiService.java` - Définition endpoints
- `PhotoManager.java` - Gestion photos
- `MediaStorageManager.java` - Stockage local

**Server:**
- `api/project-notes.php` - Endpoint principal ✅ MODIFIÉ
- `database/migrations/2025_10_24_0001_add_image_support_to_project_notes.sql` ✅ CRÉÉ

**Documentation:**
- `IMAGE_IMPORT_IMPLEMENTATION.md` - Implémentation Android
- `IMAGE_UPLOAD_SERVER_IMPLEMENTATION.md` - Ce fichier

---

**Status final**: ✅ Code serveur complet et testé (syntaxe). Migration DB prête à être exécutée.

**Prochaine étape**: Démarrer MySQL et exécuter la migration, puis tester l'upload end-to-end depuis l'app Android.
