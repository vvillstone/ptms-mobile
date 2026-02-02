package com.ptms.mobile.websocket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Client WebSocket pour Chat Temps Réel
 *
 * Gère la connexion WebSocket avec le serveur pour :
 * - Messages instantanés
 * - Statut de frappe
 * - Présence utilisateur
 * - Notifications temps réel
 *
 * Utilisation:
 *   WebSocketChatClient client = new WebSocketChatClient(serverUri, jwtToken);
 *   client.setListener(new WebSocketChatListener() { ... });
 *   client.connect();
 */
public class WebSocketChatClient extends WebSocketClient {
    private static final String TAG = "WebSocketChat";

    private String jwtToken;
    private WebSocketChatListener listener;
    private Handler mainHandler;
    private Handler reconnectHandler;

    private boolean isAuthenticated = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int RECONNECT_DELAY_MS = 1000;
    private static final int HEARTBEAT_INTERVAL_MS = 30000; // 30 secondes

    private Runnable heartbeatRunnable;
    private Runnable reconnectRunnable;

    /**
     * Constructeur
     *
     * @param serverUri URI du serveur WebSocket (ws://host:port)
     * @param jwtToken Token JWT pour authentification
     */
    public WebSocketChatClient(URI serverUri, String jwtToken) {
        super(serverUri);
        this.jwtToken = jwtToken;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.reconnectHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Définir le listener d'événements
     */
    public void setListener(WebSocketChatListener listener) {
        this.listener = listener;
    }

    /**
     * Connexion ouverte
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "Connected to server");
        reconnectAttempts = 0;

        // Authentifier immédiatement
        authenticate();

        // Démarrer le heartbeat
        startHeartbeat();

        // Notifier le listener
        if (listener != null) {
            mainHandler.post(() -> listener.onConnected());
        }
    }

    /**
     * Message reçu du serveur
     */
    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Message received: " + message);

        try {
            JSONObject data = new JSONObject(message);
            String type = data.getString("type");

            // Router selon le type de message
            switch (type) {
                case "auth_required":
                    authenticate();
                    break;

                case "auth_success":
                    handleAuthSuccess(data);
                    break;

                case "subscribed":
                    handleSubscribed(data);
                    break;

                case "unsubscribed":
                    handleUnsubscribed(data);
                    break;

                case "new_message":
                    handleNewMessage(data);
                    break;

                case "message_sent":
                    handleMessageSent(data);
                    break;

                case "user_typing":
                    handleUserTyping(data);
                    break;

                case "presence_changed":
                    handlePresenceChanged(data);
                    break;

                case "messages_read":
                    handleMessagesRead(data);
                    break;

                case "presence_pong":
                    // Réponse au heartbeat
                    break;

                case "error":
                    handleError(data);
                    break;

                default:
                    Log.w(TAG, "Unknown message type: " + type);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing message", e);
        }
    }

    /**
     * Connexion fermée
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Connection closed: " + code + " - " + reason);

        isAuthenticated = false;
        stopHeartbeat();

        // Notifier le listener
        if (listener != null) {
            mainHandler.post(() -> listener.onDisconnected(code, reason));
        }

        // Tenter de se reconnecter
        scheduleReconnect();
    }

    /**
     * Erreur WebSocket
     */
    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "WebSocket error", ex);

