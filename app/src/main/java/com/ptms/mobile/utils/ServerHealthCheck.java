package com.ptms.mobile.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Système de health check pour le serveur PTMS
 * Détecte rapidement si le serveur est accessible avec un ping intelligent
 */
public class ServerHealthCheck {

    private static final String TAG = "ServerHealthCheck";

    // Timeout très court pour détecter rapidement l'indisponibilité
    private static final int QUICK_PING_TIMEOUT = 3000; // 3 secondes
    private static final int STANDARD_PING_TIMEOUT = 8000; // 8 secondes
    private static final int RETRY_COUNT = 2; // Nombre de tentatives

    // État du serveur
    private static ServerStatus lastKnownStatus = ServerStatus.UNKNOWN;
    private static long lastCheckTime = 0;
    private static final long CACHE_DURATION = 10000; // 10 secondes de cache

    // Gestion du monitoring continu
    private static AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private static Handler monitoringHandler = new Handler(Looper.getMainLooper());
    private static StatusChangeListener globalListener = null;

    /**
     * États possibles du serveur
     */
    public enum ServerStatus {
        ONLINE,      // Serveur accessible et fonctionnel
        OFFLINE,     // Serveur inaccessible
        SLOW,        // Serveur accessible mais lent
        ERROR,       // Erreur lors du test
        UNKNOWN      // État inconnu (jamais testé)
    }

    /**
     * Interface de callback pour les résultats
     */
    public interface HealthCheckCallback {
        void onHealthCheckComplete(ServerStatus status, long responseTime, String message);
    }

    /**
     * Interface pour écouter les changements d'état
     */
    public interface StatusChangeListener {
        void onStatusChanged(ServerStatus oldStatus, ServerStatus newStatus, String message);
    }

    /**
     * Classe pour le résultat détaillé du ping
     */
    public static class PingResult {
        public final ServerStatus status;
        public final long responseTime;
        public final String message;
        public final String details;

        public PingResult(ServerStatus status, long responseTime, String message, String details) {
            this.status = status;
            this.responseTime = responseTime;
            this.message = message;
            this.details = details;
        }

        public boolean isOnline() {
            return status == ServerStatus.ONLINE || status == ServerStatus.SLOW;
        }
    }

    // ==================== PING RAPIDE ====================

    /**
     * Ping rapide avec timeout court (3 secondes)
     * Utilisé pour détecter rapidement l'indisponibilité
     */
    public static void quickPing(Context context, HealthCheckCallback callback) {
        ping(context, QUICK_PING_TIMEOUT, 1, callback);
    }

    /**
     * Ping standard avec timeout normal (8 secondes) et retry
     * Utilisé pour une vérification plus complète
     */
    public static void standardPing(Context context, HealthCheckCallback callback) {
        ping(context, STANDARD_PING_TIMEOUT, RETRY_COUNT, callback);
    }

    /**
     * Ping synchrone (à utiliser uniquement dans un thread background)
     */
    public static PingResult syncPing(Context context) {
        return syncPingWithRetry(context, QUICK_PING_TIMEOUT, 1);
    }

    /**
     * Ping asynchrone avec cache
     */
    public static void cachedPing(Context context, HealthCheckCallback callback) {
        long now = System.currentTimeMillis();

        // Utiliser le cache si récent
        if (lastKnownStatus != ServerStatus.UNKNOWN && (now - lastCheckTime) < CACHE_DURATION) {
            Log.d(TAG, "Utilisation du cache: " + lastKnownStatus + " (âge: " + (now - lastCheckTime) + "ms)");
            callback.onHealthCheckComplete(lastKnownStatus, 0, "Cache récent");
            return;
        }

        // Sinon faire un ping rapide
        quickPing(context, callback);
    }

    // ==================== IMPLÉMENTATION INTERNE ====================

    private static void ping(Context context, int timeout, int retries, HealthCheckCallback callback) {
        new Thread(() -> {
            PingResult result = syncPingWithRetry(context, timeout, retries);

            // Mettre à jour le cache
            lastKnownStatus = result.status;
            lastCheckTime = System.currentTimeMillis();

            // Notifier via le callback
            new Handler(Looper.getMainLooper()).post(() ->
                callback.onHealthCheckComplete(result.status, result.responseTime, result.message)
            );

            // Notifier le listener global si changement d'état
            if (globalListener != null && result.status != lastKnownStatus) {
                ServerStatus oldStatus = lastKnownStatus;
                new Handler(Looper.getMainLooper()).post(() ->
                    globalListener.onStatusChanged(oldStatus, result.status, result.message)
                );
            }
        }).start();
    }

