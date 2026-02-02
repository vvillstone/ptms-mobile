# 📊 PATTERN DE SYNCHRONISATION DES DONNÉES - PTMS

**Date**: 2025-01-19
**Version**: 1.0
**Application**: PTMS v2.0 (Web + Android)

---

## 🎯 OBJECTIF

Assurer la **cohérence totale** des structures de données entre:
1. **Serveur Web** (PHP + MySQL)
2. **Android Online** (API REST)
3. **Android Offline** (SQLite local)

---

## 📐 ARCHITECTURE DES DONNÉES

```
┌──────────────────────────────────────────────────────────────┐
│                    SERVEUR WEB (MySQL)                        │
│                   ptms_db (Production)                        │
│                                                                │
│  project_list, work_type_list, report_list, users            │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         │ API REST (JSON)
                         │ /api/employee/*
                         │
┌────────────────────────▼─────────────────────────────────────┐
│             ANDROID APP (Java + SQLite)                       │
│                                                                │
│  ┌────────────────┐          ┌─────────────────┐             │
│  │  MODE ONLINE   │          │  MODE OFFLINE   │             │
│  │                │          │                 │             │
│  │  ApiService    │◄────────►│ SQLiteDatabase  │             │
│  │  (Retrofit)    │  Sync    │ (ptms_offline)  │             │
│  │                │          │                 │             │
│  │  Models:       │          │  Tables:        │             │
│  │  - Project     │          │  - projects     │             │
│  │  - TimeReport  │          │  - time_reports │             │
│  │  - WorkType    │          │  - work_types   │             │
│  │  - ProjectNote │          │  - project_notes│             │
│  │  - NoteType    │          │  - note_types   │             │
│  └────────────────┘          └─────────────────┘             │
└──────────────────────────────────────────────────────────────┘
```

---

## 🗄️ TABLES & MODELS - MAPPING COMPLET

### 1️⃣ **PROJETS** (`project_list` ↔ `Project` ↔ `projects`)

#### **MySQL** (Serveur) - Table `project_list`
```sql
CREATE TABLE project_list (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    status TINYINT(1) NOT NULL DEFAULT 0,          -- 0=inactif, 1=actif
    is_placeholder TINYINT(1) DEFAULT 0,
    delete_flag TINYINT(1) NOT NULL DEFAULT 0,
    date_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    assigned_user_id INT,                          -- Foreign key vers users
    client VARCHAR(255),
    priority ENUM('low', 'medium', 'high') DEFAULT 'medium',
    progress DECIMAL(5,2) DEFAULT 0.00
);
```

#### **Java Model** (Android) - Class `Project`
```java
public class Project {
    private int id;                    // ✅ id
    private String name;               // ✅ name
    private String description;        // ✅ description
    private int status;                // ✅ status (0/1)
    private boolean isPlaceholder;     // ✅ is_placeholder
    private String assignedUserId;     // ✅ assigned_user_id
    private String client;             // ✅ client
    private String priority;           // ✅ priority (low/medium/high)
    private double progress;           // ✅ progress
    private String dateCreated;        // ✅ date_created (ISO 8601)
    private String dateUpdated;        // ✅ date_updated (ISO 8601)
}
```

#### **SQLite** (Android Offline) - Table `projects`
```sql
CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER,                               -- ✅ Référence au serveur
    name TEXT NOT NULL,                              -- ✅ name
    description TEXT,                                -- ✅ description
    project_code TEXT,                               -- ❌ Non utilisé (à supprimer)
    status TEXT DEFAULT 'active',                    -- ⚠️ ERREUR: devrait être INTEGER!
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,   -- ✅ date_created
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,   -- ✅ date_updated
    synced INTEGER DEFAULT 0                         -- ✅ Statut de synchronisation
);
```

#### ⚠️ **PROBLÈMES IDENTIFIÉS**
1. **Type incompatible**: `status` est TEXT en SQLite mais INT en MySQL
2. **Colonne inutile**: `project_code` n'existe pas dans le modèle
3. **Colonnes manquantes**: `assigned_user_id`, `client`, `priority`, `progress`

