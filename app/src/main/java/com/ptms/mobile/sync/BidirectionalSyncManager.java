package com.ptms.mobile.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.models.WorkType;
import com.ptms.mobile.storage.MediaStorageManager;
import com.ptms.mobile.utils.NetworkUtils;
import com.ptms.mobile.workers.MediaUploadWorker;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ✅ GESTIONNAIRE UNIQUE DE SYNCHRONISATION ET CACHE OFFLINE
 *
 * RÈGLES:
 * - MODE ONLINE: Charge depuis serveur + sync vers SQLite + upload modifications locales
 * - MODE OFFLINE: Charge depuis SQLite uniquement + queue modifications
 * - CONFLIT: Serveur gagne toujours (Master-Slave)
 *
 * ARCHITECTURE:
 * - Cache: OfflineDatabaseHelper (SQLite uniquement)
 * - Sync: BidirectionalSyncManager (ce fichier)
 * - PAS d'autres managers (OfflineDataManager, JsonSyncManager supprimés)
 *
 * @version 2.1 - UNIFIÉ
 * @date 2025-01-19
 */
public class BidirectionalSyncManager {

    private static final String TAG = "UnifiedSync";
    private static final String PREFS_NAME = "unified_sync_prefs";
    private static final String KEY_LAST_FULL_SYNC = "last_full_sync";
    private static final String KEY_LAST_UPLOAD_SYNC = "last_upload_sync";
    private static final String KEY_LAST_DOWNLOAD_SYNC = "last_download_sync";
    private static final String KEY_SYNC_IN_PROGRESS = "sync_in_progress";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private Context context;
    private OfflineDatabaseHelper dbHelper;
    private ApiClient apiClient;
    private ApiService apiService;
    private SharedPreferences prefs;
    private SharedPreferences authPrefs;

    // ✅ FIX: Use Locale.US for ISO dates (prevents locale-specific crashes)
    // Format de date pour comparaison
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * Getter pour accéder à OfflineDatabaseHelper depuis l'extérieur
     */
    public OfflineDatabaseHelper getOfflineDatabaseHelper() {
        return dbHelper;
    }

    /**
     * Interface de callback pour les événements de synchronisation
     */
    public interface SyncCallback {
        void onSyncStarted(String phase);
        void onSyncProgress(String message, int current, int total);
        void onSyncCompleted(SyncResult result);
        void onSyncError(String error);
    }

    /**
     * Résultat de la synchronisation
     */
    public static class SyncResult {
        public int uploadedCount = 0;
        public int downloadedCount = 0;
        public int conflictsResolved = 0;
        public int failedCount = 0;
        public List<String> errors = new ArrayList<>();

        public String getSummary() {
            return String.format(Locale.getDefault(),
                "📤 Uploaded: %d | 📥 Downloaded: %d | ⚔️ Conflicts: %d | ❌ Failed: %d",
                uploadedCount, downloadedCount, conflictsResolved, failedCount);
        }
    }

    /**
     * Type de synchronisation
     */
    public enum SyncType {
        FULL,           // Complète (download + upload)
        UPLOAD_ONLY,    // Upload uniquement (local → serveur)
        DOWNLOAD_ONLY   // Download uniquement (serveur → local)
    }

    public BidirectionalSyncManager(Context context) {
        this.context = context;
        this.dbHelper = new OfflineDatabaseHelper(context);
        this.apiClient = ApiClient.getInstance(context);
        this.apiService = apiClient.getApiService();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.authPrefs = context.getSharedPreferences("ptms_prefs", Context.MODE_PRIVATE);
    }

    // ==================== SYNCHRONISATION COMPLÈTE ====================

    /**
     * Lance une synchronisation bidirectionnelle complète
     */
    public void syncFull(SyncCallback callback) {
        sync(SyncType.FULL, callback);
    }

    /**
     * Upload uniquement les modifications locales
     */
    public void syncUpload(SyncCallback callback) {
        sync(SyncType.UPLOAD_ONLY, callback);
    }

    /**
     * Download uniquement les données du serveur
     */
    public void syncDownload(SyncCallback callback) {
        sync(SyncType.DOWNLOAD_ONLY, callback);
    }

