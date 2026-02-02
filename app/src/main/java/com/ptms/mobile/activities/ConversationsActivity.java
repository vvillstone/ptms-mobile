package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.adapters.ChatRoomsAdapter;
import com.ptms.mobile.adapters.ChatRoomsGroupedAdapter;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.ChatRoom;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité pour afficher la liste des salles de chat
 */
public class ConversationsActivity extends AppCompatActivity implements ChatRoomsAdapter.OnChatRoomClickListener {
    
    private Toolbar toolbar;
    private RecyclerView recyclerChatRooms;
    private LinearLayout emptyState;
    private Button btnRefresh;
    private ProgressBar progressLoading;
    
    private ChatRoomsAdapter chatRoomsAdapter;
    private ChatRoomsGroupedAdapter chatRoomsGroupedAdapter;
    private ApiService apiService;
    private SharedPreferences prefs;
    private String authToken;
    private com.ptms.mobile.utils.SettingsManager settingsManager;
    private boolean useGroupedView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);
        
        try {
            // Initialisation
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            apiService = ApiClient.getInstance(this).getApiService();
            authToken = prefs.getString("auth_token", null);
            settingsManager = new com.ptms.mobile.utils.SettingsManager(this);
            useGroupedView = settingsManager.isChatGroupedView();

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
            loadChatRooms();
            
        } catch (Exception e) {
            android.util.Log.e("CHAT_ROOMS", "Erreur dans onCreate", e);
            Toast.makeText(this, "Erreur initialisation salles de chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerChatRooms = findViewById(R.id.recycler_chat_rooms);
        emptyState = findViewById(R.id.empty_state);
        btnRefresh = findViewById(R.id.btn_refresh);
        progressLoading = findViewById(R.id.progress_loading);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.chat_rooms));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupRecyclerView() {
        recyclerChatRooms.setLayoutManager(new LinearLayoutManager(this));

        if (useGroupedView) {
            // Vue regroupée par type
            chatRoomsGroupedAdapter = new ChatRoomsGroupedAdapter(this);
            chatRoomsGroupedAdapter.setOnChatRoomClickListener(this);
            recyclerChatRooms.setAdapter(chatRoomsGroupedAdapter);
        } else {
            // Vue liste simple
            chatRoomsAdapter = new ChatRoomsAdapter(this);
            chatRoomsAdapter.setOnChatRoomClickListener(this);
            recyclerChatRooms.setAdapter(chatRoomsAdapter);
        }
    }
    
    private void setupListeners() {
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadChatRooms();
            }
        });
    }
    
    private void loadChatRooms() {
        if (apiService == null || authToken == null) {
            android.util.Log.w("CHAT_ROOMS", "API Service ou token null");
            showEmptyState();
            return;
        }
        
        android.util.Log.d("CHAT_ROOMS", "Chargement des salles de chat...");
        
        progressLoading.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        
        Call<ApiService.ChatRoomsResponse> call = apiService.getChatRooms(authToken);
        call.enqueue(new Callback<ApiService.ChatRoomsResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatRoomsResponse> call, Response<ApiService.ChatRoomsResponse> response) {
                progressLoading.setVisibility(View.GONE);
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.ChatRoomsResponse chatResponse = response.body();
                        if (chatResponse.success && chatResponse.rooms != null) {
                            List<ChatRoom> chatRooms = chatResponse.rooms;
                            android.util.Log.d("CHAT_ROOMS", "Salles de chat chargées: " + chatRooms.size());
                            
                            if (chatRooms.isEmpty()) {
                                showEmptyState();
                            } else {
                                if (useGroupedView) {
                                    chatRoomsGroupedAdapter.updateChatRooms(chatRooms);
                                } else {
                                    chatRoomsAdapter.updateChatRooms(chatRooms);
                                }
                                showChatRooms();
                            }
                        } else {
                            android.util.Log.e("CHAT_ROOMS", "Erreur API: " + chatResponse.message);
                            Toast.makeText(ConversationsActivity.this, "Erreur: " + chatResponse.message, Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    } else {
                        android.util.Log.e("CHAT_ROOMS", "Erreur chargement salles: " + response.code() + " " + response.message());
                        Toast.makeText(ConversationsActivity.this, "Erreur chargement salles: " + response.code(), Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                } catch (Exception e) {
                    android.util.Log.e("CHAT_ROOMS", "Erreur dans onResponse", e);
                    showEmptyState();
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ChatRoomsResponse> call, Throwable t) {
                progressLoading.setVisibility(View.GONE);
                android.util.Log.e("CHAT_ROOMS", "Échec chargement salles", t);
                Toast.makeText(ConversationsActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }
    
    private void showChatRooms() {
        recyclerChatRooms.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }
    
    private void showEmptyState() {
        recyclerChatRooms.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onChatRoomClick(ChatRoom chatRoom) {
        android.util.Log.d("CHAT_ROOMS", "Clic sur la salle: " + chatRoom.getName());

        // Déterminer quelle version du chat utiliser
        int chatVersion = settingsManager.getChatVersion();
        Intent intent;

        if (chatVersion == com.ptms.mobile.utils.SettingsManager.CHAT_VERSION_WEBSOCKET) {
            // Version 2: WebSocket (Temps Réel)
            android.util.Log.d("CHAT_ROOMS", "Ouverture Chat V2 (WebSocket)");
            intent = new Intent(this, ChatWebSocketActivity.class);
        } else {
            // Version 1: Polling HTTP (Par défaut)
            android.util.Log.d("CHAT_ROOMS", "Ouverture Chat V1 (Polling)");
            intent = new Intent(this, ChatActivity.class);
        }

        // Passer seulement les données essentielles pour éviter problèmes de sérialisation
        intent.putExtra("ROOM_ID", chatRoom.getId());
        intent.putExtra("ROOM_NAME", chatRoom.getName());
        intent.putExtra("ROOM_TYPE", chatRoom.getType());
        startActivity(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_rooms_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            loadChatRooms();
            return true;
        } else if (item.getItemId() == R.id.action_new_chat) {
            // Ouvrir la liste des utilisateurs pour conversation privée
            startActivity(new Intent(this, ChatUsersActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_new_group) {
            // Ouvrir l'activité pour créer une conversation groupe/projet/département
            startActivity(new Intent(this, NewConversationActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les salles de chat quand on revient sur cette activité
        // pour avoir les dernières informations (messages non lus, etc.)
        loadChatRooms();
    }
}
