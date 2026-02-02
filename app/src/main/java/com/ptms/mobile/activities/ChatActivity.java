package com.ptms.mobile.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.adapters.ChatMessagesAdapter;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.ChatMessage;
import com.ptms.mobile.models.ChatRoom;
import com.ptms.mobile.utils.MediaUploadManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité de chat pour une salle spécifique
 */
public class ChatActivity extends AppCompatActivity {
    
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
    private ApiService apiService;
    private SharedPreferences prefs;
    private com.ptms.mobile.utils.SettingsManager settingsManager;
    private MediaUploadManager uploadManager;

    // Polling automatique
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL = 5000; // 5 secondes
    private boolean isPolling = false;

    // Indicateur "en train d'écrire"
    private Handler typingHandler;
    private Runnable typingStopRunnable;
    private static final long TYPING_TIMEOUT = 3000; // 3 secondes
    private boolean isTyping = false;

    // Enregistrement audio
    private static final int PERMISSION_RECORD_AUDIO = 200;
    private static final String TAG = "ChatActivity";
    private MediaRecorder mediaRecorder = null;
    private MediaPlayer mediaPlayer = null;
    private String audioFilePath = null;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private long recordingStartTime = 0;
    private int recordedDuration = 0; // Durée en secondes
    private Handler recordingHandler;
    private LinearLayout audioRecordingPanel;
    private LinearLayout audioPreviewPanel;
    private TextView tvRecordingTimer;
    private TextView tvAudioDuration;
    private Button btnStartRecording;
    private Button btnStopRecording;
    private Button btnCancelRecording;
    private Button btnPlayAudio;
    private Button btnDeleteAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        try {
            // Récupérer les données de la salle
            Intent intent = getIntent();

            // Nouveau: priorité aux données passées directement (pas de sérialisation)
            if (intent.hasExtra("ROOM_ID")) {
                roomId = intent.getIntExtra("ROOM_ID", -1);
                roomName = intent.getStringExtra("ROOM_NAME");
                android.util.Log.d("CHAT", "Ouverture salle via paramètres directs: ID=" + roomId + ", Nom=" + roomName);
            }
            // Ancien: fallback vers l'objet sérialisé (peut causer des problèmes)
            else if (intent.hasExtra(EXTRA_CHAT_ROOM)) {
                chatRoom = (ChatRoom) intent.getSerializableExtra(EXTRA_CHAT_ROOM);
                roomId = chatRoom.getId();
                roomName = chatRoom.getName();
                android.util.Log.d("CHAT", "Ouverture salle via objet sérialisé: ID=" + roomId);
            }
            // Très ancien: fallback vers les anciens paramètres
            else {
                roomId = intent.getIntExtra(EXTRA_ROOM_ID, -1);
                roomName = intent.getStringExtra(EXTRA_ROOM_NAME);
                android.util.Log.d("CHAT", "Ouverture salle via anciens paramètres");
            }
            
            if (roomId == -1) {
                Toast.makeText(this, "Erreur: ID de salle invalide", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // Initialisation
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            apiService = ApiClient.getInstance(this).getApiService();
            authToken = prefs.getString("auth_token", null);
            currentUserId = prefs.getInt("employee_id", -1);
            settingsManager = new com.ptms.mobile.utils.SettingsManager(this);

            // Initialiser MediaUploadManager
            String baseUrl = prefs.getString("server_url", "https://serveralpha.protti.group");
            uploadManager = new MediaUploadManager(baseUrl);
            uploadManager.setAuthToken(authToken);
            
            if (authToken == null) {
                Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AuthenticationActivity.class));
                finish();
                return;
            }
            
            initViews();
            setupToolbar();
            setupRecyclerView();
            setupListeners();
            setupChatMode(); // ✅ Configurer le mode lecture seule si chat désactivé
            setupPolling();
            loadMessages();

        } catch (Exception e) {
            android.util.Log.e("CHAT", "Erreur dans onCreate", e);
            Toast.makeText(this, "Erreur initialisation chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * ✅ Configure le mode du chat (actif ou lecture seule)
     */
    private void setupChatMode() {
        if (!settingsManager.isChatEnabled()) {
            // Mode lecture seule - Chat désactivé
            android.util.Log.w("CHAT", "⚠️ Chat désactivé - Mode lecture seule activé");

            // Désactiver les contrôles d'envoi
            if (etMessage != null) {
                etMessage.setEnabled(false);
                etMessage.setHint("💬 Chat désactivé - Mode lecture seule");
            }
            if (btnSend != null) {
                btnSend.setEnabled(false);
                btnSend.setAlpha(0.5f);
            }
            if (btnAttach != null) {
                btnAttach.setEnabled(false);
                btnAttach.setAlpha(0.5f);
            }

            // Afficher un avertissement
            Toast.makeText(this,
                "💬 Chat en mode lecture seule\nActivez le chat dans les paramètres pour envoyer des messages",
                Toast.LENGTH_LONG).show();
        } else {
            android.util.Log.d("CHAT", "✅ Chat activé - Mode normal");
        }
    }

    /**
     * Configure le polling automatique pour rafraîchir les messages
     */
    private void setupPolling() {
        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    loadMessagesQuietly();
                    pollingHandler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        };
    }

    /**
     * Démarre le polling automatique (seulement si activé dans les paramètres)
     */
    private void startPolling() {
        // ✅ Ne pas démarrer le polling si le chat est désactivé
        if (!settingsManager.isChatEnabled()) {
            android.util.Log.d("CHAT", "⚠️ Chat désactivé - Polling non démarré");
            return;
        }

        // Vérifier si le polling est activé dans les paramètres
        if (!settingsManager.isChatPollingEnabled()) {
            android.util.Log.d("CHAT", "Polling automatique désactivé dans les paramètres");
            return;
        }

        if (!isPolling) {
            isPolling = true;
            android.util.Log.d("CHAT", "Démarrage du polling automatique");
            pollingHandler.post(pollingRunnable);
        }
    }

    /**
     * Arrête le polling automatique
     */
    private void stopPolling() {
        if (isPolling) {
            isPolling = false;
            android.util.Log.d("CHAT", "Arrêt du polling automatique");
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    /**
     * Charge les messages sans afficher de toast (pour le polling automatique)
     */
    private void loadMessagesQuietly() {
        if (apiService == null || authToken == null) {
            return;
        }

        Call<ApiService.ChatMessagesResponse> call = apiService.getChatMessages(authToken, roomId, 50, 0);
        call.enqueue(new Callback<ApiService.ChatMessagesResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatMessagesResponse> call, Response<ApiService.ChatMessagesResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.ChatMessagesResponse chatResponse = response.body();
                        if (chatResponse.success && chatResponse.messages != null) {
                            List<ChatMessage> messages = chatResponse.messages;
                            int oldCount = messagesAdapter.getItemCount();
                            messagesAdapter.updateMessages(messages);

                            // Scroll vers le bas seulement si de nouveaux messages
                            if (messages.size() > oldCount) {
                                recyclerMessages.scrollToPosition(messages.size() - 1);
                                android.util.Log.d("CHAT", "Nouveaux messages: " + (messages.size() - oldCount));
                            }
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("CHAT", "Erreur dans loadMessagesQuietly", e);
                }
            }

            @Override
            public void onFailure(Call<ApiService.ChatMessagesResponse> call, Throwable t) {
                // Pas de toast pour les échecs de polling automatique
                android.util.Log.w("CHAT", "Échec polling messages", t);
            }
        });
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
            getSupportActionBar().setTitle(getString(R.string.chat));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Informations de la salle
        tvRoomName.setText(roomName != null ? roomName : "Salle de chat");
        tvRoomStatus.setText(getString(R.string.chat_online));
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
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        
        // Bouton d'enregistrement audio (btnAttach devient bouton microphone)
        btnAttach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAudioPermission();
            }
        });
        
        // Bouton d'informations de la salle
        btnRoomInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Afficher les informations de la salle
                Toast.makeText(ChatActivity.this, "Informations de la salle à venir", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Détection de frappe
        typingHandler = new Handler(Looper.getMainLooper());
        typingStopRunnable = new Runnable() {
            @Override
            public void run() {
                setTypingStatus(false);
            }
        };

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // L'utilisateur est en train d'écrire
                    if (!isTyping) {
                        setTypingStatus(true);
                    }
                    // Réinitialiser le timer pour arrêter le statut "en train d'écrire"
                    typingHandler.removeCallbacks(typingStopRunnable);
                    typingHandler.postDelayed(typingStopRunnable, TYPING_TIMEOUT);
                } else {
                    // Le champ est vide, arrêter le statut
                    setTypingStatus(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void loadMessages() {
        if (apiService == null || authToken == null) {
            android.util.Log.w("CHAT", "API Service ou token null");
            return;
        }
        
        android.util.Log.d("CHAT", "Chargement des messages pour la salle: " + roomId);
        
        Call<ApiService.ChatMessagesResponse> call = apiService.getChatMessages(authToken, roomId, 50, 0);
        call.enqueue(new Callback<ApiService.ChatMessagesResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatMessagesResponse> call, Response<ApiService.ChatMessagesResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.ChatMessagesResponse chatResponse = response.body();
                        if (chatResponse.success && chatResponse.messages != null) {
                            List<ChatMessage> messages = chatResponse.messages;
                            android.util.Log.d("CHAT", "Messages chargés: " + messages.size());
                            messagesAdapter.updateMessages(messages);
                            
                            // Scroll vers le bas
                            if (!messages.isEmpty()) {
                                recyclerMessages.scrollToPosition(messages.size() - 1);
                            }
                        } else {
                            android.util.Log.e("CHAT", "Erreur API: " + chatResponse.message);
                            Toast.makeText(ChatActivity.this, "Erreur: " + chatResponse.message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        android.util.Log.e("CHAT", "Erreur chargement messages: " + response.code() + " " + response.message());
                        Toast.makeText(ChatActivity.this, "Erreur chargement messages: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("CHAT", "Erreur dans onResponse", e);
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ChatMessagesResponse> call, Throwable t) {
                android.util.Log.e("CHAT", "Échec chargement messages", t);
                Toast.makeText(ChatActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Envoie le statut "en train d'écrire" au serveur
     */
    private void setTypingStatus(boolean typing) {
        if (apiService == null || authToken == null) {
            return;
        }

        isTyping = typing;
        android.util.Log.d("CHAT", "Statut 'en train d'écrire': " + typing);

        ApiService.TypingRequest request = new ApiService.TypingRequest(roomId, typing);

        Call<ApiService.ChatTypingResponse> call = apiService.setTypingStatus(authToken, request);
        call.enqueue(new Callback<ApiService.ChatTypingResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatTypingResponse> call, Response<ApiService.ChatTypingResponse> response) {
                // Pas de traitement spécial nécessaire
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("CHAT", "Statut typing envoyé avec succès");
                }
            }

            @Override
            public void onFailure(Call<ApiService.ChatTypingResponse> call, Throwable t) {
                android.util.Log.w("CHAT", "Échec envoi statut typing", t);
            }
        });
    }

    private void sendMessage() {
        // ✅ Vérifier que le chat est activé
        if (!settingsManager.isChatEnabled()) {
            Toast.makeText(this, "💬 Chat désactivé - Activez-le dans les paramètres", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        if (apiService == null || authToken == null) {
            Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
            return;
        }

        // Arrêter le statut "en train d'écrire"
        setTypingStatus(false);

        // Afficher le progress
        progressSending.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        
        // Créer la requête
        ApiService.SendMessageRequest request = new ApiService.SendMessageRequest(roomId, messageText);
        
        android.util.Log.d("CHAT", "Envoi du message: " + messageText);
        
        Call<ApiService.ChatSendResponse> call = apiService.sendChatMessage(authToken, request);
        call.enqueue(new Callback<ApiService.ChatSendResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatSendResponse> call, Response<ApiService.ChatSendResponse> response) {
                progressSending.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.ChatSendResponse chatResponse = response.body();
                        if (chatResponse.success) {
                            // Message envoyé avec succès
                            etMessage.setText("");
                            android.util.Log.d("CHAT", "Message envoyé avec succès");
                            
                            // Ajouter le message retourné par le serveur
                            if (chatResponse.chatMessage != null) {
                                messagesAdapter.addMessage(chatResponse.chatMessage);
                            }
                            
                            // Recharger les messages pour avoir la version complète
                            loadMessages();
                        } else {
                            Toast.makeText(ChatActivity.this, "Erreur: " + chatResponse.message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        android.util.Log.e("CHAT", "Erreur envoi message: " + response.code() + " " + response.message());
                        Toast.makeText(ChatActivity.this, "Erreur envoi message: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("CHAT", "Erreur dans onResponse envoi", e);
                    Toast.makeText(ChatActivity.this, "Erreur envoi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ChatSendResponse> call, Throwable t) {
                progressSending.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                android.util.Log.e("CHAT", "Échec envoi message", t);
                Toast.makeText(ChatActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========== ENREGISTREMENT AUDIO ==========

    /**
     * Vérifier les permissions microphone
     */
    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Demander la permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
        } else {
            // Permission déjà accordée
            showAudioRecordingDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAudioRecordingDialog();
            } else {
                Toast.makeText(this, "Permission microphone refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Afficher le dialogue d'enregistrement audio
     */
    private void showAudioRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_audio_recording, null);
        builder.setView(dialogView);

        audioRecordingPanel = dialogView.findViewById(R.id.audio_recording_panel);
        audioPreviewPanel = dialogView.findViewById(R.id.audio_preview_panel);
        tvRecordingTimer = dialogView.findViewById(R.id.tv_recording_timer);
        tvAudioDuration = dialogView.findViewById(R.id.tv_audio_duration);
        btnStartRecording = dialogView.findViewById(R.id.btn_start_recording);
        btnStopRecording = dialogView.findViewById(R.id.btn_stop_recording);
        btnCancelRecording = dialogView.findViewById(R.id.btn_cancel_recording);
        btnPlayAudio = dialogView.findViewById(R.id.btn_play_audio);
        btnDeleteAudio = dialogView.findViewById(R.id.btn_delete_audio);
        Button btnSendAudio = dialogView.findViewById(R.id.btn_send_audio);

        final AlertDialog dialog = builder.create();

        // Bouton démarrer enregistrement
        btnStartRecording.setOnClickListener(v -> startAudioRecording());

        // Bouton arrêter enregistrement
        btnStopRecording.setOnClickListener(v -> stopAudioRecording());

        // Bouton annuler enregistrement
        btnCancelRecording.setOnClickListener(v -> {
            cancelAudioRecording();
            dialog.dismiss();
        });

        // Bouton écouter
        btnPlayAudio.setOnClickListener(v -> playAudio());

        // Bouton supprimer
        btnDeleteAudio.setOnClickListener(v -> deleteAudio());

        // Bouton envoyer
        btnSendAudio.setOnClickListener(v -> {
            sendAudioMessage();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Démarrer l'enregistrement audio
     */
    private void startAudioRecording() {
        try {
            // Créer le répertoire si nécessaire
            File audioDir = new File(getExternalFilesDir(null), "chat_audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            // Créer le fichier audio
            audioFilePath = new File(audioDir, "voice_msg_" + System.currentTimeMillis() + ".3gp").getAbsolutePath();

            // Configurer le MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            // Afficher le panneau d'enregistrement
            audioRecordingPanel.setVisibility(View.VISIBLE);
            audioPreviewPanel.setVisibility(View.GONE);
            btnStartRecording.setEnabled(false);
            btnStopRecording.setEnabled(true);

            // Démarrer le timer
            recordingHandler = new Handler(Looper.getMainLooper());
            updateRecordingTimer();

            android.util.Log.d(TAG, "Enregistrement audio démarré: " + audioFilePath);

        } catch (IOException e) {
            android.util.Log.e(TAG, "Erreur démarrage enregistrement", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Arrêter l'enregistrement audio
     */
    private void stopAudioRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                isRecording = false;

                // Calculer et sauvegarder la durée RÉELLE
                long elapsed = System.currentTimeMillis() - recordingStartTime;
                recordedDuration = (int) (elapsed / 1000);

                // Masquer le panneau d'enregistrement
                audioRecordingPanel.setVisibility(View.GONE);
                audioPreviewPanel.setVisibility(View.VISIBLE);
                tvAudioDuration.setText(formatDuration(recordedDuration));

                btnStartRecording.setEnabled(true);
                btnStopRecording.setEnabled(false);

                recordingHandler.removeCallbacksAndMessages(null);

                Toast.makeText(this, "Enregistrement terminé: " + formatDuration(recordedDuration), Toast.LENGTH_SHORT).show();

                android.util.Log.d(TAG, "Audio recorded: " + audioFilePath + " (duration: " + recordedDuration + "s)");

            } catch (Exception e) {
                android.util.Log.e(TAG, "Erreur arrêt enregistrement", e);
                Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Annuler l'enregistrement
     */
    private void cancelAudioRecording() {
        if (mediaRecorder != null) {
            try {
                if (isRecording) {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Erreur annulation", e);
            }
            mediaRecorder = null;
        }

        // Supprimer le fichier
        if (audioFilePath != null) {
            File file = new File(audioFilePath);
            if (file.exists()) {
                file.delete();
            }
            audioFilePath = null;
        }

        isRecording = false;
        recordedDuration = 0;

        if (recordingHandler != null) {
            recordingHandler.removeCallbacksAndMessages(null);
        }

        android.util.Log.d(TAG, "Enregistrement annulé");
    }

    /**
     * Supprimer l'audio enregistré
     */
    private void deleteAudio() {
        if (audioFilePath != null) {
            File file = new File(audioFilePath);
            if (file.exists()) {
                file.delete();
            }
            audioFilePath = null;
            recordedDuration = 0;
        }

        audioPreviewPanel.setVisibility(View.GONE);
        btnStartRecording.setEnabled(true);

        Toast.makeText(this, "Audio supprimé", Toast.LENGTH_SHORT).show();
    }

    /**
     * Lire l'audio enregistré
     */
    private void playAudio() {
        if (audioFilePath == null || !new File(audioFilePath).exists()) {
            Toast.makeText(this, "Aucun audio à lire", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            stopPlaying();
        } else {
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(audioFilePath);
                mediaPlayer.prepare();
                mediaPlayer.start();

                isPlaying = true;
                btnPlayAudio.setText("⏸️ Pause");

                mediaPlayer.setOnCompletionListener(mp -> {
                    isPlaying = false;
                    btnPlayAudio.setText("▶️ Écouter");
                    Toast.makeText(ChatActivity.this, "Lecture terminée", Toast.LENGTH_SHORT).show();
                });

                Toast.makeText(this, "Lecture en cours...", Toast.LENGTH_SHORT).show();
                android.util.Log.d(TAG, "Playing audio: " + audioFilePath);

            } catch (IOException e) {
                android.util.Log.e(TAG, "Erreur lecture audio", e);
                Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                isPlaying = false;
                btnPlayAudio.setText("▶️ Écouter");
            }
        }
    }

    /**
     * Arrêter la lecture
     */
    private void stopPlaying() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Erreur arrêt lecture", e);
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        btnPlayAudio.setText("▶️ Écouter");
    }

    /**
     * Mise à jour du timer d'enregistrement
     */
    private void updateRecordingTimer() {
        if (isRecording) {
            long elapsed = System.currentTimeMillis() - recordingStartTime;
            int seconds = (int) (elapsed / 1000);
            tvRecordingTimer.setText(formatDuration(seconds));
            recordingHandler.postDelayed(this::updateRecordingTimer, 1000);
        }
    }

    /**
     * Formater la durée en MM:SS
     */
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    /**
     * Envoyer le message audio
     */
    private void sendAudioMessage() {
        if (audioFilePath == null || !new File(audioFilePath).exists()) {
            Toast.makeText(this, "Aucun audio à envoyer", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recordedDuration == 0) {
            Toast.makeText(this, "Durée invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uploadManager == null || authToken == null) {
            Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = new File(audioFilePath);
        android.util.Log.d(TAG, "Envoi message audio: " + audioFile.getName() + " (" + audioFile.length() + " bytes, " + recordedDuration + "s)");

        Toast.makeText(this, "Envoi du message audio (" + formatDuration(recordedDuration) + ")...", Toast.LENGTH_SHORT).show();
        progressSending.setVisibility(View.VISIBLE);

        // Utiliser MediaUploadManager pour uploader l'audio
        uploadManager.upload(
            audioFile,
            MediaUploadManager.MediaType.AUDIO,
            MediaUploadManager.Context.CHAT,
            false, // Pas de miniature pour l'audio
            new MediaUploadManager.MediaUploadCallback() {
                @Override
                public void onSuccess(MediaUploadManager.MediaUploadResult result) {
                    runOnUiThread(() -> {
                        progressSending.setVisibility(View.GONE);

                        // Créer le message avec le chemin de l'audio uploadé
                        sendChatMessageWithAudio(result.getPath(), recordedDuration);

                        Toast.makeText(ChatActivity.this, "Message audio envoyé!", Toast.LENGTH_SHORT).show();

                        // Nettoyer après envoi
                        audioFilePath = null;
                        recordedDuration = 0;

                        // Recharger les messages
                        loadMessages();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        progressSending.setVisibility(View.GONE);
                        Toast.makeText(ChatActivity.this, "Erreur upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        android.util.Log.e(TAG, "Erreur upload audio", e);
                    });
                }

                @Override
                public void onProgress(int percent) {
                    runOnUiThread(() -> {
                        android.util.Log.d(TAG, "Upload audio: " + percent + "%");
                    });
                }
            }
        );
    }

    /**
     * Envoyer un message chat avec l'audio uploadé
     */
    private void sendChatMessageWithAudio(String audioPath, int duration) {
        if (apiService == null || authToken == null) {
            return;
        }

        // Créer le message avec type "audio"
        ApiService.SendMessageRequest request = new ApiService.SendMessageRequest(
            roomId,
            "🎤 Message vocal (" + formatDuration(duration) + ")"
        );
        // Note: Si l'API supporte les messages audio, ajouter les champs nécessaires ici
        // Par exemple: request.setMessageType("audio"); request.setAudioPath(audioPath);

        Call<ApiService.ChatSendResponse> call = apiService.sendChatMessage(authToken, request);
        call.enqueue(new Callback<ApiService.ChatSendResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatSendResponse> call, Response<ApiService.ChatSendResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.ChatSendResponse chatResponse = response.body();
                    if (chatResponse.success) {
                        android.util.Log.d(TAG, "Message audio envoyé avec succès");
                        if (chatResponse.chatMessage != null) {
                            messagesAdapter.addMessage(chatResponse.chatMessage);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.ChatSendResponse> call, Throwable t) {
                android.util.Log.e(TAG, "Échec envoi message audio", t);
            }
        });
    }

    // ========== FIN ENREGISTREMENT AUDIO ==========

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
            loadMessages();
            return true;
        } else if (item.getItemId() == R.id.action_room_info) {
            // Ouvrir l'activité des participants
            Intent intent = new Intent(this, ChatParticipantsActivity.class);
            intent.putExtra(ChatParticipantsActivity.EXTRA_ROOM_ID, roomId);
            intent.putExtra(ChatParticipantsActivity.EXTRA_ROOM_NAME, roomName);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Démarrer le polling quand l'activité est visible
        startPolling();
        android.util.Log.d("CHAT", "onResume - polling démarré");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Arrêter le polling quand l'activité n'est plus visible
        stopPolling();
        android.util.Log.d("CHAT", "onPause - polling arrêté");
    }

    @Override
    protected void onDestroy() {
        // Nettoyer les ressources audio
        if (mediaRecorder != null) {
            try {
                if (isRecording) {
                    mediaRecorder.stop();
                }
                mediaRecorder.release();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Erreur release mediaRecorder", e);
            }
            mediaRecorder = null;
        }

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Erreur release mediaPlayer", e);
            }
            mediaPlayer = null;
        }

        if (recordingHandler != null) {
            recordingHandler.removeCallbacksAndMessages(null);
        }

        // S'assurer que le polling est arrêté
        stopPolling();
        android.util.Log.d("CHAT", "onDestroy - polling arrêté");

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