#### ✅ **CORRECTION PROPOSÉE**
```sql
-- ✅ NOUVELLE STRUCTURE SQLite (projects)
CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER UNIQUE,                        -- ID du serveur MySQL
    name TEXT NOT NULL,
    description TEXT,
    status INTEGER NOT NULL DEFAULT 1,               -- ✅ 0=inactif, 1=actif
    is_placeholder INTEGER DEFAULT 0,
    assigned_user_id INTEGER,                        -- ✅ AJOUTÉ
    client VARCHAR(255),                             -- ✅ AJOUTÉ
    priority TEXT DEFAULT 'medium',                  -- ✅ AJOUTÉ (low/medium/high)
    progress REAL DEFAULT 0.00,                      -- ✅ AJOUTÉ
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    synced INTEGER DEFAULT 1                         -- 1=sync, 0=en attente
);
```

---

### 2️⃣ **RAPPORTS DE TEMPS** (`report_list` ↔ `TimeReport` ↔ `time_reports`)

#### **MySQL** (Serveur) - Table `report_list`
```sql
CREATE TABLE report_list (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,                         -- Foreign key vers project_list
    employee_id INT NOT NULL,                        -- Foreign key vers employee_list/users
    report_date DATE,
    work_type_id INT NOT NULL,                       -- Foreign key vers work_type_list
    description TEXT NOT NULL,
    datetime_from DATETIME NOT NULL,
    datetime_to DATETIME NOT NULL,
    duration FLOAT NOT NULL DEFAULT 0,               -- Durée en secondes
    date_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    hours DECIMAL(5,2),                              -- Heures calculées
    timer_started_at DATETIME,                       -- Timer feature
    timer_paused_duration INT DEFAULT 0,
    validation_status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
    validated_by INT,
    validated_at DATETIME,
    rejection_reason TEXT
);
```

#### **Java Model** (Android) - Class `TimeReport`
```java
public class TimeReport {
    private int id;                    // ✅ id
    private int projectId;             // ✅ project_id
    private int employeeId;            // ✅ employee_id
    private int workTypeId;            // ✅ work_type_id
    private String reportDate;         // ✅ report_date (YYYY-MM-DD)
    private String datetimeFrom;       // ✅ datetime_from (ISO 8601)
    private String datetimeTo;         // ✅ datetime_to (ISO 8601)
    private double hours;              // ✅ hours
    private String description;        // ✅ description
    private String validationStatus;   // ✅ validation_status (pending/approved/rejected)
    private String projectName;        // ✅ Jointure (cache local)
    private String workTypeName;       // ✅ Jointure (cache local)
    private String dateCreated;        // ✅ date_created (ISO 8601)
    private String dateUpdated;        // ✅ date_updated (ISO 8601)

    // Champs locaux (offline)
    private boolean isSynced = true;   // ✅ Statut sync
    private boolean isLocal = false;   // ✅ Provient du cache local
}
```

#### **SQLite** (Android Offline) - Table `time_reports`
```sql
CREATE TABLE time_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER,                               -- ✅ ID du serveur MySQL
    project_id INTEGER,                              -- ✅ project_id
    employee_id INTEGER,                             -- ✅ employee_id
    work_type_id INTEGER,                            -- ✅ work_type_id
    report_date TEXT,                                -- ✅ report_date (YYYY-MM-DD)
    datetime_from TEXT,                              -- ✅ datetime_from (ISO 8601)
    datetime_to TEXT,                                -- ✅ datetime_to (ISO 8601)
    hours REAL,                                      -- ✅ hours
    description TEXT,                                -- ✅ description
    validation_status TEXT DEFAULT 'pending',        -- ✅ validation_status
    project_name TEXT,                               -- ✅ Cache local (pour affichage offline)
    work_type_name TEXT,                             -- ✅ Cache local (pour affichage offline)
    date_created DATETIME DEFAULT CURRENT_TIMESTAMP, -- ✅ date_created
    date_updated DATETIME DEFAULT CURRENT_TIMESTAMP, -- ✅ date_updated
    sync_status TEXT DEFAULT 'pending',              -- ✅ Statut de synchronisation
    sync_error TEXT,                                 -- ✅ Message d'erreur si échec
    sync_attempts INTEGER DEFAULT 0                  -- ✅ Nombre de tentatives
);
```

#### ✅ **VALIDATION**
- **Structure**: ✅ Cohérente
- **Types**: ✅ Compatibles (TEXT pour dates, REAL pour nombres)
- **Statut sync**: ✅ Présent

---

