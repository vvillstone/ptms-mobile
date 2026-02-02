package com.ptms.mobile.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.models.WorkType;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de base de données locale pour le mode hors ligne
 */
public class OfflineDatabaseHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "OfflineDatabaseHelper";
    private static final String DATABASE_NAME = "ptms_offline.db";
    private static final int DATABASE_VERSION = 7; // ✅ NOUVELLE VERSION: v6→v7 pour support multimédia complet

    // Tables
    private static final String TABLE_PROJECTS = "projects";
    private static final String TABLE_WORK_TYPES = "work_types";
    private static final String TABLE_TIME_REPORTS = "time_reports";
    private static final String TABLE_PROJECT_NOTES = "project_notes";
    private static final String TABLE_NOTE_TYPES = "note_types";
    
    // Colonnes communes
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_SYNCED = "synced";
    private static final String COLUMN_SERVER_ID = "server_id";
    
    // Colonnes spécifiques aux projets
    private static final String COLUMN_PROJECT_CODE = "project_code";
    private static final String COLUMN_PROJECT_STATUS = "status";
    private static final String COLUMN_IS_PLACEHOLDER = "is_placeholder"; // ✅ AJOUTÉ
    private static final String COLUMN_ASSIGNED_USER_ID = "assigned_user_id"; // ✅ AJOUTÉ
    private static final String COLUMN_CLIENT = "client"; // ✅ AJOUTÉ
    private static final String COLUMN_PRIORITY = "priority"; // ✅ AJOUTÉ
    private static final String COLUMN_PROGRESS = "progress"; // ✅ AJOUTÉ

    // Colonnes spécifiques aux types de travail
    private static final String COLUMN_WORK_TYPE_CODE = "work_type_code";
    private static final String COLUMN_WORK_TYPE_RATE = "rate";
    private static final String COLUMN_WORK_TYPE_STATUS = "status"; // ✅ AJOUTÉ pour work_types

    // ✅ AJOUTÉ: Cache en mémoire pour performance
    private List<Project> cachedProjects = null;
    private List<WorkType> cachedWorkTypes = null;
    private long lastProjectsCacheTime = 0;
    private long lastWorkTypesCacheTime = 0;
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    
    // Colonnes spécifiques aux rapports de temps
    private static final String COLUMN_PROJECT_ID = "project_id";
    private static final String COLUMN_EMPLOYEE_ID = "employee_id";
    private static final String COLUMN_WORK_TYPE_ID = "work_type_id";
    private static final String COLUMN_REPORT_DATE = "report_date";
    private static final String COLUMN_DATETIME_FROM = "datetime_from";
    private static final String COLUMN_DATETIME_TO = "datetime_to";
    private static final String COLUMN_HOURS = "hours";
    private static final String COLUMN_VALIDATION_STATUS = "validation_status";
    private static final String COLUMN_PROJECT_NAME = "project_name";
    private static final String COLUMN_WORK_TYPE_NAME = "work_type_name";
    private static final String COLUMN_DATE_CREATED = "date_created";
    private static final String COLUMN_DATE_UPDATED = "date_updated";
    private static final String COLUMN_SYNC_STATUS = "sync_status";
    private static final String COLUMN_SYNC_ERROR = "sync_error";
    private static final String COLUMN_ATTEMPTS = "sync_attempts";
    
    // Requêtes de création des tables
    // ✅ CORRIGÉ: status TEXT→INTEGER, colonnes ajoutées
    private static final String CREATE_TABLE_PROJECTS =
        "CREATE TABLE " + TABLE_PROJECTS + "(" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COLUMN_SERVER_ID + " INTEGER UNIQUE," +
        COLUMN_NAME + " TEXT NOT NULL," +
        COLUMN_DESCRIPTION + " TEXT," +
        COLUMN_PROJECT_STATUS + " INTEGER NOT NULL DEFAULT 1," + // ✅ INTEGER (0/1)
        COLUMN_IS_PLACEHOLDER + " INTEGER DEFAULT 0," + // ✅ AJOUTÉ
        COLUMN_ASSIGNED_USER_ID + " INTEGER," + // ✅ AJOUTÉ
        COLUMN_CLIENT + " VARCHAR(255)," + // ✅ AJOUTÉ
        COLUMN_PRIORITY + " TEXT DEFAULT 'medium'," + // ✅ AJOUTÉ
        COLUMN_PROGRESS + " REAL DEFAULT 0.00," + // ✅ AJOUTÉ
        COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_SYNCED + " INTEGER DEFAULT 1" +
        ")";
    
    // ✅ CORRIGÉ: colonne status ajoutée
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
    
    private static final String CREATE_TABLE_TIME_REPORTS =
        "CREATE TABLE " + TABLE_TIME_REPORTS + "(" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COLUMN_SERVER_ID + " INTEGER," +
        COLUMN_PROJECT_ID + " INTEGER," +
        COLUMN_EMPLOYEE_ID + " INTEGER," +
        COLUMN_WORK_TYPE_ID + " INTEGER," +
        COLUMN_REPORT_DATE + " TEXT," +
        COLUMN_DATETIME_FROM + " TEXT," +
        COLUMN_DATETIME_TO + " TEXT," +
        COLUMN_HOURS + " REAL," +
        COLUMN_DESCRIPTION + " TEXT," +
        COLUMN_VALIDATION_STATUS + " TEXT DEFAULT 'pending'," +
        COLUMN_PROJECT_NAME + " TEXT," +
        COLUMN_WORK_TYPE_NAME + " TEXT," +
        COLUMN_DATE_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_DATE_UPDATED + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_SYNC_STATUS + " TEXT DEFAULT 'pending'," +
        COLUMN_SYNC_ERROR + " TEXT," +
        COLUMN_ATTEMPTS + " INTEGER DEFAULT 0" +
        ")";

    private static final String CREATE_TABLE_NOTE_TYPES =
        "CREATE TABLE " + TABLE_NOTE_TYPES + "(" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COLUMN_SERVER_ID + " INTEGER," +
        "user_id INTEGER," +
        "name TEXT NOT NULL," +
        "slug TEXT NOT NULL," +
        "icon TEXT," +
        "color TEXT DEFAULT '#6c757d'," +
        "description TEXT," +
        "is_system INTEGER DEFAULT 0," +
        "sort_order INTEGER DEFAULT 0," +
        COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_SYNCED + " INTEGER DEFAULT 1" +
        ")";

    private static final String CREATE_TABLE_PROJECT_NOTES =
        "CREATE TABLE " + TABLE_PROJECT_NOTES + "(" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COLUMN_SERVER_ID + " INTEGER," +
        COLUMN_PROJECT_ID + " INTEGER," + // ✅ NULLABLE pour notes personnelles
        "user_id INTEGER NOT NULL," + // ✅ User ID obligatoire par sécurité
        "note_type TEXT NOT NULL," + // text, audio, dictation, image, video
        "note_group TEXT DEFAULT 'project'," + // project, personal, meeting, todo, idea, issue, other
        "note_type_id INTEGER," + // ✅ ID de la catégorie personnalisée
        "title TEXT," +
        "content TEXT," +
        "audio_path TEXT," + // ⚠️ DEPRECATED (v6) - Conservé pour rétrocompatibilité - Utiliser server_url
        "local_audio_path TEXT," + // ⚠️ DEPRECATED (v6) - Conservé pour rétrocompatibilité - Utiliser local_file_path
        "audio_duration INTEGER," + // Durée audio/vidéo en secondes
        "transcription TEXT," + // Transcription audio
        "is_important INTEGER DEFAULT 0," +
        "tags TEXT," + // Stocké comme JSON array string
        "author_name TEXT," +
        // ✅ NOUVEAU: Support multimédia complet (v7)
        "local_file_path TEXT," + // Chemin local du fichier (audio/image/vidéo)
        "server_url TEXT," + // URL serveur après upload
        "file_size INTEGER," + // Taille en bytes
        "mime_type TEXT," + // audio/m4a, image/jpeg, video/mp4, etc.
        "thumbnail_path TEXT," + // Miniature locale (images/vidéos)
        "upload_progress INTEGER DEFAULT 0," + // Progress upload 0-100%
        // ✅ NOUVEAU: Champs additionnels pour gestion avancée
        "priority TEXT DEFAULT 'medium'," + // low, medium, high, urgent
        "scheduled_date TEXT," + // Date planifiée YYYY-MM-DD HH:MM:SS
        "reminder_date TEXT," + // Date de rappel YYYY-MM-DD HH:MM:SS
        COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
        COLUMN_SYNC_STATUS + " TEXT DEFAULT 'pending'," +
        COLUMN_SYNC_ERROR + " TEXT," +
        COLUMN_ATTEMPTS + " INTEGER DEFAULT 0," +
        COLUMN_SYNCED + " INTEGER DEFAULT 0" +
        ")";
    
    public OfflineDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Création de la base de données hors ligne");
        db.execSQL(CREATE_TABLE_PROJECTS);
        db.execSQL(CREATE_TABLE_WORK_TYPES);
        db.execSQL(CREATE_TABLE_TIME_REPORTS);
        db.execSQL(CREATE_TABLE_NOTE_TYPES);
        db.execSQL(CREATE_TABLE_PROJECT_NOTES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Mise à jour de la base de données hors ligne de v" + oldVersion + " à v" + newVersion);

        // Migration progressive selon la version
        if (oldVersion < 2) {
            // Ajout de la table project_notes en version 2
            db.execSQL(CREATE_TABLE_PROJECT_NOTES);
            Log.d(TAG, "Table project_notes créée");
        }

        if (oldVersion < 3) {
            // Ajout de la colonne note_group en version 3
            db.execSQL("ALTER TABLE " + TABLE_PROJECT_NOTES + " ADD COLUMN note_group TEXT DEFAULT 'project'");
            Log.d(TAG, "Colonne note_group ajoutée à project_notes");
        }

        if (oldVersion < 5) {
            // Version 5: Ajout table note_types
            db.execSQL(CREATE_TABLE_NOTE_TYPES);
            Log.d(TAG, "Table note_types créée");
        }

        if (oldVersion < 4) {
            // Version 4: Corrections importantes pour les notes
            // 1. Rendre project_id nullable (impossible de modifier NOT NULL directement en SQLite)
            // 2. Ajouter user_id NOT NULL pour sécurité
            // 3. Ajouter note_type_id pour catégories personnalisées

            Log.d(TAG, "Migration v4: Recréation de project_notes avec project_id nullable");

            // Créer une table temporaire avec la nouvelle structure
            db.execSQL("CREATE TABLE project_notes_temp (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_SERVER_ID + " INTEGER," +
                COLUMN_PROJECT_ID + " INTEGER," + // NULLABLE
                "user_id INTEGER NOT NULL," + // NOT NULL
                "note_type TEXT NOT NULL," +
                "note_group TEXT DEFAULT 'project'," +
                "note_type_id INTEGER," + // NOUVEAU
                "title TEXT," +
                "content TEXT," +
                "audio_path TEXT," +
                "local_audio_path TEXT," +
                "audio_duration INTEGER," +
                "transcription TEXT," +
                "is_important INTEGER DEFAULT 0," +
                "tags TEXT," +
                "author_name TEXT," +
                COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                COLUMN_SYNC_STATUS + " TEXT DEFAULT 'pending'," +
                COLUMN_SYNC_ERROR + " TEXT," +
                COLUMN_ATTEMPTS + " INTEGER DEFAULT 0," +
                COLUMN_SYNCED + " INTEGER DEFAULT 0" +
            ")");

            // Copier les données existantes (si la table existe)
            db.execSQL("INSERT INTO project_notes_temp " +
                "(id, server_id, project_id, user_id, note_type, note_group, title, content, " +
                "audio_path, local_audio_path, audio_duration, transcription, is_important, tags, " +
                "author_name, created_at, updated_at, sync_status, sync_error, sync_attempts, synced) " +
                "SELECT id, server_id, project_id, COALESCE(user_id, 0), note_type, note_group, title, content, " +
                "audio_path, local_audio_path, audio_duration, transcription, is_important, tags, " +
                "author_name, created_at, updated_at, sync_status, sync_error, sync_attempts, synced " +
                "FROM " + TABLE_PROJECT_NOTES);

            // Supprimer l'ancienne table
            db.execSQL("DROP TABLE " + TABLE_PROJECT_NOTES);

            // Renommer la table temporaire
            db.execSQL("ALTER TABLE project_notes_temp RENAME TO " + TABLE_PROJECT_NOTES);

            Log.d(TAG, "Migration v4 terminée: project_id est maintenant nullable");
        }

        // ✅ MIGRATION V6: Correction des types de données (2025-01-19)
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

            // Copier les données en convertissant status TEXT → INTEGER
            // Note: is_placeholder est une nouvelle colonne, donc on la met à 0 par défaut
            db.execSQL("INSERT INTO projects_new " +
                "(id, server_id, name, description, status, is_placeholder, created_at, updated_at, synced) " +
                "SELECT id, server_id, name, description, " +
                "CASE WHEN status = 'active' OR status = '1' THEN 1 ELSE 0 END, " +
                "0, created_at, updated_at, synced " +
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

        // ✅ MIGRATION V7: Support multimédia complet (2025-10-20)
        if (oldVersion < 7) {
            Log.d(TAG, "========================================");
            Log.d(TAG, "MIGRATION V7: Support multimédia complet");
            Log.d(TAG, "========================================");

            // Ajouter les colonnes multimédia à project_notes
            try {
                db.execSQL("ALTER TABLE " + TABLE_PROJECT_NOTES + " ADD COLUMN local_file_path TEXT");
                Log.d(TAG, "✅ Colonne local_file_path ajoutée");
            } catch (Exception e) {
                Log.d(TAG, "⚠️ Colonne local_file_path déjà existante");
            }

            try {
                db.execSQL("ALTER TABLE " + TABLE_PROJECT_NOTES + " ADD COLUMN server_url TEXT");
                Log.d(TAG, "✅ Colonne server_url ajoutée");
            } catch (Exception e) {
                Log.d(TAG, "⚠️ Colonne server_url déjà existante");
            }

            try {
                db.execSQL("ALTER TABLE " + TABLE_PROJECT_NOTES + " ADD COLUMN file_size INTEGER");
                Log.d(TAG, "✅ Colonne file_size ajoutée");
            } catch (Exception e) {
                Log.d(TAG, "⚠️ Colonne file_size déjà existante");
            }

            try {
                db.execSQL("ALTER TABLE " + TABLE_PROJECT_NOTES + " ADD COLUMN mime_type TEXT");
                Log.d(TAG, "✅ Colonne mime_type ajoutée");
            } catch (Exception e) {
                Log.d(TAG, "⚠️ Colonne mime_type déjà existante");
            }

            try {
                db.execSQL("ALTER TABLE " + TABLE_PROJECT_NOTES + " ADD COLUMN thumbnail_path TEXT");
                Log.d(TAG, "✅ Colonne thumbnail_path ajoutée");
            } catch (Exception e) {
                Log.d(TAG, "⚠️ Colonne thumbnail_path déjà existante");
            }

            try {
                db.execSQL("ALTER TABLE " + TABLE_PROJECT_NOTES + " ADD COLUMN upload_progress INTEGER DEFAULT 0");
                Log.d(TAG, "✅ Colonne upload_progress ajoutée");
            } catch (Exception e) {
                Log.d(TAG, "⚠️ Colonne upload_progress déjà existante");
            }

            Log.d(TAG, "========================================");
            Log.d(TAG, "MIGRATION V7 TERMINÉE AVEC SUCCÈS");
            Log.d(TAG, "Support complet audio, images, vidéos");
            Log.d(TAG, "========================================");
        }

        // Si besoin de tout recréer (en dernier recours - NE PAS UTILISER EN PRODUCTION)
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROJECT_NOTES);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIME_REPORTS);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORK_TYPES);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROJECTS);
        // onCreate(db);
    }

    // ==================== GESTION DU CACHE MÉMOIRE ====================

    /**
     * ✅ AJOUTÉ: Invalide le cache des projets
     */
    private void invalidateProjectsCache() {
        cachedProjects = null;
        lastProjectsCacheTime = 0;
    }

    /**
     * ✅ AJOUTÉ: Invalide le cache des types de travail
     */
    private void invalidateWorkTypesCache() {
        cachedWorkTypes = null;
        lastWorkTypesCacheTime = 0;
    }

    /**
     * ✅ AJOUTÉ: Vérifie si le cache des projets est encore valide
     */
    private boolean isProjectsCacheValid() {
        return cachedProjects != null &&
               (System.currentTimeMillis() - lastProjectsCacheTime) < CACHE_VALIDITY_MS;
    }

    /**
     * ✅ AJOUTÉ: Vérifie si le cache des types de travail est encore valide
     */
    private boolean isWorkTypesCacheValid() {
        return cachedWorkTypes != null &&
               (System.currentTimeMillis() - lastWorkTypesCacheTime) < CACHE_VALIDITY_MS;
    }

    // ==================== GESTION DES PROJETS ====================
    
    public synchronized long insertProject(Project project) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SERVER_ID, project.getId());
        values.put(COLUMN_NAME, project.getName() != null ? project.getName() : "");

        // ✅ CORRECTION: Gérer les valeurs null pour éviter les crashs
        if (project.getDescription() != null) {
            values.put(COLUMN_DESCRIPTION, project.getDescription());
        } else {
            values.put(COLUMN_DESCRIPTION, "");
        }

        // ✅ CORRIGÉ V6: status est maintenant INTEGER (0 ou 1)
        int statusValue = (project.getStatus() == 1 || project.isActive()) ? 1 : 0;
        values.put(COLUMN_PROJECT_STATUS, statusValue);

        // ✅ NOUVELLES COLONNES V6 (optionnelles - valeurs par défaut si non fournies)
        // Ces colonnes n'existent pas dans le modèle Project actuel, donc on met des valeurs par défaut
        values.put(COLUMN_IS_PLACEHOLDER, 0); // Par défaut: projet réel (pas placeholder)
        // assigned_user_id, client, priority, progress sont nullable dans la migration - on ne les met pas ici

        // ✅ CORRECTION: Gérer les timestamps null
        if (project.getDateCreated() != null) {
            values.put(COLUMN_CREATED_AT, project.getDateCreated());
        } else {
            values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        }

        if (project.getDateUpdated() != null) {
            values.put(COLUMN_UPDATED_AT, project.getDateUpdated());
        } else {
            values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
        }

        values.put(COLUMN_SYNCED, 1); // Marqué comme synchronisé

        long id = db.insertWithOnConflict(TABLE_PROJECTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        Log.d(TAG, "Projet inséré en local: " + project.getName() + " (ID: " + id + ")");
        invalidateProjectsCache(); // ✅ Invalider le cache après insertion
        return id;
    }
    
    public synchronized int getProjectCount() {
        // Si le cache est valide, retourner la taille du cache
        if (isProjectsCacheValid()) {
            return cachedProjects.size();
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PROJECTS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public synchronized List<Project> getAllProjects() {
        // ✅ CACHE: Vérifier si le cache est encore valide
        if (isProjectsCacheValid()) {
            Log.d(TAG, "Retour du cache mémoire (projets): " + cachedProjects.size());
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

                // ✅ CORRIGÉ V6: status est maintenant INTEGER (0 ou 1)
                int statusInt = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROJECT_STATUS));
                project.setStatus(statusInt);

                project.setDateCreated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
                project.setDateUpdated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
                projects.add(project);
            } while (cursor.moveToNext());
        }

        cursor.close();

        // ✅ CACHE: Stocker en mémoire
        cachedProjects = projects;
        lastProjectsCacheTime = System.currentTimeMillis();

        Log.d(TAG, "Projets récupérés depuis SQLite et mis en cache: " + projects.size());
        return projects;
    }
    
    public synchronized void clearProjects() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PROJECTS, null, null);
        invalidateProjectsCache(); // ✅ Invalider le cache après suppression
        Log.d(TAG, "Cache des projets vidé");
    }

    /**
     * ✅ AJOUTÉ: Remplace tous les projets en une seule transaction (performance)
     */
    public synchronized void replaceAllProjects(List<Project> projects) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            // Vider la table
            db.delete(TABLE_PROJECTS, null, null);

            // Insérer tous les nouveaux projets
            for (Project project : projects) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_SERVER_ID, project.getId());
                values.put(COLUMN_NAME, project.getName() != null ? project.getName() : "");

                if (project.getDescription() != null) {
                    values.put(COLUMN_DESCRIPTION, project.getDescription());
                }

                // ✅ Gérer le status comme INTEGER (0 ou 1)
                int statusValue = project.isActive() ? 1 : 0;
                values.put(COLUMN_PROJECT_STATUS, statusValue);

                // ✅ CORRECTION: Ajouter is_placeholder (requis par la migration v6)
                values.put(COLUMN_IS_PLACEHOLDER, 0); // Par défaut: projet réel

                // Colonnes optionnelles (timestamps)
                values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
                values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());

                values.put(COLUMN_SYNCED, 1);

                db.insertWithOnConflict(TABLE_PROJECTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
            invalidateProjectsCache();
            Log.d(TAG, "✅ " + projects.size() + " projets remplacés avec succès (transaction)");

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur lors du remplacement des projets", e);
        } finally {
            db.endTransaction();
        }
    }

    // ==================== GESTION DES TYPES DE TRAVAIL ====================
    
    public synchronized long insertWorkType(WorkType workType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SERVER_ID, workType.getId());
        values.put(COLUMN_NAME, workType.getName() != null ? workType.getName() : "");

        // ✅ CORRECTION: Gérer les valeurs null pour éviter les crashs
        if (workType.getDescription() != null) {
            values.put(COLUMN_DESCRIPTION, workType.getDescription());
        } else {
            values.put(COLUMN_DESCRIPTION, "");
        }

        // ✅ CORRIGÉ V6: Ajouter la colonne status INTEGER (actif par défaut)
        values.put(COLUMN_WORK_TYPE_STATUS, 1);

        // ✅ CORRECTION: Gérer les timestamps null
        if (workType.getDateCreated() != null) {
            values.put(COLUMN_CREATED_AT, workType.getDateCreated());
        } else {
            values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        }

        if (workType.getDateUpdated() != null) {
            values.put(COLUMN_UPDATED_AT, workType.getDateUpdated());
        } else {
            values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
        }

        values.put(COLUMN_SYNCED, 1); // Marqué comme synchronisé

        long id = db.insertWithOnConflict(TABLE_WORK_TYPES, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        Log.d(TAG, "Type de travail inséré en local: " + workType.getName() + " (ID: " + id + ")");
        invalidateWorkTypesCache(); // ✅ Invalider le cache après insertion
        return id;
    }
    
    public synchronized List<WorkType> getAllWorkTypes() {
        // ✅ CACHE: Vérifier si le cache est encore valide
        if (isWorkTypesCacheValid()) {
            Log.d(TAG, "Retour du cache mémoire (work types): " + cachedWorkTypes.size());
            return new ArrayList<>(cachedWorkTypes);
        }

        List<WorkType> workTypes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_WORK_TYPES + " ORDER BY " + COLUMN_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                WorkType workType = new WorkType();
                workType.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVER_ID)));
                workType.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                workType.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                workType.setDateCreated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
                workType.setDateUpdated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
                workTypes.add(workType);
            } while (cursor.moveToNext());
        }

        cursor.close();

        // ✅ CACHE: Stocker en mémoire
        cachedWorkTypes = workTypes;
        lastWorkTypesCacheTime = System.currentTimeMillis();

        Log.d(TAG, "Types de travail récupérés depuis SQLite et mis en cache: " + workTypes.size());
        return workTypes;
    }
    
    public synchronized void clearWorkTypes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WORK_TYPES, null, null);
        invalidateWorkTypesCache(); // ✅ Invalider le cache après suppression
        Log.d(TAG, "Cache des types de travail vidé");
    }

    /**
     * ✅ AJOUTÉ: Remplace tous les types de travail en une seule transaction (performance)
     */
    public synchronized void replaceAllWorkTypes(List<WorkType> workTypes) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            // Vider la table
            db.delete(TABLE_WORK_TYPES, null, null);

            // Insérer tous les nouveaux types de travail
            for (WorkType workType : workTypes) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_SERVER_ID, workType.getId());
                values.put(COLUMN_NAME, workType.getName() != null ? workType.getName() : "");

                if (workType.getDescription() != null) {
                    values.put(COLUMN_DESCRIPTION, workType.getDescription());
                }

                // ✅ Gérer le status comme INTEGER (actif par défaut = 1)
                values.put(COLUMN_WORK_TYPE_STATUS, 1);

                values.put(COLUMN_SYNCED, 1);

                db.insertWithOnConflict(TABLE_WORK_TYPES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
            invalidateWorkTypesCache();
            Log.d(TAG, "✅ " + workTypes.size() + " types de travail remplacés avec succès (transaction)");

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur lors du remplacement des types de travail", e);
        } finally {
            db.endTransaction();
        }
    }

    // ==================== GESTION DES RAPPORTS DE TEMPS ====================
    
    public synchronized long insertTimeReport(TimeReport report) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_PROJECT_ID, report.getProjectId());
        values.put(COLUMN_EMPLOYEE_ID, report.getEmployeeId());
        values.put(COLUMN_WORK_TYPE_ID, report.getWorkTypeId());
        values.put(COLUMN_REPORT_DATE, report.getReportDate());
        values.put(COLUMN_DATETIME_FROM, report.getDatetimeFrom());
        values.put(COLUMN_DATETIME_TO, report.getDatetimeTo());
        values.put(COLUMN_HOURS, report.getHours());
        values.put(COLUMN_DESCRIPTION, report.getDescription());
        values.put(COLUMN_VALIDATION_STATUS, report.getValidationStatus());
        values.put(COLUMN_PROJECT_NAME, report.getProjectName());
        values.put(COLUMN_WORK_TYPE_NAME, report.getWorkTypeName());
        values.put(COLUMN_DATE_CREATED, report.getDateCreated());
        values.put(COLUMN_DATE_UPDATED, report.getDateUpdated());
        values.put(COLUMN_SYNC_STATUS, "pending");
        values.putNull(COLUMN_SYNC_ERROR);
        values.put(COLUMN_ATTEMPTS, 0);
        
        long id = db.insert(TABLE_TIME_REPORTS, null, values);
        
        Log.d(TAG, "Rapport de temps inséré en local: " + report.getProjectName() + " - " + report.getHours() + "h (ID: " + id + ")");
        return id;
    }
    
    public synchronized List<TimeReport> getAllPendingTimeReports() {
        List<TimeReport> reports = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_TIME_REPORTS + 
            " WHERE " + COLUMN_SYNC_STATUS + " IN ('pending', 'failed')" +
            " ORDER BY " + COLUMN_DATE_CREATED + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                TimeReport report = new TimeReport();
                report.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                report.setProjectId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROJECT_ID)));
                report.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_ID)));
                report.setWorkTypeId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WORK_TYPE_ID)));
                report.setReportDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_DATE)));
                report.setDatetimeFrom(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATETIME_FROM)));
                report.setDatetimeTo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATETIME_TO)));
                report.setHours(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HOURS)));
                report.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                report.setValidationStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALIDATION_STATUS)));
                report.setProjectName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROJECT_NAME)));
                report.setWorkTypeName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORK_TYPE_NAME)));
                report.setDateCreated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_CREATED)));
                report.setDateUpdated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_UPDATED)));

                // ✅ FIX: Lire le server_id pour détecter les rapports partiellement synchronisés
                int serverIdIndex = cursor.getColumnIndex(COLUMN_SERVER_ID);
                if (serverIdIndex >= 0 && !cursor.isNull(serverIdIndex)) {
                    report.setServerId(cursor.getInt(serverIdIndex));
                }

                reports.add(report);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Rapports de temps en attente de synchronisation: " + reports.size());
        return reports;
    }
    
    public synchronized void updateTimeReportSyncStatus(long localId, String status, String error, int attempts) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SYNC_STATUS, status);
        values.put(COLUMN_SYNC_ERROR, error);
        values.put(COLUMN_ATTEMPTS, attempts);
        values.put(COLUMN_DATE_UPDATED, System.currentTimeMillis());

        int rows = db.update(TABLE_TIME_REPORTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(localId)});

        Log.d(TAG, "Statut de synchronisation mis à jour pour le rapport " + localId + ": " + status);
    }

    /**
     * ✅ NOUVEAU: Surcharge pour mise à jour status sans attempts
     */
    public synchronized void updateTimeReportSyncStatus(long localId, String status, String error) {
        updateTimeReportSyncStatus(localId, status, error, 0);
    }
    
    public synchronized void markTimeReportAsSynced(long localId, int serverId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SERVER_ID, serverId);
        values.put(COLUMN_SYNC_STATUS, "synced");
        values.putNull(COLUMN_SYNC_ERROR);
        values.put(COLUMN_DATE_UPDATED, System.currentTimeMillis());

        int rows = db.update(TABLE_TIME_REPORTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(localId)});

        Log.d(TAG, "Rapport marqué comme synchronisé: local ID " + localId + " -> server ID " + serverId);
    }

    /**
     * ✅ NOUVEAU: Récupère les TimeReports avec photos locales non-uploadées
     */
    public synchronized List<TimeReport> getTimeReportsWithPendingMedia() {
        List<TimeReport> reports = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Requête: rapports avec image_path non null ET qui ne commence pas par "/uploads/"
        // Status: "pending_media" OU image_path locale
        String query = "SELECT * FROM " + TABLE_TIME_REPORTS +
                       " WHERE image_path IS NOT NULL" +
                       " AND image_path != ''" +
                       " AND image_path NOT LIKE '/uploads/%'" +
                       " AND image_path NOT LIKE 'uploads/%'";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TimeReport report = extractTimeReportFromCursor(cursor);
                reports.add(report);
            } while (cursor.moveToNext());
        }

        cursor.close();
        Log.d(TAG, "TimeReports avec photos pending: " + reports.size());
        return reports;
    }

    public synchronized int getPendingSyncCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_TIME_REPORTS +
            " WHERE " + COLUMN_SYNC_STATUS + " IN ('pending', 'failed')", null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();

        return count;
    }

    /**
     * ✅ AJOUTÉ: Récupère un rapport de temps par son server_id
     */
    public synchronized TimeReport getTimeReportByServerId(int serverId) {
        SQLiteDatabase db = this.getReadableDatabase();
        TimeReport report = null;

        Cursor cursor = db.query(
            TABLE_TIME_REPORTS,
            null, // toutes les colonnes
            COLUMN_SERVER_ID + " = ?",
            new String[]{String.valueOf(serverId)},
            null, null, null
        );

        if (cursor.moveToFirst()) {
            report = extractTimeReportFromCursor(cursor);
        }

        cursor.close();
        return report;
    }

    /**
     * ✅ AJOUTÉ: Met à jour un rapport de temps existant
     */
    public synchronized int updateTimeReport(TimeReport report) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (report.getServerId() != null && report.getServerId() > 0) {
            values.put(COLUMN_SERVER_ID, report.getServerId());
        }
        if (report.getProjectId() > 0) {
            values.put(COLUMN_PROJECT_ID, report.getProjectId());
        }
        if (report.getEmployeeId() > 0) {
            values.put(COLUMN_EMPLOYEE_ID, report.getEmployeeId());
        }
        if (report.getWorkTypeId() > 0) {
            values.put(COLUMN_WORK_TYPE_ID, report.getWorkTypeId());
        }
        if (report.getReportDate() != null) {
            values.put(COLUMN_REPORT_DATE, report.getReportDate());
        }
        if (report.getDatetimeFrom() != null) {
            values.put(COLUMN_DATETIME_FROM, report.getDatetimeFrom());
        }
        if (report.getDatetimeTo() != null) {
            values.put(COLUMN_DATETIME_TO, report.getDatetimeTo());
        }
        values.put(COLUMN_HOURS, report.getHours());
        if (report.getDescription() != null) {
            values.put(COLUMN_DESCRIPTION, report.getDescription());
        }
        if (report.getValidationStatus() != null) {
            values.put(COLUMN_VALIDATION_STATUS, report.getValidationStatus());
        }
        if (report.getProjectName() != null) {
            values.put(COLUMN_PROJECT_NAME, report.getProjectName());
        }
        if (report.getWorkTypeName() != null) {
            values.put(COLUMN_WORK_TYPE_NAME, report.getWorkTypeName());
        }
        if (report.getDateUpdated() != null) {
            values.put(COLUMN_DATE_UPDATED, report.getDateUpdated());
        }
        if (report.getSyncStatus() != null) {
            values.put(COLUMN_SYNC_STATUS, report.getSyncStatus());
        }
        if (report.getSyncError() != null) {
            values.put(COLUMN_SYNC_ERROR, report.getSyncError());
        }
        values.put(COLUMN_ATTEMPTS, report.getSyncAttempts());

        int rows = 0;
        // Chercher par server_id d'abord, sinon par id local
        if (report.getServerId() != null && report.getServerId() > 0) {
            rows = db.update(TABLE_TIME_REPORTS, values,
                COLUMN_SERVER_ID + " = ?",
                new String[]{String.valueOf(report.getServerId())});
        } else if (report.getId() > 0) {
            rows = db.update(TABLE_TIME_REPORTS, values,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(report.getId())});
        }

        Log.d(TAG, "Rapport mis à jour: " + rows + " ligne(s)");
        return rows;
    }

    /**
     * ✅ AJOUTÉ: Extrait un TimeReport depuis un Cursor
     */
    private TimeReport extractTimeReportFromCursor(Cursor cursor) {
        TimeReport report = new TimeReport();

        int idIndex = cursor.getColumnIndex(COLUMN_ID);
        int serverIdIndex = cursor.getColumnIndex(COLUMN_SERVER_ID);
        int projectIdIndex = cursor.getColumnIndex(COLUMN_PROJECT_ID);
        int employeeIdIndex = cursor.getColumnIndex(COLUMN_EMPLOYEE_ID);
        int workTypeIdIndex = cursor.getColumnIndex(COLUMN_WORK_TYPE_ID);
        int reportDateIndex = cursor.getColumnIndex(COLUMN_REPORT_DATE);
        int datetimeFromIndex = cursor.getColumnIndex(COLUMN_DATETIME_FROM);
        int datetimeToIndex = cursor.getColumnIndex(COLUMN_DATETIME_TO);
        int hoursIndex = cursor.getColumnIndex(COLUMN_HOURS);
        int descriptionIndex = cursor.getColumnIndex(COLUMN_DESCRIPTION);
        int validationStatusIndex = cursor.getColumnIndex(COLUMN_VALIDATION_STATUS);
        int projectNameIndex = cursor.getColumnIndex(COLUMN_PROJECT_NAME);
        int workTypeNameIndex = cursor.getColumnIndex(COLUMN_WORK_TYPE_NAME);
        int dateCreatedIndex = cursor.getColumnIndex(COLUMN_DATE_CREATED);
        int dateUpdatedIndex = cursor.getColumnIndex(COLUMN_DATE_UPDATED);
        int syncStatusIndex = cursor.getColumnIndex(COLUMN_SYNC_STATUS);
        int syncErrorIndex = cursor.getColumnIndex(COLUMN_SYNC_ERROR);
        int attemptsIndex = cursor.getColumnIndex(COLUMN_ATTEMPTS);

        if (idIndex >= 0) report.setId(cursor.getInt(idIndex));
        if (serverIdIndex >= 0 && !cursor.isNull(serverIdIndex)) {
            report.setServerId(cursor.getInt(serverIdIndex));
        }
        if (projectIdIndex >= 0 && !cursor.isNull(projectIdIndex)) {
            report.setProjectId(cursor.getInt(projectIdIndex));
        }
        if (employeeIdIndex >= 0 && !cursor.isNull(employeeIdIndex)) {
            report.setEmployeeId(cursor.getInt(employeeIdIndex));
        }
        if (workTypeIdIndex >= 0 && !cursor.isNull(workTypeIdIndex)) {
            report.setWorkTypeId(cursor.getInt(workTypeIdIndex));
        }
        if (reportDateIndex >= 0) report.setReportDate(cursor.getString(reportDateIndex));
        if (datetimeFromIndex >= 0) report.setDatetimeFrom(cursor.getString(datetimeFromIndex));
        if (datetimeToIndex >= 0) report.setDatetimeTo(cursor.getString(datetimeToIndex));
        if (hoursIndex >= 0) report.setHours(cursor.getDouble(hoursIndex));
        if (descriptionIndex >= 0) report.setDescription(cursor.getString(descriptionIndex));
        if (validationStatusIndex >= 0) report.setValidationStatus(cursor.getString(validationStatusIndex));
        if (projectNameIndex >= 0) report.setProjectName(cursor.getString(projectNameIndex));
        if (workTypeNameIndex >= 0) report.setWorkTypeName(cursor.getString(workTypeNameIndex));
        if (dateCreatedIndex >= 0) report.setDateCreated(cursor.getString(dateCreatedIndex));
        if (dateUpdatedIndex >= 0) report.setDateUpdated(cursor.getString(dateUpdatedIndex));
        if (syncStatusIndex >= 0) report.setSyncStatus(cursor.getString(syncStatusIndex));
        if (syncErrorIndex >= 0) report.setSyncError(cursor.getString(syncErrorIndex));
        if (attemptsIndex >= 0) report.setSyncAttempts(cursor.getInt(attemptsIndex));

        return report;
    }

    // ==================== GESTION DES NOTES DE PROJETS ====================

    public synchronized long insertProjectNote(ProjectNote note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // ✅ project_id peut être NULL (notes personnelles)
        if (note.getProjectId() != null && note.getProjectId() > 0) {
            values.put(COLUMN_PROJECT_ID, note.getProjectId());
        } else {
            values.putNull(COLUMN_PROJECT_ID);
        }

        values.put("user_id", note.getUserId());
        values.put("note_type", note.getNoteType());
        values.put("note_group", note.getNoteGroup());

        // ✅ Support pour note_type_id (catégories personnalisées)
        if (note.getNoteTypeId() != null && note.getNoteTypeId() > 0) {
            values.put("note_type_id", note.getNoteTypeId());
        } else {
            values.putNull("note_type_id");
        }

        values.put("title", note.getTitle());
        values.put("content", note.getContent());
        values.put("audio_path", note.getAudioPath());
        values.put("local_audio_path", note.getLocalAudioPath());
        values.put("audio_duration", note.getAudioDuration());
        values.put("transcription", note.getTranscription());
        values.put("is_important", note.isImportant() ? 1 : 0);

        // Convertir les tags en JSON string
        if (note.getTags() != null && !note.getTags().isEmpty()) {
            StringBuilder tagsJson = new StringBuilder("[");
            for (int i = 0; i < note.getTags().size(); i++) {
                tagsJson.append("\"").append(note.getTags().get(i)).append("\"");
                if (i < note.getTags().size() - 1) tagsJson.append(",");
            }
            tagsJson.append("]");
            values.put("tags", tagsJson.toString());
        } else {
            values.putNull("tags");
        }

        values.put("author_name", note.getAuthorName());

        // ✅ AJOUT: Champs additionnels pour gestion complète des notes
        if (note.getPriority() != null) {
            values.put("priority", note.getPriority());
        } else {
            values.put("priority", "medium"); // Valeur par défaut
        }

        if (note.getScheduledDate() != null) {
            values.put("scheduled_date", note.getScheduledDate());
        } else {
            values.putNull("scheduled_date");
        }

        if (note.getReminderDate() != null) {
            values.put("reminder_date", note.getReminderDate());
        } else {
            values.putNull("reminder_date");
        }

        values.put(COLUMN_SYNC_STATUS, "pending");
        values.putNull(COLUMN_SYNC_ERROR);
        values.put(COLUMN_ATTEMPTS, 0);
        values.put(COLUMN_SYNCED, 0);

        long id = db.insert(TABLE_PROJECT_NOTES, null, values);

        Log.d(TAG, "Note de projet insérée en local: " + note.getTitle() + " (ID: " + id + ")");
        return id;
    }

    public synchronized List<ProjectNote> getAllPendingProjectNotes() {
        List<ProjectNote> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE " + COLUMN_SYNC_STATUS + " IN ('pending', 'failed')" +
            " ORDER BY " + COLUMN_CREATED_AT + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                ProjectNote note = extractProjectNoteFromCursor(cursor);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Notes de projets en attente de synchronisation: " + notes.size());
        return notes;
    }

    /**
     * Récupère les notes en attente de synchronisation pour un utilisateur spécifique
     * IMPORTANT: Filtre par user_id pour la sécurité
     */
    public synchronized List<ProjectNote> getPendingProjectNotesByUserId(int userId) {
        List<ProjectNote> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE " + COLUMN_SYNC_STATUS + " IN ('pending', 'failed')" +
            " AND user_id = ?" +
            " ORDER BY " + COLUMN_CREATED_AT + " DESC",
            new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                ProjectNote note = extractProjectNoteFromCursor(cursor);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Notes en attente de synchronisation pour l'utilisateur " + userId + ": " + notes.size());
        return notes;
    }

    public synchronized List<ProjectNote> getProjectNotesByProjectId(int projectId) {
        List<ProjectNote> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE " + COLUMN_PROJECT_ID + " = ?" +
            " ORDER BY " + COLUMN_CREATED_AT + " DESC",
            new String[]{String.valueOf(projectId)});

        if (cursor.moveToFirst()) {
            do {
                ProjectNote note = extractProjectNoteFromCursor(cursor);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Notes récupérées pour le projet " + projectId + ": " + notes.size());
        return notes;
    }

    /**
     * Récupère les notes d'un projet pour un utilisateur spécifique
     * IMPORTANT: Filtre par user_id pour la sécurité
     */
    public synchronized List<ProjectNote> getProjectNotesByProjectIdAndUserId(int projectId, int userId) {
        List<ProjectNote> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE " + COLUMN_PROJECT_ID + " = ?" +
            " AND user_id = ?" +
            " ORDER BY " + COLUMN_CREATED_AT + " DESC",
            new String[]{String.valueOf(projectId), String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                ProjectNote note = extractProjectNoteFromCursor(cursor);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Notes récupérées pour le projet " + projectId + " (utilisateur " + userId + "): " + notes.size());
        return notes;
    }

    /**
     * ✅ NOUVEAU: Récupère les notes PERSONNELLES (sans projet) pour un utilisateur
     * IMPORTANT: Filtre par user_id pour la sécurité
     */
    public synchronized List<ProjectNote> getPersonalNotesByUserId(int userId) {
        List<ProjectNote> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE " + COLUMN_PROJECT_ID + " IS NULL" +
            " AND user_id = ?" +
            " ORDER BY is_important DESC, " + COLUMN_CREATED_AT + " DESC",
            new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                ProjectNote note = extractProjectNoteFromCursor(cursor);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Notes personnelles récupérées pour l'utilisateur " + userId + ": " + notes.size());
        return notes;
    }

    /**
     * ✅ NOUVEAU: Récupère toutes les notes (projet + personnelles) pour un utilisateur
     * IMPORTANT: Filtre par user_id pour la sécurité
     */
    public synchronized List<ProjectNote> getAllNotesByUserId(int userId) {
        List<ProjectNote> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE user_id = ?" +
            " ORDER BY is_important DESC, " + COLUMN_CREATED_AT + " DESC",
            new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                ProjectNote note = extractProjectNoteFromCursor(cursor);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Toutes les notes récupérées pour l'utilisateur " + userId + ": " + notes.size());
        return notes;
    }

    /**
     * ✅ NOUVEAU: Récupère les notes par groupe pour un utilisateur
     * @param noteGroup project, personal, meeting, todo, idea, issue, other
     */
    public synchronized List<ProjectNote> getNotesByGroupAndUserId(String noteGroup, int userId) {
        List<ProjectNote> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE note_group = ?" +
            " AND user_id = ?" +
            " ORDER BY is_important DESC, " + COLUMN_CREATED_AT + " DESC",
            new String[]{noteGroup, String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                ProjectNote note = extractProjectNoteFromCursor(cursor);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Notes récupérées pour le groupe '" + noteGroup + "' (utilisateur " + userId + "): " + notes.size());
        return notes;
    }

    private ProjectNote extractProjectNoteFromCursor(Cursor cursor) {
        ProjectNote note = new ProjectNote();
        note.setLocalId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));

        int serverIdIndex = cursor.getColumnIndexOrThrow(COLUMN_SERVER_ID);
        if (!cursor.isNull(serverIdIndex)) {
            note.setId(cursor.getInt(serverIdIndex));
        }

        // ✅ project_id peut être NULL
        int projectIdIndex = cursor.getColumnIndexOrThrow(COLUMN_PROJECT_ID);
        if (!cursor.isNull(projectIdIndex)) {
            note.setProjectId(cursor.getInt(projectIdIndex));
        } else {
            note.setProjectId(null); // Note personnelle
        }

        note.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
        note.setNoteType(cursor.getString(cursor.getColumnIndexOrThrow("note_type")));
        note.setNoteGroup(cursor.getString(cursor.getColumnIndexOrThrow("note_group")));

        // ✅ note_type_id peut être NULL
        int noteTypeIdIndex = cursor.getColumnIndexOrThrow("note_type_id");
        if (!cursor.isNull(noteTypeIdIndex)) {
            note.setNoteTypeId(cursor.getInt(noteTypeIdIndex));
        } else {
            note.setNoteTypeId(null);
        }

        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        note.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
        note.setAudioPath(cursor.getString(cursor.getColumnIndexOrThrow("audio_path")));
        note.setLocalAudioPath(cursor.getString(cursor.getColumnIndexOrThrow("local_audio_path")));

        int durationIndex = cursor.getColumnIndexOrThrow("audio_duration");
        if (!cursor.isNull(durationIndex)) {
            note.setAudioDuration(cursor.getInt(durationIndex));
        }

        note.setTranscription(cursor.getString(cursor.getColumnIndexOrThrow("transcription")));
        note.setImportant(cursor.getInt(cursor.getColumnIndexOrThrow("is_important")) == 1);

        // Parser les tags depuis JSON string
        String tagsJson = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
        if (tagsJson != null && !tagsJson.isEmpty()) {
            List<String> tags = new ArrayList<>();
            tagsJson = tagsJson.replaceAll("[\\[\\]\"]", "");
            if (!tagsJson.isEmpty()) {
                String[] tagArray = tagsJson.split(",");
                for (String tag : tagArray) {
                    tags.add(tag.trim());
                }
            }
            note.setTags(tags);
        }

        note.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow("author_name")));
        note.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        note.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
        note.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)));
        note.setSyncError(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNC_ERROR)));
        note.setSyncAttempts(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ATTEMPTS)));
        note.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNCED)) == 1);

        // ✅ Nouveaux champs multimédia (Phase 2 - Offline-First)
        note.setLocalFilePath(cursor.getString(cursor.getColumnIndexOrThrow("local_file_path")));
        note.setServerUrl(cursor.getString(cursor.getColumnIndexOrThrow("server_url")));

        int fileSizeIndex = cursor.getColumnIndexOrThrow("file_size");
        if (!cursor.isNull(fileSizeIndex)) {
            note.setFileSize(cursor.getLong(fileSizeIndex));
        }

        note.setMimeType(cursor.getString(cursor.getColumnIndexOrThrow("mime_type")));
        note.setThumbnailPath(cursor.getString(cursor.getColumnIndexOrThrow("thumbnail_path")));

        int uploadProgressIndex = cursor.getColumnIndexOrThrow("upload_progress");
        if (!cursor.isNull(uploadProgressIndex)) {
            note.setUploadProgress(cursor.getInt(uploadProgressIndex));
        }

        // ✅ AJOUT: Champs additionnels pour gestion complète des notes (priority, scheduled_date, reminder_date, server_id)
        note.setPriority(cursor.getString(cursor.getColumnIndexOrThrow("priority")));
        note.setScheduledDate(cursor.getString(cursor.getColumnIndexOrThrow("scheduled_date")));
        note.setReminderDate(cursor.getString(cursor.getColumnIndexOrThrow("reminder_date")));

        int serverIdCol = cursor.getColumnIndexOrThrow(COLUMN_SERVER_ID);
        if (!cursor.isNull(serverIdCol)) {
            note.setServerId(cursor.getInt(serverIdCol));
        }

        return note;
    }

    /**
     * Alias pour extractProjectNoteFromCursor (compatibilité Phase 2)
     */
    private ProjectNote cursorToProjectNote(Cursor cursor) {
        return extractProjectNoteFromCursor(cursor);
    }

    /**
     * Retourne le timestamp actuel en millisecondes
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Récupère une ProjectNote par son ID local
     */
    public synchronized ProjectNote getProjectNoteById(int localId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PROJECT_NOTES, null,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(localId)},
                null, null, null);

        ProjectNote note = null;
        if (cursor != null && cursor.moveToFirst()) {
            note = extractProjectNoteFromCursor(cursor);
            cursor.close();
        }

        return note;
    }

    public synchronized void updateProjectNoteSyncStatus(long localId, String status, String error, int attempts) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SYNC_STATUS, status);
        values.put(COLUMN_SYNC_ERROR, error);
        values.put(COLUMN_ATTEMPTS, attempts);
        values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());

        int rows = db.update(TABLE_PROJECT_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(localId)});

        Log.d(TAG, "Statut de synchronisation mis à jour pour la note " + localId + ": " + status);
    }

    public synchronized void markProjectNoteAsSynced(long localId, int serverId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SERVER_ID, serverId);
        values.put(COLUMN_SYNC_STATUS, "synced");
        values.put(COLUMN_SYNCED, 1);
        values.putNull(COLUMN_SYNC_ERROR);
        values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());

        int rows = db.update(TABLE_PROJECT_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(localId)});

        Log.d(TAG, "Note marquée comme synchronisée: local ID " + localId + " -> server ID " + serverId);
    }

    public synchronized int getPendingNotesSyncCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_PROJECT_NOTES +
            " WHERE " + COLUMN_SYNC_STATUS + " IN ('pending', 'failed')", null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();

        return count;
    }

    /**
     * Compte les notes en attente de synchronisation pour un utilisateur spécifique
     * IMPORTANT: Filtre par user_id pour la sécurité
     */
    public synchronized int getPendingNotesSyncCountByUserId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_PROJECT_NOTES +
            " WHERE " + COLUMN_SYNC_STATUS + " IN ('pending', 'failed')" +
            " AND user_id = ?",
            new String[]{String.valueOf(userId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();

        return count;
    }

    /**
     * Insère ou met à jour une note depuis le serveur (upsert par server_id)
     * ✅ NOUVEAU: Évite les doublons lors de la synchronisation serveur → local
     */
    public synchronized long upsertNoteFromServer(ProjectNote note) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Vérifier si une note avec ce server_id existe déjà
        Cursor cursor = db.rawQuery(
            "SELECT " + COLUMN_ID + " FROM " + TABLE_PROJECT_NOTES +
            " WHERE " + COLUMN_SERVER_ID + " = ?",
            new String[]{String.valueOf(note.getId())});

        long localId = -1;
        if (cursor.moveToFirst()) {
            // Note existe → UPDATE
            localId = cursor.getLong(0);
            cursor.close();

            ContentValues values = new ContentValues();
            if (note.getProjectId() != null && note.getProjectId() > 0) {
                values.put(COLUMN_PROJECT_ID, note.getProjectId());
            } else {
                values.putNull(COLUMN_PROJECT_ID);
            }
            values.put("user_id", note.getUserId());
            values.put("note_type", note.getNoteType());
            values.put("note_group", note.getNoteGroup());
            if (note.getNoteTypeId() != null && note.getNoteTypeId() > 0) {
                values.put("note_type_id", note.getNoteTypeId());
            } else {
                values.putNull("note_type_id");
            }
            values.put("title", note.getTitle());
            values.put("content", note.getContent());
            values.put("audio_path", note.getAudioPath());
            values.put("transcription", note.getTranscription());
            values.put("is_important", note.isImportant() ? 1 : 0);
            values.put("author_name", note.getAuthorName());
            values.put(COLUMN_SYNC_STATUS, "synced");
            values.put(COLUMN_SYNCED, 1);
            values.putNull(COLUMN_SYNC_ERROR);

            db.update(TABLE_PROJECT_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(localId)});
            Log.d(TAG, "Note mise à jour depuis serveur: " + note.getTitle() + " (server_id: " + note.getId() + ")");

        } else {
            // Note n'existe pas → INSERT
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(COLUMN_SERVER_ID, note.getId());
            if (note.getProjectId() != null && note.getProjectId() > 0) {
                values.put(COLUMN_PROJECT_ID, note.getProjectId());
            } else {
                values.putNull(COLUMN_PROJECT_ID);
            }
            values.put("user_id", note.getUserId());
            values.put("note_type", note.getNoteType());
            values.put("note_group", note.getNoteGroup());
            if (note.getNoteTypeId() != null && note.getNoteTypeId() > 0) {
                values.put("note_type_id", note.getNoteTypeId());
            } else {
                values.putNull("note_type_id");
            }
            values.put("title", note.getTitle());
            values.put("content", note.getContent());
            values.put("audio_path", note.getAudioPath());
            values.put("transcription", note.getTranscription());
            values.put("is_important", note.isImportant() ? 1 : 0);
            values.put("author_name", note.getAuthorName());
            values.put(COLUMN_SYNC_STATUS, "synced");
            values.put(COLUMN_SYNCED, 1);
            values.putNull(COLUMN_SYNC_ERROR);
            values.put(COLUMN_ATTEMPTS, 0);

            localId = db.insert(TABLE_PROJECT_NOTES, null, values);
            Log.d(TAG, "Note insérée depuis serveur: " + note.getTitle() + " (server_id: " + note.getId() + ", local_id: " + localId + ")");
        }

        return localId;
    }

    public synchronized void clearProjectNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PROJECT_NOTES, null, null);
        Log.d(TAG, "Cache des notes de projets vidé");
    }

    // ==================== GESTION DES TYPES DE NOTES ====================

    public synchronized long insertNoteType(com.ptms.mobile.models.NoteType noteType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SERVER_ID, noteType.getId());
        if (noteType.getUserId() != null) {
            values.put("user_id", noteType.getUserId());
        } else {
            values.putNull("user_id");
        }
        values.put("name", noteType.getName());
        values.put("slug", noteType.getSlug());
        values.put("icon", noteType.getIcon());
        values.put("color", noteType.getColor());
        values.put("description", noteType.getDescription());
        values.put("is_system", noteType.isSystem() ? 1 : 0);
        values.put("sort_order", noteType.getSortOrder());
        values.put(COLUMN_SYNCED, 1);

        long id = db.insert(TABLE_NOTE_TYPES, null, values);
        return id;
    }

    public synchronized List<com.ptms.mobile.models.NoteType> getAllNoteTypes() {
        List<com.ptms.mobile.models.NoteType> types = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_NOTE_TYPES +
            " ORDER BY is_system DESC, sort_order ASC", null);

        if (cursor.moveToFirst()) {
            do {
                com.ptms.mobile.models.NoteType type = new com.ptms.mobile.models.NoteType();
                type.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVER_ID)));
                int userIdIdx = cursor.getColumnIndexOrThrow("user_id");
                if (!cursor.isNull(userIdIdx)) type.setUserId(cursor.getInt(userIdIdx));
                type.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                type.setSlug(cursor.getString(cursor.getColumnIndexOrThrow("slug")));
                type.setIcon(cursor.getString(cursor.getColumnIndexOrThrow("icon")));
                type.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
                type.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                type.setSystem(cursor.getInt(cursor.getColumnIndexOrThrow("is_system")) == 1);
                type.setSortOrder(cursor.getInt(cursor.getColumnIndexOrThrow("sort_order")));
                types.add(type);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return types;
    }

    public synchronized void clearNoteTypes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTE_TYPES, null, null);
    }

    // ==================== MÉTHODES UTILITAIRES ====================
    
    public synchronized void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PROJECT_NOTES, null, null);
        db.delete(TABLE_NOTE_TYPES, null, null);
        db.delete(TABLE_TIME_REPORTS, null, null);
        db.delete(TABLE_WORK_TYPES, null, null);
        db.delete(TABLE_PROJECTS, null, null);

        Log.d(TAG, "Toutes les données locales supprimées");
    }

    public synchronized long getDatabaseSize() {
        SQLiteDatabase db = this.getReadableDatabase();
        long size = 0;

        // Taille de chaque table
        String[] tables = {TABLE_PROJECTS, TABLE_WORK_TYPES, TABLE_TIME_REPORTS, TABLE_PROJECT_NOTES, TABLE_NOTE_TYPES};
        for (String table : tables) {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
            if (cursor.moveToFirst()) {
                size += cursor.getInt(0);
            }
            cursor.close();
        }

        return size;
    }

    // ==================== MÉTHODES POUR STATISTIQUES UTILISATEUR ====================

    /**
     * ✅ NOUVEAU: Compte le nombre de rapports de temps pour un utilisateur
     */
    public synchronized int getTimeReportsCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_TIME_REPORTS +
            " WHERE " + COLUMN_EMPLOYEE_ID + " = ?",
            new String[]{String.valueOf(userId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();

        Log.d(TAG, "Nombre de rapports pour l'utilisateur " + userId + ": " + count);
        return count;
    }

    /**
     * ✅ NOUVEAU: Calcule le total d'heures pour un utilisateur
     */
    public synchronized double getTotalHours(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT SUM(" + COLUMN_HOURS + ") FROM " + TABLE_TIME_REPORTS +
            " WHERE " + COLUMN_EMPLOYEE_ID + " = ?",
            new String[]{String.valueOf(userId)});

        double totalHours = 0.0;
        if (cursor.moveToFirst()) {
            totalHours = cursor.getDouble(0);
        }

        cursor.close();

        Log.d(TAG, "Total d'heures pour l'utilisateur " + userId + ": " + totalHours);
        return totalHours;
    }

    // ==================== MÉTHODES POUR GESTION FICHIERS MULTIMÉDIA (V7) ====================

    /**
     * ✅ NOUVEAU (V7): Récupère les notes avec fichiers en attente d'upload
     */
    public synchronized List<ProjectNote> getPendingMediaUploads() {
        List<ProjectNote> pendingMedia = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Récupérer toutes les notes avec local_file_path ET sync_status = "pending"
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE local_file_path IS NOT NULL" +
            " AND local_file_path != ''" +
            " AND " + COLUMN_SYNC_STATUS + " = 'pending'" +
            " ORDER BY " + COLUMN_CREATED_AT + " DESC",
            null
        );

        while (cursor.moveToNext()) {
            try {
                ProjectNote note = cursorToProjectNote(cursor);
                pendingMedia.add(note);
            } catch (Exception e) {
                Log.e(TAG, "Erreur conversion note: " + e.getMessage());
            }
        }

        cursor.close();
        Log.d(TAG, "Notes avec fichiers en attente d'upload: " + pendingMedia.size());
        return pendingMedia;
    }

    /**
     * ✅ NOUVEAU (V7): Met à jour le progress d'upload d'un fichier
     */
    public synchronized void updateUploadProgress(long noteId, int progress) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("upload_progress", progress);
        values.put(COLUMN_UPDATED_AT, getCurrentTimestamp());

        int rows = db.update(
            TABLE_PROJECT_NOTES,
            values,
            COLUMN_ID + " = ?",
            new String[]{String.valueOf(noteId)}
        );

        Log.d(TAG, "Progress mis à jour pour note #" + noteId + ": " + progress + "%");
    }

    /**
     * ✅ NOUVEAU (V7): Marque un fichier comme synchronisé avec URL serveur
     */
    public synchronized void markMediaAsSynced(long noteId, String serverUrl) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("server_url", serverUrl);
        values.put(COLUMN_SYNC_STATUS, "synced");
        values.put(COLUMN_SYNCED, 1);
        values.put("upload_progress", 100);
        values.put(COLUMN_ATTEMPTS, 0);
        values.put(COLUMN_SYNC_ERROR, (String) null);
        values.put(COLUMN_UPDATED_AT, getCurrentTimestamp());

        int rows = db.update(
            TABLE_PROJECT_NOTES,
            values,
            COLUMN_ID + " = ?",
            new String[]{String.valueOf(noteId)}
        );

        Log.d(TAG, "Note #" + noteId + " marquée comme synchronisée avec URL: " + serverUrl);
    }

    /**
     * ✅ NOUVEAU (V7): Compte le nombre de fichiers en attente d'upload
     */
    public synchronized int getPendingMediaUploadsCount() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_PROJECT_NOTES +
            " WHERE local_file_path IS NOT NULL" +
            " AND local_file_path != ''" +
            " AND " + COLUMN_SYNC_STATUS + " = 'pending'",
            null
        );

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        return count;
    }

    /**
     * ✅ NOUVEAU (V7): Récupère les notes avec fichiers synchronisés (pour nettoyage cache)
     */
    public synchronized List<ProjectNote> getSyncedMediaOlderThan(long timestampMs) {
        List<ProjectNote> oldMedia = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Calculer la date limite (ex: 30 jours avant)
        // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
        String dateLimit = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .format(new java.util.Date(timestampMs));

        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_PROJECT_NOTES +
            " WHERE server_url IS NOT NULL" +
            " AND server_url != ''" +
            " AND " + COLUMN_SYNC_STATUS + " = 'synced'" +
            " AND " + COLUMN_CREATED_AT + " < ?" +
            " ORDER BY " + COLUMN_CREATED_AT + " ASC",
            new String[]{dateLimit}
        );

        while (cursor.moveToNext()) {
            try {
                ProjectNote note = cursorToProjectNote(cursor);
                oldMedia.add(note);
            } catch (Exception e) {
                Log.e(TAG, "Erreur conversion note: " + e.getMessage());
            }
        }

        cursor.close();
        Log.d(TAG, "Notes avec fichiers synchronisés anciens (> 30j): " + oldMedia.size());
        return oldMedia;
    }

    /**
     * ✅ NOUVEAU (V7): Supprime le fichier local après sync (pour économie d'espace)
     */
    public synchronized void clearLocalMediaFile(long noteId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("local_file_path", (String) null);
        values.put("thumbnail_path", (String) null);
        values.put(COLUMN_UPDATED_AT, getCurrentTimestamp());

        int rows = db.update(
            TABLE_PROJECT_NOTES,
            values,
            COLUMN_ID + " = ?",
            new String[]{String.valueOf(noteId)}
        );

        Log.d(TAG, "Fichier local supprimé pour note #" + noteId + " (serveur: " + rows + " rows)");
    }

    /**
     * Marque une note comme synchronisée
     * @param noteId ID local de la note
     */
    public void markNoteAsSynced(int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_SYNCED, 1);
        values.put(COLUMN_SYNC_STATUS, "synced");
        values.putNull(COLUMN_SYNC_ERROR);
        values.put(COLUMN_UPDATED_AT, getCurrentTimestamp());

        int rows = db.update(
            TABLE_PROJECT_NOTES,
            values,
            COLUMN_ID + " = ?",
            new String[]{String.valueOf(noteId)}
        );

        Log.d(TAG, "Note #" + noteId + " marquée comme synchronisée (" + rows + " rows updated)");
    }

    /**
     * Met à jour le server_id d'une note après upload
     * @param localId ID local de la note
     * @param serverId ID retourné par le serveur
     */
    public void updateNoteServerId(int localId, int serverId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVER_ID, serverId);
        values.put(COLUMN_SYNCED, 1);
        values.put(COLUMN_SYNC_STATUS, "synced");
        values.put(COLUMN_UPDATED_AT, getCurrentTimestamp());

        int rows = db.update(
            TABLE_PROJECT_NOTES,
            values,
            COLUMN_ID + " = ?",
            new String[]{String.valueOf(localId)}
        );

        Log.d(TAG, "Note #" + localId + " server_id mis à jour: " + serverId + " (" + rows + " rows)");
    }

    /**
     * ✅ NOUVEAU: Récupère les rapports de temps dans une plage de dates
     * Pour les statistiques du dashboard
     */
    public synchronized List<TimeReport> getTimeReportsByDateRange(String startDate, String endDate) {
        List<TimeReport> reports = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
            TABLE_TIME_REPORTS,
            null, // toutes les colonnes
            COLUMN_REPORT_DATE + " >= ? AND " + COLUMN_REPORT_DATE + " <= ?",
            new String[]{startDate, endDate},
            null, null,
            COLUMN_REPORT_DATE + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                TimeReport report = new TimeReport();
                report.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                report.setProjectId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROJECT_ID)));
                report.setEmployeeId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EMPLOYEE_ID)));
                report.setWorkTypeId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WORK_TYPE_ID)));
                report.setReportDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPORT_DATE)));
                report.setDatetimeFrom(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATETIME_FROM)));
                report.setDatetimeTo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATETIME_TO)));
                report.setHours(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HOURS)));
                report.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                report.setValidationStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALIDATION_STATUS)));
                report.setProjectName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROJECT_NAME)));
                report.setWorkTypeName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORK_TYPE_NAME)));
                report.setDateCreated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_CREATED)));
                report.setDateUpdated(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_UPDATED)));

                // ✅ FIX: Lire le server_id
                int serverIdIndex = cursor.getColumnIndex(COLUMN_SERVER_ID);
                if (serverIdIndex >= 0 && !cursor.isNull(serverIdIndex)) {
                    report.setServerId(cursor.getInt(serverIdIndex));
                }

                reports.add(report);
            } while (cursor.moveToNext());
        }

        cursor.close();

        Log.d(TAG, "Rapports trouvés pour période " + startDate + " - " + endDate + ": " + reports.size());
        return reports;
    }

    /**
     * ✅ NOUVEAU: Récupère un projet par son ID
     * Pour les statistiques du dashboard
     */
    public synchronized Project getProjectById(int projectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Project project = null;

        Cursor cursor = db.query(
            TABLE_PROJECTS,
            null, // toutes les colonnes
            COLUMN_ID + " = ?",
            new String[]{String.valueOf(projectId)},
            null, null, null
        );

        if (cursor.moveToFirst()) {
            project = new Project();
            project.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            project.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            project.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
            project.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROJECT_STATUS)));
            project.setClient(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT)));
        }

        cursor.close();
        return project;
    }

    /**
     * Supprime un rapport de temps par son ID
     * @param reportId ID du rapport à supprimer
     * @return true si la suppression a réussi
     */
    public synchronized boolean deleteTimeReport(int reportId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int rowsAffected = db.delete(
                TABLE_TIME_REPORTS,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(reportId)}
            );
            Log.d(TAG, "Rapport de temps supprimé: ID=" + reportId + ", lignes=" + rowsAffected);
            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e(TAG, "Erreur suppression rapport de temps ID=" + reportId, e);
            return false;
        }
    }

    /**
     * Supprime une note de projet par son ID
     * @param noteId ID de la note à supprimer
     * @return true si la suppression a réussi
     */
    public synchronized boolean deleteProjectNote(int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int rowsAffected = db.delete(
                TABLE_PROJECT_NOTES,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(noteId)}
            );
            Log.d(TAG, "Note de projet supprimée: ID=" + noteId + ", lignes=" + rowsAffected);
            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e(TAG, "Erreur suppression note de projet ID=" + noteId, e);
            return false;
        }
    }
}
