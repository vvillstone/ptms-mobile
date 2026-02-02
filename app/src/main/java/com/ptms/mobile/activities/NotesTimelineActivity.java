package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptms.mobile.R;
import com.ptms.mobile.adapters.AgendaNotesAdapter;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.auth.TokenManager;
import com.ptms.mobile.models.NoteType;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.sync.BidirectionalSyncManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité pour afficher l'agenda des notes avec calendrier
 */
public class NotesTimelineActivity extends AppCompatActivity implements AgendaNotesAdapter.OnNoteClickListener {

    private static final String TAG = "NotesAgenda";

    // UI Components
    private CalendarView calendarView;
    private AutoCompleteTextView spinnerFilterProject;
    private AutoCompleteTextView spinnerFilterCategory;
    private MaterialCheckBox checkFilterImportant;
    private TextView tvSelectedDate;
    private TextView tvNotesCount;
    private RecyclerView recyclerViewNotes;
    private LinearLayout layoutEmptyState;
    private FloatingActionButton fabCreateNote;

    // Data
    private BidirectionalSyncManager syncManager;
    private ApiService apiService;
    private String authToken;
    private AgendaNotesAdapter adapter;
    private List<ProjectNote> allNotes = new ArrayList<>();
    private List<Project> projects = new ArrayList<>();
    private List<NoteType> noteTypes = new ArrayList<>();

