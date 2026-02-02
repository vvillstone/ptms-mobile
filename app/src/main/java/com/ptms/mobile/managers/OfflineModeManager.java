package com.ptms.mobile.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ptms.mobile.sync.BidirectionalSyncManager;
import com.ptms.mobile.utils.NetworkUtils;
import com.ptms.mobile.utils.ServerHealthCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestionnaire du mode offline avec détection automatique
 * Gère le passage online/offline et la synchronisation intelligente
 *
 * ✅ CORRECTION 2025-01-19: Utilise BidirectionalSyncManager au lieu de OfflineSyncManager
 */
public class OfflineModeManager {

    private static final String TAG = "OfflineModeManager";
    private static final String PREFS_NAME = "offline_mode_prefs";
    private static final String KEY_MODE = "current_mode";
    private static final String KEY_LAST_ONLINE = "last_online_time";

    // Singleton
    private static OfflineModeManager instance;

    private Context context;
    private SharedPreferences prefs;
    private BidirectionalSyncManager syncManager;
    private Handler handler;

    // État actuel
    private ConnectionMode currentMode = ConnectionMode.UNKNOWN;
    private AtomicBoolean isSyncing = new AtomicBoolean(false);
    private AtomicBoolean isChecking = new AtomicBoolean(false);

    // Listeners
    private List<ModeChangeListener> listeners = new ArrayList<>();

    /**
     * Modes de connexion
     */
    public enum ConnectionMode {
        ONLINE,      // Connexion active au serveur
        OFFLINE,     // Mode hors ligne (pas de connexion)
        SYNCING,     // En cours de synchronisation
        UNKNOWN      // État inconnu
    }

    /**
     * Interface pour écouter les changements de mode
     */
    public interface ModeChangeListener {
        void onModeChanged(ConnectionMode oldMode, ConnectionMode newMode, String reason);
        void onSyncStarted();
        void onSyncProgress(String message);
        void onSyncCompleted(int syncedCount, int failedCount);
        void onSyncError(String error);
    }

    /**
     * Callback simplifié pour les checks
     */
    public interface ConnectionCheckCallback {
        void onResult(boolean isOnline, String message);
    }

    // ==================== SINGLETON ====================