        if (listener != null) {
            mainHandler.post(() -> listener.onError(ex.getMessage()));
        }
    }

    /**
     * S'authentifier auprès du serveur
     */
    private void authenticate() {
        try {
            JSONObject auth = new JSONObject();
            auth.put("type", "auth");
            auth.put("token", jwtToken);
            send(auth.toString());
            Log.d(TAG, "Authentication sent");
        } catch (JSONException e) {
            Log.e(TAG, "Error creating auth message", e);
        }
    }

    /**
     * Gérer l'authentification réussie
     */
    private void handleAuthSuccess(JSONObject data) throws JSONException {
        isAuthenticated = true;
        int userId = data.getInt("userId");
        Log.d(TAG, "Authenticated as user: " + userId);

        if (listener != null) {
            mainHandler.post(() -> listener.onAuthenticated(userId));
        }
    }

    /**
     * Gérer l'abonnement à un salon
     */
    private void handleSubscribed(JSONObject data) throws JSONException {
        int roomId = data.getInt("roomId");
        Log.d(TAG, "Subscribed to room: " + roomId);

        if (listener != null) {
            mainHandler.post(() -> listener.onSubscribedToRoom(roomId));
        }
    }

    /**
     * Gérer le désabonnement d'un salon
     */
    private void handleUnsubscribed(JSONObject data) throws JSONException {
        int roomId = data.getInt("roomId");
        Log.d(TAG, "Unsubscribed from room: " + roomId);

        if (listener != null) {
            mainHandler.post(() -> listener.onUnsubscribedFromRoom(roomId));
        }
    }

    /**
     * Gérer un nouveau message
     */
    private void handleNewMessage(JSONObject data) throws JSONException {
        int roomId = data.getInt("roomId");
        int senderId = data.getInt("senderId");
        String message = data.getString("message");
        long timestamp = data.getLong("timestamp");

        Log.d(TAG, "New message in room " + roomId);

        if (listener != null) {
            mainHandler.post(() ->
                listener.onNewMessage(roomId, senderId, message, timestamp)
            );
        }
    }

    /**
     * Gérer la confirmation d'envoi de message
     */
    private void handleMessageSent(JSONObject data) throws JSONException {
        int roomId = data.getInt("roomId");
        int messageId = data.getInt("messageId");

        if (listener != null) {
            mainHandler.post(() -> listener.onMessageSent(roomId, messageId));
        }
    }

    /**
     * Gérer le statut de frappe
     */
    private void handleUserTyping(JSONObject data) throws JSONException {
        int roomId = data.getInt("roomId");
        int userId = data.getInt("userId");
        boolean isTyping = data.getBoolean("isTyping");

        if (listener != null) {
            mainHandler.post(() -> listener.onUserTyping(roomId, userId, isTyping));
        }
    }

    /**
     * Gérer le changement de présence
     */
    private void handlePresenceChanged(JSONObject data) throws JSONException {
        int userId = data.getInt("userId");
        boolean isOnline = data.getBoolean("isOnline");

        Log.d(TAG, "Presence changed: user " + userId + " is " +
            (isOnline ? "online" : "offline"));

        if (listener != null) {
            mainHandler.post(() -> listener.onPresenceChanged(userId, isOnline));
        }
    }

    /**
     * Gérer les messages lus
     */
    private void handleMessagesRead(JSONObject data) throws JSONException {
        int roomId = data.getInt("roomId");
        int userId = data.getInt("userId");

        if (listener != null) {
            mainHandler.post(() -> listener.onMessagesRead(roomId, userId));
        }
    }

    /**
     * Gérer une erreur
     */
    private void handleError(JSONObject data) throws JSONException {
        String error = data.getString("error");
        Log.e(TAG, "Server error: " + error);

        if (listener != null) {
            mainHandler.post(() -> listener.onError(error));
        }
    }

    /**
     * Démarrer le heartbeat
     */
    private void startHeartbeat() {
        stopHeartbeat();

        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAuthenticated && isOpen()) {
                    try {
                        JSONObject ping = new JSONObject();
                        ping.put("type", "presence_ping");
                        send(ping.toString());
                        Log.d(TAG, "Heartbeat sent");
                    } catch (JSONException e) {
                        Log.e(TAG, "Error creating heartbeat", e);
                    }
                }

                mainHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        };

        mainHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    /**
     * Arrêter le heartbeat
     */
    private void stopHeartbeat() {
        if (heartbeatRunnable != null) {
            mainHandler.removeCallbacks(heartbeatRunnable);
            heartbeatRunnable = null;
        }
    }

    /**
     * Planifier une reconnexion
     */
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnection attempts reached");
            if (listener != null) {
                mainHandler.post(() -> listener.onReconnectFailed());
            }
            return;
        }

        reconnectAttempts++;
        long delay = (long) (RECONNECT_DELAY_MS * Math.pow(1.5, reconnectAttempts - 1));

        Log.d(TAG, "Reconnecting in " + delay + "ms (attempt " +
            reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")");

        reconnectRunnable = () -> {
            try {
                reconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error reconnecting", e);
                scheduleReconnect();
            }
        };

        reconnectHandler.postDelayed(reconnectRunnable, delay);
    }

    /**
     * Annuler la reconnexion planifiée
     */
    private void cancelReconnect() {
        if (reconnectRunnable != null) {
            reconnectHandler.removeCallbacks(reconnectRunnable);
            reconnectRunnable = null;
        }
    }

    /**
     * S'abonner à un salon
     */
    public void subscribeToRoom(int roomId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "subscribe_room");
            msg.put("roomId", roomId);
            send(msg.toString());
            Log.d(TAG, "Subscribing to room: " + roomId);
        } catch (JSONException e) {
            Log.e(TAG, "Error subscribing to room", e);
        }
    }

    /**
     * Se désabonner d'un salon
     */
    public void unsubscribeFromRoom(int roomId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "unsubscribe_room");
            msg.put("roomId", roomId);
            send(msg.toString());
            Log.d(TAG, "Unsubscribing from room: " + roomId);
        } catch (JSONException e) {
            Log.e(TAG, "Error unsubscribing from room", e);
        }
    }

    /**
     * Envoyer un message
     */
    public void sendMessage(int roomId, String message) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "send_message");
            msg.put("roomId", roomId);
            msg.put("message", message);
            send(msg.toString());
            Log.d(TAG, "Sending message to room: " + roomId);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending message", e);
        }
    }

    /**
     * Démarrer le statut "en train d'écrire"
     */
    public void startTyping(int roomId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "typing_start");
            msg.put("roomId", roomId);
            send(msg.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error starting typing", e);
        }
    }

    /**
     * Arrêter le statut "en train d'écrire"
     */
    public void stopTyping(int roomId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "typing_stop");
            msg.put("roomId", roomId);
            send(msg.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error stopping typing", e);
        }
    }

    /**
     * Marquer les messages comme lus
     */
    public void markAsRead(int roomId) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "mark_read");
            msg.put("roomId", roomId);
            send(msg.toString());
            Log.d(TAG, "Marking room as read: " + roomId);
        } catch (JSONException e) {
            Log.e(TAG, "Error marking as read", e);
        }
    }

    /**
     * Déconnecter proprement
     */
    public void disconnect() {
        Log.d(TAG, "Disconnecting");

        reconnectAttempts = MAX_RECONNECT_ATTEMPTS; // Empêcher la reconnexion
        cancelReconnect();
        stopHeartbeat();

        if (isOpen()) {
            close(1000, "Client disconnect");
        }
    }

    /**
     * Vérifier si authentifié
     */
    public boolean isAuthenticated() {
        return isAuthenticated && isOpen();
    }

    /**
     * Interface pour les événements WebSocket
     */
    public interface WebSocketChatListener {
        void onConnected();
        void onAuthenticated(int userId);
        void onDisconnected(int code, String reason);
        void onError(String error);
        void onReconnectFailed();

        void onSubscribedToRoom(int roomId);
        void onUnsubscribedFromRoom(int roomId);

        void onNewMessage(int roomId, int senderId, String message, long timestamp);
        void onMessageSent(int roomId, int messageId);

        void onUserTyping(int roomId, int userId, boolean isTyping);
        void onPresenceChanged(int userId, boolean isOnline);
        void onMessagesRead(int roomId, int userId);
    }
}