    // Filtres
    private String selectedDate;
    private String filterProjectName = "Tous";
    private String filterCategoryName = "Toutes";
    private boolean filterImportantOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_timeline);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📅 Agenda des Notes");
        }

        // Initialiser les services
        syncManager = new BidirectionalSyncManager(this);
        apiService = com.ptms.mobile.api.ApiClient.getInstance(this).getApiService();

        // ✅ Utiliser TokenManager centralisé au lieu de SharedPreferences direct
        TokenManager tokenManager = TokenManager.getInstance(this);
        authToken = tokenManager.getToken();

        // Initialiser les vues
        initViews();

        // Charger les données
        loadProjects();
        loadNoteTypes();

        // Date par défaut: aujourd'hui
        Calendar today = Calendar.getInstance();
        // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        selectedDate = dateFormat.format(today.getTime());
        updateSelectedDateDisplay();

        // Charger les notes
        loadNotesForDate(selectedDate);
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        spinnerFilterProject = findViewById(R.id.spinnerFilterProject);
        spinnerFilterCategory = findViewById(R.id.spinnerFilterCategory);
        checkFilterImportant = findViewById(R.id.checkFilterImportant);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNotesCount = findViewById(R.id.tvNotesCount);
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabCreateNote = findViewById(R.id.fabCreateNote);

        // Setup RecyclerView
        adapter = new AgendaNotesAdapter(this);
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotes.setAdapter(adapter);

        // Setup Listeners
        setupListeners();
    }

    private void setupListeners() {
        // Calendrier - changement de date
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            selectedDate = dateFormat.format(cal.getTime());
            updateSelectedDateDisplay();
            loadNotesForDate(selectedDate);
        });

        // Filtre Projet
        spinnerFilterProject.setOnItemClickListener((parent, view, position, id) -> {
            filterProjectName = spinnerFilterProject.getText().toString();
            applyFilters();
        });

        // Filtre Catégorie
        spinnerFilterCategory.setOnItemClickListener((parent, view, position, id) -> {
            filterCategoryName = spinnerFilterCategory.getText().toString();
            applyFilters();
        });

        // Filtre Important
        checkFilterImportant.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterImportantOnly = isChecked;
            applyFilters();
        });

        // FAB - Créer une note
        fabCreateNote.setOnClickListener(v -> {
            Intent intent = new Intent(NotesTimelineActivity.this, NoteEditorActivity.class);
            startActivity(intent);
        });
    }

    private void loadProjects() {
        projects = syncManager.getProjects();

        List<String> projectNames = new ArrayList<>();
        projectNames.add("Tous");
        for (Project project : projects) {
            projectNames.add(project.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, projectNames);
        spinnerFilterProject.setAdapter(adapter);
        spinnerFilterProject.setText("Tous", false);
    }

    private void loadNoteTypes() {
        // TODO: Implémenter getNoteTypes() dans BidirectionalSyncManager ou OfflineDatabaseHelper
        noteTypes = new ArrayList<>();

        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("Toutes");
        for (NoteType type : noteTypes) {
            categoryNames.add(type.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryNames);
        spinnerFilterCategory.setAdapter(adapter);
        spinnerFilterCategory.setText("Toutes", false);
    }

    private void loadNotesForDate(String date) {
        // Charger toutes les notes du serveur
        Call<ApiService.NotesResponse> call = apiService.getProjectNotes(
                "Bearer " + authToken,
                0  // 0 = toutes les notes
        );

        call.enqueue(new Callback<ApiService.NotesResponse>() {
            @Override
            public void onResponse(Call<ApiService.NotesResponse> call,
                                    Response<ApiService.NotesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    allNotes = response.body().notes;
                    applyFilters();
                } else {
                    Toast.makeText(NotesTimelineActivity.this,
                            "Erreur de chargement des notes", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Load notes failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiService.NotesResponse> call, Throwable t) {
                Toast.makeText(NotesTimelineActivity.this,
                        "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Load notes failed", t);
            }
        });
    }

    private void applyFilters() {
        List<ProjectNote> filteredNotes = new ArrayList<>();

        for (ProjectNote note : allNotes) {
            // Filtre par date
            if (!matchesDate(note, selectedDate)) {
                continue;
            }

            // Filtre par projet
            if (!filterProjectName.equals("Tous")) {
                if (note.getProjectName() == null ||
                        !note.getProjectName().equals(filterProjectName)) {
                    continue;
                }
            }

            // Filtre par catégorie
            if (!filterCategoryName.equals("Toutes")) {
                boolean matchesCategory = false;
                for (NoteType type : noteTypes) {
                    if (type.getName().equals(filterCategoryName)) {
                        if (note.getNoteTypeId() != null &&
                                note.getNoteTypeId() == type.getId()) {
                            matchesCategory = true;
                            break;
                        }
                        // Aussi vérifier par slug pour compatibilité
                        if (note.getNoteGroup() != null &&
                                note.getNoteGroup().equals(type.getSlug())) {
                            matchesCategory = true;
                            break;
                        }
                    }
                }
                if (!matchesCategory) {
                    continue;
                }
            }

            // Filtre par importance
            if (filterImportantOnly && !note.isImportant()) {
                continue;
            }

            filteredNotes.add(note);
        }

        // Mettre à jour l'affichage
        adapter.setNotes(filteredNotes);
        updateNotesCount(filteredNotes.size());

        // Afficher/masquer empty state
        if (filteredNotes.isEmpty()) {
            recyclerViewNotes.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewNotes.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private boolean matchesDate(ProjectNote note, String date) {
        if (note.getCreatedAt() == null) {
            return false;
        }
        // Extraire la date (YYYY-MM-DD) de createdAt (YYYY-MM-DD HH:MM:SS)
        String noteDate = note.getCreatedAt().substring(0, 10);
        return noteDate.equals(date);
    }

    private void updateSelectedDateDisplay() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH);
            String displayDate = outputFormat.format(inputFormat.parse(selectedDate));
            tvSelectedDate.setText(displayDate);
        } catch (Exception e) {
            tvSelectedDate.setText(selectedDate);
        }
    }

    private void updateNotesCount(int count) {
        if (count == 0) {
            tvNotesCount.setText("Aucune note");
        } else if (count == 1) {
            tvNotesCount.setText("1 note");
        } else {
            tvNotesCount.setText(count + " notes");
        }
    }

    @Override
    public void onNoteClick(ProjectNote note) {
        // TODO: Ouvrir une activité de détail/édition de note
        Toast.makeText(this,
                "Note: " + note.getTitle() + "\n(Détails à implémenter)",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les notes quand on revient sur l'activité
        loadNotesForDate(selectedDate);
    }
}