    public static synchronized OfflineModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineModeManager(context.getApplicationContext());
        }
        return instance;
    }

    private OfflineModeManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.syncManager = new BidirectionalSyncManager(context);  // ✅ CORRECTION: Utiliser BidirectionalSyncManager
        this.handler = new Handler(Looper.getMainLooper());

        // Restaurer le mode sauvegardé
        String savedMode = prefs.getString(KEY_MODE, ConnectionMode.UNKNOWN.name());
        try {
            this.currentMode = ConnectionMode.valueOf(savedMode);
        } catch (Exception e) {
            this.currentMode = ConnectionMode.UNKNOWN;
        }

        Log.d(TAG, "OfflineModeManager initialisé - Mode: " + currentMode);
    }

    // ==================== GESTION DU MODE ====================

    /**
     * Obtient le mode de connexion actuel
     */
    public ConnectionMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Vérifie si on est en ligne
     */
    public boolean isOnline() {
        return currentMode == ConnectionMode.ONLINE || currentMode == ConnectionMode.SYNCING;
    }

    /**
     * Vérifie si on est hors ligne
     */
    public boolean isOffline() {
        return currentMode == ConnectionMode.OFFLINE;
    }

    /**
     * Vérifie si une synchronisation est en cours
     */
    public boolean isSyncing() {
        return isSyncing.get() || currentMode == ConnectionMode.SYNCING;
    }

    /**
     * Change le mode et notifie les listeners
     */
    private void changeMode(ConnectionMode newMode, String reason) {
        if (currentMode == newMode) {
            return;
        }

        Log.d(TAG, "Changement de mode: " + currentMode + " → " + newMode + " (" + reason + ")");

        ConnectionMode oldMode = currentMode;
        currentMode = newMode;

        // Sauvegarder
        prefs.edit()
            .putString(KEY_MODE, newMode.name())
            .apply();

        // Si passage en ligne, mettre à jour le timestamp
        if (newMode == ConnectionMode.ONLINE) {
            prefs.edit()
                .putLong(KEY_LAST_ONLINE, System.currentTimeMillis())
                .apply();
        }

        // Notifier les listeners
        notifyModeChanged(oldMode, newMode, reason);
    }

    // ==================== DÉTECTION AUTOMATIQUE ====================

    /**
     * Détecte automatiquement le mode de connexion
     * - Ping au serveur PTMS
     * - Si pas de réponse → mode offline
     * - Si réponse → mode online + synchronisation
     */
    public void detectConnectionMode(ConnectionCheckCallback callback) {
        if (isChecking.get()) {
            Log.d(TAG, "Détection déjà en cours");
            if (callback != null) {
                callback.onResult(isOnline(), "Détection en cours");
            }
            return;
        }

        isChecking.set(true);
        Log.d(TAG, "Détection du mode de connexion...");

        // Vérifier d'abord la connectivité réseau basique
        if (!NetworkUtils.isOnline(context)) {
            isChecking.set(false);
            changeMode(ConnectionMode.OFFLINE, "Pas de connexion réseau");
            if (callback != null) {
                callback.onResult(false, "Pas de connexion réseau");
            }
            return;
        }

        // Ping rapide au serveur PTMS
        ServerHealthCheck.quickPing(context, (status, responseTime, message) -> {
            isChecking.set(false);

            boolean online = (status == ServerHealthCheck.ServerStatus.ONLINE ||
                            status == ServerHealthCheck.ServerStatus.SLOW);

            if (online) {
                Log.d(TAG, "✅ Serveur accessible (" + responseTime + "ms)");
                changeMode(ConnectionMode.ONLINE, "Serveur accessible");

                // Lancer la synchronisation automatique
                autoSync();

                if (callback != null) {
                    callback.onResult(true, "Serveur accessible (" + responseTime + "ms)");
                }
            } else {
                Log.d(TAG, "❌ Serveur inaccessible: " + message);
                changeMode(ConnectionMode.OFFLINE, "Serveur inaccessible");

                if (callback != null) {
                    callback.onResult(false, message);
                }
            }
        });
    }

    /**
     * Retry manuel de la détection
     * Utilisé quand l'utilisateur clique sur "Réessayer"
     */
    public void retryConnection(ConnectionCheckCallback callback) {
        Log.d(TAG, "Retry manuel de connexion demandé");

        // Réinitialiser le cache du health check
        ServerHealthCheck.clearCache();

        // Forcer une nouvelle détection
        detectConnectionMode(callback);
    }

    // ==================== SYNCHRONISATION AUTOMATIQUE ====================

    /**
     * Synchronisation automatique (upload + download)
     * - Upload: Envoyer les données en attente au serveur
     * - Download: Télécharger les dernières données depuis le serveur
     */
    private void autoSync() {
        if (isSyncing.get()) {
            Log.d(TAG, "Synchronisation déjà en cours");
            return;
        }

        if (!isOnline()) {
            Log.d(TAG, "Pas en ligne - Synchronisation annulée");
            return;
        }

        Log.d(TAG, "Démarrage synchronisation automatique");
        isSyncing.set(true);
        changeMode(ConnectionMode.SYNCING, "Synchronisation en cours");
        notifySyncStarted();

        // ✅ CORRECTION: Utiliser syncFull() de BidirectionalSyncManager au lieu de syncPendingData()
        syncManager.syncFull(new BidirectionalSyncManager.SyncCallback() {
            @Override
            public void onSyncStarted(String phase) {
                Log.d(TAG, "Sync: Démarrage - " + phase);
                handler.post(() -> notifySyncProgress("Démarrage de la synchronisation..."));
            }

            @Override
            public void onSyncProgress(String message, int current, int total) {
                Log.d(TAG, "Sync: " + message + " (" + current + "/" + total + ")");
                handler.post(() -> notifySyncProgress(message));
            }

            @Override
            public void onSyncCompleted(BidirectionalSyncManager.SyncResult result) {
                Log.d(TAG, "Sync: Terminée - " + result.getSummary());
                isSyncing.set(false);

                handler.post(() -> {
                    changeMode(ConnectionMode.ONLINE, "Synchronisation terminée");
                    notifySyncCompleted(result.uploadedCount + result.downloadedCount, result.failedCount);
                });
            }

            @Override
            public void onSyncError(String error) {
                Log.e(TAG, "Sync: Erreur - " + error);
                isSyncing.set(false);

                handler.post(() -> {
                    changeMode(ConnectionMode.OFFLINE, "Erreur de synchronisation");
                    notifySyncError(error);
                });
            }
        });
    }

    /**
     * Synchronisation manuelle déclenchée par l'utilisateur
     */
    public void manualSync(BidirectionalSyncManager.SyncCallback callback) {
        Log.d(TAG, "Synchronisation manuelle demandée");

        if (!isOnline()) {
            Log.d(TAG, "Pas en ligne - Vérification de la connexion d'abord");

            // Vérifier la connexion d'abord
            detectConnectionMode((online, message) -> {
                if (online) {
                    // Connexion OK, lancer la sync
                    autoSync();
                } else {
                    // Toujours offline
                    if (callback != null) {
                        callback.onSyncError("Impossible de se connecter au serveur");
                    }
                }
            });
            return;
        }

        // Déjà online, lancer directement
        autoSync();
    }

    // ==================== MONITORING CONTINU ====================

    /**
     * Démarre le monitoring continu de la connexion
     * Vérifie toutes les 30 secondes
     */
    public void startMonitoring() {
        Log.d(TAG, "Démarrage du monitoring continu");

        ServerHealthCheck.startMonitoring(context, (oldStatus, newStatus, message) -> {
            Log.d(TAG, "Changement d'état détecté: " + oldStatus + " → " + newStatus);

            boolean wasOnline = (oldStatus == ServerHealthCheck.ServerStatus.ONLINE ||
                                oldStatus == ServerHealthCheck.ServerStatus.SLOW);
            boolean isNowOnline = (newStatus == ServerHealthCheck.ServerStatus.ONLINE ||
                                  newStatus == ServerHealthCheck.ServerStatus.SLOW);

            if (!wasOnline && isNowOnline) {
                // Passage offline → online
                Log.d(TAG, "🔄 Reconnexion détectée!");
                changeMode(ConnectionMode.ONLINE, "Reconnexion détectée");
                autoSync();
            } else if (wasOnline && !isNowOnline) {
                // Passage online → offline
                Log.d(TAG, "⚠️ Perte de connexion détectée!");
                changeMode(ConnectionMode.OFFLINE, "Perte de connexion");
            }
        });
    }

    /**
     * Arrête le monitoring
     */
    public void stopMonitoring() {
        Log.d(TAG, "Arrêt du monitoring");
        ServerHealthCheck.stopMonitoring();
    }

    // ==================== GESTION DES LISTENERS ====================

    public void addListener(ModeChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ModeChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyModeChanged(ConnectionMode oldMode, ConnectionMode newMode, String reason) {
        for (ModeChangeListener listener : listeners) {
            try {
                listener.onModeChanged(oldMode, newMode, reason);
            } catch (Exception e) {
                Log.e(TAG, "Erreur notification listener", e);
            }
        }
    }

    private void notifySyncStarted() {
        for (ModeChangeListener listener : listeners) {
            try {
                listener.onSyncStarted();
            } catch (Exception e) {
                Log.e(TAG, "Erreur notification listener", e);
            }
        }
    }

    private void notifySyncProgress(String message) {
        for (ModeChangeListener listener : listeners) {
            try {
                listener.onSyncProgress(message);
            } catch (Exception e) {
                Log.e(TAG, "Erreur notification listener", e);
            }
        }
    }

    private void notifySyncCompleted(int syncedCount, int failedCount) {
        for (ModeChangeListener listener : listeners) {
            try {
                listener.onSyncCompleted(syncedCount, failedCount);
            } catch (Exception e) {
                Log.e(TAG, "Erreur notification listener", e);
            }
        }
    }

    private void notifySyncError(String error) {
        for (ModeChangeListener listener : listeners) {
            try {
                listener.onSyncError(error);
            } catch (Exception e) {
                Log.e(TAG, "Erreur notification listener", e);
            }
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtient le nombre de données en attente de synchronisation
     */
    public int getPendingSyncCount() {
        return syncManager.getPendingSyncCount();
    }

    /**
     * Obtient le temps écoulé depuis la dernière connexion
     */
    public long getTimeSinceLastOnline() {
        long lastOnline = prefs.getLong(KEY_LAST_ONLINE, 0);
        if (lastOnline == 0) {
            return -1;
        }
        return System.currentTimeMillis() - lastOnline;
    }

    /**
     * Formatte le temps depuis la dernière connexion
     */
    public String getFormattedTimeSinceLastOnline() {
        long time = getTimeSinceLastOnline();
        if (time < 0) {
            return "Jamais";
        }

        long minutes = time / (1000 * 60);
        long hours = time / (1000 * 60 * 60);
        long days = time / (1000 * 60 * 60 * 24);

        if (minutes < 1) {
            return "À l'instant";
        } else if (minutes < 60) {
            return "Il y a " + minutes + " min";
        } else if (hours < 24) {
            return "Il y a " + hours + "h";
        } else {
            return "Il y a " + days + " jour" + (days > 1 ? "s" : "");
        }
    }

    /**
     * Obtient un message descriptif pour le mode actuel
     */
    public String getModeMessage() {
        switch (currentMode) {
            case ONLINE:
                return "✅ Connecté au serveur";
            case OFFLINE:
                int pending = getPendingSyncCount();
                if (pending > 0) {
                    return "❌ Hors ligne (" + pending + " en attente)";
                } else {
                    return "❌ Hors ligne";
                }
            case SYNCING:
                return "🔄 Synchronisation...";
            case UNKNOWN:
            default:
                return "❔ État inconnu";
        }
    }

    /**
     * Obtient une couleur pour le mode actuel
     */
    public int getModeColor() {
        switch (currentMode) {
            case ONLINE:
                return 0xFF4CAF50; // Vert
            case OFFLINE:
                return 0xFFF44336; // Rouge
            case SYNCING:
                return 0xFF2196F3; // Bleu
            case UNKNOWN:
            default:
                return 0xFF9E9E9E; // Gris
        }
    }

    /**
     * Force le passage en mode offline (pour tests)
     */
    public void forceOfflineMode() {
        changeMode(ConnectionMode.OFFLINE, "Mode offline forcé");
    }

    /**
     * Force le passage en mode online (pour tests)
     */
    public void forceOnlineMode() {
        changeMode(ConnectionMode.ONLINE, "Mode online forcé");
    }
}
