package com.ptms.mobile.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ptms.mobile.R;
import com.ptms.mobile.activities.SplashActivity;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.sync.BidirectionalSyncManager;
import com.ptms.mobile.utils.NetworkUtils;

/**
 * ✅ AMÉLIORÉ: Service de synchronisation automatique bidirectionnelle en arrière-plan
 *
 * - Auto-sync toutes les 5 minutes si online
 * - Utilise BidirectionalSyncManager pour sync complète
 * - Notifications de progression
 *
 * @version 2.0
 * @date 2025-10-19
 */
public class AutoSyncService extends Service {

    private static final String TAG = "AutoSyncService";
    private static final String CHANNEL_ID = "PTMS_SYNC_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SYNC_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

    private final IBinder binder = new AutoSyncBinder();
    private BidirectionalSyncManager bidirectionalSyncManager; // ✅ NOUVEAU
    private OfflineDatabaseHelper dbHelper;
    private ApiService apiService;
    private SharedPreferences prefs;
    private boolean isRunning = false;
    private Thread syncThread;
    
    public class AutoSyncBinder extends Binder {
        public AutoSyncService getService() {
            return AutoSyncService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ Service de synchronisation automatique bidirectionnelle créé");

        bidirectionalSyncManager = new BidirectionalSyncManager(this); // ✅ NOUVEAU
        dbHelper = new OfflineDatabaseHelper(this);
        apiService = ApiClient.getInstance(this).getApiService();
        prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service de synchronisation automatique démarré");
        
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createNotification("Synchronisation automatique active"));
            startAutoSync();
        }
        
