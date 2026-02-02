package com.ptms.mobile.api;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ✅ Intercepteur pour gérer le cache HTTP intelligent
 *
 * Stratégie de cache par type de données :
 * - Données statiques (projets, types de travail) : 1 heure
 * - Données dynamiques (rapports, profil) : 5 minutes
 * - Données temps réel (chat, présence) : pas de cache
 * - Données critiques (login, création) : pas de cache
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class CacheInterceptor implements Interceptor {

    private static final String TAG = "CacheInterceptor";

    // Durées de cache (en secondes)
    private static final int CACHE_1_HOUR = 60 * 60;           // 3600s = 1h
    private static final int CACHE_30_MINUTES = 60 * 30;       // 1800s = 30min
    private static final int CACHE_5_MINUTES = 60 * 5;         // 300s = 5min
    private static final int CACHE_1_MINUTE = 60;              // 60s = 1min
    private static final int NO_CACHE = 0;                     // Pas de cache

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();
        String method = request.method();

        // Déterminer la durée de cache selon l'endpoint
        int cacheMaxAge = getCacheMaxAge(url, method);

        // Si GET et cacheable, ajouter Cache-Control à la requête
        if ("GET".equals(method) && cacheMaxAge > 0) {
            Request cacheRequest = request.newBuilder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(cacheMaxAge, java.util.concurrent.TimeUnit.SECONDS)
                            .build())
                    .build();

            Response response = chain.proceed(cacheRequest);

            // Forcer le cache dans la réponse (car le serveur ne le fait pas toujours)
            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "public, max-age=" + cacheMaxAge)
                    .build();

        } else {
            // Pas de cache pour POST/PUT/DELETE ou endpoints non cachables
            Request noCacheRequest = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build();

            return chain.proceed(noCacheRequest);
        }
    }

    /**
     * Détermine la durée de cache selon l'endpoint
     *
     * @param url    URL complète de la requête
     * @param method Méthode HTTP (GET, POST, etc.)
     * @return Durée de cache en secondes (0 = pas de cache)
     */
    private int getCacheMaxAge(String url, String method) {
        // Pas de cache pour les méthodes de modification
        if (!"GET".equals(method)) {
            return NO_CACHE;
        }

        // ========== DONNÉES STATIQUES (1 heure) ==========
        if (url.contains("/projects.php") ||
                url.contains("/work-types.php") ||
                url.contains("/project-types.php") ||
                url.contains("/departments.php") ||
                url.contains("/teams.php")) {
            Log.d(TAG, "📦 Cache 1h: " + extractEndpoint(url));
            return CACHE_1_HOUR;
        }

        // ========== DONNÉES SEMI-STATIQUES (30 minutes) ==========
        if (url.contains("/profile.php") ||
                url.contains("/employee-profile.php") ||
                url.contains("/project-details.php")) {
            Log.d(TAG, "📦 Cache 30min: " + extractEndpoint(url));
            return CACHE_30_MINUTES;
        }

        // ========== DONNÉES DYNAMIQUES (5 minutes) ==========
        if (url.contains("/reports.php") ||
                url.contains("/time-entries.php") ||
                url.contains("/statistics.php") ||
                url.contains("/dashboard.php") ||
                url.contains("/project-notes.php")) {
            Log.d(TAG, "📦 Cache 5min: " + extractEndpoint(url));
            return CACHE_5_MINUTES;
        }

        // ========== DONNÉES TEMPS RÉEL (1 minute) ==========
        if (url.contains("/chat-rooms.php") ||
                url.contains("/notifications.php") ||
                url.contains("/alerts.php")) {
            Log.d(TAG, "📦 Cache 1min: " + extractEndpoint(url));
            return CACHE_1_MINUTE;
        }

        // ========== PAS DE CACHE ==========
        // Chat messages, présence, login, création, etc.
        if (url.contains("/chat-messages.php") ||
                url.contains("/chat-send.php") ||
                url.contains("/presence/ping") ||
                url.contains("/presence.php") ||
                url.contains("/login.php") ||
                url.contains("/logout.php") ||
                url.contains("/time-entry.php") ||
                url.contains("/create-") ||
                url.contains("/update-") ||
                url.contains("/delete-")) {
            Log.d(TAG, "🚫 Pas de cache: " + extractEndpoint(url));
            return NO_CACHE;
        }

        // Par défaut: cache court pour les autres endpoints GET
        Log.d(TAG, "📦 Cache par défaut (1min): " + extractEndpoint(url));
        return CACHE_1_MINUTE;
    }

    /**
     * Extrait le nom de l'endpoint pour les logs
     */
    private String extractEndpoint(String url) {
        if (url == null) return "unknown";

        // Extraire le dernier segment (ex: /api/projects.php → projects.php)
        String[] parts = url.split("/");
        if (parts.length > 0) {
            String endpoint = parts[parts.length - 1];
            // Retirer les paramètres de query
            int queryIndex = endpoint.indexOf('?');
            if (queryIndex > 0) {
                endpoint = endpoint.substring(0, queryIndex);
            }
            return endpoint;
        }
        return "unknown";
    }

    /**
     * Vérifie si une réponse a été servie depuis le cache
     */
    public static boolean isFromCache(Response response) {
        return response.cacheResponse() != null;
    }

    /**
     * Vérifie si une réponse vient du réseau
     */
    public static boolean isFromNetwork(Response response) {
        return response.networkResponse() != null;
    }

    /**
     * Log les informations de cache d'une réponse
     */
    public static void logCacheStatus(Response response, String tag) {
        if (isFromCache(response)) {
            Log.d(tag, "✅ Réponse depuis CACHE");
        } else if (isFromNetwork(response)) {
            Log.d(tag, "🌐 Réponse depuis RÉSEAU");
        } else {
            Log.d(tag, "❓ Source de réponse inconnue");
        }
    }
}
