package com.ptms.mobile.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ptms.mobile.R;
import com.ptms.mobile.adapters.ChatUsersAdapter;
import com.ptms.mobile.models.ChatUser;
import com.ptms.mobile.utils.ApiManager;
import com.ptms.mobile.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activité pour afficher les participants d'une conversation
 */
public class ChatParticipantsActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_ROOM_NAME = "room_name";

    private RecyclerView recyclerView;
    private ChatUsersAdapter adapter;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private int roomId;
    private String roomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_participants);

        // Récupérer les extras
        roomId = getIntent().getIntExtra(EXTRA_ROOM_ID, 0);
        roomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);

        if (roomId == 0) {
            Toast.makeText(this, "Erreur: salle non trouvée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialiser SessionManager
        sessionManager = new SessionManager(this);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("👥 Participants");
            if (roomName != null) {
                getSupportActionBar().setSubtitle(roomName);
            }
        }

        // Initialiser les vues
        recyclerView = findViewById(R.id.rv_participants);
        progressBar = findViewById(R.id.progress_bar);

        // Configurer le RecyclerView
        adapter = new ChatUsersAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Charger les participants
        loadParticipants();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Charge la liste des participants depuis l'API
     */
    private void loadParticipants() {
        progressBar.setVisibility(View.VISIBLE);

        String url = ApiManager.getBaseUrl() + "/api/chat-participants.php?room_id=" + roomId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray participantsArray = response.getJSONArray("participants");
                            List<ChatUser> participants = parseParticipants(participantsArray);
                            adapter.updateUsers(participants);

                            if (participants.isEmpty()) {
                                Toast.makeText(this, "Aucun participant", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String error = response.optString("error", "Erreur inconnue");
                            Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur de parsing", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Erreur réseau: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();

                // Ajouter le token JWT si disponible
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }

                // Ajouter le cookie de session
                String sessionCookie = sessionManager.getSessionCookie();
                if (sessionCookie != null && !sessionCookie.isEmpty()) {
                    headers.put("Cookie", sessionCookie);
                }

                return headers;
            }
        };

        ApiManager.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Parse le JSON des participants
     */
    private List<ChatUser> parseParticipants(JSONArray participantsArray) throws JSONException {
        List<ChatUser> participants = new ArrayList<>();

        for (int i = 0; i < participantsArray.length(); i++) {
            JSONObject participantObj = participantsArray.getJSONObject(i);

            ChatUser user = new ChatUser();
            user.setId(participantObj.getInt("user_id"));
            user.setUsername(participantObj.getString("username"));
            user.setFirstname(participantObj.optString("firstname", ""));
            user.setLastname(participantObj.optString("lastname", ""));
            user.setChatPseudo(participantObj.optString("chat_pseudo", ""));

            // Déterminer le nom d'affichage
            String displayName = participantObj.optString("name", user.getUsername());
            user.setDisplayName(displayName);

            participants.add(user);
        }

        return participants;
    }
}
