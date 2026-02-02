package com.ptms.mobile.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ptms.mobile.utils.SessionManager;

/**
 * ✅ Gestionnaire des notifications push Firebase
 *
 * Fonctionnalités :
 * - Récupération du token FCM
 * - Abonnement/désabonnement aux topics
 * - Gestion des permissions
 * - Statistiques des notifications
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class NotificationManager {

    private static final String TAG = "NotificationManager";

    // Topics Firebase
    public static final String TOPIC_ALL = "all_users";
    public static final String TOPIC_CHAT = "chat_updates";
    public static final String TOPIC_PROJECTS = "project_updates";
    public static final String TOPIC_REMINDERS = "reminders";
    public static final String TOPIC_SYSTEM = "system_announcements";

    private final Context context;
    private final SessionManager sessionManager;

    public NotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new SessionManager(context);
    }

    /**
     * Initialise les notifications push
     * À appeler au démarrage de l'application
     */
    public void initialize() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🔔 Initialisation Notifications Push");
        Log.d(TAG, "========================================");

        // Récupérer le token FCM
        getFcmToken(new FcmTokenCallback() {
            @Override
            public void onSuccess(String token) {
                Log.d(TAG, "✅ Token FCM récupéré: " + token.substring(0, Math.min(20, token.length())) + "...");

                // Sauvegarder le token
                sessionManager.saveFcmToken(token);

                // S'abonner aux topics par défaut
                subscribeToDefaultTopics();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Erreur récupération token FCM", e);
            }
        });
    }

    /**
     * Récupère le token FCM actuel
     */
    public void getFcmToken(FcmTokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "⚠️ Échec récupération token FCM", task.getException());
                            if (callback != null) {
                                callback.onFailure(task.getException());
                            }
                            return;
                        }

                        // Token FCM récupéré
                        String token = task.getResult();
                        if (callback != null) {
                            callback.onSuccess(token);
                        }
                    }
                });
    }

    /**
     * S'abonne aux topics par défaut
     */
    private void subscribeToDefaultTopics() {
        // Tous les utilisateurs
        subscribeToTopic(TOPIC_ALL);

        // Basé sur les préférences utilisateur
        if (sessionManager.isChatNotificationsEnabled()) {
            subscribeToTopic(TOPIC_CHAT);
        }

        if (sessionManager.isProjectNotificationsEnabled()) {
            subscribeToTopic(TOPIC_PROJECTS);
        }

        if (sessionManager.isReminderNotificationsEnabled()) {
            subscribeToTopic(TOPIC_REMINDERS);
        }

        // Toujours s'abonner aux annonces système
        subscribeToTopic(TOPIC_SYSTEM);
    }

    /**
     * S'abonne à un topic Firebase
     */
    public void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ Abonné au topic: " + topic);
                        } else {
                            Log.w(TAG, "⚠️ Échec abonnement au topic: " + topic, task.getException());
                        }
                    }
                });
    }

    /**
     * Se désabonne d'un topic Firebase
     */
    public void unsubscribeFromTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ Désabonné du topic: " + topic);
                        } else {
                            Log.w(TAG, "⚠️ Échec désabonnement du topic: " + topic, task.getException());
                        }
                    }
                });
    }

    /**
     * Active les notifications chat
     */
    public void enableChatNotifications() {
        sessionManager.setChatNotificationsEnabled(true);
        subscribeToTopic(TOPIC_CHAT);
        Log.d(TAG, "🔔 Notifications chat activées");
    }

    /**
     * Désactive les notifications chat
     */
    public void disableChatNotifications() {
        sessionManager.setChatNotificationsEnabled(false);
        unsubscribeFromTopic(TOPIC_CHAT);
        Log.d(TAG, "🔕 Notifications chat désactivées");
    }

    /**
     * Active les notifications projet
     */
    public void enableProjectNotifications() {
        sessionManager.setProjectNotificationsEnabled(true);
        subscribeToTopic(TOPIC_PROJECTS);
        Log.d(TAG, "🔔 Notifications projet activées");
    }

    /**
     * Désactive les notifications projet
     */
    public void disableProjectNotifications() {
        sessionManager.setProjectNotificationsEnabled(false);
        unsubscribeFromTopic(TOPIC_PROJECTS);
        Log.d(TAG, "🔕 Notifications projet désactivées");
    }

    /**
     * Active les rappels
     */
    public void enableReminders() {
        sessionManager.setReminderNotificationsEnabled(true);
        subscribeToTopic(TOPIC_REMINDERS);
        Log.d(TAG, "🔔 Rappels activés");
    }

    /**
     * Désactive les rappels
     */
    public void disableReminders() {
        sessionManager.setReminderNotificationsEnabled(false);
        unsubscribeFromTopic(TOPIC_REMINDERS);
        Log.d(TAG, "🔕 Rappels désactivés");
    }

    /**
     * Supprime le token FCM (lors du logout)
     */
    public void deleteFcmToken() {
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            sessionManager.clearFcmToken();
                            Log.d(TAG, "✅ Token FCM supprimé");
                        } else {
                            Log.w(TAG, "⚠️ Échec suppression token FCM", task.getException());
                        }
                    }
                });
    }

    /**
     * Vérifie si les notifications sont activées
     */
    public boolean areNotificationsEnabled() {
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            return notificationManager.areNotificationsEnabled();
        }
        return false;
    }

    /**
     * Ouvre les paramètres de notification de l'application
     */
    public void openNotificationSettings() {
        android.content.Intent intent = new android.content.Intent();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
        }
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Affiche les statistiques des notifications
     */
    public void logStatistics() {
        Log.d(TAG, "========================================");
        Log.d(TAG, "📊 STATISTIQUES NOTIFICATIONS");
        Log.d(TAG, "========================================");
        Log.d(TAG, "🔔 Notifications activées: " + (areNotificationsEnabled() ? "OUI" : "NON"));
        Log.d(TAG, "💬 Notifications chat: " + (sessionManager.isChatNotificationsEnabled() ? "ON" : "OFF"));
        Log.d(TAG, "📁 Notifications projet: " + (sessionManager.isProjectNotificationsEnabled() ? "ON" : "OFF"));
        Log.d(TAG, "⏰ Rappels: " + (sessionManager.isReminderNotificationsEnabled() ? "ON" : "OFF"));

        String token = sessionManager.getFcmToken();
        if (token != null && !token.isEmpty()) {
            Log.d(TAG, "📱 Token FCM: " + token.substring(0, Math.min(20, token.length())) + "...");
        } else {
            Log.d(TAG, "📱 Token FCM: Non disponible");
        }
        Log.d(TAG, "========================================");
    }

    // ==================== INTERFACES ====================

    /**
     * Callback pour la récupération du token FCM
     */
    public interface FcmTokenCallback {
        void onSuccess(String token);
        void onFailure(Exception e);
    }
}