    private static PingResult syncPingWithRetry(Context context, int timeout, int retries) {
        SettingsManager settings = new SettingsManager(context);
        String serverUrl = settings.getServerUrl();
        boolean ignoreSsl = settings.isIgnoreSsl();

        Log.d(TAG, "Ping du serveur: " + serverUrl + " (timeout: " + timeout + "ms, retries: " + retries + ")");

        // Vérifier d'abord la connectivité réseau
        if (!NetworkUtils.isOnline(context)) {
            Log.d(TAG, "Pas de connexion réseau");
            return new PingResult(ServerStatus.OFFLINE, 0, "Pas de connexion réseau", "Vérifiez votre connexion WiFi/mobile");
        }

        PingResult lastResult = null;

        // Tentatives avec retry
        for (int attempt = 1; attempt <= retries; attempt++) {
            Log.d(TAG, "Tentative " + attempt + "/" + retries);
            lastResult = performSinglePing(serverUrl, timeout, ignoreSsl);

            // Si succès, retourner immédiatement
            if (lastResult.isOnline()) {
                Log.d(TAG, "✅ Ping réussi: " + lastResult.responseTime + "ms");
                return lastResult;
            }

            // Attendre un peu avant de retry (sauf dernière tentative)
            if (attempt < retries) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrompu", e);
                }
            }
        }

        Log.d(TAG, "❌ Échec après " + retries + " tentatives");
        return lastResult != null ? lastResult : new PingResult(ServerStatus.ERROR, 0, "Échec du ping", "Impossible de contacter le serveur");
    }

    private static PingResult performSinglePing(String serverUrl, int timeout, boolean ignoreSsl) {
        long startTime = System.currentTimeMillis();

        try {
            // Désactiver SSL si nécessaire
            if (ignoreSsl && serverUrl.startsWith("https")) {
                disableSslVerification();
            }

            // Construire l'URL du healthcheck
            String healthCheckUrl = serverUrl;
            if (!healthCheckUrl.endsWith("/")) {
                healthCheckUrl += "/";
            }
            // Utiliser un endpoint léger pour le ping
            healthCheckUrl += "api/health.php";

            Log.d(TAG, "Ping vers: " + healthCheckUrl);

            URL url = new URL(healthCheckUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setInstanceFollowRedirects(true);

            // Ajouter des headers pour identifier la requête
            connection.setRequestProperty("User-Agent", "PTMSMobile/1.0");
            connection.setRequestProperty("X-Health-Check", "true");

            int responseCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;

            connection.disconnect();

            Log.d(TAG, "Réponse: " + responseCode + " en " + responseTime + "ms");

            // Analyser la réponse
            if (responseCode >= 200 && responseCode < 300) {
                // Succès
                if (responseTime < 2000) {
                    return new PingResult(ServerStatus.ONLINE, responseTime,
                        "Serveur accessible", "Temps de réponse: " + responseTime + "ms");
                } else {
                    return new PingResult(ServerStatus.SLOW, responseTime,
                        "Serveur lent", "Temps de réponse: " + responseTime + "ms");
                }
            } else if (responseCode == 404) {
                // 404 signifie que le serveur répond mais l'endpoint n'existe pas
                // C'est toujours un bon signe pour la connectivité
                return new PingResult(ServerStatus.ONLINE, responseTime,
                    "Serveur accessible (404)", "Endpoint de health check manquant mais serveur joignable");
            } else if (responseCode >= 500) {
                return new PingResult(ServerStatus.ERROR, responseTime,
                    "Erreur serveur (" + responseCode + ")", "Le serveur a retourné une erreur");
            } else {
                return new PingResult(ServerStatus.ERROR, responseTime,
                    "Réponse inattendue (" + responseCode + ")", "Code HTTP: " + responseCode);
            }

        } catch (java.net.SocketTimeoutException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            Log.d(TAG, "Timeout après " + elapsed + "ms");
            return new PingResult(ServerStatus.OFFLINE, elapsed,
                "Timeout - Serveur trop lent", "Le serveur n'a pas répondu dans le délai imparti");

        } catch (java.net.UnknownHostException e) {
            Log.d(TAG, "Host inconnu: " + e.getMessage());
            return new PingResult(ServerStatus.OFFLINE, 0,
                "Serveur introuvable", "Vérifiez l'URL du serveur: " + serverUrl);

        } catch (java.net.ConnectException e) {
            Log.d(TAG, "Connexion refusée: " + e.getMessage());
            return new PingResult(ServerStatus.OFFLINE, 0,
                "Connexion refusée", "Le serveur refuse la connexion");

        } catch (javax.net.ssl.SSLException e) {
            Log.d(TAG, "Erreur SSL: " + e.getMessage());
            return new PingResult(ServerStatus.ERROR, 0,
                "Erreur SSL/Certificat", "Activez 'Ignorer SSL' dans les paramètres");

        } catch (Exception e) {
            Log.e(TAG, "Erreur ping", e);
            return new PingResult(ServerStatus.ERROR, 0,
                "Erreur: " + e.getClass().getSimpleName(), e.getMessage());
        }
    }

    // ==================== MONITORING CONTINU ====================

    /**
     * Démarre le monitoring continu du serveur
     * Vérifie l'état toutes les 30 secondes
     */
    public static void startMonitoring(Context context, StatusChangeListener listener) {
        if (isMonitoring.get()) {
            Log.d(TAG, "Monitoring déjà actif");
            return;
        }

        Log.d(TAG, "Démarrage du monitoring continu");
        isMonitoring.set(true);
        globalListener = listener;

        // Lancer le monitoring
        scheduleNextCheck(context);
    }

    /**
     * Arrête le monitoring continu
     */
    public static void stopMonitoring() {
        Log.d(TAG, "Arrêt du monitoring");
        isMonitoring.set(false);
        globalListener = null;
        monitoringHandler.removeCallbacksAndMessages(null);
    }

    private static void scheduleNextCheck(Context context) {
        if (!isMonitoring.get()) {
            return;
        }

        monitoringHandler.postDelayed(() -> {
            // Faire un ping rapide
            quickPing(context, (status, responseTime, message) -> {
                Log.d(TAG, "Check périodique: " + status + " (" + responseTime + "ms)");

                // Notifier si changement d'état
                if (status != lastKnownStatus && globalListener != null) {
                    ServerStatus oldStatus = lastKnownStatus;
                    globalListener.onStatusChanged(oldStatus, status, message);
                }

                // Planifier le prochain check
                scheduleNextCheck(context);
            });
        }, 30000); // Toutes les 30 secondes
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtient le dernier état connu du serveur
     */
    public static ServerStatus getLastKnownStatus() {
        return lastKnownStatus;
    }

    /**
     * Obtient le temps écoulé depuis le dernier check
     */
    public static long getTimeSinceLastCheck() {
        return System.currentTimeMillis() - lastCheckTime;
    }

    /**
     * Réinitialise le cache
     */
    public static void clearCache() {
        lastKnownStatus = ServerStatus.UNKNOWN;
        lastCheckTime = 0;
    }

    /**
     * Formatte le temps de réponse
     */
    public static String formatResponseTime(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else {
            return String.format("%.1f s", ms / 1000.0);
        }
    }

    /**
     * Obtient un message descriptif pour un status
     */
    public static String getStatusMessage(ServerStatus status) {
        switch (status) {
            case ONLINE:
                return "✅ Serveur accessible";
            case OFFLINE:
                return "❌ Serveur hors ligne";
            case SLOW:
                return "⚠️ Serveur lent";
            case ERROR:
                return "⚠️ Erreur de connexion";
            case UNKNOWN:
            default:
                return "❔ État inconnu";
        }
    }

    /**
     * Obtient une couleur pour un status (code couleur Android)
     */
    public static int getStatusColor(ServerStatus status) {
        switch (status) {
            case ONLINE:
                return 0xFF4CAF50; // Vert
            case OFFLINE:
                return 0xFFF44336; // Rouge
            case SLOW:
                return 0xFFFF9800; // Orange
            case ERROR:
                return 0xFFFF9800; // Orange
            case UNKNOWN:
            default:
                return 0xFF9E9E9E; // Gris
        }
    }

    /**
     * Désactive la vérification SSL (à utiliser uniquement en développement)
     */
    private static void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la désactivation SSL", e);
        }
    }
}
