package com.ptms.mobile.api;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;

/**
 * ✅ Gestionnaire centralisé du cache HTTP
 *
 * Fonctionnalités :
 * - Création et gestion du cache OkHttp
 * - Statistiques d'utilisation du cache
 * - Nettoyage manuel du cache
 * - Taille du cache configurable
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class CacheManager {

    private static final String TAG = "CacheManager";
    private static final String CACHE_DIR_NAME = "http_cache";

    // Taille du cache : 50 MB par défaut
    private static final long DEFAULT_CACHE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final long MAX_CACHE_SIZE = 100 * 1024 * 1024;    // 100 MB maximum
    private static final long MIN_CACHE_SIZE = 10 * 1024 * 1024;     // 10 MB minimum

    private final Context context;
    private Cache okHttpCache;
    private File cacheDirectory;

    public CacheManager(Context context) {
        this.context = context.getApplicationContext();
        initializeCache(DEFAULT_CACHE_SIZE);
    }

    /**
     * Initialise le cache OkHttp
     */
    private void initializeCache(long cacheSize) {
        try {
            // Créer le dossier cache
            cacheDirectory = new File(context.getCacheDir(), CACHE_DIR_NAME);
            if (!cacheDirectory.exists()) {
                boolean created = cacheDirectory.mkdirs();
                if (created) {
                    Log.d(TAG, "📁 Dossier cache créé: " + cacheDirectory.getAbsolutePath());
                } else {
                    Log.w(TAG, "⚠️ Impossible de créer le dossier cache");
                }
            }

            // Créer le cache OkHttp
            okHttpCache = new Cache(cacheDirectory, cacheSize);
            Log.d(TAG, "✅ Cache HTTP initialisé (" + formatSize(cacheSize) + ")");
            Log.d(TAG, "📍 Localisation: " + cacheDirectory.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur initialisation cache", e);
        }
    }

    /**
     * Retourne l'instance du cache OkHttp
     */
    public Cache getOkHttpCache() {
        return okHttpCache;
    }

    /**
     * Obtient la taille actuelle du cache
     */
    public long getCacheSize() {
        try {
            if (okHttpCache != null) {
                return okHttpCache.size();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lecture taille cache", e);
        }
        return 0;
    }

    /**
     * Obtient la taille maximale configurée du cache
     */
    public long getCacheMaxSize() {
        try {
            if (okHttpCache != null) {
                return okHttpCache.maxSize();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lecture taille max cache", e);
        }
        return DEFAULT_CACHE_SIZE;
    }

    /**
     * Obtient le nombre d'entrées dans le cache
     */
    public int getCacheEntryCount() {
        try {
            if (okHttpCache != null) {
                return okHttpCache.networkCount() + okHttpCache.hitCount();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur comptage entrées cache", e);
        }
        return 0;
    }

    /**
     * Obtient le taux de succès du cache (hit rate)
     */
    public float getCacheHitRate() {
        try {
            if (okHttpCache != null) {
                int hits = okHttpCache.hitCount();
                int requests = hits + okHttpCache.networkCount();
                if (requests > 0) {
                    return (float) hits / requests * 100;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur calcul hit rate", e);
        }
        return 0;
    }

    /**
     * Nettoie le cache (supprime les entrées expirées)
     */
    public void cleanCache() {
        try {
            if (okHttpCache != null) {
                long sizeBefore = getCacheSize();
                okHttpCache.evictAll();
                long sizeAfter = getCacheSize();
                long freed = sizeBefore - sizeAfter;

                Log.d(TAG, "🧹 Cache nettoyé");
                Log.d(TAG, "  • Avant: " + formatSize(sizeBefore));
                Log.d(TAG, "  • Après: " + formatSize(sizeAfter));
                Log.d(TAG, "  • Libéré: " + formatSize(freed));
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur nettoyage cache", e);
        }
    }

    /**
     * Vide complètement le cache
     */
    public void clearCache() {
        try {
            if (okHttpCache != null) {
                okHttpCache.delete();
                Log.d(TAG, "🗑️ Cache supprimé complètement");

                // Recréer le cache
                initializeCache(DEFAULT_CACHE_SIZE);
            }
        } catch (IOException e) {
            Log.e(TAG, "❌ Erreur suppression cache", e);
        }
    }

    /**
     * Supprime une entrée spécifique du cache
     */
    public void removeCacheEntry(String url) {
        try {
            if (okHttpCache != null) {
                // OkHttp ne permet pas de supprimer une entrée spécifique facilement
                // On peut forcer un refresh en vidant le cache complet
                // Alternativement, on peut simplement ignorer le cache pour cette requête
                Log.d(TAG, "⚠️ Pour supprimer une entrée spécifique, utilisez CacheControl.FORCE_NETWORK");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur suppression entrée cache", e);
        }
    }

    /**
     * Affiche les statistiques du cache
     */
    public void logCacheStatistics() {
        if (okHttpCache == null) {
            Log.d(TAG, "❌ Cache non initialisé");
            return;
        }

        try {
            Log.d(TAG, "========================================");
            Log.d(TAG, "📊 STATISTIQUES DU CACHE HTTP");
            Log.d(TAG, "========================================");
            Log.d(TAG, "📍 Localisation: " + cacheDirectory.getAbsolutePath());
            Log.d(TAG, "📦 Taille actuelle: " + formatSize(getCacheSize()));
            Log.d(TAG, "📦 Taille maximale: " + formatSize(getCacheMaxSize()));
            Log.d(TAG, "📈 Utilisation: " + String.format("%.1f%%",
                    (float) getCacheSize() / getCacheMaxSize() * 100));
            Log.d(TAG, "🎯 Hits (cache): " + okHttpCache.hitCount());
            Log.d(TAG, "🌐 Misses (réseau): " + okHttpCache.networkCount());
            Log.d(TAG, "📊 Taux de succès: " + String.format("%.1f%%", getCacheHitRate()));
            Log.d(TAG, "📝 Nombre d'écritures: " + okHttpCache.writeSuccessCount());
            Log.d(TAG, "❌ Échecs écriture: " + okHttpCache.writeAbortCount());
            Log.d(TAG, "========================================");
        } catch (Exception e) {
            Log.e(TAG, "Erreur affichage statistiques", e);
        }
    }

    /**
     * Vérifie la santé du cache
     */
    public boolean isCacheHealthy() {
        if (okHttpCache == null) {
            return false;
        }

        try {
            // Vérifier que le dossier cache existe
            if (!cacheDirectory.exists() || !cacheDirectory.canWrite()) {
                Log.w(TAG, "⚠️ Dossier cache inaccessible");
                return false;
            }

            // Vérifier que le cache n'est pas plein à 95%
            float usage = (float) getCacheSize() / getCacheMaxSize();
            if (usage > 0.95f) {
                Log.w(TAG, "⚠️ Cache presque plein (" + String.format("%.1f%%", usage * 100) + ")");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Erreur vérification santé cache", e);
            return false;
        }
    }

    /**
     * Configure la taille du cache
     */
    public void setCacheSize(long newSize) {
        if (newSize < MIN_CACHE_SIZE) {
            Log.w(TAG, "⚠️ Taille minimum: " + formatSize(MIN_CACHE_SIZE));
            newSize = MIN_CACHE_SIZE;
        }
        if (newSize > MAX_CACHE_SIZE) {
            Log.w(TAG, "⚠️ Taille maximum: " + formatSize(MAX_CACHE_SIZE));
            newSize = MAX_CACHE_SIZE;
        }

        try {
            // Fermer et recréer le cache avec nouvelle taille
            if (okHttpCache != null) {
                okHttpCache.close();
            }
            initializeCache(newSize);
            Log.d(TAG, "✅ Taille du cache modifiée: " + formatSize(newSize));

        } catch (IOException e) {
            Log.e(TAG, "❌ Erreur modification taille cache", e);
        }
    }

    /**
     * Formate une taille en bytes vers un format lisible
     */
    public String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Ferme proprement le cache
     */
    public void close() {
        try {
            if (okHttpCache != null) {
                okHttpCache.close();
                Log.d(TAG, "✅ Cache fermé proprement");
            }
        } catch (IOException e) {
            Log.e(TAG, "Erreur fermeture cache", e);
        }
    }

    /**
     * Flush le cache (force l'écriture sur disque)
     */
    public void flushCache() {
        try {
            if (okHttpCache != null) {
                okHttpCache.flush();
                Log.d(TAG, "✅ Cache synchronisé sur disque");
            }
        } catch (IOException e) {
            Log.e(TAG, "Erreur flush cache", e);
        }
    }
}
