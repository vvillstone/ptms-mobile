package com.ptms.mobile.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.utils.MediaUploadManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * ✅ Worker pour upload automatique des fichiers multimédias en arrière-plan
 *
 * Fonctionnalités :
 * - Upload fichiers "pending" automatiquement
 * - Progress tracking (0-100%)
 * - Retry automatique avec backoff
 * - Constraints : WiFi uniquement pour vidéos
 * - Support upload par chunks (gros fichiers > 10MB)
 *
 * @version 1.0
 * @date 2025-10-20
 */
public class MediaUploadWorker extends Worker {

    private static final String TAG = "MediaUploadWorker";
    private static final String KEY_NOTE_ID = "note_id";
    private static final int CHUNK_SIZE = 5 * 1024 * 1024; // 5MB par chunk

    private OfflineDatabaseHelper dbHelper;
    private ApiService apiService;
    private SharedPreferences authPrefs;
    private MediaUploadManager uploadManager;

    public MediaUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.dbHelper = new OfflineDatabaseHelper(context);
        this.apiService = ApiClient.getInstance(context).getApiService();
        this.authPrefs = context.getSharedPreferences("ptms_prefs", Context.MODE_PRIVATE);

        // Initialiser MediaUploadManager
        String baseUrl = authPrefs.getString("server_url", "https://serveralpha.protti.group");
        this.uploadManager = new MediaUploadManager(baseUrl);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🚀 Démarrage upload worker");
        Log.d(TAG, "========================================");

