package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.adapters.ChatMessagesAdapter;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.ChatMessage;
import com.ptms.mobile.models.ChatRoom;
import com.ptms.mobile.models.ChatUser;
import com.ptms.mobile.websocket.WebSocketChatClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activité de chat V2 avec WebSocket (Temps Réel)
 * Utilise WebSocketChatClient au lieu du polling HTTP
 */
public class ChatWebSocketActivity extends AppCompatActivity implements WebSocketChatClient.WebSocketChatListener {

    private static final String TAG = "ChatActivityV2";

    public static final String EXTRA_CHAT_ROOM = "chat_room";
    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_ROOM_NAME = "room_name";

    private ChatRoom chatRoom;
    private int roomId;
    private String roomName;
    private int currentUserId;
    private String authToken;

    private Toolbar toolbar;
    private TextView tvRoomName;
    private TextView tvRoomStatus;
    private ImageView btnRoomInfo;
    private RecyclerView recyclerMessages;
    private LinearLayout typingIndicator;
    private TextView tvTyping;
    private EditText etMessage;
    private ImageButton btnAttach;
    private ImageButton btnSend;
    private ProgressBar progressSending;

    private ChatMessagesAdapter messagesAdapter;
    private SharedPreferences prefs;
    private com.ptms.mobile.utils.SettingsManager settingsManager;

    // WebSocket
    private WebSocketChatClient webSocketClient;
    private boolean isConnected = false;

    // Indicateur "en train d'écrire"
    private Handler typingHandler;
    private Runnable typingStopRunnable;
    private static final long TYPING_TIMEOUT = 3000; // 3 secondes
    private boolean isTyping = false;

