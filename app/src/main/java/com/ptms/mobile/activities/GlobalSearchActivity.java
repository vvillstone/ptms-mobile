package com.ptms.mobile.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.ptms.mobile.R;
import com.ptms.mobile.utils.SearchManager;
import com.ptms.mobile.utils.SearchManager.SearchCriteria;
import com.ptms.mobile.utils.SearchManager.SearchResults;
import com.ptms.mobile.utils.SearchManager.SearchType;
import com.ptms.mobile.utils.SearchManager.SortBy;
import com.ptms.mobile.adapters.SearchResultsAdapter;

import java.util.List;

/**
 * ✅ Activité de recherche avancée
 *
 * Fonctionnalités :
 * - Recherche en temps réel
 * - Filtres multiples (type, date, projet, tags)
 * - Suggestions intelligentes
 * - Historique de recherche
 * - Tri personnalisable
 * - Résultats groupés par catégorie
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class GlobalSearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";

    // UI Components
    private TextInputEditText editSearchQuery;
    private AutoCompleteTextView spinnerSearchType;
    private AutoCompleteTextView spinnerSortBy;
    private ChipGroup chipGroupFilters;
    private Chip chipProjects;
    private Chip chipNotes;
    private Chip chipReports;
    private Chip chipImportant;
    private RecyclerView recyclerResults;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private TextView textEmptyMessage;
    private TextView textResultCount;
    private Button buttonClearFilters;

    // Manager
    private SearchManager searchManager;

    // Adapter
    private SearchResultsAdapter resultsAdapter;

    // Critères actuels
    private SearchCriteria currentCriteria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_search);

        Log.d(TAG, "========================================");
        Log.d(TAG, "🔍 ACTIVITÉ DE RECHERCHE");
        Log.d(TAG, "========================================");

        // Initialiser le manager
        searchManager = new SearchManager(this);

        // Initialiser les critères par défaut
        currentCriteria = new SearchCriteria();
        currentCriteria.searchType = SearchType.ALL;
        currentCriteria.sortBy = SortBy.RELEVANCE;

        // Initialiser l'UI
        initializeViews();
        setupSearchInput();
        setupFilters();
        setupRecyclerView();

        // Charger l'historique de recherche
        loadSearchHistory();
    }

    /**
     * Initialise les vues
     */
    private void initializeViews() {
        editSearchQuery = findViewById(R.id.editSearchQuery);
        spinnerSearchType = findViewById(R.id.spinnerSearchType);
        spinnerSortBy = findViewById(R.id.spinnerSortBy);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        chipProjects = findViewById(R.id.chipProjects);
        chipNotes = findViewById(R.id.chipNotes);
        chipReports = findViewById(R.id.chipReports);
        chipImportant = findViewById(R.id.chipImportant);
        recyclerResults = findViewById(R.id.recyclerResults);
        progressBar = findViewById(R.id.progressBar);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        textEmptyMessage = findViewById(R.id.textEmptyMessage);
        textResultCount = findViewById(R.id.textResultCount);
        buttonClearFilters = findViewById(R.id.buttonClearFilters);

        // Titre
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Recherche avancée");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Configure le champ de recherche avec suggestions
     */
    private void setupSearchInput() {
        // Recherche en temps réel avec debounce
        editSearchQuery.addTextChangedListener(new TextWatcher() {
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Annuler la recherche précédente
                if (searchRunnable != null) {
                    editSearchQuery.removeCallbacks(searchRunnable);
                }

                // Attendre 500ms avant de lancer la recherche
                searchRunnable = () -> {
                    String query = s.toString().trim();
                    currentCriteria.query = query;
                    performSearch();
                };

                editSearchQuery.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Configure les filtres
     */
    private void setupFilters() {
        // Type de recherche
        String[] searchTypes = {"Tout", "Projets", "Notes", "Rapports"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            searchTypes
        );
        spinnerSearchType.setAdapter(typeAdapter);
        spinnerSearchType.setText("Tout", false);
        spinnerSearchType.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0: currentCriteria.searchType = SearchType.ALL; break;
                case 1: currentCriteria.searchType = SearchType.PROJECTS; break;
                case 2: currentCriteria.searchType = SearchType.NOTES; break;
                case 3: currentCriteria.searchType = SearchType.REPORTS; break;
            }
            performSearch();
        });

        // Tri
        String[] sortOptions = {"Pertinence", "Plus récent", "Plus ancien", "Titre A-Z", "Titre Z-A"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            sortOptions
        );
        spinnerSortBy.setAdapter(sortAdapter);
        spinnerSortBy.setText("Pertinence", false);
        spinnerSortBy.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0: currentCriteria.sortBy = SortBy.RELEVANCE; break;
                case 1: currentCriteria.sortBy = SortBy.DATE_DESC; break;
                case 2: currentCriteria.sortBy = SortBy.DATE_ASC; break;
                case 3: currentCriteria.sortBy = SortBy.TITLE_ASC; break;
                case 4: currentCriteria.sortBy = SortBy.TITLE_DESC; break;
            }
            performSearch();
        });

        // Filtres chips
        chipImportant.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentCriteria.importantOnly = isChecked;
            performSearch();
        });

        // Bouton effacer filtres
        buttonClearFilters.setOnClickListener(v -> clearFilters());
    }

    /**
     * Configure le RecyclerView
     */
    private void setupRecyclerView() {
        resultsAdapter = new SearchResultsAdapter(this);
        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerResults.setAdapter(resultsAdapter);
    }

    /**
     * Charge l'historique de recherche
     */
    private void loadSearchHistory() {
        List<String> history = searchManager.getSearchHistory();

        if (history.isEmpty()) {
            textEmptyMessage.setText("Aucune recherche récente\n\nCommencez à taper pour rechercher...");
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerResults.setVisibility(View.GONE);
        }
    }

    /**
     * Effectue la recherche
     */
    private void performSearch() {
        String query = currentCriteria.query;

        // Si query vide, afficher historique
        if (query == null || query.isEmpty()) {
            loadSearchHistory();
            return;
        }

        Log.d(TAG, "🔎 Lancement recherche: " + query);

        // Afficher le loader
        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        recyclerResults.setVisibility(View.GONE);

        // Effectuer la recherche en arrière-plan
        new Thread(() -> {
            try {
                SearchResults results = searchManager.search(currentCriteria);

                // Mettre à jour l'UI sur le thread principal
                runOnUiThread(() -> displayResults(results));

            } catch (Exception e) {
                Log.e(TAG, "Erreur recherche", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Erreur lors de la recherche", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Affiche les résultats
     */
    private void displayResults(SearchResults results) {
        progressBar.setVisibility(View.GONE);

        if (results.totalCount == 0) {
            // Aucun résultat
            textEmptyMessage.setText("Aucun résultat trouvé\n\nEssayez avec d'autres mots-clés");
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerResults.setVisibility(View.GONE);
            textResultCount.setVisibility(View.GONE);
        } else {
            // Afficher les résultats
            layoutEmptyState.setVisibility(View.GONE);
            recyclerResults.setVisibility(View.VISIBLE);
            textResultCount.setVisibility(View.VISIBLE);

            // Mettre à jour le compteur
            String countText = results.totalCount + " résultat" +
                              (results.totalCount > 1 ? "s" : "") + " trouvé" +
                              (results.totalCount > 1 ? "s" : "");
            textResultCount.setText(countText);

            // Mettre à jour l'adapter
            resultsAdapter.setResults(results);

            Log.d(TAG, "✅ Affichage de " + results.totalCount + " résultats");
        }
    }

    /**
     * Efface tous les filtres
     */
    private void clearFilters() {
        currentCriteria = new SearchCriteria();
        currentCriteria.searchType = SearchType.ALL;
        currentCriteria.sortBy = SortBy.RELEVANCE;

        editSearchQuery.setText("");
        spinnerSearchType.setText("Tout", false);
        spinnerSortBy.setText("Pertinence", false);
        chipImportant.setChecked(false);

        searchManager.clearCache();

        Toast.makeText(this, "Filtres effacés", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔴 Fermeture activité de recherche");
    }
}