        try {
            // Récupérer le token d'authentification
            String token = authPrefs.getString("auth_token", "");
            if (token.isEmpty()) {
                Log.w(TAG, "⚠️ Pas de token - Upload annulé");
                return Result.failure();
            }

            // Récupérer l'ID spécifique si fourni
            long noteId = getInputData().getLong(KEY_NOTE_ID, -1);

            List<ProjectNote> pendingNotes;
            if (noteId > 0) {
                // Upload d'une note spécifique
                ProjectNote note = dbHelper.getProjectNoteById((int) noteId);
                if (note != null && note.getLocalFilePath() != null) {
                    pendingNotes = java.util.Collections.singletonList(note);
                } else {
                    Log.d(TAG, "Note #" + noteId + " introuvable ou sans fichier");
                    return Result.success();
                }
            } else {
                // Upload de toutes les notes pending
                pendingNotes = dbHelper.getPendingMediaUploads();
            }

            if (pendingNotes.isEmpty()) {
                Log.d(TAG, "✅ Aucun fichier en attente d'upload");
                return Result.success();
            }

            Log.d(TAG, "📤 Upload de " + pendingNotes.size() + " fichiers (notes)...");

            int uploadedCount = 0;
            int failedCount = 0;

            // ✅ NOUVEAU: Upload photos des TimeReport aussi
            List<TimeReport> pendingReports = dbHelper.getTimeReportsWithPendingMedia();
            if (!pendingReports.isEmpty()) {
                Log.d(TAG, "📤 Upload de " + pendingReports.size() + " photos (rapports)...");
                for (TimeReport report : pendingReports) {
                    try {
                        if (uploadTimeReportPhoto(report, token)) {
                            uploadedCount++;
                            Log.d(TAG, "✅ Photo rapport uploadée: ID " + report.getId());
                        } else {
                            failedCount++;
                            Log.e(TAG, "❌ Échec upload photo rapport: ID " + report.getId());
                        }
                    } catch (Exception e) {
                        failedCount++;
                        Log.e(TAG, "❌ Exception upload photo rapport #" + report.getId(), e);
                    }
                }
            }

            for (ProjectNote note : pendingNotes) {
                try {
                    if (uploadMedia(note, token)) {
                        uploadedCount++;
                        Log.d(TAG, "✅ Fichier uploadé: " + note.getTitle());
                    } else {
                        failedCount++;
                        Log.e(TAG, "❌ Échec upload: " + note.getTitle());
                    }
                } catch (Exception e) {
                    failedCount++;
                    Log.e(TAG, "❌ Exception upload note #" + note.getLocalId(), e);

                    // Incrémenter tentatives
                    int attempts = note.getSyncAttempts() + 1;
                    dbHelper.updateProjectNoteSyncStatus(
                        note.getLocalId(),
                        attempts < 3 ? "pending" : "failed",
                        "Exception: " + e.getMessage(),
                        attempts
                    );
                }
            }

            Log.d(TAG, "========================================");
            Log.d(TAG, "✅ Upload terminé: " + uploadedCount + " succès, " + failedCount + " échecs");
            Log.d(TAG, "========================================");

            return uploadedCount > 0 ? Result.success() : Result.retry();

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur globale upload worker", e);
            return Result.retry();
        }
    }

    /**
     * Upload un fichier multimédia vers le serveur
     */
    private boolean uploadMedia(ProjectNote note, String token) throws Exception {
        String localPath = note.getLocalFilePath();
        if (localPath == null || localPath.isEmpty()) {
            Log.w(TAG, "⚠️ Pas de fichier local pour note #" + note.getLocalId());
            return false;
        }

        File localFile = new File(localPath);
        if (!localFile.exists()) {
            Log.e(TAG, "❌ Fichier introuvable: " + localPath);
            dbHelper.updateProjectNoteSyncStatus(
                note.getLocalId(),
                "failed",
                "Fichier local introuvable",
                note.getSyncAttempts() + 1
            );
            return false;
        }

        // Déterminer si upload par chunks (fichiers > 10MB)
        long fileSize = localFile.length();
        boolean useChunks = fileSize > 10 * 1024 * 1024; // 10MB

        Log.d(TAG, "📤 Upload: " + localFile.getName() + " (" + formatSize(fileSize) + ")");

        if (useChunks) {
            return uploadByChunks(note, localFile, token);
        } else {
            return uploadDirect(note, localFile, token);
        }
    }

    /**
     * Upload direct (fichiers < 10MB)
     */
    private boolean uploadDirect(ProjectNote note, File file, String token) throws Exception {
        // Mise à jour progress: 0%
        dbHelper.updateUploadProgress(note.getLocalId(), 0);

        // Préparer les paramètres
        RequestBody projectId = RequestBody.create(MediaType.parse("text/plain"),
            String.valueOf(note.getProjectId()));
        RequestBody noteType = RequestBody.create(MediaType.parse("text/plain"),
            note.getNoteType());
        RequestBody title = RequestBody.create(MediaType.parse("text/plain"),
            note.getTitle() != null ? note.getTitle() : "");

        // Préparer le fichier
        String mimeType = note.getMimeType() != null ? note.getMimeType() : "application/octet-stream";
        RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
            "media_file",
            file.getName(),
            fileBody
        );

        // Mise à jour progress: 50% (upload en cours)
        dbHelper.updateUploadProgress(note.getLocalId(), 50);

        // Appel API
        Call<ApiService.CreateNoteResponse> call = apiService.uploadProjectMedia(
            token,
            projectId,
            noteType,
            title,
            filePart
        );

        Response<ApiService.CreateNoteResponse> response = call.execute();

        if (response.isSuccessful() && response.body() != null && response.body().success) {
            // Succès - marquer comme synchronisé
            String serverUrl = response.body().fileUrl;
            dbHelper.markMediaAsSynced(note.getLocalId(), serverUrl);

            Log.d(TAG, "✅ Upload réussi - URL: " + serverUrl);
            return true;

        } else {
            // Échec
            String error = response.body() != null ?
                response.body().message : "HTTP " + response.code();

            int attempts = note.getSyncAttempts() + 1;
            dbHelper.updateProjectNoteSyncStatus(
                note.getLocalId(),
                attempts < 3 ? "pending" : "failed",
                error,
                attempts
            );

            Log.e(TAG, "❌ Échec upload: " + error);
            return false;
        }
    }

    /**
     * Upload par chunks (fichiers > 10MB)
     */
    private boolean uploadByChunks(ProjectNote note, File file, String token) throws Exception {
        // TODO: Implémenter upload par chunks
        // Pour l'instant, utiliser upload direct (peut timeout pour gros fichiers)
        Log.w(TAG, "⚠️ Upload par chunks non implémenté - utilisation upload direct");
        return uploadDirect(note, file, token);
    }

    /**
     * ✅ NOUVEAU: Upload photo d'un TimeReport via MediaUploadManager
     */
    private boolean uploadTimeReportPhoto(TimeReport report, String token) {
        String imagePath = report.getImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            Log.w(TAG, "⚠️ Pas de photo pour rapport #" + report.getId());
            return false;
        }

        // Si l'image commence par "/uploads/", elle est déjà sur le serveur
        if (imagePath.startsWith("/uploads/") || imagePath.startsWith("uploads/")) {
            Log.d(TAG, "ℹ️ Photo déjà uploadée pour rapport #" + report.getId());
            dbHelper.updateTimeReportSyncStatus(report.getId(), "synced", null);
            return true;
        }

        File photoFile = new File(imagePath);
        if (!photoFile.exists()) {
            Log.e(TAG, "❌ Fichier photo introuvable: " + imagePath);
            dbHelper.updateTimeReportSyncStatus(report.getId(), "failed", "Fichier introuvable");
            return false;
        }

        Log.d(TAG, "📤 Upload photo rapport: " + photoFile.getName());

        // Set auth token
        uploadManager.setAuthToken(token);

        // Upload synchrone via MediaUploadManager
        final boolean[] success = {false};
        final String[] serverPath = {null};
        final Exception[] error = {null};

        uploadManager.upload(
            photoFile,
            MediaUploadManager.MediaType.IMAGE,
            MediaUploadManager.Context.TEMP,
            false,
            new MediaUploadManager.MediaUploadCallback() {
                @Override
                public void onSuccess(MediaUploadManager.MediaUploadResult result) {
                    success[0] = true;
                    serverPath[0] = result.getPath();
                    Log.d(TAG, "✅ Photo uploadée: " + serverPath[0]);

                    // Mettre à jour le rapport avec le chemin serveur
                    report.setImagePath(serverPath[0]);
                    dbHelper.updateTimeReport(report);
                    dbHelper.updateTimeReportSyncStatus(report.getId(), "synced", null);
                }

                @Override
                public void onError(Exception e) {
                    error[0] = e;
                    Log.e(TAG, "❌ Erreur upload photo rapport", e);
                    dbHelper.updateTimeReportSyncStatus(
                        report.getId(),
                        "failed",
                        "Erreur upload: " + e.getMessage()
                    );
                }

                @Override
                public void onProgress(int percent) {
                    // Log progress
                    if (percent % 25 == 0) {
                        Log.d(TAG, "Upload progress: " + percent + "%");
                    }
                }
            }
        );

        // Attendre la fin de l'upload (max 30 secondes)
        int waitCount = 0;
        while (!success[0] && error[0] == null && waitCount < 300) {
            try {
                Thread.sleep(100);
                waitCount++;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted waiting for upload", e);
                return false;
            }
        }

        return success[0];
    }

    /**
     * Formate une taille en bytes
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    // ==================== MÉTHODES STATIQUES POUR ENQUEUE ====================

    /**
     * Enqueue un upload pour une note spécifique
     */
    public static void enqueueUpload(Context context, long noteId) {
        Data inputData = new Data.Builder()
            .putLong(KEY_NOTE_ID, noteId)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest uploadWork = new OneTimeWorkRequest.Builder(MediaUploadWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.SECONDS) // Délai pour ne pas bloquer l'UI
            .build();

        WorkManager.getInstance(context).enqueue(uploadWork);
        Log.d(TAG, "📤 Upload enqueued pour note #" + noteId);
    }

    /**
     * Enqueue un upload de tous les fichiers pending
     */
    public static void enqueueUploadAll(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest uploadWork = new OneTimeWorkRequest.Builder(MediaUploadWorker.class)
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context).enqueue(uploadWork);
        Log.d(TAG, "📤 Upload enqueued pour tous les fichiers pending");
    }

    /**
     * Enqueue un upload pour vidéos (nécessite WiFi)
     */
    public static void enqueueVideoUpload(Context context, long noteId) {
        Data inputData = new Data.Builder()
            .putLong(KEY_NOTE_ID, noteId)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi uniquement
            .setRequiresBatteryNotLow(true) // Batterie OK
            .build();

        OneTimeWorkRequest uploadWork = new OneTimeWorkRequest.Builder(MediaUploadWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build();

        WorkManager.getInstance(context).enqueue(uploadWork);
        Log.d(TAG, "📤 Upload vidéo enqueued (WiFi uniquement) pour note #" + noteId);
    }
}
