package com.ptms.mobile.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
 * Activité pour créer une nouvelle conversation (groupe, projet, département)
 */
public class NewConversationActivity extends AppCompatActivity {

    private Spinner spinnerType;
    private EditText etName;
    private EditText etDepartmentName;
    private Spinner spinnerProject;
    private LinearLayout layoutDepartment;
    private LinearLayout layoutProject;
    private RecyclerView rvUsers;
    private Button btnCreate;
    private ProgressBar progressBar;

    private ChatUsersAdapter usersAdapter;
    private SessionManager sessionManager;
    private List<JSONObject> projectsList = new ArrayList<>();
    private List<ChatUser> selectedUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_conversation);

        sessionManager = new SessionManager(this);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nouvelle conversation");
        }

        // Initialiser les vues
        spinnerType = findViewById(R.id.spinner_type);
        etName = findViewById(R.id.et_name);
        etDepartmentName = findViewById(R.id.et_department_name);
        spinnerProject = findViewById(R.id.spinner_project);
        layoutDepartment = findViewById(R.id.layout_department);
        layoutProject = findViewById(R.id.layout_project);
        rvUsers = findViewById(R.id.rv_users);
        btnCreate = findViewById(R.id.btn_create);
        progressBar = findViewById(R.id.progress_bar);

        // Configurer le spinner de type
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Groupe", "Projet", "Département"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // Listener pour changer les champs selon le type
        spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateFieldsVisibility(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Configurer le RecyclerView des utilisateurs
        usersAdapter = new ChatUsersAdapter(this);
        usersAdapter.setSelectable(true);
        usersAdapter.setOnUserClickListener(user -> {
            // Toggle selection
            if (selectedUsers.contains(user)) {
                selectedUsers.remove(user);
            } else {
                selectedUsers.add(user);
            }
        });
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(usersAdapter);

        // Bouton créer
        btnCreate.setOnClickListener(v -> createConversation());

        // Charger les données
        loadUsers();
        loadProjects();
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
     * Met à jour la visibilité des champs selon le type sélectionné
     */
    private void updateFieldsVisibility(int typePosition) {
        switch (typePosition) {
            case 0: // Groupe
                layoutProject.setVisibility(View.GONE);
                layoutDepartment.setVisibility(View.GONE);
                break;
            case 1: // Projet
                layoutProject.setVisibility(View.VISIBLE);
                layoutDepartment.setVisibility(View.GONE);
                break;
            case 2: // Département
                layoutProject.setVisibility(View.GONE);
                layoutDepartment.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Charge la liste des utilisateurs
     */
    private void loadUsers() {
        String url = ApiManager.getBaseUrl() + "/api/chat-users.php";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray usersArray = response.getJSONArray("users");
                            List<ChatUser> users = parseUsers(usersArray);
                            usersAdapter.updateUsers(users);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erreur chargement utilisateurs", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        ApiManager.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Charge la liste des projets
     */
    private void loadProjects() {
        String url = ApiManager.getBaseUrl() + "/api/projects.php";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray projectsArray = response.getJSONArray("projects");
                            projectsList.clear();

                            List<String> projectNames = new ArrayList<>();
                            for (int i = 0; i < projectsArray.length(); i++) {
                                JSONObject project = projectsArray.getJSONObject(i);
                                projectsList.add(project);
                                projectNames.add(project.getString("name"));
                            }

                            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                    android.R.layout.simple_spinner_item, projectNames);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerProject.setAdapter(adapter);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erreur chargement projets", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        ApiManager.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Crée la conversation
     */
    private void createConversation() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Le nom est requis", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "Sélectionnez au moins un participant", Toast.LENGTH_SHORT).show();
            return;
        }

        int typePosition = spinnerType.getSelectedItemPosition();
        String type;
        switch (typePosition) {
            case 0:
                type = "group";
                break;
            case 1:
                type = "project";
                break;
            case 2:
                type = "department";
                break;
            default:
                type = "group";
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("type", type);
            requestBody.put("name", name);

            // Ajouter project_id si type projet
            if (type.equals("project") && spinnerProject.getSelectedItemPosition() >= 0) {
                JSONObject selectedProject = projectsList.get(spinnerProject.getSelectedItemPosition());
                requestBody.put("project_id", selectedProject.getInt("id"));
            }

            // Ajouter department_name si type département
            if (type.equals("department")) {
                String deptName = etDepartmentName.getText().toString().trim();
                if (!deptName.isEmpty()) {
                    requestBody.put("department_name", deptName);
                }
            }

            // Ajouter les participants
            JSONArray participantsArray = new JSONArray();
            for (ChatUser user : selectedUsers) {
                participantsArray.put(user.getId());
            }
            requestBody.put("participants", participantsArray);

            progressBar.setVisibility(View.VISIBLE);
            btnCreate.setEnabled(false);

            String url = ApiManager.getBaseUrl() + "/api/chat/conversations/create";

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        btnCreate.setEnabled(true);

                        try {
                            if (response.getBoolean("success")) {
                                Toast.makeText(this, "Conversation créée!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
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
                        btnCreate.setEnabled(true);
                        Toast.makeText(this, "Erreur réseau: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return getAuthHeaders();
                }
            };

            ApiManager.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur de création", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Parse le JSON des utilisateurs
     */
    private List<ChatUser> parseUsers(JSONArray usersArray) throws JSONException {
        List<ChatUser> users = new ArrayList<>();

        for (int i = 0; i < usersArray.length(); i++) {
            JSONObject userObj = usersArray.getJSONObject(i);

            ChatUser user = new ChatUser();
            user.setId(userObj.getInt("id"));
            user.setUsername(userObj.getString("username"));
            user.setFirstname(userObj.optString("firstname", ""));
            user.setLastname(userObj.optString("lastname", ""));
            user.setChatPseudo(userObj.optString("chat_pseudo", ""));

            String displayName = userObj.optString("name", user.getUsername());
            user.setDisplayName(displayName);

            users.add(user);
        }

        return users;
    }

    /**
     * Retourne les headers d'authentification
     */
    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();

        String token = sessionManager.getAuthToken();
        if (token != null && !token.isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }

        String sessionCookie = sessionManager.getSessionCookie();
        if (sessionCookie != null && !sessionCookie.isEmpty()) {
            headers.put("Cookie", sessionCookie);
        }

        headers.put("Content-Type", "application/json");

        return headers;
    }
}
