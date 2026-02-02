package com.ptms.mobile.sync;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Gestionnaire d'état global de synchronisation
 * Permet de notifier toutes les activités de l'état de la synchronisation
 */
public class SyncStateManager {

    private static final String TAG = "SyncStateManager";

    // Actions de broadcast
    public static final String ACTION_SYNC_STATE_CHANGED = "com.ptms.mobile.SYNC_STATE_CHANGED";
    public static final String EXTRA_IS_SYNCING = "is_syncing";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_TOTAL = "total";

    private static SyncStateManager instance;
    private Context context;
    private boolean isSyncing = false;
    private int currentProgress = 0;
    private int totalItems = 0;

    private SyncStateManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized SyncStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncStateManager(context);
        }
        return instance;
    }

    /**
     * Démarrer la synchronisation
     */
    public void startSync(int totalItems) {
        this.isSyncing = true;
        this.currentProgress = 0;
        this.totalItems = totalItems;
        broadcastSyncState();
    }

    /**
     * Mettre à jour la progression
     */
    public void updateProgress(int progress) {
        this.currentProgress = progress;
        if (isSyncing) {
            broadcastSyncState();
        }
    }

    /**
     * Terminer la synchronisation
     */
    public void endSync() {
        this.isSyncing = false;
        this.currentProgress = 0;
        this.totalItems = 0;
        broadcastSyncState();
    }

    /**
     * Vérifier si une synchronisation est en cours
     */
    public boolean isSyncing() {
        return isSyncing;
    }

    /**
     * Obtenir la progression actuelle
     */
    public int getCurrentProgress() {
        return currentProgress;
    }

    /**
     * Obtenir le nombre total d'items
     */
    public int getTotalItems() {
        return totalItems;
    }

    /**
     * Diffuser l'état de synchronisation à toutes les activités
     */
    private void broadcastSyncState() {
        Intent intent = new Intent(ACTION_SYNC_STATE_CHANGED);
        intent.putExtra(EXTRA_IS_SYNCING, isSyncing);
        intent.putExtra(EXTRA_PROGRESS, currentProgress);
        intent.putExtra(EXTRA_TOTAL, totalItems);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
