package com.ptms.mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptms.mobile.R;
import com.ptms.mobile.adapters.ProjectNotesAdapter;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.sync.BidirectionalSyncManager;
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
 * Activité pour afficher toutes les notes (sans filtre de projet)
 */
public class NotesListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProjectNotesAdapter adapter;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private TextView tvTotalNotes, tvCategoryCount, tvImportantCount, tvEmptyMessage;
    private Button btnViewList, btnViewAgenda;
    private SessionManager sessionManager;
    private BidirectionalSyncManager syncManager;
    private List<ProjectNote> allNotes = new ArrayList<>();
    private List<ProjectNote> filteredNotes = new ArrayList<>();
    private String currentFilter = "all";
    private boolean isAgendaView = false;
    private String initialFilter = null; // Filtre passé par Intent
    private boolean isLoading = false; // ✅ Flag pour éviter les chargements simultanés

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_notes_list);

        // Récupérer le filtre initial depuis l'Intent
        initialFilter = getIntent().getStringExtra("filter");
        if (initialFilter == null) {
            initialFilter = "all";
        }
        currentFilter = initialFilter;

        // Initialiser SessionManager et SyncManager
        sessionManager = new SessionManager(this);
        syncManager = new BidirectionalSyncManager(this);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getTitleForFilter(initialFilter));
        }

        // Initialiser les vues
        recyclerView = findViewById(R.id.rv_notes);
        progressBar = findViewById(R.id.progress_bar);
        fabAdd = findViewById(R.id.fab_add_note);
        tabLayout = findViewById(R.id.tab_layout);
        tvTotalNotes = findViewById(R.id.tv_total_notes);
        tvCategoryCount = findViewById(R.id.tv_category_count);
        tvImportantCount = findViewById(R.id.tv_important_count);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        btnViewList = findViewById(R.id.btn_view_list);
        btnViewAgenda = findViewById(R.id.btn_view_agenda);

        // Configurer le RecyclerView
        adapter = new ProjectNotesAdapter(this, filteredNotes, new ProjectNotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(ProjectNote note) {
                showNoteDetails(note);
            }

            @Override
            public void onDeleteClick(ProjectNote note) {
                deleteNote(note.getId());
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // FAB pour ajouter une note (sans projet)
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(NotesListActivity.this, NoteEditorActivity.class);
            startActivity(intent);
        });

        // Configurer les onglets
        setupTabs();

        // Configurer les boutons de vue
        setupViewButtons();

        // Charger les notes
        loadNotes();
    }

    /**
     * Retourne le titre selon le filtre
     */
    private String getTitleForFilter(String filter) {
        switch (filter) {
            case "personal":
                return "👤 Notes Personnelles";
            case "meeting":
                return "👥 Notes de Groupe";
            case "important":
                return "⭐ Notes Importantes";
            case "all":
            default:
                return "📝 Toutes les Notes";
        }
    }

    /**
     * Configure les onglets de catégories
     */
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Toutes"));
        tabLayout.addTab(tabLayout.newTab().setText("📁 Projet"));
        tabLayout.addTab(tabLayout.newTab().setText("👤 Personnel"));
        tabLayout.addTab(tabLayout.newTab().setText("🤝 Réunion"));
        tabLayout.addTab(tabLayout.newTab().setText("✅ TODO"));
        tabLayout.addTab(tabLayout.newTab().setText("💡 Idée"));
        tabLayout.addTab(tabLayout.newTab().setText("⚠️ Problème"));
        tabLayout.addTab(tabLayout.newTab().setText("📌 Autre"));

        // Sélectionner l'onglet initial
        selectInitialTab();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0: currentFilter = initialFilter.equals("important") ? "important" : "all"; break;
                    case 1: currentFilter = "project"; break;
                    case 2: currentFilter = "personal"; break;
                    case 3: currentFilter = "meeting"; break;
                    case 4: currentFilter = "todo"; break;
                    case 5: currentFilter = "idea"; break;
                    case 6: currentFilter = "issue"; break;
                    case 7: currentFilter = "other"; break;
                }
                filterNotes();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Sélectionne l'onglet initial selon le filtre
     */
    private void selectInitialTab() {
        int tabIndex = 0;
        switch (initialFilter) {
            case "project":
                tabIndex = 1;
                break;
            case "personal":
                tabIndex = 2;
                break;
            case "meeting":
                tabIndex = 3;
                break;
            case "important":
            case "all":
            default:
                tabIndex = 0;
                break;
        }

        TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
        if (tab != null) {
            tab.select();
        }
    }

    /**
     * Configure les boutons de vue Liste/Agenda
     */
    private void setupViewButtons() {
        btnViewList.setBackgroundColor(0xFF2196F3);
        btnViewAgenda.setBackgroundColor(0xFFE0E0E0);

        btnViewList.setOnClickListener(v -> {
            isAgendaView = false;
            btnViewList.setBackgroundColor(0xFF2196F3);
            btnViewAgenda.setBackgroundColor(0xFFE0E0E0);
            filterNotes();
        });

        btnViewAgenda.setOnClickListener(v -> {
            isAgendaView = true;
            btnViewList.setBackgroundColor(0xFFE0E0E0);
            btnViewAgenda.setBackgroundColor(0xFF2196F3);
            filterNotes();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Ne recharger que si la liste est vide (évite le double affichage)
        if (allNotes.isEmpty()) {
            loadNotes();
        }
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
     * Charge toutes les notes depuis l'API ou depuis le cache local
     * ✅ CORRIGÉ: Mode ONLINE = serveur uniquement (évite les doublons)
     * ✅ Mode OFFLINE = cache local uniquement
     * ✅ Protection contre les chargements simultanés
     */
    private void loadNotes() {
        // ✅ Éviter les chargements simultanés (cause de doublons)
        if (isLoading) {
            Log.d("AllNotesActivity", "⚠️ Chargement déjà en cours, ignoré");
            return;
        }

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        allNotes.clear();

        // Charger selon le mode de connexion
        if (com.ptms.mobile.utils.NetworkUtils.isOnline(this)) {
            // Mode ONLINE: Charger UNIQUEMENT depuis le serveur
            // Les notes offline sont automatiquement synchronisées avant l'affichage
            loadNotesFromServer();
        } else {
            // Mode OFFLINE: Charger UNIQUEMENT depuis la base de données locale
            loadNotesFromCache();
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Mode hors ligne - " + allNotes.size() + " notes en cache", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Charge les notes depuis le cache local (mode offline)
     * ✅ CORRIGÉ: Charge TOUTES les notes locales (pas seulement pending)
     */
    private void loadNotesFromCache() {
        try {
            int userId = sessionManager.getUserId();
            if (userId > 0) {
                // ✅ NOUVEAU: Utiliser getAllNotesByUserId() pour charger TOUTES les notes
                com.ptms.mobile.database.OfflineDatabaseHelper dbHelper =
                    new com.ptms.mobile.database.OfflineDatabaseHelper(this);
                List<ProjectNote> cachedNotes = dbHelper.getAllNotesByUserId(userId);

                if (cachedNotes != null && !cachedNotes.isEmpty()) {
                    allNotes.addAll(cachedNotes);
                    Log.d("AllNotesActivity", "Notes offline chargées: " + cachedNotes.size());
                }
            }
            filterNotes();
        } catch (Exception e) {
            Log.e("AllNotesActivity", "Erreur chargement notes offline", e);
            Toast.makeText(this, "Erreur chargement notes offline", Toast.LENGTH_SHORT).show();
        } finally {
            isLoading = false; // ✅ Libérer le flag
        }
    }

    /**
     * Filtre les notes selon la catégorie sélectionnée
     */
    private void filterNotes() {
        filteredNotes.clear();

        for (ProjectNote note : allNotes) {
            boolean matchesFilter = false;

            if (currentFilter.equals("all")) {
                matchesFilter = true;
            } else if (currentFilter.equals("important")) {
                matchesFilter = note.isImportant();
            } else {
                matchesFilter = note.getNoteGroup() != null && note.getNoteGroup().equals(currentFilter);
            }

            if (matchesFilter) {
                filteredNotes.add(note);
            }
        }

        adapter.notifyDataSetChanged();
        updateStatistics();

        if (filteredNotes.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Met à jour les statistiques affichées
     */
    private void updateStatistics() {
        tvTotalNotes.setText("Total: " + allNotes.size());
        tvCategoryCount.setText("Catégorie: " + filteredNotes.size());

        int importantCount = 0;
        for (ProjectNote note : filteredNotes) {
            if (note.isImportant()) {
                importantCount++;
            }
        }
        tvImportantCount.setText("⭐ Important: " + importantCount);
    }

    /**
     * Charge les notes depuis le serveur (sans filtre de projet)
     * ✅ CORRIGÉ: Charge UNIQUEMENT depuis le serveur (pas de fusion avec cache)
     * Les notes créées offline sont automatiquement synchronisées avant d'appeler cette méthode
     */
    private void loadNotesFromServer() {
        String url = ApiManager.getBaseUrl() + "/api/project-notes.php?all=1"; // ✅ Toutes les notes

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    isLoading = false; // ✅ Libérer le flag
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray notesArray = response.getJSONArray("notes");

                            // ✅ CORRIGÉ: Charger UNIQUEMENT les notes du serveur
                            List<ProjectNote> serverNotes = new ArrayList<>();
                            com.ptms.mobile.database.OfflineDatabaseHelper dbHelper =
                                new com.ptms.mobile.database.OfflineDatabaseHelper(NotesListActivity.this);

                            for (int i = 0; i < notesArray.length(); i++) {
                                ProjectNote note = parseNote(notesArray.getJSONObject(i));
                                serverNotes.add(note);

                                // ✅ NOUVEAU: Sauvegarder chaque note dans la base locale
                                // pour disponibilité en mode offline (upsert évite les doublons)
                                dbHelper.upsertNoteFromServer(note);
                            }

                            // ✅ CORRIGÉ: Ajouter uniquement les notes du serveur (pas de cache)
                            allNotes.addAll(serverNotes);

                            filterNotes();

                            if (allNotes.isEmpty()) {
                                Toast.makeText(this, "Aucune note", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("AllNotesActivity", "Notes online chargées: " + allNotes.size());
                            }
                        } else {
                            String error = response.optString("message", "Erreur inconnue");
                            Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur de parsing", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    isLoading = false; // ✅ Libérer le flag
                    // ✅ CORRIGÉ: En cas d'erreur réseau, charger TOUTES les notes locales
                    int userId = sessionManager.getUserId();
                    if (userId > 0) {
                        com.ptms.mobile.database.OfflineDatabaseHelper dbHelper =
                            new com.ptms.mobile.database.OfflineDatabaseHelper(this);
                        List<ProjectNote> cachedNotes = dbHelper.getAllNotesByUserId(userId);
                        allNotes.addAll(cachedNotes);
                        filterNotes();
                        Toast.makeText(this, "Erreur réseau - " + cachedNotes.size() + " notes en cache", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erreur réseau et pas de cache disponible", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };

        ApiManager.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Parse un objet JSON en ProjectNote
     */
    private ProjectNote parseNote(JSONObject json) throws JSONException {
        ProjectNote note = new ProjectNote();
        note.setId(json.getInt("id"));
        note.setProjectId(json.optInt("projectId", 0)); // Peut être 0 pour notes personnelles
        note.setProjectName(json.optString("projectName", null)); // Nom du projet (du serveur)
        note.setUserId(json.getInt("userId"));
        note.setNoteType(json.getString("noteType"));
        note.setNoteGroup(json.optString("noteGroup", "project"));
        note.setTitle(json.optString("title", null));
        note.setContent(json.optString("content", null)); // Contenu complet (pas de limitation)
        note.setAudioPath(json.optString("audioPath", null));
        note.setAudioDuration(json.optInt("audioDuration", 0));
        note.setTranscription(json.optString("transcription", null));
        note.setImportant(json.getBoolean("isImportant"));
        note.setAuthorName(json.getString("authorName"));
        note.setCreatedAt(json.getString("createdAt"));
        note.setUpdatedAt(json.optString("updatedAt", null));

        // Parse tags (peut être un array JSON)
        JSONArray tagsArray = json.optJSONArray("tags");
        if (tagsArray != null) {
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < tagsArray.length(); i++) {
                tags.add(tagsArray.getString(i));
            }
            note.setTags(tags);
        }

        // Marquer comme synchronisée (vient du serveur)
        note.setSynced(true);
        note.setSyncStatus("synced");

        return note;
    }

    /**
     * Supprime une note
     */
    private void deleteNote(int noteId) {
        String url = ApiManager.getBaseUrl() + "/api/project-notes.php?note_id=" + noteId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            Toast.makeText(this, "Note supprimée", Toast.LENGTH_SHORT).show();
                            loadNotes();
                        } else {
                            String error = response.optString("message", "Erreur");
                            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erreur: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };

        ApiManager.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Affiche les détails d'une note via NoteViewerActivity
     */
    private void showNoteDetails(ProjectNote note) {
        Intent intent = new Intent(this, NoteDetailActivity.class);

        // Passer toutes les données de la note via Intent extras
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_local_id", note.getLocalId());
        intent.putExtra("note_project_id", note.getProjectId());
        intent.putExtra("note_project_name", note.getProjectName());
        intent.putExtra("note_user_id", note.getUserId());
        intent.putExtra("note_type", note.getNoteType());
        intent.putExtra("note_group", note.getNoteGroup());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());
        intent.putExtra("note_transcription", note.getTranscription());
        intent.putExtra("note_audio_path", note.getAudioPath());
        intent.putExtra("note_audio_duration", note.getAudioDuration() != null ? note.getAudioDuration() : 0);
        intent.putExtra("note_important", note.isImportant());
        intent.putExtra("note_author", note.getAuthorName());
        intent.putExtra("note_created_at", note.getCreatedAt());
        intent.putExtra("note_updated_at", note.getUpdatedAt());

        // Tags (convertir liste en string)
        if (note.getTags() != null && !note.getTags().isEmpty()) {
            intent.putExtra("note_tags", String.join(",", note.getTags()));
        }

        startActivityForResult(intent, 100); // 100 = REQUEST_CODE_VIEW_NOTE
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            // La note a été supprimée ou modifiée, recharger la liste
            allNotes.clear();
            loadNotes();
        }
    }
}
