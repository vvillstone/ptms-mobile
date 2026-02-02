package com.ptms.mobile.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.ChatMessage;
import com.ptms.mobile.models.ChatRoom;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Service de polling pour le chat PTMS
 * Gère la récupération périodique des messages et salles de chat
 */
public class ChatPollingService extends Service {

    private static final String TAG = "ChatPollingService";

    // Intervalles de polling (en millisecondes)
    private static final long POLL_INTERVAL_MESSAGES = 5000;      // 5 secondes
    private static final long POLL_INTERVAL_ROOMS = 30000;         // 30 secondes
    private static final long POLL_INTERVAL_PRESENCE = 60000;      // 60 secondes

    // Actions pour les broadcasts
    public static final String ACTION_NEW_MESSAGES = "com.ptms.mobile.NEW_MESSAGES";
    public static final String ACTION_ROOMS_UPDATED = "com.ptms.mobile.ROOMS_UPDATED";
    public static final String ACTION_PRESENCE_UPDATED = "com.ptms.mobile.PRESENCE_UPDATED";

    // Extras pour les broadcasts
    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_MESSAGE_COUNT = "message_count";

    private Handler handler;
    private ApiService apiService;
    private SharedPreferences prefs;
    private String authToken;
    private int currentRoomId = -1;
    private String lastMessageTimestamp;

    private Runnable pollMessagesRunnable;
    private Runnable pollRoomsRunnable;
    private Runnable pollPresenceRunnable;

    private boolean isPolling = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service créé");

        handler = new Handler(Looper.getMainLooper());
        apiService = ApiClient.getInstance(this).getApiService();
        prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
        authToken = prefs.getString("auth_token", null);

        if (authToken == null) {
            Log.w(TAG, "Pas de token d'authentification - arrêt du service");
            stopSelf();
            return;
        }

        setupRunnables();
    }

    private void setupRunnables() {
        // Polling des messages
        pollMessagesRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling && currentRoomId > 0) {
                    pollMessages();
                }
                handler.postDelayed(this, POLL_INTERVAL_MESSAGES);
            }
        };

        // Polling des salles
        pollRoomsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    pollRooms();
                }
                handler.postDelayed(this, POLL_INTERVAL_ROOMS);
            }
        };

        // Polling de la présence (heartbeat)
        pollPresenceRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    sendPresencePing();
                }
                handler.postDelayed(this, POLL_INTERVAL_PRESENCE);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service démarré");

        if (intent != null && intent.hasExtra(EXTRA_ROOM_ID)) {
            currentRoomId = intent.getIntExtra(EXTRA_ROOM_ID, -1);
            Log.d(TAG, "Room ID défini: " + currentRoomId);
        }

        startPolling();

        return START_STICKY; // Redémarrer si tué par le système
    }

    /**
     * Démarrer le polling
     */
    private void startPolling() {
        if (isPolling) {
            Log.d(TAG, "Polling déjà actif");
            return;
        }

        Log.d(TAG, "Démarrage du polling");
        isPolling = true;

        // Démarrer les différents polling
        handler.post(pollMessagesRunnable);
        handler.post(pollRoomsRunnable);
        handler.post(pollPresenceRunnable);
    }

    /**
     * Arrêter le polling
     */
    private void stopPolling() {
        Log.d(TAG, "Arrêt du polling");
        isPolling = false;

        // Arrêter tous les callbacks
        handler.removeCallbacks(pollMessagesRunnable);
        handler.removeCallbacks(pollRoomsRunnable);
        handler.removeCallbacks(pollPresenceRunnable);
    }

    /**
     * Polling des messages
     */
    private void pollMessages() {
        if (apiService == null || authToken == null || currentRoomId <= 0) {
            return;
        }

        Log.d(TAG, "Polling messages pour room: " + currentRoomId);

        Call<ApiService.ChatMessagesResponse> call = apiService.getChatMessages(
            authToken,
            currentRoomId,
            50,  // limit
            0    // offset
        );

        call.enqueue(new Callback<ApiService.ChatMessagesResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatMessagesResponse> call, Response<ApiService.ChatMessagesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.ChatMessagesResponse chatResponse = response.body();

                    if (chatResponse.success && chatResponse.messages != null && !chatResponse.messages.isEmpty()) {
                        List<ChatMessage> messages = chatResponse.messages;
                        Log.d(TAG, "Nouveaux messages: " + messages.size());

                        // Mettre à jour le timestamp
                        ChatMessage lastMessage = messages.get(messages.size() - 1);
                        if (lastMessage.getTimestamp() != null) {
                            lastMessageTimestamp = lastMessage.getTimestamp().toString();
                        }

                        // Envoyer broadcast pour notifier l'activité
                        broadcastNewMessages(currentRoomId, messages.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.ChatMessagesResponse> call, Throwable t) {
                Log.e(TAG, "Erreur polling messages", t);
            }
        });
    }

    /**
     * Polling des salles de chat
     */
    private void pollRooms() {
        if (apiService == null || authToken == null) {
            return;
        }

        Log.d(TAG, "Polling salles de chat");

        Call<ApiService.ChatRoomsResponse> call = apiService.getChatRooms(authToken);

        call.enqueue(new Callback<ApiService.ChatRoomsResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatRoomsResponse> call, Response<ApiService.ChatRoomsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.ChatRoomsResponse chatResponse = response.body();

                    if (chatResponse.success && chatResponse.rooms != null) {
                        List<ChatRoom> rooms = chatResponse.rooms;
                        Log.d(TAG, "Salles mises à jour: " + rooms.size());

                        // Envoyer broadcast
                        broadcastRoomsUpdated(rooms.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.ChatRoomsResponse> call, Throwable t) {
                Log.e(TAG, "Erreur polling salles", t);
            }
        });
    }

    /**
     * Envoyer un ping de présence (heartbeat)
     */
    private void sendPresencePing() {
        // TODO: Implémenter l'endpoint de présence
        // Pour l'instant, simple log
        Log.d(TAG, "Ping de présence envoyé");
    }

    /**
     * Broadcast: nouveaux messages
     */
    private void broadcastNewMessages(int roomId, int messageCount) {
        Intent intent = new Intent(ACTION_NEW_MESSAGES);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.putExtra(EXTRA_MESSAGE_COUNT, messageCount);
        sendBroadcast(intent);
    }

    /**
     * Broadcast: salles mises à jour
     */
    private void broadcastRoomsUpdated(int roomCount) {
        Intent intent = new Intent(ACTION_ROOMS_UPDATED);
        intent.putExtra("room_count", roomCount);
        sendBroadcast(intent);
    }

    /**
     * Définir la salle de chat courante
     */
    public void setCurrentRoom(int roomId) {
        Log.d(TAG, "Changement de salle: " + currentRoomId + " -> " + roomId);
        this.currentRoomId = roomId;
        this.lastMessageTimestamp = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service détruit");
        stopPolling();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Service non lié
        return null;
    }
}