### 3️⃣ **TYPES DE TRAVAIL** (`work_type_list` ↔ `WorkType` ↔ `work_types`)

#### **MySQL** (Serveur) - Table `work_type_list`
```sql
CREATE TABLE work_type_list (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    status TINYINT(4) NOT NULL DEFAULT 1,            -- 0=inactif, 1=actif
    delete_flag TINYINT(1) NOT NULL DEFAULT 0,
    date_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);
```

#### **Java Model** (Android) - Class `WorkType`
```java
public class WorkType {
    private int id;                    // ✅ id
    private String name;               // ✅ name
    private String description;        // ✅ description
    private int status;                // ✅ status (0/1)
    private String dateCreated;        // ✅ date_created (ISO 8601)
    private String dateUpdated;        // ✅ date_updated (ISO 8601)
}
```

#### **SQLite** (Android Offline) - Table `work_types`
```sql
CREATE TABLE work_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER,
    name TEXT NOT NULL,
    description TEXT,
    work_type_code TEXT,                             -- ❌ Non utilisé (à supprimer)
    rate REAL,                                       -- ❌ Non utilisé (à supprimer)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    synced INTEGER DEFAULT 0
);
```

#### ⚠️ **PROBLÈMES IDENTIFIÉS**
1. **Colonnes inutiles**: `work_type_code`, `rate` n'existent pas dans le modèle
2. **Colonne manquante**: `status` (actif/inactif)

#### ✅ **CORRECTION PROPOSÉE**
```sql
-- ✅ NOUVELLE STRUCTURE SQLite (work_types)
CREATE TABLE work_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER UNIQUE,                        -- ID du serveur MySQL
    name TEXT NOT NULL,
    description TEXT,
    status INTEGER NOT NULL DEFAULT 1,               -- ✅ AJOUTÉ: 0=inactif, 1=actif
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    synced INTEGER DEFAULT 1                         -- 1=sync, 0=en attente
);
```

---

### 4️⃣ **NOTES DE PROJET** (`project_notes` ↔ `ProjectNote` ↔ `project_notes`)

#### **MySQL** (Serveur) - Table `project_notes`
```sql
CREATE TABLE project_notes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT,                                  -- ✅ NULL si note personnelle
    user_id INT NOT NULL,                            -- ✅ Auteur de la note
    note_type ENUM('text', 'audio', 'dictation') NOT NULL,
    note_group VARCHAR(50) DEFAULT 'project',        -- ✅ project/personal/meeting/todo/idea/issue/other
    note_type_id INT,                                -- ✅ Catégorie personnalisée
    title VARCHAR(255),
    content TEXT,
    audio_path VARCHAR(255),
    audio_duration INT,                              -- Durée en secondes
    transcription TEXT,
    is_important TINYINT(1) DEFAULT 0,
    tags JSON,                                       -- ✅ Tags en JSON
    author_name VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project_list(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (note_type_id) REFERENCES note_types(id)
);
```

#### **SQLite** (Android Offline) - Table `project_notes`
```sql
CREATE TABLE project_notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER,
    project_id INTEGER,                              -- ✅ NULLABLE (notes personnelles)
    user_id INTEGER NOT NULL,
    note_type TEXT NOT NULL,                         -- text/audio/dictation
    note_group TEXT DEFAULT 'project',               -- ✅ project/personal/meeting/etc.
    note_type_id INTEGER,                            -- ✅ Catégorie personnalisée
    title TEXT,
    content TEXT,
    audio_path TEXT,
    local_audio_path TEXT,                           -- ✅ Chemin local du fichier audio
    audio_duration INTEGER,
    transcription TEXT,
    is_important INTEGER DEFAULT 0,
    tags TEXT,                                       -- ✅ JSON array en string
    author_name TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    sync_status TEXT DEFAULT 'pending',              -- ✅ pending/syncing/synced/failed
    sync_error TEXT,
    sync_attempts INTEGER DEFAULT 0,
    synced INTEGER DEFAULT 0
);
```

#### ✅ **VALIDATION**
- **Structure**: ✅ Cohérente
- **Nullable project_id**: ✅ Supporté (notes personnelles)
- **Audio local**: ✅ Supporté (`local_audio_path`)
- **Catégories**: ✅ Supporté (`note_type_id`)

---

### 5️⃣ **TYPES DE NOTES** (`note_types` ↔ `NoteType` ↔ `note_types`)

