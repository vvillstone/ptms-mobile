package com.ptms.mobile.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ptms.mobile.R;
import com.ptms.mobile.activities.HomeActivity;

/**
 * Service pour gérer le timer de travail en arrière-plan
 * Affiche une notification permanente avec le temps écoulé
 */
public class TimerService extends Service {

    private static final String TAG = "TimerService";
    private static final String CHANNEL_ID = "timer_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Actions
    public static final String ACTION_START = "com.ptms.mobile.ACTION_START_TIMER";
    public static final String ACTION_PAUSE = "com.ptms.mobile.ACTION_PAUSE_TIMER";
    public static final String ACTION_RESUME = "com.ptms.mobile.ACTION_RESUME_TIMER";
    public static final String ACTION_STOP = "com.ptms.mobile.ACTION_STOP_TIMER";
    public static final String ACTION_UPDATE = "com.ptms.mobile.ACTION_UPDATE_TIMER";

    // Broadcast actions
    public static final String BROADCAST_TIMER_UPDATE = "com.ptms.mobile.TIMER_UPDATE";
    public static final String ACTION_TIMER_UPDATE = "com.ptms.mobile.TIMER_UPDATE";
    public static final String EXTRA_ELAPSED_SECONDS = "elapsed_seconds";
    public static final String EXTRA_IS_RUNNING = "is_running";
    public static final String EXTRA_PROJECT_NAME = "project_name";

    // États du timer
    public static final int STATE_STOPPED = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_PAUSED = 2;

    // État du timer
    private SharedPreferences prefs;
    private Handler handler;
    private Runnable timerRunnable;

    private boolean isRunning = false;
    private boolean isPaused = false;
    private long startTime = 0;
    private long elapsedSeconds = 0;
    private long pausedDuration = 0;
    private long pauseStartTime = 0;

    private int projectId = 0;
    private String projectName = "";
    private int timerId = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service créé");

        prefs = getSharedPreferences("timer_prefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());

        // Créer le canal de notification
        createNotificationChannel();

        // Restaurer l'état si le service a été tué
        restoreTimerState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "Action reçue: " + action);