    // Cache des noms d'utilisateurs
    private Map<Integer, String> userNamesCache = new HashMap<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            // Récupérer les données de la salle
            Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_CHAT_ROOM)) {
                chatRoom = (ChatRoom) intent.getSerializableExtra(EXTRA_CHAT_ROOM);
                roomId = chatRoom.getId();
                roomName = chatRoom.getName();
            } else {
                roomId = intent.getIntExtra(EXTRA_ROOM_ID, -1);
                roomName = intent.getStringExtra(EXTRA_ROOM_NAME);
            }

            if (roomId == -1) {
                Toast.makeText(this, "Erreur: ID de salle invalide", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialisation
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            authToken = prefs.getString("auth_token", null);
            currentUserId = prefs.getInt("employee_id", -1);
            settingsManager = new com.ptms.mobile.utils.SettingsManager(this);

            if (authToken == null) {
                Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AuthenticationActivity.class));
                finish();
                return;
            }

            // Initialiser ApiService
            apiService = ApiClient.getInstance(this).getApiService();

            initViews();
            setupToolbar();
            setupRecyclerView();
            setupListeners();
            loadChatUsers(); // Charger les utilisateurs pour le cache
            setupWebSocket();

        } catch (Exception e) {
            android.util.Log.e(TAG, "Erreur dans onCreate", e);
            Toast.makeText(this, "Erreur initialisation chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Configure la connexion WebSocket
     */
    private void setupWebSocket() {
        try {
            // Construire l'URI WebSocket à partir de l'URL du serveur
            String serverUrl = settingsManager.getServerUrlRaw();
            serverUrl = serverUrl.replace("https://", "ws://").replace("http://", "ws://");
            if (!serverUrl.endsWith("/")) {
                serverUrl += "/";
            }
            serverUrl += "websocket/chat";

            URI wsUri = new URI(serverUrl);

            android.util.Log.d(TAG, "Connexion WebSocket: " + wsUri.toString());

            webSocketClient = new WebSocketChatClient(wsUri, authToken);
            webSocketClient.setListener(this);
            webSocketClient.connect();

        } catch (Exception e) {
            android.util.Log.e(TAG, "Erreur setup WebSocket", e);
            Toast.makeText(this, "Erreur connexion WebSocket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvRoomName = findViewById(R.id.tv_room_name);
        tvRoomStatus = findViewById(R.id.tv_room_status);
        btnRoomInfo = findViewById(R.id.btn_room_info);
        recyclerMessages = findViewById(R.id.recycler_messages);
        typingIndicator = findViewById(R.id.typing_indicator);
        tvTyping = findViewById(R.id.tv_typing);
        etMessage = findViewById(R.id.et_message);
        btnAttach = findViewById(R.id.btn_attach);
        btnSend = findViewById(R.id.btn_send);
        progressSending = findViewById(R.id.progress_sending);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.chat) + " (WebSocket)");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Informations de la salle
        tvRoomName.setText(roomName != null ? roomName : "Salle de chat");
        updateConnectionStatus(false);
    }

    private void updateConnectionStatus(boolean connected) {
        isConnected = connected;
        if (connected) {
            tvRoomStatus.setText("Connecté (Temps réel)");
            tvRoomStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvRoomStatus.setText("Déconnecté");
            tvRoomStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void setupRecyclerView() {
        messagesAdapter = new ChatMessagesAdapter(this, currentUserId);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(messagesAdapter);

        // Scroll automatique vers le bas pour les nouveaux messages
        messagesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if (positionStart == messagesAdapter.getItemCount() - 1) {
                    recyclerMessages.scrollToPosition(positionStart);
                }
            }
        });
    }

    private void setupListeners() {
        // Bouton d'envoi
        btnSend.setOnClickListener(v -> sendMessage());

        // Bouton de pièce jointe
        btnAttach.setOnClickListener(v -> {
            Toast.makeText(ChatWebSocketActivity.this, "Fonctionnalité de pièce jointe à venir", Toast.LENGTH_SHORT).show();
        });

        // Bouton d'informations de la salle
        btnRoomInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatParticipantsActivity.class);
            intent.putExtra(ChatParticipantsActivity.EXTRA_ROOM_ID, roomId);
            intent.putExtra(ChatParticipantsActivity.EXTRA_ROOM_NAME, roomName);
            startActivity(intent);
        });

        // Détection de frappe
        typingHandler = new Handler(Looper.getMainLooper());
        typingStopRunnable = () -> setTypingStatus(false);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    if (!isTyping) {
                        setTypingStatus(true);
                    }
                    typingHandler.removeCallbacks(typingStopRunnable);
                    typingHandler.postDelayed(typingStopRunnable, TYPING_TIMEOUT);
                } else {
                    setTypingStatus(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Envoie le statut "en train d'écrire" via WebSocket
     */
    private void setTypingStatus(boolean typing) {
        if (webSocketClient == null || !webSocketClient.isAuthenticated()) {
            return;
        }

        isTyping = typing;
        android.util.Log.d(TAG, "Statut 'en train d'écrire': " + typing);

        if (typing) {
            webSocketClient.startTyping(roomId);
        } else {
            webSocketClient.stopTyping(roomId);
        }
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        if (webSocketClient == null || !webSocketClient.isAuthenticated()) {
            Toast.makeText(this, "Erreur de connexion WebSocket", Toast.LENGTH_SHORT).show();
            return;
        }

        // Arrêter le statut "en train d'écrire"
        setTypingStatus(false);

        // Afficher le progress
        progressSending.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        android.util.Log.d(TAG, "Envoi du message: " + messageText);

        // Envoyer via WebSocket
        webSocketClient.sendMessage(roomId, messageText);

        // Vider le champ (optimistic UI)
        etMessage.setText("");

        // Réactiver le bouton après un court délai
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressSending.setVisibility(View.GONE);
            btnSend.setEnabled(true);
        }, 500);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            // Pas besoin de refresh manuel avec WebSocket
            Toast.makeText(this, "Mode temps réel activé", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.action_room_info) {
            Intent intent = new Intent(this, ChatParticipantsActivity.class);
            intent.putExtra(ChatParticipantsActivity.EXTRA_ROOM_ID, roomId);
            intent.putExtra(ChatParticipantsActivity.EXTRA_ROOM_NAME, roomName);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================== WebSocket Listener ====================

    @Override
    public void onConnected() {
        android.util.Log.d(TAG, "WebSocket connecté");
        runOnUiThread(() -> {
            updateConnectionStatus(true);
            Toast.makeText(this, "Connexion temps réel établie", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onAuthenticated(int userId) {
        android.util.Log.d(TAG, "WebSocket authentifié: userId=" + userId);
        runOnUiThread(() -> {
            // S'abonner à la salle de chat
            if (webSocketClient != null) {
                webSocketClient.subscribeToRoom(roomId);
            }
        });
    }

    @Override
    public void onDisconnected(int code, String reason) {
        android.util.Log.d(TAG, "WebSocket déconnecté: " + code + " - " + reason);
        runOnUiThread(() -> {
            updateConnectionStatus(false);
            Toast.makeText(this, "Déconnecté: " + reason, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(String error) {
        android.util.Log.e(TAG, "WebSocket error: " + error);
        runOnUiThread(() -> {
            Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onReconnectFailed() {
        android.util.Log.e(TAG, "WebSocket reconnection failed");
        runOnUiThread(() -> {
            Toast.makeText(this, "Échec de reconnexion", Toast.LENGTH_SHORT).show();
            updateConnectionStatus(false);
        });
    }

    @Override
    public void onSubscribedToRoom(int roomId) {
        android.util.Log.d(TAG, "Abonné à la salle: " + roomId);
        runOnUiThread(() -> {
            Toast.makeText(this, "Salle rejointe", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onUnsubscribedFromRoom(int roomId) {
        android.util.Log.d(TAG, "Désabonné de la salle: " + roomId);
    }

    @Override
    public void onNewMessage(int roomId, int senderId, String message, long timestamp) {
        android.util.Log.d(TAG, "Nouveau message reçu: " + message);
        runOnUiThread(() -> {
            // Créer un ChatMessage et l'ajouter à l'adaptateur
            ChatMessage chatMessage = new ChatMessage(
                roomId,
                senderId,
                getUserDisplayName(senderId), // Récupère le vrai nom depuis le cache
                message
            );
            chatMessage.setTimestamp(new java.util.Date(timestamp));
            messagesAdapter.addMessage(chatMessage);
        });
    }

    @Override
    public void onMessageSent(int roomId, int messageId) {
        android.util.Log.d(TAG, "Message envoyé confirmé: " + messageId);
        runOnUiThread(() -> {
            progressSending.setVisibility(View.GONE);
            btnSend.setEnabled(true);
        });
    }

    @Override
    public void onUserTyping(int roomId, int userId, boolean isTyping) {
        if (userId == currentUserId) {
            return; // Ne pas afficher notre propre statut
        }

        android.util.Log.d(TAG, "Utilisateur " + userId + " en train d'écrire: " + isTyping);
        runOnUiThread(() -> {
            if (isTyping) {
                typingIndicator.setVisibility(View.VISIBLE);
                tvTyping.setText(getUserDisplayName(userId) + " écrit...");
            } else {
                typingIndicator.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onPresenceChanged(int userId, boolean isOnline) {
        android.util.Log.d(TAG, "Présence changée: userId=" + userId + ", online=" + isOnline);
        // TODO: Mettre à jour l'UI pour montrer qui est en ligne
    }

    @Override
    public void onMessagesRead(int roomId, int userId) {
        android.util.Log.d(TAG, "Messages lus: roomId=" + roomId + ", userId=" + userId);
        // TODO: Mettre à jour l'UI pour montrer les messages lus
    }

    // ==================== User Names Cache ====================

    /**
     * Charge la liste des utilisateurs depuis l'API et remplit le cache
     */
    private void loadChatUsers() {
        if (apiService == null || authToken == null) {
            return;
        }

        android.util.Log.d(TAG, "Chargement des utilisateurs pour le cache...");

        apiService.getChatUsers("Bearer " + authToken).enqueue(new retrofit2.Callback<ApiService.ChatUsersResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ChatUsersResponse> call, retrofit2.Response<ApiService.ChatUsersResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<ChatUser> users = response.body().users;
                    if (users != null) {
                        for (ChatUser user : users) {
                            userNamesCache.put(user.getId(), user.getDisplayName());
                        }
                        android.util.Log.d(TAG, "Cache utilisateurs chargé: " + userNamesCache.size() + " utilisateurs");
                    }
                } else {
                    android.util.Log.e(TAG, "Erreur lors du chargement des utilisateurs: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiService.ChatUsersResponse> call, Throwable t) {
                android.util.Log.e(TAG, "Échec du chargement des utilisateurs", t);
            }
        });
    }

    /**
     * Récupère le nom d'affichage d'un utilisateur depuis le cache
     * @param userId ID de l'utilisateur
     * @return Nom d'affichage ou "Utilisateur #ID" si non trouvé
     */
    private String getUserDisplayName(int userId) {
        String name = userNamesCache.get(userId);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return "Utilisateur #" + userId;
    }

    // ==================== Lifecycle ====================

    @Override
    protected void onResume() {
        super.onResume();
        if (webSocketClient != null && !webSocketClient.isOpen()) {
            try {
                webSocketClient.reconnect();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Erreur reconnexion WebSocket", e);
            }
        }
        android.util.Log.d(TAG, "onResume - WebSocket");
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.d(TAG, "onPause - WebSocket");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Déconnecter proprement le WebSocket
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
        android.util.Log.d(TAG, "onDestroy - WebSocket déconnecté");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
