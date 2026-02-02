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
import com.ptms.mobile.adapters.ChatUsersAdapter;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.ChatUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité pour sélectionner un utilisateur et démarrer une conversation
 */
public class ChatUsersActivity extends AppCompatActivity implements ChatUsersAdapter.OnUserClickListener {
    
    private Toolbar toolbar;
    private RecyclerView recyclerUsers;
    private LinearLayout emptyState;
    private Button btnRefresh;
    private ProgressBar progressLoading;
    
    private ChatUsersAdapter usersAdapter;
    private ApiService apiService;
    private SharedPreferences prefs;
    private String authToken;
    private int currentUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_users);
        
        try {
            // Initialisation
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            apiService = ApiClient.getInstance(this).getApiService();
            authToken = prefs.getString("auth_token", null);
            currentUserId = prefs.getInt("employee_id", -1);
            
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
            loadUsers();
            
        } catch (Exception e) {
            android.util.Log.e("CHAT_USERS", "Erreur dans onCreate", e);
            Toast.makeText(this, "Erreur initialisation: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerUsers = findViewById(R.id.recycler_users);
        emptyState = findViewById(R.id.empty_state);
        btnRefresh = findViewById(R.id.btn_refresh);
        progressLoading = findViewById(R.id.progress_loading);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Nouvelle conversation");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupRecyclerView() {
        usersAdapter = new ChatUsersAdapter(this);
        usersAdapter.setOnUserClickListener(this);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(usersAdapter);
    }
    
    private void setupListeners() {
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUsers();
            }
        });
    }
    
    private void loadUsers() {
        if (apiService == null || authToken == null) {
            android.util.Log.w("CHAT_USERS", "API Service ou token null");
            showEmptyState();
            return;
        }
        
        android.util.Log.d("CHAT_USERS", "Chargement des utilisateurs...");
        
        progressLoading.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        
        Call<ApiService.ChatUsersResponse> call = apiService.getChatUsers(authToken);
        call.enqueue(new Callback<ApiService.ChatUsersResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatUsersResponse> call, Response<ApiService.ChatUsersResponse> response) {
                progressLoading.setVisibility(View.GONE);
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.ChatUsersResponse usersResponse = response.body();
                        if (usersResponse.success && usersResponse.users != null) {
                            List<ChatUser> users = usersResponse.users;
                            android.util.Log.d("CHAT_USERS", "Utilisateurs chargés: " + users.size());
                            
                            if (users.isEmpty()) {
                                showEmptyState();
                            } else {
                                usersAdapter.updateUsers(users);
                                showUsers();
                            }
                        } else {
                            android.util.Log.e("CHAT_USERS", "Erreur API: " + usersResponse.message);
                            Toast.makeText(ChatUsersActivity.this, "Erreur: " + usersResponse.message, Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    } else {
                        android.util.Log.e("CHAT_USERS", "Erreur chargement: " + response.code() + " " + response.message());
                        Toast.makeText(ChatUsersActivity.this, "Erreur chargement: " + response.code(), Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                } catch (Exception e) {
                    android.util.Log.e("CHAT_USERS", "Erreur dans onResponse", e);
                    showEmptyState();
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ChatUsersResponse> call, Throwable t) {
                progressLoading.setVisibility(View.GONE);
                android.util.Log.e("CHAT_USERS", "Échec chargement", t);
                Toast.makeText(ChatUsersActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }
    
    private void showUsers() {
        recyclerUsers.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }
    
    private void showEmptyState() {
        recyclerUsers.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onUserClick(ChatUser user) {
        android.util.Log.d("CHAT_USERS", "Clic sur l'utilisateur: " + user.getName());
        
        // Créer ou obtenir une conversation avec cet utilisateur
        createOrGetConversation(user);
    }
    
    private void createOrGetConversation(final ChatUser user) {
        progressLoading.setVisibility(View.VISIBLE);
        
        // Appel à l'API pour créer ou obtenir une conversation
        Call<ApiService.ChatConversationResponse> call = apiService.createOrGetConversation(authToken, user.getId());
        call.enqueue(new Callback<ApiService.ChatConversationResponse>() {
            @Override
            public void onResponse(Call<ApiService.ChatConversationResponse> call, Response<ApiService.ChatConversationResponse> response) {
                progressLoading.setVisibility(View.GONE);
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.ChatConversationResponse convResponse = response.body();
                        if (convResponse.success && convResponse.conversationId > 0) {
                            // Ouvrir l'activité de chat
                            Intent intent = new Intent(ChatUsersActivity.this, ChatWebSocketActivity.class);
                            intent.putExtra(ChatWebSocketActivity.EXTRA_ROOM_ID, convResponse.conversationId);
                            intent.putExtra(ChatWebSocketActivity.EXTRA_ROOM_NAME, user.getName());
                            startActivity(intent);
                            finish(); // Fermer cette activité
                        } else {
                            Toast.makeText(ChatUsersActivity.this, "Erreur: " + convResponse.message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ChatUsersActivity.this, "Erreur création conversation: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("CHAT_USERS", "Erreur dans onResponse", e);
                    Toast.makeText(ChatUsersActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ChatConversationResponse> call, Throwable t) {
                progressLoading.setVisibility(View.GONE);
                android.util.Log.e("CHAT_USERS", "Échec création conversation", t);
                Toast.makeText(ChatUsersActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

