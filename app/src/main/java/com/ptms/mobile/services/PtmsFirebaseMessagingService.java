package com.ptms.mobile.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ptms.mobile.R;
import com.ptms.mobile.activities.SplashActivity;
import com.ptms.mobile.activities.ChatActivity;
import com.ptms.mobile.utils.SessionManager;

import java.util.Map;

/**
 * ✅ Service Firebase Cloud Messaging pour les notifications push
 *
 * Gère la réception et l'affichage des notifications push :
 * - Nouveaux messages chat
 * - Rappels saisie d'heures
 * - Notifications projet
 * - Alertes système
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class PtmsFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";

    // Canaux de notification (Android 8+)
    private static final String CHANNEL_CHAT = "chat_notifications";
    private static final String CHANNEL_REMINDERS = "reminder_notifications";
    private static final String CHANNEL_PROJECTS = "project_notifications";
    private static final String CHANNEL_SYSTEM = "system_notifications";

    /**
     * Appelé quand un nouveau token FCM est généré
     * Important pour enregistrer le device sur le serveur
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "📱 Nouveau token FCM: " + token);

        // Sauvegarder le token localement
        saveFcmToken(token);

        // Envoyer le token au serveur backend
        sendTokenToServer(token);
    }

    /**
     * Appelé quand une notification est reçue (app au premier plan)
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "========================================");
        Log.d(TAG, "📬 Notification reçue de: " + remoteMessage.getFrom());

        // Vérifier si la notification contient des données
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "📦 Données: " + remoteMessage.getData());
            handleDataPayload(remoteMessage.getData());
        }

        // Vérifier si la notification contient un message visuel
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "📨 Notification: " + remoteMessage.getNotification().getTitle());
            handleNotificationPayload(remoteMessage);
        }

        Log.d(TAG, "========================================");
    }

    /**
     * Gère les données personnalisées (data payload)
     */
    private void handleDataPayload(Map<String, String> data) {
        String type = data.get("type");
        String title = data.get("title");
        String message = data.get("message");
        String targetId = data.get("target_id");

        if (type == null) {
            Log.w(TAG, "⚠️ Type de notification manquant");
            return;
        }

        switch (type) {
            case "chat":
                showChatNotification(title, message, targetId);
                break;

            case "reminder":
                showReminderNotification(title, message);
                break;

            case "project":
                showProjectNotification(title, message, targetId);
                break;

            case "system":
                showSystemNotification(title, message);
                break;

            default:
                Log.w(TAG, "⚠️ Type de notification inconnu: " + type);
                showDefaultNotification(title, message);
                break;
        }
    }

    /**
     * Gère les notifications standards (notification payload)
     */
    private void handleNotificationPayload(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification == null) return;

        String title = notification.getTitle();
        String message = notification.getBody();

        // Par défaut, afficher comme notification système
        showDefaultNotification(title, message);
    }

    /**
     * Affiche une notification de chat
     */
    private void showChatNotification(String title, String message, String conversationId) {
        Log.d(TAG, "💬 Notification chat: " + title);

        // Intent pour ouvrir la conversation
        Intent intent = new Intent(this, ChatActivity.class);
        if (conversationId != null) {
            intent.putExtra("conversation_id", conversationId);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Son de notification
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Construire la notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_CHAT)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(title != null ? title : "Nouveau message")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Créer le canal si nécessaire (Android 8+)
        createNotificationChannel(notificationManager, CHANNEL_CHAT,
                "Messages Chat", "Notifications des nouveaux messages chat");

        // Afficher la notification
        notificationManager.notify(generateNotificationId("chat"), builder.build());
    }

    /**
     * Affiche une notification de rappel
     */
    private void showReminderNotification(String title, String message) {
        Log.d(TAG, "⏰ Notification rappel: " + title);

        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(title != null ? title : "Rappel")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel(notificationManager, CHANNEL_REMINDERS,
                "Rappels", "Rappels de saisie d'heures et tâches");

        notificationManager.notify(generateNotificationId("reminder"), builder.build());
    }

    /**
     * Affiche une notification de projet
     */
    private void showProjectNotification(String title, String message, String projectId) {
        Log.d(TAG, "📁 Notification projet: " + title);

        // Utiliser MainActivity pour l'instant (ProjectDetailsActivity peut être ajoutée plus tard)
        Intent intent = new Intent(this, SplashActivity.class);
        if (projectId != null) {
            try {
                intent.putExtra("project_id", Integer.parseInt(projectId));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Erreur parsing project_id", e);
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_PROJECTS)
                .setSmallIcon(R.drawable.ic_project)
                .setContentTitle(title != null ? title : "Projet")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_EVENT);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel(notificationManager, CHANNEL_PROJECTS,
                "Projets", "Notifications liées aux projets");

        notificationManager.notify(generateNotificationId("project"), builder.build());
    }

    /**
     * Affiche une notification système
     */
    private void showSystemNotification(String title, String message) {
        Log.d(TAG, "🔔 Notification système: " + title);

        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_SYSTEM)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title != null ? title : "PTMS Mobile")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel(notificationManager, CHANNEL_SYSTEM,
                "Système", "Notifications système importantes");

        notificationManager.notify(generateNotificationId("system"), builder.build());
    }

    /**
     * Affiche une notification par défaut
     */
    private void showDefaultNotification(String title, String message) {
        Log.d(TAG, "📢 Notification par défaut: " + title);

        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_SYSTEM)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title != null ? title : "PTMS Mobile")
                .setContentText(message != null ? message : "Nouvelle notification")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel(notificationManager, CHANNEL_SYSTEM,
                "Notifications", "Notifications générales");

        notificationManager.notify(generateNotificationId("default"), builder.build());
    }

    /**
     * Crée un canal de notification (Android 8+)
     */
    private void createNotificationChannel(NotificationManager notificationManager,
                                            String channelId, String channelName, String channelDescription) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                channel = new NotificationChannel(channelId, channelName, importance);
                channel.setDescription(channelDescription);
                channel.enableLights(true);
                channel.enableVibration(true);

                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Canal de notification créé: " + channelId);
            }
        }
    }

    /**
     * Génère un ID unique pour chaque type de notification
     */
    private int generateNotificationId(String type) {
        // Utiliser un ID basé sur le timestamp pour permettre plusieurs notifications
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    /**
     * Sauvegarde le token FCM localement
     */
    private void saveFcmToken(String token) {
        SessionManager sessionManager = new SessionManager(getApplicationContext());
        sessionManager.saveFcmToken(token);
        Log.d(TAG, "💾 Token FCM sauvegardé localement");
    }

    /**
     * Envoie le token FCM au serveur backend
     */
    private void sendTokenToServer(String token) {
        // TODO: Implémenter l'envoi du token au serveur via API
        // Endpoint suggéré: POST /api/fcm/register
        //
        // Payload:
        // {
        //   "token": "fcm_token_here",
        //   "device_id": "unique_device_id",
        //   "platform": "android"
        // }

        Log.d(TAG, "📤 TODO: Envoyer token au serveur: " + token.substring(0, Math.min(20, token.length())) + "...");

        // Exemple d'implémentation:
        /*
        ApiClient apiClient = ApiClient.getInstance(getApplicationContext());
        ApiService apiService = apiClient.getApiService();

        SessionManager sessionManager = new SessionManager(getApplicationContext());
        String authToken = sessionManager.getToken();

        if (authToken != null && !authToken.isEmpty()) {
            Call<JsonObject> call = apiService.registerFcmToken(
                "Bearer " + authToken,
                new FcmTokenRequest(token, getDeviceId(), "android")
            );

            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Token FCM enregistré sur serveur");
                    } else {
                        Log.e(TAG, "❌ Erreur enregistrement token: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "❌ Échec enregistrement token", t);
                }
            });
        }
        */
    }
}