#### **MySQL** (Serveur) - Table `note_types`
```sql
CREATE TABLE note_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,                                     -- ✅ NULL = catégorie système
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(7) DEFAULT '#6c757d',
    description TEXT,
    is_system TINYINT(1) DEFAULT 0,                  -- ✅ 1 = catégorie système
    sort_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_slug (user_id, slug)
);
```

#### **SQLite** (Android Offline) - Table `note_types`
```sql
CREATE TABLE note_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id INTEGER,
    user_id INTEGER,                                 -- ✅ NULL = catégorie système
    name TEXT NOT NULL,
    slug TEXT NOT NULL,
    icon TEXT,
    color TEXT DEFAULT '#6c757d',
    description TEXT,
    is_system INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    synced INTEGER DEFAULT 1
);
```

#### ✅ **VALIDATION**
- **Structure**: ✅ Cohérente
- **Catégories système**: ✅ Supporté (`is_system`)
- **Catégories utilisateur**: ✅ Supporté (`user_id`)

---

## 📋 RÉSUMÉ DES CORRECTIONS NÉCESSAIRES

### 🔴 **URGENT - Corrections SQLite Android**

#### 1. Table `projects`
```sql
-- ❌ PROBLÈME ACTUEL
status TEXT DEFAULT 'active'

-- ✅ CORRECTION
status INTEGER NOT NULL DEFAULT 1

-- ✅ AJOUTER COLONNES MANQUANTES
ALTER TABLE projects ADD COLUMN assigned_user_id INTEGER;
ALTER TABLE projects ADD COLUMN client VARCHAR(255);
ALTER TABLE projects ADD COLUMN priority TEXT DEFAULT 'medium';
ALTER TABLE projects ADD COLUMN progress REAL DEFAULT 0.00;
ALTER TABLE projects ADD COLUMN is_placeholder INTEGER DEFAULT 0;
```

#### 2. Table `work_types`
```sql
-- ✅ AJOUTER COLONNE MANQUANTE
ALTER TABLE work_types ADD COLUMN status INTEGER NOT NULL DEFAULT 1;

-- ✅ SUPPRIMER COLONNES INUTILES
-- (Faire migration pour ne pas perdre de données)
```

#### 3. Migration de `status` de TEXT → INTEGER
```java
// OfflineDatabaseHelper.java - Méthode onUpgrade()
if (oldVersion < 6) {
    // Migration: status TEXT → INTEGER
    db.execSQL("UPDATE projects SET status = CASE WHEN status = 'active' THEN 1 ELSE 0 END");

    // Recréer la table avec le bon type
    db.execSQL("CREATE TABLE projects_new (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "server_id INTEGER UNIQUE," +
        "name TEXT NOT NULL," +
        "description TEXT," +
        "status INTEGER NOT NULL DEFAULT 1," +  // ✅ INTEGER
        "is_placeholder INTEGER DEFAULT 0," +
        "assigned_user_id INTEGER," +
        "client VARCHAR(255)," +
        "priority TEXT DEFAULT 'medium'," +
        "progress REAL DEFAULT 0.00," +
        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
        "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
        "synced INTEGER DEFAULT 1" +
    ")");

    // Copier les données
    db.execSQL("INSERT INTO projects_new SELECT * FROM projects");
    db.execSQL("DROP TABLE projects");
    db.execSQL("ALTER TABLE projects_new RENAME TO projects");
}
```

---

## 🔄 RÈGLES DE SYNCHRONISATION

### Flux de données

```
┌─────────────────────────────────────────────────────────────┐
│ 1. PREMIER LANCEMENT (Download)                              │
│                                                               │
│  Server MySQL  ──API GET──►  Android Online  ──INSERT──►     │
│  (project_list)   (JSON)     (Project.class)    SQLite       │
│                                                 (projects)    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 2. MODE OFFLINE (Local Insert)                               │
│                                                               │
│  Utilisateur  ──CREATE──►  SQLite          ──PENDING──►      │
│  (Formulaire)              (time_reports)    sync_status     │
│                            sync_status='pending'              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 3. RECONNEXION (Upload Sync)                                 │
│                                                               │
│  SQLite  ──SELECT WHERE sync_status='pending'──►             │
│  (time_reports)                                               │
│                                                               │
│  ──API POST──►  Server MySQL  ──INSERT──►  report_list      │
│     (JSON)      (validation)                                  │
│                                                               │
│  ◄──SUCCESS──  Server MySQL                                  │
│                                                               │
│  SQLite  ──UPDATE sync_status='synced', server_id=X──►       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 4. REFRESH PÉRIODIQUE (Download Sync)                        │
│                                                               │
│  Server MySQL  ──API GET (last 7 days)──►  Android Online   │
│  (report_list)   (JSON)                                       │
│                                                               │
│  Android  ──UPDATE/INSERT (UPSERT)──►  SQLite                │
│                                         (time_reports)        │
│                                                               │
│  Règle: Si server_id existe → UPDATE, sinon → INSERT         │
└─────────────────────────────────────────────────────────────┘
```

