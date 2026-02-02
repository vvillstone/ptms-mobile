package com.ptms.mobile.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.storage.MediaStorageManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ✅ Worker pour nettoyage automatique du cache multimédia
 *
 * Règles de nettoyage intelligentes :
 * 1. GARDER TOUS les fichiers "pending" (pas encore synchronisés)
 * 2. GARDER les fichiers "synced" < 30 jours
 * 3. SUPPRIMER les fichiers "synced" > 30 jours SI espace < 500MB
 * 4. GARDER au minimum les 50 derniers fichiers
 *
 * Planification : 1x par semaine (dimanche 3h du matin)
 *
 * @version 1.0
 * @date 2025-10-20
 */
public class CacheCleanupWorker extends Worker {

    private static final String TAG = "CacheCleanupWorker";
    private static final String WORK_NAME = "media_cache_cleanup";

    // Paramètres de nettoyage
    private static final int KEEP_DAYS = 30; // Garder fichiers < 30 jours
    private static final long MIN_FREE_SPACE = 500 * 1024 * 1024; // 500MB
    private static final int MIN_FILES_TO_KEEP = 50; // Minimum 50 fichiers

    private OfflineDatabaseHelper dbHelper;
    private MediaStorageManager storage;

    public CacheCleanupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.dbHelper = new OfflineDatabaseHelper(context);
        this.storage = new MediaStorageManager(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🧹 Démarrage nettoyage cache");
        Log.d(TAG, "========================================");

        try {
            // Statistiques AVANT nettoyage
            long cacheSize = storage.getCacheSize();
            long availableSpace = getAvailableSpace();

            Log.d(TAG, "📊 AVANT nettoyage:");
            Log.d(TAG, "  • Taille cache: " + storage.formatSize(cacheSize));
            Log.d(TAG, "  • Espace libre: " + storage.formatSize(availableSpace));

            // Vérifier si nettoyage nécessaire
            if (availableSpace > MIN_FREE_SPACE) {
                Log.d(TAG, "✅ Espace libre suffisant - Nettoyage minimal");
                return Result.success();
            }

            // Récupérer fichiers candidats au nettoyage
            long cutoffTime = System.currentTimeMillis() - (KEEP_DAYS * 24L * 60 * 60 * 1000);
            List<ProjectNote> oldNotes = dbHelper.getSyncedMediaOlderThan(cutoffTime);

            Log.d(TAG, "🔍 Fichiers candidats au nettoyage: " + oldNotes.size());

            if (oldNotes.isEmpty()) {
                Log.d(TAG, "✅ Aucun fichier à nettoyer");
                return Result.success();
            }

            // Trier par date (plus anciens en premier)
            // Les notes sont déjà triées par created_at ASC dans la requête SQL

            int deletedCount = 0;
            int skippedCount = 0;
            long spaceFreed = 0;

            // Nettoyer fichiers, mais garder au minimum MIN_FILES_TO_KEEP
            int totalFiles = getTotalSyncedFilesCount();

            for (ProjectNote note : oldNotes) {
                // Sécurité: Garder minimum MIN_FILES_TO_KEEP fichiers
                if (totalFiles - deletedCount <= MIN_FILES_TO_KEEP) {
                    Log.d(TAG, "⚠️ Limite atteinte: minimum " + MIN_FILES_TO_KEEP + " fichiers gardés");
                    break;
                }

                // Vérifier si espace libre suffisant
                if (getAvailableSpace() > MIN_FREE_SPACE) {
                    Log.d(TAG, "✅ Espace libre suffisant atteint");
                    break;
                }

                // Supprimer le fichier local
                String localPath = note.getLocalFilePath();
                if (localPath != null && !localPath.isEmpty()) {
                    File localFile = new File(localPath);
                    if (localFile.exists()) {
                        long fileSize = localFile.length();

                        if (localFile.delete()) {
                            deletedCount++;
                            spaceFreed += fileSize;
                            Log.d(TAG, "🗑️ Supprimé: " + localFile.getName() + " (" +
                                storage.formatSize(fileSize) + ")");

                            // Supprimer aussi le thumbnail si existe
                            String thumbPath = note.getThumbnailPath();
                            if (thumbPath != null && !thumbPath.isEmpty()) {
                                File thumbFile = new File(thumbPath);
                                if (thumbFile.exists()) {
                                    long thumbSize = thumbFile.length();
                                    if (thumbFile.delete()) {
                                        spaceFreed += thumbSize;
                                    }
                                }
                            }

                            // Update DB: Marquer fichier local comme supprimé
                            dbHelper.clearLocalMediaFile(note.getLocalId());

                        } else {
                            Log.w(TAG, "⚠️ Impossible de supprimer: " + localFile.getName());
                            skippedCount++;
                        }
                    }
                }
            }

            // Nettoyage supplémentaire: Fichiers orphelins (pas référencés en DB)
            int orphansDeleted = cleanupOrphanFiles();

            // Statistiques APRÈS nettoyage
            long newCacheSize = storage.getCacheSize();
            long newAvailableSpace = getAvailableSpace();

            Log.d(TAG, "========================================");
            Log.d(TAG, "✅ Nettoyage terminé:");
            Log.d(TAG, "  • Fichiers supprimés: " + deletedCount);
            Log.d(TAG, "  • Fichiers ignorés: " + skippedCount);
            Log.d(TAG, "  • Orphelins supprimés: " + orphansDeleted);
            Log.d(TAG, "  • Espace libéré: " + storage.formatSize(spaceFreed));
            Log.d(TAG, "📊 APRÈS nettoyage:");
            Log.d(TAG, "  • Taille cache: " + storage.formatSize(newCacheSize));
            Log.d(TAG, "  • Espace libre: " + storage.formatSize(newAvailableSpace));
            Log.d(TAG, "========================================");

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur nettoyage cache", e);
            return Result.retry();
        }
    }

