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
 * Activité pour gérer les notes d'un projet
 */
public class ProjectSelectorForNotesActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT_ID = "project_id";
    public static final String EXTRA_PROJECT_NAME = "project_name";

    private RecyclerView recyclerView;
    private ProjectNotesAdapter adapter;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private TextView tvTotalNotes, tvCategoryCount, tvImportantCount, tvEmptyMessage;
    private Button btnViewList, btnViewAgenda;
    private SessionManager sessionManager;
    private BidirectionalSyncManager syncManager;
    private int projectId;
    private String projectName;
    private List<ProjectNote> allNotes = new ArrayList<>();
    private List<ProjectNote> filteredNotes = new ArrayList<>();
    private String currentFilter = "all";
    private boolean isAgendaView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_notes_list);

        // Récupérer les extras
        projectId = getIntent().getIntExtra(EXTRA_PROJECT_ID, 0);
        projectName = getIntent().getStringExtra(EXTRA_PROJECT_NAME);

        if (projectId == 0) {
            Toast.makeText(this, "Erreur: projet non trouvé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialiser SessionManager et SyncManager
        sessionManager = new SessionManager(this);
        syncManager = new BidirectionalSyncManager(this);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📝 Notes");
            if (projectName != null) {
                getSupportActionBar().setSubtitle(projectName);
            }
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
                // Afficher les détails de la note
                showNoteDetails(note);
            }

            @Override
            public void onDeleteClick(ProjectNote note) {
                deleteNote(note.getId());
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // FAB pour ajouter une note
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ProjectSelectorForNotesActivity.this, NoteEditorActivity.class);
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

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0: currentFilter = "all"; break;
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
     * Configure les boutons de vue Liste/Agenda
     */
    private void setupViewButtons() {
        btnViewList.setBackgroundColor(0xFF2196F3); // Bleu = actif
        btnViewAgenda.setBackgroundColor(0xFFE0E0E0); // Gris = inactif

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
        // Ne rien faire - le chargement est fait dans onCreate()
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
     * Charge les notes depuis l'API et la base de données locale
     */
    private void loadNotes() {
        progressBar.setVisibility(View.VISIBLE);
        allNotes.clear();

        // Si ONLINE: charger du serveur ET synchroniser les notes locales non synchronisées
        if (com.ptms.mobile.utils.NetworkUtils.isOnline(this)) {
            // 1. Synchroniser d'abord les notes locales non synchronisées vers le serveur
            syncManager.syncFull(new BidirectionalSyncManager.SyncCallback() {
                @Override
                public void onSyncStarted(String phase) {
                    Log.d("ProjectNotes", "Synchronisation des notes en attente... Phase: " + phase);
                }

                @Override
                public void onSyncProgress(String message, int current, int total) {
                    Log.d("ProjectNotes", message + " (" + current + "/" + total + ")");
                }

                @Override
                public void onSyncCompleted(BidirectionalSyncManager.SyncResult result) {
                    Log.d("ProjectNotes", "Sync terminée: " + result.getSummary());
                    // 2. Après la sync, charger les données du serveur
                    loadNotesFromServer();
                }

                @Override
                public void onSyncError(String error) {
                    Log.e("ProjectNotes", "Erreur sync: " + error);
                    // Même en cas d'erreur, charger du serveur
                    loadNotesFromServer();
                }
            });
        } else {
            // Si OFFLINE: afficher TOUTES les notes locales (synchronisées ou non)
            // TODO: Implémenter getProjectNotes() dans OfflineDatabaseHelper
            List<ProjectNote> localNotes = new ArrayList<>();
            for (ProjectNote note : localNotes) {
                // Marquer les notes non synchronisées
                if (!note.isSynced()) {
                    note.setTitle((note.getTitle() != null ? note.getTitle() : "Sans titre") + " [Local]");
                }
                allNotes.add(note);
            }

            progressBar.setVisibility(View.GONE);
            filterNotes();
            Toast.makeText(this, "Mode hors ligne - Affichage des notes locales uniquement", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Filtre les notes selon la catégorie sélectionnée
     */
    private void filterNotes() {
        filteredNotes.clear();

        for (ProjectNote note : allNotes) {
            if (currentFilter.equals("all") ||
                (note.getNoteGroup() != null && note.getNoteGroup().equals(currentFilter))) {
                filteredNotes.add(note);
            }
        }

        // Trier par date (plus récentes en premier) si mode liste
        if (!isAgendaView) {
            // Mode liste normal
            adapter.notifyDataSetChanged();
        } else {
            // Mode agenda : TODO - regrouper par date
            // Pour l'instant, afficher simplement trié par date
            adapter.notifyDataSetChanged();
        }

        // Mettre à jour les statistiques
        updateStatistics();

        // Afficher/masquer le message vide
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
        // Total général
        tvTotalNotes.setText("Total: " + allNotes.size());

        // Catégorie actuelle
        tvCategoryCount.setText("Catégorie: " + filteredNotes.size());

        // Compter les notes importantes dans la catégorie actuelle
        int importantCount = 0;
        for (ProjectNote note : filteredNotes) {
            if (note.isImportant()) {
                importantCount++;
            }
        }
        tvImportantCount.setText("⭐ Important: " + importantCount);
    }

    /**
     * Charge les notes depuis le serveur
     */
    private void loadNotesFromServer() {
        String url = ApiManager.getBaseUrl() + "/api/project-notes.php?project_id=" + projectId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray notesArray = response.getJSONArray("notes");

                            // Ajouter les notes du serveur (après les notes locales)
                            for (int i = 0; i < notesArray.length(); i++) {
                                ProjectNote note = parseNote(notesArray.getJSONObject(i));
                                allNotes.add(note);

                                // ✅ Les notes sont automatiquement synchronisées dans SQLite par BidirectionalSyncManager
                                note.setSynced(true); // Note vient du serveur, donc synchronisée
                            }
                            filterNotes();

                            if (allNotes.isEmpty()) {
                                Toast.makeText(this, "Aucune note pour ce projet", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("ProjectNotes", "Chargé " + allNotes.size() + " note(s) depuis le serveur et mise en cache");
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

                    // ✅ FIX: En cas d'erreur réseau, charger les notes locales depuis le cache
                    Log.e("ProjectNotes", "Erreur réseau, chargement depuis cache local", error);
                    // TODO: Implémenter getProjectNotes() dans OfflineDatabaseHelper
                    List<ProjectNote> localNotes = new ArrayList<>();

                    if (localNotes != null && !localNotes.isEmpty()) {
                        for (ProjectNote note : localNotes) {
                            // Marquer les notes non synchronisées
                            if (!note.isSynced()) {
                                note.setTitle((note.getTitle() != null ? note.getTitle() : "Sans titre") + " [Local]");
                            }
                            allNotes.add(note);
                        }
                        filterNotes();
                        Toast.makeText(this, "Erreur réseau - Affichage de " + localNotes.size() + " note(s) locale(s)", Toast.LENGTH_SHORT).show();
                    } else {
                        filterNotes();
                        Toast.makeText(this, "Erreur réseau et pas de cache disponible", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = com.ptms.mobile.auth.TokenManager.getInstance(ProjectSelectorForNotesActivity.this).getToken();
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
        note.setNoteGroup(json.optString("noteGroup", "project")); // Catégorie de la note
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

        // ✅ AJOUT: Champs additionnels pour gestion complète des notes
        note.setPriority(json.optString("priority", "medium"));
        note.setScheduledDate(json.optString("scheduled_date", null));
        note.setReminderDate(json.optString("reminder_date", null));
        if (json.has("server_id") && !json.isNull("server_id")) {
            note.setServerId(json.getInt("server_id"));
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
                            loadNotes(); // Recharger la liste
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
                String token = com.ptms.mobile.auth.TokenManager.getInstance(ProjectSelectorForNotesActivity.this).getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };

        ApiManager.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Affiche les détails d'une note dans un dialog
     */
    private void showNoteDetails(ProjectNote note) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        // Titre
        String title = note.getTitle();
        if (title == null || title.isEmpty()) {
            title = "Note sans titre";
        }
        builder.setTitle(note.getTypeIcon() + " " + title);

        // Message avec le contenu
        StringBuilder message = new StringBuilder();

        if ("text".equals(note.getNoteType()) && note.getContent() != null) {
            message.append(note.getContent());
        } else if ("dictation".equals(note.getNoteType()) && note.getTranscription() != null) {
            message.append("📝 Transcription:\n\n");
            message.append(note.getTranscription());
        } else if ("audio".equals(note.getNoteType())) {
            message.append("🎵 Note audio\n\n");
            if (note.getAudioDuration() != null && note.getAudioDuration() > 0) {
                message.append("Durée: ").append(note.getFormattedDuration()).append("\n\n");
            }
            message.append("URL: ").append(ApiManager.getBaseUrl())
                   .append("/api/project-notes-audio.php?note_id=").append(note.getId());
        }

        message.append("\n\n---\n");
        message.append("👤 ").append(note.getAuthorName()).append("\n");
        message.append("📅 ").append(note.getCreatedAt());

        if (note.isImportant()) {
            message.append("\n⭐ Important");
        }

        if (note.getTags() != null && !note.getTags().isEmpty()) {
            message.append("\n\n🏷️ Tags: ").append(String.join(", ", note.getTags()));
        }

        builder.setMessage(message.toString());

        // Bouton pour ouvrir l'audio dans le navigateur (pour les notes audio)
        if ("audio".equals(note.getNoteType())) {
            builder.setPositiveButton("🎵 Écouter", (dialog, which) -> {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);

                    // Si la note a un chemin local (non synchronisée), utiliser le fichier local
                    if (note.getLocalAudioPath() != null && !note.getLocalAudioPath().isEmpty()) {
                        java.io.File audioFile = new java.io.File(note.getLocalAudioPath());
                        if (audioFile.exists()) {
                            android.net.Uri audioUri = android.net.Uri.fromFile(audioFile);
                            intent.setDataAndType(audioUri, "audio/*");
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Fichier audio local introuvable", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Sinon, charger depuis le serveur
                        String audioUrl = ApiManager.getBaseUrl() + "/api/project-notes-audio.php?note_id=" + note.getId();
                        intent.setDataAndType(android.net.Uri.parse(audioUrl), "audio/*");
                        startActivity(intent);
                    }
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(this, "Aucune application pour lire l'audio", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Erreur lecture audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        builder.setNegativeButton("Fermer", null);
        builder.show();
    }
}