### Règles de conflict resolution

#### Stratégie: **Server Wins**
- En cas de conflit, la donnée du serveur écrase toujours la locale
- Exception: Données `sync_status='pending'` ne sont **jamais écrasées**

```sql
-- ✅ UPSERT correct
INSERT INTO time_reports (server_id, project_id, ...)
VALUES (?, ?, ...)
ON CONFLICT(server_id) DO UPDATE SET
    project_id = excluded.project_id,
    ...
WHERE sync_status != 'pending';  -- ✅ Ne pas écraser les données en attente!
```

---

## 🧪 TESTS DE VALIDATION

### Checklist de conformité

#### ✅ **Test 1: Cohérence des types**
- [ ] `Project.status` (Java) = `status` (MySQL INT) = `status` (SQLite INTEGER)
- [ ] `TimeReport.hours` (Java) = `hours` (MySQL DECIMAL) = `hours` (SQLite REAL)
- [ ] `ProjectNote.tags` (Java List<String>) = `tags` (MySQL JSON) = `tags` (SQLite TEXT JSON)

#### ✅ **Test 2: Synchronisation bidirectionnelle**
- [ ] Créer un rapport en mode offline
- [ ] Reconnecter → Vérifier upload vers serveur
- [ ] Créer un rapport sur le serveur web
- [ ] Rafraîchir l'app → Vérifier download vers SQLite

#### ✅ **Test 3: Gestion des NULL**
- [ ] Créer une note personnelle (`project_id = NULL`)
- [ ] Vérifier insertion SQLite sans erreur
- [ ] Vérifier synchronisation vers serveur
- [ ] Vérifier lecture depuis serveur

#### ✅ **Test 4: Migration de schéma**
- [ ] Installer app avec ancienne version SQLite
- [ ] Mettre à jour vers nouvelle version
- [ ] Vérifier migration `status TEXT → INTEGER`
- [ ] Vérifier aucune perte de données

---

## 📐 CONVENTIONS DE NOMMAGE

### Serveur MySQL → Android

| MySQL          | Java (Model)    | SQLite         | Type      |
|----------------|-----------------|----------------|-----------|
| `id`           | `id`            | `server_id`    | INTEGER   |
| `name`         | `name`          | `name`         | TEXT      |
| `status`       | `status`        | `status`       | INTEGER   |
| `date_created` | `dateCreated`   | `created_at`   | DATETIME  |
| `date_updated` | `dateUpdated`   | `updated_at`   | DATETIME  |
| `is_important` | `isImportant()` | `is_important` | INTEGER   |

### Règles
1. **MySQL**: `snake_case`
2. **Java**: `camelCase`
3. **SQLite**: `snake_case` + suffixes (`_at`, `_id`)
4. **JSON API**: `snake_case`

---

## 🚀 PROCHAINES ÉTAPES

### Phase 1: Corrections immédiates
1. ✅ Créer migration SQLite version 6 (status TEXT → INTEGER)
2. ✅ Ajouter colonnes manquantes (`assigned_user_id`, `client`, etc.)
3. ✅ Mettre à jour `OfflineDatabaseHelper.java`
4. ✅ Tester migration sur app existante

### Phase 2: Validation
5. ✅ Créer tests unitaires de synchronisation
6. ✅ Vérifier cohérence des données après sync
7. ✅ Tester mode offline + reconnexion

### Phase 3: Documentation
8. ✅ Mettre à jour ce document avec les résultats
9. ✅ Créer guide de migration pour utilisateurs existants

---

**Auteur**: Claude Code
**Dernière mise à jour**: 2025-01-19
**Version du document**: 1.0