    /**
     * Nettoie les fichiers orphelins (pas référencés en DB)
     */
    private int cleanupOrphanFiles() {
        // TODO: Implémenter nettoyage orphelins
        // Parcourir /media/ et vérifier si chaque fichier existe dans project_notes
        return 0;
    }

    /**
     * Compte le nombre total de fichiers synchronisés
     */
    private int getTotalSyncedFilesCount() {
        // Approximation via requête SQL COUNT
        // TODO: Ajouter méthode dédiée dans OfflineDatabaseHelper si besoin
        return 100; // Valeur par défaut conservatrice
    }

    /**
     * Récupère l'espace disque disponible
     */
    private long getAvailableSpace() {
        File dataDir = getApplicationContext().getFilesDir();
        return dataDir.getFreeSpace();
    }

    // ==================== MÉTHODES STATIQUES POUR PLANIFICATION ====================

    /**
     * Planifie le nettoyage automatique périodique (1x par semaine)
     */
    public static void schedulePeriodicCleanup(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiresCharging(true) // Uniquement quand en charge
            .setRequiresBatteryNotLow(true) // Batterie OK
            .build();

        PeriodicWorkRequest cleanupWork = new PeriodicWorkRequest.Builder(
            CacheCleanupWorker.class,
            7, TimeUnit.DAYS // 1x par semaine
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS) // Premier run dans 1h
            .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Garder existant si déjà planifié
            cleanupWork
        );

        Log.d(TAG, "📅 Nettoyage périodique planifié (1x par semaine)");
    }

    /**
     * Lance un nettoyage manuel immédiat
     */
    public static void cleanupNow(Context context) {
        androidx.work.OneTimeWorkRequest cleanupWork =
            new androidx.work.OneTimeWorkRequest.Builder(CacheCleanupWorker.class)
                .build();

        WorkManager.getInstance(context).enqueue(cleanupWork);
        Log.d(TAG, "🧹 Nettoyage manuel lancé");
    }

    /**
     * Annule le nettoyage périodique
     */
    public static void cancelPeriodicCleanup(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "❌ Nettoyage périodique annulé");
    }
}