    /**
     * Synchronisation générique
     */
    private void sync(SyncType syncType, SyncCallback callback) {
        // Vérifier connexion
        if (!NetworkUtils.isOnline(context)) {
            Log.w(TAG, "❌ Pas de connexion - Synchronisation impossible");
            if (callback != null) {
                callback.onSyncError("Pas de connexion internet");
            }
            return;
        }

        // Vérifier token
        String token = getAuthToken();
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "❌ Pas de token - Synchronisation impossible");
            if (callback != null) {
                callback.onSyncError("Non authentifié - Token manquant");
            }
            return;
        }

        // Vérifier si sync déjà en cours
        if (isSyncInProgress()) {
            Log.w(TAG, "⚠️ Synchronisation déjà en cours");
            if (callback != null) {
                callback.onSyncError("Synchronisation déjà en cours");
            }
            return;
        }

        // Marquer comme en cours
        setSyncInProgress(true);

        // Lancer dans un thread séparé
        new Thread(() -> {
            SyncResult result = new SyncResult();

            try {
                Log.d(TAG, "🔄 Début synchronisation: " + syncType);

                // Phase 1: DOWNLOAD (Serveur → Local)
                if (syncType == SyncType.FULL || syncType == SyncType.DOWNLOAD_ONLY) {
                    if (callback != null) {
                        callback.onSyncStarted("📥 Téléchargement des données du serveur");
                    }
                    downloadFromServer(token, result, callback);
                }

                // Phase 2: UPLOAD (Local → Serveur)
                if (syncType == SyncType.FULL || syncType == SyncType.UPLOAD_ONLY) {
                    if (callback != null) {
                        callback.onSyncStarted("📤 Envoi des modifications locales");
                    }
                    uploadToServer(token, result, callback);
                }

                // Enregistrer timestamp de sync
                updateSyncTimestamps(syncType);

                Log.d(TAG, "✅ Synchronisation terminée: " + result.getSummary());

                if (callback != null) {
                    callback.onSyncCompleted(result);
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur synchronisation", e);
                result.errors.add("Erreur: " + e.getMessage());
                if (callback != null) {
                    callback.onSyncError("Erreur de synchronisation: " + e.getMessage());
                }
            } finally {
                setSyncInProgress(false);
            }
        }).start();
    }

    // ==================== DOWNLOAD (Serveur → Local) ====================

    /**
     * Télécharge les données du serveur et met à jour le cache local
     */
    private void downloadFromServer(String token, SyncResult result, SyncCallback callback) {
        Log.d(TAG, "📥 Début download depuis serveur");

        // Synchronisation Projects (données de référence)
        downloadProjects(token, result, callback);

        // Synchronisation Work Types (données de référence)
        downloadWorkTypes(token, result, callback);

        // Synchronisation Time Reports (données modifiables)
        // Note: Les rapports sont normalement créés localement et uploadés
        // Mais on peut récupérer les rapports créés/modifiés sur le serveur (web)
        downloadTimeReports(token, result, callback);
    }

    /**
     * Download projets depuis serveur
     */
    private void downloadProjects(String token, SyncResult result, SyncCallback callback) {
        try {
            if (callback != null) {
                callback.onSyncProgress("Téléchargement des projets...", 0, 0);
            }

            Call<ApiService.ProjectsResponse> call = apiService.getProjects(token);
            Response<ApiService.ProjectsResponse> response = call.execute(); // Synchrone

            if (response.isSuccessful() && response.body() != null && response.body().success) {
                List<Project> serverProjects = response.body().projects;

                // Remplacer tous les projets (serveur = master)
                dbHelper.replaceAllProjects(serverProjects);

                result.downloadedCount += serverProjects.size();
                Log.d(TAG, "✅ Projets téléchargés: " + serverProjects.size());
            } else {
                result.failedCount++;
                result.errors.add("Erreur download projets: " + response.code());
                Log.e(TAG, "❌ Erreur download projets: " + response.code());
            }
        } catch (Exception e) {
            result.failedCount++;
            result.errors.add("Exception download projets: " + e.getMessage());
            Log.e(TAG, "❌ Exception download projets", e);
        }
    }

    /**
     * Download types de travail depuis serveur
     */
    private void downloadWorkTypes(String token, SyncResult result, SyncCallback callback) {
        try {
            if (callback != null) {
                callback.onSyncProgress("Téléchargement des types de travail...", 0, 0);
            }

            Call<List<WorkType>> call = apiService.getWorkTypes(token);
            Response<List<WorkType>> response = call.execute(); // Synchrone

            if (response.isSuccessful() && response.body() != null) {
                List<WorkType> serverWorkTypes = response.body();

                // Remplacer tous les types de travail (serveur = master)
                dbHelper.replaceAllWorkTypes(serverWorkTypes);

                result.downloadedCount += serverWorkTypes.size();
                Log.d(TAG, "✅ Types de travail téléchargés: " + serverWorkTypes.size());
            } else {
                result.failedCount++;
                result.errors.add("Erreur download work types: " + response.code());
                Log.e(TAG, "❌ Erreur download work types: " + response.code());
            }
        } catch (Exception e) {
            result.failedCount++;
            result.errors.add("Exception download work types: " + e.getMessage());
            Log.e(TAG, "❌ Exception download work types", e);
        }
    }

    /**
     * Download rapports de temps depuis serveur
     * Gère les conflits: serveur gagne toujours
     */
    private void downloadTimeReports(String token, SyncResult result, SyncCallback callback) {
        try {
            if (callback != null) {
                callback.onSyncProgress("Téléchargement des rapports...", 0, 0);
            }

            // Récupérer les rapports des 30 derniers jours
            // ✅ FIX: Use Locale.US for ISO dates
            SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date dateFrom = new Date(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000));
            Date dateTo = new Date();

            Call<List<TimeReport>> call = apiService.getReports(
                token,
                apiDateFormat.format(dateFrom),
                apiDateFormat.format(dateTo),
                null // tous les projets
            );
            Response<List<TimeReport>> response = call.execute(); // Synchrone

            if (response.isSuccessful() && response.body() != null) {
                List<TimeReport> serverReports = response.body();

                // Gérer les conflits avec les rapports locaux
                int conflictsResolved = resolveTimeReportConflicts(serverReports);
                result.conflictsResolved += conflictsResolved;
                result.downloadedCount += serverReports.size();

                Log.d(TAG, "✅ Rapports téléchargés: " + serverReports.size() + " (conflits résolus: " + conflictsResolved + ")");
            } else {
                result.failedCount++;
                result.errors.add("Erreur download rapports: " + response.code());
                Log.e(TAG, "❌ Erreur download rapports: " + response.code());
            }
        } catch (Exception e) {
            result.failedCount++;
            result.errors.add("Exception download rapports: " + e.getMessage());
            Log.e(TAG, "❌ Exception download rapports", e);
        }
    }

    /**
     * Résout les conflits entre rapports serveur et locaux
     * RÈGLE: Serveur gagne toujours (MASTER)
     */
    private int resolveTimeReportConflicts(List<TimeReport> serverReports) {
        int conflictsResolved = 0;

        // Pour chaque rapport du serveur
        for (TimeReport serverReport : serverReports) {
            if (serverReport.getServerId() != null && serverReport.getServerId() > 0) {
                // Vérifier si existe localement
                TimeReport localReport = dbHelper.getTimeReportByServerId(serverReport.getServerId());

                if (localReport != null) {
                    // Conflit détecté - comparer timestamps
                    if (isServerNewer(serverReport, localReport)) {
                        // Serveur plus récent → Remplacer local
                        serverReport.setSyncStatus("synced");
                        serverReport.setSyncAttempts(0);
                        dbHelper.updateTimeReport(serverReport);
                        conflictsResolved++;
                        Log.d(TAG, "⚔️ Conflit résolu (serveur gagne): Report #" + serverReport.getServerId());
                    } else {
                        // Local plus récent → Marquer explicitement pour upload
                        localReport.setSyncStatus("pending");
                        dbHelper.updateTimeReport(localReport);
                        Log.d(TAG, "📤 Local plus récent: Report #" + serverReport.getServerId() + " marqué pending pour upload");
                    }
                } else {
                    // Nouveau rapport du serveur → Ajouter localement
                    serverReport.setSyncStatus("synced");
                    dbHelper.insertTimeReport(serverReport);
                }
            }
        }

        return conflictsResolved;
    }

    /**
     * Compare les timestamps pour déterminer qui est plus récent
     */
    private boolean isServerNewer(TimeReport serverReport, TimeReport localReport) {
        try {
            String serverUpdated = serverReport.getDateUpdated();
            String localUpdated = localReport.getDateUpdated();

            if (serverUpdated == null || localUpdated == null) {
                return true; // En cas de doute, serveur gagne
            }

            Date serverDate = dateFormat.parse(serverUpdated);
            Date localDate = dateFormat.parse(localUpdated);

            return serverDate.after(localDate);
        } catch (ParseException e) {
            Log.w(TAG, "Erreur parsing dates - Serveur gagne par défaut", e);
            return true; // Serveur gagne en cas d'erreur
        }
    }

    // ==================== UPLOAD (Local → Serveur) ====================

    /**
     * Upload les modifications locales vers le serveur
     */
    private void uploadToServer(String token, SyncResult result, SyncCallback callback) {
        Log.d(TAG, "📤 Début upload vers serveur");

        // Upload Time Reports pending
        uploadPendingTimeReports(token, result, callback);

        // Upload Project Notes pending
        uploadPendingProjectNotes(token, result, callback);
    }

    /**
     * Upload rapports de temps en attente
     */
    private void uploadPendingTimeReports(String token, SyncResult result, SyncCallback callback) {
        try {
            List<TimeReport> pendingReports = dbHelper.getAllPendingTimeReports();

            if (pendingReports.isEmpty()) {
                Log.d(TAG, "✅ Aucun rapport en attente");
                return;
            }

            Log.d(TAG, "📤 Upload de " + pendingReports.size() + " rapports...");

            int uploaded = 0;
            for (int i = 0; i < pendingReports.size(); i++) {
                TimeReport report = pendingReports.get(i);

                if (callback != null) {
                    callback.onSyncProgress("Upload rapport " + (i + 1) + "/" + pendingReports.size(), i + 1, pendingReports.size());
                }

                try {
                    Call<ApiService.ApiResponse> call = apiService.saveTimeEntry(token, report);
                    Response<ApiService.ApiResponse> response = call.execute(); // Synchrone

                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        // ✅ FIX: Extraire le server_id de la réponse pour éviter les doublons
                        int serverId = 0;
                        try {
                            Object data = response.body().data;
                            if (data instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                                Object idObj = dataMap.get("id");
                                if (idObj instanceof Number) {
                                    serverId = ((Number) idObj).intValue();
                                }
                            }
                        } catch (Exception ex) {
                            Log.w(TAG, "Impossible d'extraire server_id de la réponse", ex);
                        }

                        // Marquer comme synchronisé avec le server_id
                        report.setSyncStatus("synced");
                        report.setSyncAttempts(0);

                        if (serverId > 0) {
                            // ✅ FIX: Utiliser markTimeReportAsSynced pour sauvegarder le server_id
                            dbHelper.markTimeReportAsSynced(report.getId(), serverId);
                            Log.d(TAG, "✅ Rapport uploadé: local #" + report.getId() + " → server #" + serverId);
                        } else {
                            // Fallback si pas de server_id dans la réponse
                            dbHelper.updateTimeReportSyncStatus(report.getId(), "synced", null, 0);
                            Log.d(TAG, "✅ Rapport uploadé: #" + report.getId() + " (sans server_id)");
                        }

                        uploaded++;
                    } else {
                        // Échec - incrémenter tentatives
                        int attempts = report.getSyncAttempts() + 1;
                        String error = "Erreur HTTP: " + response.code();
                        dbHelper.updateTimeReportSyncStatus(report.getId(), "pending", error, attempts);

                        result.failedCount++;
                        result.errors.add("Échec upload rapport #" + report.getId() + ": " + error);
                        Log.e(TAG, "❌ Échec upload rapport: " + error);
                    }
                } catch (Exception e) {
                    // Exception - incrémenter tentatives
                    int attempts = report.getSyncAttempts() + 1;
                    String error = e.getMessage();
                    dbHelper.updateTimeReportSyncStatus(report.getId(), "pending", error, attempts);

                    result.failedCount++;
                    result.errors.add("Exception upload rapport #" + report.getId() + ": " + error);
                    Log.e(TAG, "❌ Exception upload rapport", e);
                }
            }

            result.uploadedCount += uploaded;
            Log.d(TAG, "✅ Rapports uploadés: " + uploaded + "/" + pendingReports.size());

        } catch (Exception e) {
            result.errors.add("Exception uploadPendingTimeReports: " + e.getMessage());
            Log.e(TAG, "❌ Erreur uploadPendingTimeReports", e);
        }
    }

    /**
     * Upload notes de projet en attente
     */
    private void uploadPendingProjectNotes(String token, SyncResult result, SyncCallback callback) {
        try {
            List<ProjectNote> pendingNotes = dbHelper.getAllPendingProjectNotes();

            if (pendingNotes.isEmpty()) {
                Log.d(TAG, "✅ Aucune note en attente");
                return;
            }

            Log.d(TAG, "📤 Upload de " + pendingNotes.size() + " notes...");

            int uploaded = 0;
            for (int i = 0; i < pendingNotes.size(); i++) {
                ProjectNote note = pendingNotes.get(i);

                if (callback != null) {
                    callback.onSyncProgress("Upload note " + (i + 1) + "/" + pendingNotes.size(), i + 1, pendingNotes.size());
                }

                try {
                    // ✅ IMPLÉMENTÉ: Upload de notes vers le serveur
                    boolean uploadSuccess = uploadNoteToServer(note);

                    if (uploadSuccess) {
                        // Note déjà marquée comme synchronisée dans uploadNoteToServer()
                        uploaded++;
                        Log.d(TAG, "✅ Note #" + note.getId() + " uploadée avec succès");
                    } else {
                        result.failedCount++;
                        result.errors.add("Échec upload note #" + note.getId());
                        Log.w(TAG, "⚠️ Échec upload note #" + note.getId());
                    }

                } catch (Exception e) {
                    result.failedCount++;
                    result.errors.add("Exception upload note #" + note.getId() + ": " + e.getMessage());
                    Log.e(TAG, "❌ Exception upload note", e);
                }
            }

            result.uploadedCount += uploaded;
            Log.d(TAG, "✅ Notes uploadées: " + uploaded + "/" + pendingNotes.size());

        } catch (Exception e) {
            result.errors.add("Exception uploadPendingProjectNotes: " + e.getMessage());
            Log.e(TAG, "❌ Erreur uploadPendingProjectNotes", e);
        }
    }

    // ==================== UPLOAD NOTE ====================

    /**
     * Upload une note vers le serveur
     * @param note La note à uploader
     * @return true si succès, false sinon
     */
    private boolean uploadNoteToServer(ProjectNote note) {
        try {
            String token = getAuthToken();
            if (token == null || token.isEmpty()) {
                Log.e(TAG, "❌ Token manquant pour upload note");
                return false;
            }

            Log.d(TAG, "📤 Upload note #" + note.getId() + ": " + note.getTitle());

            // Préparer les RequestBody pour Multipart
            okhttp3.RequestBody projectIdBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                String.valueOf(note.getProjectId() != null ? note.getProjectId() : 0)
            );

            okhttp3.RequestBody noteTypeBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                note.getNoteType() != null ? note.getNoteType() : "text"
            );

            okhttp3.RequestBody noteTypeIdBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                String.valueOf(note.getNoteTypeId() != null ? note.getNoteTypeId() : 0)
            );

            okhttp3.RequestBody noteGroupBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                note.getNoteGroup() != null ? note.getNoteGroup() : "project"
            );

            okhttp3.RequestBody titleBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                note.getTitle() != null ? note.getTitle() : ""
            );

            okhttp3.RequestBody contentBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                note.getContent() != null ? note.getContent() : ""
            );

            okhttp3.RequestBody transcriptionBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                note.getTranscription() != null ? note.getTranscription() : ""
            );

            okhttp3.RequestBody isImportantBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                note.isImportant() ? "1" : "0"
            );

            // Tags en JSON
            String tagsJson = "[]";
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                tagsJson = new com.google.gson.Gson().toJson(note.getTags());
            }
            okhttp3.RequestBody tagsBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("text/plain"),
                tagsJson
            );

            // Audio file (nullable)
            okhttp3.MultipartBody.Part audioFilePart = null;
            if (note.getLocalFilePath() != null) {
                File audioFile = new File(note.getLocalFilePath());
                if (audioFile.exists()) {
                    okhttp3.RequestBody audioFileBody = okhttp3.RequestBody.create(
                        okhttp3.MediaType.parse(note.getMimeType() != null ? note.getMimeType() : "audio/*"),
                        audioFile
                    );
                    audioFilePart = okhttp3.MultipartBody.Part.createFormData(
                        "audio_file",
                        audioFile.getName(),
                        audioFileBody
                    );
                }
            }

            // Appel API synchrone (dans un thread de sync)
            Call<com.ptms.mobile.api.ApiService.CreateNoteResponse> call =
                apiService.createProjectNote(
                    "Bearer " + token,
                    projectIdBody,
                    noteTypeBody,
                    noteTypeIdBody,
                    noteGroupBody,
                    titleBody,
                    contentBody,
                    transcriptionBody,
                    isImportantBody,
                    tagsBody,
                    audioFilePart,
                    null  // imageFilePart - pas encore géré dans le sync
                );

            Response<com.ptms.mobile.api.ApiService.CreateNoteResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                com.ptms.mobile.api.ApiService.CreateNoteResponse noteResponse = response.body();

                if (noteResponse.success) {
                    Log.d(TAG, "✅ Note uploadée - Server response: " + noteResponse.message);

                    // Marquer la note comme synchronisée
                    dbHelper.markNoteAsSynced(note.getId());

                    // Mettre à jour le server_id si retourné
                    // Note: Adapter selon la structure réelle de CreateNoteResponse
                    if (noteResponse.message != null && noteResponse.message.contains("id")) {
                        // Tenter d'extraire l'ID (à adapter selon la réponse réelle)
                        Log.d(TAG, "Server response message: " + noteResponse.message);
                    }

                    return true;
                } else {
                    Log.e(TAG, "❌ Serveur a refusé la note: " + noteResponse.message);
                    return false;
                }
            } else {
                Log.e(TAG, "❌ Réponse serveur invalide: " + response.code());
                if (response.errorBody() != null) {
                    Log.e(TAG, "Error body: " + response.errorBody().string());
                }
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception lors upload note", e);
            return false;
        }
    }

    // ==================== UTILITAIRES ====================

    private String getAuthToken() {
        return authPrefs.getString("auth_token", "");
    }

    private boolean isSyncInProgress() {
        return prefs.getBoolean(KEY_SYNC_IN_PROGRESS, false);
    }

    private void setSyncInProgress(boolean inProgress) {
        prefs.edit().putBoolean(KEY_SYNC_IN_PROGRESS, inProgress).apply();
    }

    private void updateSyncTimestamps(SyncType syncType) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();

        switch (syncType) {
            case FULL:
                editor.putLong(KEY_LAST_FULL_SYNC, now);
                editor.putLong(KEY_LAST_UPLOAD_SYNC, now);
                editor.putLong(KEY_LAST_DOWNLOAD_SYNC, now);
                break;
            case UPLOAD_ONLY:
                editor.putLong(KEY_LAST_UPLOAD_SYNC, now);
                break;
            case DOWNLOAD_ONLY:
                editor.putLong(KEY_LAST_DOWNLOAD_SYNC, now);
                break;
        }

        editor.apply();
    }

    public long getLastFullSync() {
        return prefs.getLong(KEY_LAST_FULL_SYNC, 0);
    }

    public long getLastUploadSync() {
        return prefs.getLong(KEY_LAST_UPLOAD_SYNC, 0);
    }

    public long getLastDownloadSync() {
        return prefs.getLong(KEY_LAST_DOWNLOAD_SYNC, 0);
    }

    /**
     * Retourne un résumé de l'état de synchronisation
     */
    public String getSyncStatusSummary() {
        long lastFullSync = getLastFullSync();
        long lastUpload = getLastUploadSync();
        long lastDownload = getLastDownloadSync();
        boolean inProgress = isSyncInProgress();

        StringBuilder summary = new StringBuilder();
        summary.append("🔄 État Synchronisation\n");
        summary.append("━━━━━━━━━━━━━━━━━━━━\n");

        if (inProgress) {
            summary.append("⏳ Synchronisation en cours...\n");
        } else {
            summary.append("✅ Prêt\n");
        }

        summary.append("\n");
        summary.append("📅 Dernières sync:\n");
        summary.append("  • Complète: ").append(formatTimestamp(lastFullSync)).append("\n");
        summary.append("  • Upload: ").append(formatTimestamp(lastUpload)).append("\n");
        summary.append("  • Download: ").append(formatTimestamp(lastDownload)).append("\n");

        return summary.toString();
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp == 0) {
            return "Jamais";
        }
        // ✅ OK: Display format can use locale-specific formatting
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return format.format(new Date(timestamp));
    }

    // ==================== MÉTHODES OFFLINE (CACHE LOCAL) ====================

    /**
     * Charge les projets depuis le cache local (mode offline)
     * TOUJOURS utiliser cette méthode pour charger les projets
     */
    public List<Project> getProjects() {
        return dbHelper.getAllProjects();
    }

    /**
     * Charge les types de travail depuis le cache local (mode offline)
     * TOUJOURS utiliser cette méthode pour charger les work types
     */
    public List<WorkType> getWorkTypes() {
        return dbHelper.getAllWorkTypes();
    }

    /**
     * Trouve un projet par ID dans le cache local
     */
    public Project getProjectById(int projectId) {
        List<Project> projects = getProjects();
        for (Project p : projects) {
            if (p.getId() == projectId) {
                return p;
            }
        }
        return null;
    }

    /**
     * Trouve un type de travail par ID dans le cache local
     */
    public WorkType getWorkTypeById(int workTypeId) {
        List<WorkType> workTypes = getWorkTypes();
        for (WorkType wt : workTypes) {
            if (wt.getId() == workTypeId) {
                return wt;
            }
        }
        return null;
    }

    /**
     * ✅ NOUVEAU (V7): Sauvegarde un rapport de temps (LOCAL-FIRST)
     *
     * ARCHITECTURE LOCAL-FIRST:
     * - TOUJOURS sauvegarder en local D'ABORD (instantané)
     * - Sync en arrière-plan si online (ne bloque pas l'utilisateur)
     * - Retry automatique si échec
     *
     * @param report Rapport de temps
     * @param callback Callback pour notifier le résultat
     */
    public void saveTimeReport(TimeReport report, SaveCallback callback) {
        // ✅ ÉTAPE 1: TOUJOURS sauvegarder en local D'ABORD
        saveTimeReportLocal(report, callback);

        // ✅ ÉTAPE 2: Si online, lancer sync en arrière-plan
        if (NetworkUtils.isOnline(context)) {
            // Sync asynchrone (ne bloque pas l'utilisateur)
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Délai pour laisser l'UI se mettre à jour
                    syncUpload(null); // Sync en background
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sync interrompue", e);
                }
            }).start();
        } else {
            Log.d(TAG, "Mode offline - Sync reportée");
        }
    }

    /**
     * Callback pour la sauvegarde
     */
    public interface SaveCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Sauvegarde en ligne (envoie à l'API)
     */
    private void saveTimeReportOnline(TimeReport report, SaveCallback callback) {
        String token = getAuthToken();
        if (token == null || token.isEmpty()) {
            if (callback != null) callback.onError("Non authentifié");
            return;
        }

        // Enrichir avec les noms pour l'affichage
        Project project = getProjectById(report.getProjectId());
        WorkType workType = getWorkTypeById(report.getWorkTypeId());
        if (project != null) report.setProjectName(project.getName());
        if (workType != null) report.setWorkTypeName(workType.getName());

        Call<ApiService.ApiResponse> call = apiService.saveTimeEntry(token, report);
        call.enqueue(new Callback<ApiService.ApiResponse>() {
            @Override
            public void onResponse(Call<ApiService.ApiResponse> call, Response<ApiService.ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    // Succès - sauvegarder aussi en cache local
                    report.setSyncStatus("synced");
                    report.setSyncAttempts(0);
                    dbHelper.insertTimeReport(report);

                    Log.d(TAG, "✅ Rapport sauvegardé online et en cache");
                    if (callback != null) callback.onSuccess("Heures sauvegardées avec succès");
                } else {
                    // Échec API - fallback local
                    Log.w(TAG, "⚠️ Échec API - fallback local");
                    saveTimeReportLocal(report, callback);
                }
            }

            @Override
            public void onFailure(Call<ApiService.ApiResponse> call, Throwable t) {
                // Erreur réseau - fallback local
                Log.w(TAG, "⚠️ Erreur réseau - fallback local: " + t.getMessage());
                saveTimeReportLocal(report, callback);
            }
        });
    }

    /**
     * ✅ NOUVEAU (V7): Sauvegarde locale (LOCAL-FIRST)
     * Utilisée TOUJOURS, que l'on soit online ou offline
     */
    private void saveTimeReportLocal(TimeReport report, SaveCallback callback) {
        try {
            // Enrichir avec les noms pour l'affichage
            Project project = getProjectById(report.getProjectId());
            WorkType workType = getWorkTypeById(report.getWorkTypeId());
            if (project != null) report.setProjectName(project.getName());
            if (workType != null) report.setWorkTypeName(workType.getName());

            // Marquer comme pending pour synchronisation ultérieure
            report.setSyncStatus("pending");
            report.setSyncAttempts(0);
            report.setSyncError(null);

            long id = dbHelper.insertTimeReport(report);

            if (id > 0) {
                Log.d(TAG, "✅ Rapport sauvegardé localement (ID: " + id + ")");
                if (callback != null) {
                    boolean isOnline = NetworkUtils.isOnline(context);
                    String message = isOnline ?
                        "📱 Saisie sauvegardée\nSynchronisation en arrière-plan..." :
                        "📱 Saisie sauvegardée localement\nSera synchronisée lors de la prochaine connexion";
                    callback.onSuccess(message);
                }
            } else {
                Log.e(TAG, "❌ Erreur sauvegarde locale");
                if (callback != null) callback.onError("Erreur sauvegarde locale");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception sauvegarde locale", e);
            if (callback != null) callback.onError("Erreur: " + e.getMessage());
        }
    }


    /**
     * Récupère le nombre de rapports en attente de synchronisation
     */
    public int getPendingSyncCount() {
        return dbHelper.getPendingSyncCount();
    }

    /**
     * Charge les projets ET work types depuis le serveur et met à jour le cache
     * À appeler au démarrage de l'app (si online) ou périodiquement
     */
    public void loadAndCacheReferenceData(LoadCallback callback) {
        if (!NetworkUtils.isOnline(context)) {
            Log.d(TAG, "⚠️ Mode offline - utilisation cache existant");
            if (callback != null) {
                callback.onLoaded(getProjects().size(), getWorkTypes().size());
            }
            return;
        }

        String token = getAuthToken();
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "⚠️ Pas de token - utilisation cache existant");
            if (callback != null) {
                callback.onLoaded(getProjects().size(), getWorkTypes().size());
            }
            return;
        }

        // Charger projets
        Call<ApiService.ProjectsResponse> projectsCall = apiService.getProjects(token);
        projectsCall.enqueue(new Callback<ApiService.ProjectsResponse>() {
            @Override
            public void onResponse(Call<ApiService.ProjectsResponse> call, Response<ApiService.ProjectsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<Project> projects = response.body().projects;
                    dbHelper.replaceAllProjects(projects);
                    Log.d(TAG, "✅ Projets chargés et mis en cache: " + projects.size());

                    if (callback != null) {
                        callback.onLoaded(projects.size(), getWorkTypes().size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Échec chargement projets: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiService.ProjectsResponse> call, Throwable t) {
                Log.e(TAG, "❌ Erreur chargement projets", t);
            }
        });

        // Charger work types
        Call<List<WorkType>> workTypesCall = apiService.getWorkTypes(token);
        workTypesCall.enqueue(new Callback<List<WorkType>>() {
            @Override
            public void onResponse(Call<List<WorkType>> call, Response<List<WorkType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WorkType> workTypes = response.body();
                    dbHelper.replaceAllWorkTypes(workTypes);
                    Log.d(TAG, "✅ Types de travail chargés et mis en cache: " + workTypes.size());

                    if (callback != null) {
                        callback.onLoaded(getProjects().size(), workTypes.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Échec chargement work types: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<WorkType>> call, Throwable t) {
                Log.e(TAG, "❌ Erreur chargement work types", t);
            }
        });
    }

    /**
     * Callback pour le chargement des données de référence
     */
    public interface LoadCallback {
        void onLoaded(int projectsCount, int workTypesCount);
    }

    /**
     * Vérifie si le cache contient des données
     */
    public boolean hasCachedData() {
        return !getProjects().isEmpty() && !getWorkTypes().isEmpty();
    }

    // ==================== SUPPORT MULTIMÉDIA COMPLET (V7 - PHASE 2) ====================

    /**
     * ✅ NOUVEAU (V7): Sauvegarde une note avec fichier multimédia (LOCAL-FIRST)
     *
     * ARCHITECTURE LOCAL-FIRST:
     * 1. Sauvegarder fichier en local storage
     * 2. Compression si image
     * 3. Génération thumbnail
     * 4. Sauvegarde métadonnées en SQLite
     * 5. Upload en arrière-plan si online
     *
     * @param note Note de projet
     * @param mediaFile Fichier audio/image/vidéo
     * @param callback Callback pour notifier le résultat
     */
    public void saveNoteWithMedia(ProjectNote note, File mediaFile, SaveCallback callback) {
        new Thread(() -> {
            try {
                com.ptms.mobile.storage.MediaStorageManager storage =
                    new com.ptms.mobile.storage.MediaStorageManager(context);

                Log.d(TAG, "📱 Sauvegarde note avec média: " + note.getTitle());
                Log.d(TAG, "Fichier source: " + mediaFile.getAbsolutePath() + " (" +
                    storage.formatSize(mediaFile.length()) + ")");

                // ✅ ÉTAPE 1: Sauvegarder fichier en local
                File localFile = storage.saveMediaFile(mediaFile, note.getNoteType());

                // ✅ ÉTAPE 2: Compression si image
                if (storage.isImageFile(localFile)) {
                    Log.d(TAG, "🖼️ Compression image...");
                    localFile = storage.compressImage(localFile, 1920, 85);
                }

                // ✅ ÉTAPE 3: Génération thumbnail si image/vidéo
                if (storage.isImageFile(localFile) || storage.isVideoFile(localFile)) {
                    try {
                        Log.d(TAG, "📸 Génération thumbnail...");
                        File thumbnail = storage.createThumbnail(localFile);
                        note.setThumbnailPath(thumbnail.getAbsolutePath());
                    } catch (Exception e) {
                        Log.w(TAG, "⚠️ Échec génération thumbnail: " + e.getMessage());
                        // Continue sans thumbnail
                    }
                }

                // ✅ ÉTAPE 4: Update note avec métadonnées fichier
                note.setLocalFilePath(localFile.getAbsolutePath());
                note.setFileSize(localFile.length());
                note.setMimeType(storage.getMimeType(localFile));
                note.setSyncStatus("pending");
                note.setSyncAttempts(0);
                note.setSyncError(null);

                // ✅ ÉTAPE 5: Sauvegarder en DB
                long id = dbHelper.insertProjectNote(note);

                if (id > 0) {
                    Log.d(TAG, "✅ Note sauvegardée localement (ID: " + id + ")");

                    // ✅ ÉTAPE 6: Upload en arrière-plan si online
                    if (NetworkUtils.isOnline(context)) {
                        Log.d(TAG, "📤 Lancement upload en arrière-plan...");

                        // Enqueue upload avec contraintes appropriées
                        if (storage.isVideoFile(localFile)) {
                            // Vidéo: WiFi uniquement
                            com.ptms.mobile.workers.MediaUploadWorker.enqueueVideoUpload(context, id);
                        } else {
                            // Audio/Image: n'importe quelle connexion
                            com.ptms.mobile.workers.MediaUploadWorker.enqueueUpload(context, id);
                        }

                        if (callback != null) {
                            callback.onSuccess("📱 Note sauvegardée\nUpload en arrière-plan...");
                        }
                    } else {
                        Log.d(TAG, "📵 Mode offline - Upload reporté");
                        if (callback != null) {
                            callback.onSuccess("📱 Note sauvegardée localement\nSera uploadée lors de la prochaine connexion");
                        }
                    }

                } else {
                    Log.e(TAG, "❌ Erreur sauvegarde note en DB");
                    if (callback != null) {
                        callback.onError("Erreur sauvegarde en base de données");
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception saveNoteWithMedia", e);
                if (callback != null) {
                    callback.onError("Erreur: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * ✅ NOUVEAU (V7): Lance la synchronisation de tous les fichiers multimédias en attente
     */
    public void syncAllPendingMedia() {
        if (!NetworkUtils.isOnline(context)) {
            Log.d(TAG, "⚠️ Pas de connexion - Sync médias reportée");
            return;
        }

        int pendingCount = dbHelper.getPendingMediaUploadsCount();
        if (pendingCount == 0) {
            Log.d(TAG, "✅ Aucun fichier multimédia en attente");
            return;
        }

        Log.d(TAG, "📤 Lancement sync de " + pendingCount + " fichiers multimédias...");
        com.ptms.mobile.workers.MediaUploadWorker.enqueueUploadAll(context);
    }

    /**
     * ✅ NOUVEAU (V7): Récupère le nombre de fichiers en attente d'upload
     */
    public int getPendingMediaCount() {
        return dbHelper.getPendingMediaUploadsCount();
    }
}