        return START_STICKY; // Redémarrer le service s'il est tué
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service de synchronisation automatique arrêté");
        stopAutoSync();
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    private void startAutoSync() {
        if (isRunning) {
            Log.d(TAG, "Synchronisation automatique déjà en cours");
            return;
        }
        
        isRunning = true;
        
        syncThread = new Thread(() -> {
            Log.d(TAG, "Thread de synchronisation automatique démarré");

            while (isRunning) {
                try {
                    // ✅ NOUVEAU: Utiliser BidirectionalSyncManager pour sync complète
                    int pendingCount = dbHelper.getPendingSyncCount();
                    boolean isOnline = NetworkUtils.isOnline(AutoSyncService.this);

                    if (isOnline) {
                        Log.d(TAG, "🔄 Synchronisation bidirectionnelle automatique: " + pendingCount + " éléments locaux en attente");

                        updateNotification("Synchronisation en cours...");

                        // ✅ SYNCHRONISATION BIDIRECTIONNELLE COMPLÈTE
                        bidirectionalSyncManager.syncFull(new BidirectionalSyncManager.SyncCallback() {
                            @Override
                            public void onSyncStarted(String phase) {
                                Log.d(TAG, "🔄 Phase: " + phase);
                                updateNotification(phase);
                            }

                            @Override
                            public void onSyncProgress(String message, int current, int total) {
                                Log.d(TAG, "📊 Progression: " + message + " (" + current + "/" + total + ")");
                                String progressText = message;
                                if (total > 0) {
                                    progressText += " (" + current + "/" + total + ")";
                                }
                                updateNotification(progressText);
                            }

                            @Override
                            public void onSyncCompleted(BidirectionalSyncManager.SyncResult result) {
                                Log.d(TAG, "✅ Synchronisation terminée: " + result.getSummary());

                                // Notification de succès
                                updateNotification("Synchronisation réussie");

                                // Notification détaillée
                                if (result.uploadedCount > 0 || result.downloadedCount > 0) {
                                    sendSyncNotification(result.uploadedCount, result.downloadedCount, result.failedCount);
                                }
                            }

                            @Override
                            public void onSyncError(String error) {
                                Log.e(TAG, "❌ Erreur synchronisation: " + error);
                                updateNotification("Erreur: " + error);
                            }
                        });
                    } else if (pendingCount > 0) {
                        Log.d(TAG, "📵 Pas de connexion - Synchronisation reportée (" + pendingCount + " éléments en attente)");
                        updateNotification("Hors ligne (" + pendingCount + " éléments en attente)");
                    } else {
                        updateNotification("Synchronisation automatique active");
                    }
                    
                    // Attendre avant la prochaine vérification
                    Thread.sleep(SYNC_INTERVAL_MS);
                    
                } catch (InterruptedException e) {
                    Log.d(TAG, "Thread de synchronisation automatique interrompu");
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Erreur dans le thread de synchronisation automatique", e);
                    try {
                        Thread.sleep(SYNC_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
            
            Log.d(TAG, "Thread de synchronisation automatique terminé");
        });
        
        syncThread.start();
    }
    
    private void stopAutoSync() {
        isRunning = false;
        
        if (syncThread != null && syncThread.isAlive()) {
            syncThread.interrupt();
            try {
                syncThread.join(1000); // Attendre 1 seconde max
            } catch (InterruptedException e) {
                Log.e(TAG, "Erreur arrêt du thread de synchronisation", e);
            }
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Synchronisation PTMS",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications de synchronisation automatique");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification(String message) {
        Intent notificationIntent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PTMS - Synchronisation")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
    
    private void updateNotification(String message) {
        Notification notification = createNotification(message);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    /**
     * ✅ MODIFIÉ: Notification bidirectionnelle (uploaded + downloaded)
     */
    private void sendSyncNotification(int uploadedCount, int downloadedCount, int failedCount) {
        // Ne pas spammer l'utilisateur avec des notifications
        SharedPreferences prefs = getSharedPreferences("sync_notifications", MODE_PRIVATE);
        long lastNotification = prefs.getLong("last_sync_notification", 0);
        long currentTime = System.currentTimeMillis();

        // Envoyer une notification seulement si la dernière date de plus de 30 minutes
        if (currentTime - lastNotification < 30 * 60 * 1000) {
            return;
        }

        String title = "Synchronisation PTMS";
        String message;

        int totalSuccess = uploadedCount + downloadedCount;

        if (totalSuccess > 0 && failedCount == 0) {
            message = String.format("📤 %d envoyées, 📥 %d reçues", uploadedCount, downloadedCount);
        } else if (totalSuccess > 0 && failedCount > 0) {
            message = String.format("📤 %d envoyées, 📥 %d reçues, ❌ %d échecs", uploadedCount, downloadedCount, failedCount);
        } else if (failedCount > 0) {
            message = failedCount + " échec(s) de synchronisation";
        } else {
            return; // Rien à notifier
        }
        
        Intent intent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build();
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID + 1, notification);
        
        // Enregistrer l'heure de la dernière notification
        prefs.edit().putLong("last_sync_notification", currentTime).apply();
    }
    
    // ==================== MÉTHODES PUBLIQUES ====================
    
    public boolean isAutoSyncRunning() {
        return isRunning;
    }
    
    public int getPendingSyncCount() {
        return bidirectionalSyncManager != null ? bidirectionalSyncManager.getPendingSyncCount() : 0;
    }

    public void forceSync() {
        if (bidirectionalSyncManager != null && apiService != null) {
            Log.d(TAG, "Synchronisation forcée demandée");

            String authToken = prefs.getString("auth_token", "");
            if (authToken != null && !authToken.isEmpty()) {
                bidirectionalSyncManager.syncFull(new BidirectionalSyncManager.SyncCallback() {
                    @Override
                    public void onSyncStarted(String phase) {
                        Log.d(TAG, "Phase de synchronisation: " + phase);
                        updateNotification("Synchronisation: " + phase);
                    }

                    @Override
                    public void onSyncProgress(String message, int current, int total) {
                        Log.d(TAG, "Progrès: " + message);
                    }

                    @Override
                    public void onSyncCompleted(BidirectionalSyncManager.SyncResult result) {
                        Log.d(TAG, "Synchronisation forcée terminée: " + result.getSummary());
                        updateNotification("Synchronisation forcée réussie");
                    }

                    @Override
                    public void onSyncError(String error) {
                        Log.e(TAG, "Erreur synchronisation forcée: " + error);
                        updateNotification("Erreur synchronisation forcée");
                    }
                });
            } else {
                Log.w(TAG, "Pas de token d'authentification pour synchronisation forcée");
                updateNotification("Authentification requise");
            }
        }
    }
    
    public static void startService(Context context) {
        Intent intent = new Intent(context, AutoSyncService.class);
        context.startForegroundService(intent);
    }
    
    public static void stopService(Context context) {
        Intent intent = new Intent(context, AutoSyncService.class);
        context.stopService(intent);
    }
}