            switch (action) {
                case ACTION_START:
                    timerId = intent.getIntExtra("timer_id", 0);
                    projectId = intent.getIntExtra("project_id", 0);
                    projectName = intent.getStringExtra("project_name");
                    startTimer();
                    break;

                case ACTION_PAUSE:
                    pauseTimer();
                    break;

                case ACTION_RESUME:
                    resumeTimer();
                    break;

                case ACTION_STOP:
                    stopTimer();
                    break;

                case ACTION_UPDATE:
                    updateNotification();
                    break;
            }
        }

        return START_STICKY; // Redémarrer si tué par le système
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service non lié
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service détruit");

        // Sauvegarder l'état
        saveTimerState();

        // Arrêter le handler
        if (handler != null && timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }

    /**
     * Démarrer le timer
     */
    private void startTimer() {
        if (isRunning) {
            Log.w(TAG, "Timer déjà en cours");
            return;
        }

        Log.d(TAG, "Démarrage du timer pour projet: " + projectName);

        isRunning = true;
        isPaused = false;
        startTime = System.currentTimeMillis();
        elapsedSeconds = 0;
        pausedDuration = 0;

        // Démarrer la notification en premier plan
        startForeground(NOTIFICATION_ID, buildNotification());

        // Démarrer le compteur
        startTimerCounter();

        // Sauvegarder l'état
        saveTimerState();

        // Broadcast
        broadcastTimerUpdate();
    }

    /**
     * Mettre en pause le timer
     */
    private void pauseTimer() {
        if (!isRunning || isPaused) {
            Log.w(TAG, "Timer non actif ou déjà en pause");
            return;
        }

        Log.d(TAG, "Mise en pause du timer");

        isPaused = true;
        pauseStartTime = System.currentTimeMillis();

        // Arrêter le compteur
        if (handler != null && timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }

        // Mettre à jour la notification
        updateNotification();

        // Sauvegarder l'état
        saveTimerState();

        // Broadcast
        broadcastTimerUpdate();
    }

    /**
     * Reprendre le timer
     */
    private void resumeTimer() {
        if (!isRunning || !isPaused) {
            Log.w(TAG, "Timer non en pause");
            return;
        }

        Log.d(TAG, "Reprise du timer");

        // Ajouter le temps de pause au total
        long pauseDuration = System.currentTimeMillis() - pauseStartTime;
        pausedDuration += pauseDuration;

        isPaused = false;

        // Redémarrer le compteur
        startTimerCounter();

        // Mettre à jour la notification
        updateNotification();

        // Sauvegarder l'état
        saveTimerState();

        // Broadcast
        broadcastTimerUpdate();
    }

    /**
     * Arrêter le timer
     */
    private void stopTimer() {
        Log.d(TAG, "Arrêt du timer");

        isRunning = false;
        isPaused = false;

        // Arrêter le compteur
        if (handler != null && timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }

        // Nettoyer l'état
        clearTimerState();

        // Arrêter le service
        stopForeground(true);
        stopSelf();

        // Broadcast
        broadcastTimerUpdate();
    }

    /**
     * Démarrer le compteur
     */
    private void startTimerCounter() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && !isPaused) {
                    // Calculer le temps écoulé
                    long currentTime = System.currentTimeMillis();
                    long totalElapsed = currentTime - startTime;
                    elapsedSeconds = (totalElapsed - pausedDuration) / 1000;

                    // Mettre à jour la notification toutes les secondes
                    updateNotification();

                    // Broadcast toutes les secondes
                    broadcastTimerUpdate();

                    // Sauvegarder toutes les 10 secondes
                    if (elapsedSeconds % 10 == 0) {
                        saveTimerState();
                    }

                    // Répéter toutes les 1 seconde
                    handler.postDelayed(this, 1000);
                }
            }
        };

        handler.post(timerRunnable);
    }

    /**
     * Créer le canal de notification (Android 8+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Timer de Travail",
                    NotificationManager.IMPORTANCE_LOW // Pas de son
            );
            channel.setDescription("Affiche le timer de travail en cours");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Construire la notification
     */
    private Notification buildNotification() {
        // Intent pour ouvrir l'app
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Actions de la notification
        Intent pauseIntent = new Intent(this, TimerService.class);
        pauseIntent.setAction(isPaused ? ACTION_RESUME : ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, TimerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Formater le temps
        String timeString = formatElapsedTime(elapsedSeconds);

        // Titre et texte
        String title = isPaused ? "⏸️ Timer en pause" : "⏱️ Timer en cours";
        String text = projectName + " • " + timeString;

        // Construire la notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer) // TODO: Créer cette icône
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Ne peut pas être balayée
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setShowWhen(false);

        // Ajouter les actions
        if (!isPaused) {
            builder.addAction(R.drawable.ic_pause, "Pause", pausePendingIntent);
        } else {
            builder.addAction(R.drawable.ic_play, "Reprendre", pausePendingIntent);
        }
        builder.addAction(R.drawable.ic_stop, "Arrêter", stopPendingIntent);

        return builder.build();
    }

    /**
     * Mettre à jour la notification
     */
    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && isRunning) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    /**
     * Formater le temps écoulé
     */
    private String formatElapsedTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Sauvegarder l'état du timer
     */
    private void saveTimerState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_running", isRunning);
        editor.putBoolean("is_paused", isPaused);
        editor.putLong("start_time", startTime);
        editor.putLong("elapsed_seconds", elapsedSeconds);
        editor.putLong("paused_duration", pausedDuration);
        editor.putLong("pause_start_time", pauseStartTime);
        editor.putInt("project_id", projectId);
        editor.putString("project_name", projectName);
        editor.putInt("timer_id", timerId);
        editor.apply();

        Log.d(TAG, "État sauvegardé: " + elapsedSeconds + "s");
    }

    /**
     * Restaurer l'état du timer
     */
    private void restoreTimerState() {
        isRunning = prefs.getBoolean("is_running", false);
        isPaused = prefs.getBoolean("is_paused", false);
        startTime = prefs.getLong("start_time", 0);
        elapsedSeconds = prefs.getLong("elapsed_seconds", 0);
        pausedDuration = prefs.getLong("paused_duration", 0);
        pauseStartTime = prefs.getLong("pause_start_time", 0);
        projectId = prefs.getInt("project_id", 0);
        projectName = prefs.getString("project_name", "");
        timerId = prefs.getInt("timer_id", 0);

        if (isRunning) {
            Log.d(TAG, "État restauré: " + elapsedSeconds + "s, running=" + isRunning + ", paused=" + isPaused);

            // Recalculer le temps écoulé si pas en pause
            if (!isPaused) {
                long currentTime = System.currentTimeMillis();
                long totalElapsed = currentTime - startTime;
                elapsedSeconds = (totalElapsed - pausedDuration) / 1000;

                // Redémarrer le compteur
                startTimerCounter();
            }

            // Redémarrer en premier plan
            startForeground(NOTIFICATION_ID, buildNotification());
        }
    }

    /**
     * Nettoyer l'état du timer
     */
    private void clearTimerState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Log.d(TAG, "État nettoyé");
    }

    /**
     * Envoyer un broadcast avec l'état actuel
     */
    private void broadcastTimerUpdate() {
        Intent intent = new Intent(BROADCAST_TIMER_UPDATE);
        intent.putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds);
        intent.putExtra(EXTRA_IS_RUNNING, isRunning && !isPaused);
        intent.putExtra(EXTRA_PROJECT_NAME, projectName);
        sendBroadcast(intent);
    }

    /**
     * Méthode statique pour démarrer le service
     */
    public static void startTimer(Context context, int timerId, int projectId, String projectName) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(ACTION_START);
        intent.putExtra("timer_id", timerId);
        intent.putExtra("project_id", projectId);
        intent.putExtra("project_name", projectName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Méthode statique pour arrêter le service
     */
    public static void stopTimer(Context context) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    /**
     * Méthode statique pour mettre en pause
     */
    public static void pauseTimer(Context context) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(ACTION_PAUSE);
        context.startService(intent);
    }

    /**
     * Méthode statique pour reprendre
     */
    public static void resumeTimer(Context context) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(ACTION_RESUME);
        context.startService(intent);
    }
}
