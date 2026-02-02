package com.ptms.mobile.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.ptms.mobile.R;
import com.ptms.mobile.adapters.ReportsPagerAdapter;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.sync.BidirectionalSyncManager;
import com.ptms.mobile.sync.SyncStateManager;
import com.ptms.mobile.utils.ReportGrouper;
import com.ptms.mobile.utils.FileLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité de consultation des rapports avec regroupements améliorés
 * Version améliorée avec statistiques, filtres et vues multiples (Jour/Semaine/Mois)
 */
public class TimeReportsActivity extends AppCompatActivity {

    // Views
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView tvNoReports;
    private EditText etSearch;
    private Button btnFilterPeriod, btnFilterProject, btnFilterStatus;

    // Statistics views
    private TextView tvTotalHours, tvReportCount, tvAverageHours;

    // Data
    private List<TimeReport> allReports = new ArrayList<>();
    private List<TimeReport> filteredReports = new ArrayList<>();
    private SharedPreferences prefs;
    private ApiService apiService;
    private BidirectionalSyncManager syncManager;

    // Adapter
    private ReportsPagerAdapter pagerAdapter;

    // Sync state management
    private Menu optionsMenu;
    private BroadcastReceiver syncStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SyncStateManager.ACTION_SYNC_STATE_CHANGED.equals(intent.getAction())) {
                boolean isSyncing = intent.getBooleanExtra(SyncStateManager.EXTRA_IS_SYNCING, false);
                updateSyncButtonState(isSyncing);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            FileLogger.separator();
            FileLogger.d("REPORTS_ENH", "========== DÉMARRAGE ReportsEnhancedActivity ==========");

            FileLogger.d("REPORTS_ENH", "1. setContentView...");
            setContentView(R.layout.activity_time_reports);
            FileLogger.d("REPORTS_ENH", "1. ✅ setContentView OK");

            // Initialisation
            FileLogger.d("REPORTS_ENH", "2. Initialisation SharedPreferences...");
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            FileLogger.d("REPORTS_ENH", "2. ✅ SharedPreferences OK");

            FileLogger.d("REPORTS_ENH", "3. Initialisation syncManager...");
            syncManager = new BidirectionalSyncManager(this);
            FileLogger.d("REPORTS_ENH", "3. ✅ syncManager OK");

            // Configuration toolbar
            FileLogger.d("REPORTS_ENH", "4. Configuration toolbar...");
            setupToolbar();
            FileLogger.d("REPORTS_ENH", "4. ✅ Toolbar OK");

            // Initialisation des vues
            FileLogger.d("REPORTS_ENH", "5. Initialisation vues...");
            initViews();
            FileLogger.d("REPORTS_ENH", "5. ✅ Vues OK");

            // Configuration API
            FileLogger.d("REPORTS_ENH", "6. Configuration API...");
            setupApiService();
            FileLogger.d("REPORTS_ENH", "6. ✅ API OK");

            // Configuration ViewPager + Tabs
            FileLogger.d("REPORTS_ENH", "7. Configuration ViewPager...");
            setupViewPager();
            FileLogger.d("REPORTS_ENH", "7. ✅ ViewPager OK");

            // Configuration des listeners
            FileLogger.d("REPORTS_ENH", "8. Configuration listeners...");
            setupListeners();
            FileLogger.d("REPORTS_ENH", "8. ✅ Listeners OK");

            // Chargement des données (90 jours comme l'ancienne interface)
            FileLogger.d("REPORTS_ENH", "9. Chargement données...");
            reconcileMissingReports();
            FileLogger.d("REPORTS_ENH", "9. ✅ Chargement lancé");

            FileLogger.d("REPORTS_ENH", "========== onCreate TERMINÉ AVEC SUCCÈS ==========");
            FileLogger.separator();

        } catch (Exception e) {
            FileLogger.e("REPORTS_ENH", "❌ CRASH dans onCreate", e);
            Toast.makeText(this, "❌ Erreur: " + e.getMessage() + "\nLog: " + FileLogger.getLogFilePath(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Mes Rapports");
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("REPORTS_ENH", "Erreur configuration toolbar", e);
        }
    }

    private void initViews() {
        // ViewPager et Tabs
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        progressBar = findViewById(R.id.progress_bar);
        tvNoReports = findViewById(R.id.tv_no_reports);

        // Statistiques
        tvTotalHours = findViewById(R.id.tv_total_hours);
        tvReportCount = findViewById(R.id.tv_report_count);
        tvAverageHours = findViewById(R.id.tv_average_hours);

        // Recherche et filtres
        etSearch = findViewById(R.id.et_search);
        btnFilterPeriod = findViewById(R.id.btn_filter_period);
        btnFilterProject = findViewById(R.id.btn_filter_project);
        btnFilterStatus = findViewById(R.id.btn_filter_status);
    }

    private void setupApiService() {
        try {
            ApiClient apiClient = ApiClient.getInstance(this);
            apiService = apiClient.getApiService();
            android.util.Log.d("REPORTS_ENH", "API Service configuré");
        } catch (Exception e) {
            android.util.Log.e("REPORTS_ENH", "Erreur setupApiService", e);
            Toast.makeText(this, "Erreur configuration API: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupViewPager() {
        // Créer l'adapter pour le ViewPager
        pagerAdapter = new ReportsPagerAdapter(this, filteredReports);
        viewPager.setAdapter(pagerAdapter);

        // Lier TabLayout et ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("📅 Jour");
                    break;
                case 1:
                    tab.setText("📆 Semaine");
                    break;
                case 2:
                    tab.setText("🗓️ Mois");
                    break;
            }
        }).attach();
    }

    private void setupListeners() {
        // Recherche en temps réel
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReports(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Boutons de filtres
        btnFilterPeriod.setOnClickListener(v -> showPeriodFilter());
        btnFilterProject.setOnClickListener(v -> showProjectFilter());
        btnFilterStatus.setOnClickListener(v -> showStatusFilter());
    }

    private void loadReports() {
        try {
            setLoading(true);

            // D'abord charger les rapports locaux (fichiers JSON)
            loadLocalReports();

            String token = prefs.getString("auth_token", "");
            if (token == null || token.isEmpty()) {
                setLoading(false);
                Toast.makeText(this, "Session expirée - Utilisation des données locales uniquement", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérifier que l'API service est disponible
            if (apiService == null) {
                android.util.Log.w("REPORTS_ENH", "API Service non disponible - Mode offline uniquement");
                setLoading(false);
                Toast.makeText(this, "⚠️ Mode offline - Affichage des rapports locaux uniquement", Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("REPORTS_ENH", "Chargement des rapports serveur...");

            // Charger les rapports des 90 derniers jours
            Calendar calendar = Calendar.getInstance();
            // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            String dateTo = dateFormat.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_YEAR, -90);
            String dateFrom = dateFormat.format(calendar.getTime());

            android.util.Log.d("REPORTS_ENH", "Période: " + dateFrom + " à " + dateTo);

            Call<List<TimeReport>> call = apiService.getReports(token, dateFrom, dateTo, null);
            call.enqueue(new Callback<List<TimeReport>>() {
                @Override
                public void onResponse(Call<List<TimeReport>> call, Response<List<TimeReport>> response) {
                    setLoading(false);

                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            List<TimeReport> serverReports = response.body();

                            // Marquer tous les rapports serveur comme synchronisés
                            for (TimeReport report : serverReports) {
                                report.setSynced(true);
                                report.setLocal(false);
                            }

                            // Ajouter les rapports serveur
                            allReports.addAll(serverReports);

                            android.util.Log.d("REPORTS_ENH", "Total rapports: " + allReports.size());

                            // Mettre à jour l'affichage
                            refreshDisplay();
                        } else {
                            android.util.Log.e("REPORTS_ENH", "Erreur API: " + response.code());
                            Toast.makeText(TimeReportsActivity.this, "❌ Erreur chargement rapports serveur", Toast.LENGTH_SHORT).show();
                            refreshDisplay();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("REPORTS_ENH", "Erreur dans onResponse", e);
                        refreshDisplay();
                    }
                }

                @Override
                public void onFailure(Call<List<TimeReport>> call, Throwable t) {
                    setLoading(false);
                    android.util.Log.e("REPORTS_ENH", "Échec chargement rapports serveur", t);
                    Toast.makeText(TimeReportsActivity.this, "⚠️ Erreur réseau - Affichage des rapports locaux uniquement", Toast.LENGTH_LONG).show();
                    refreshDisplay();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("REPORTS_ENH", "Erreur loadReports", e);
            setLoading(false);
            Toast.makeText(this, "❌ Erreur chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            refreshDisplay();
        }
    }

    /**
     * Charge les rapports locaux depuis SQLite
     */
    private void loadLocalReports() {
        try {
            android.util.Log.d("REPORTS_ENH", "Chargement des rapports locaux...");

            // ✅ Charger depuis SQLite via BidirectionalSyncManager
            // Note: On charge tous les rapports en attente (pas encore synchronisés)
            List<TimeReport> localReports = syncManager.getOfflineDatabaseHelper().getAllPendingTimeReports();

            allReports.clear();
            for (TimeReport report : localReports) {
                report.setLocal(true);
                report.setSynced(report.getServerId() > 0); // Si serverId existe, c'est synchronisé
                allReports.add(report);
            }

            android.util.Log.d("REPORTS_ENH", "Rapports locaux chargés: " + allReports.size());

            // Initialiser filteredReports avec les rapports locaux
            filteredReports.clear();
            filteredReports.addAll(allReports);

            // Rafraîchir l'affichage immédiatement avec les données locales
            refreshDisplay();

        } catch (Exception e) {
            android.util.Log.e("REPORTS_ENH", "Erreur chargement rapports locaux", e);
        }
    }

    /**
     * Filtre les rapports selon le texte de recherche
     */
    private void filterReports(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredReports.clear();
            filteredReports.addAll(allReports);
        } else {
            String searchLower = query.toLowerCase();
            filteredReports.clear();

            for (TimeReport report : allReports) {
                boolean matches = false;

                // Recherche dans la description
                if (report.getDescription() != null &&
                    report.getDescription().toLowerCase().contains(searchLower)) {
                    matches = true;
                }

                // Recherche dans le nom du projet
                if (report.getProjectName() != null &&
                    report.getProjectName().toLowerCase().contains(searchLower)) {
                    matches = true;
                }

                // Recherche dans le type de travail
                if (report.getWorkTypeName() != null &&
                    report.getWorkTypeName().toLowerCase().contains(searchLower)) {
                    matches = true;
                }

                if (matches) {
                    filteredReports.add(report);
                }
            }
        }

        refreshDisplay();
    }

    /**
     * Rafraîchit l'affichage (statistiques + adapter)
     */
    private void refreshDisplay() {
        try {
            FileLogger.d("REPORTS_ENH", ">>> refreshDisplay() START");
            FileLogger.d("REPORTS_ENH", "filteredReports size: " + filteredReports.size());

            // Mettre à jour les statistiques
            FileLogger.d("REPORTS_ENH", "Appel updateStatistics()...");
            updateStatistics();
            FileLogger.d("REPORTS_ENH", "✅ updateStatistics() OK");

            // Mettre à jour l'adapter
            if (pagerAdapter != null) {
                FileLogger.d("REPORTS_ENH", "Appel pagerAdapter.updateReports()...");
                pagerAdapter.updateReports(filteredReports);
                FileLogger.d("REPORTS_ENH", "✅ pagerAdapter.updateReports() OK");
            } else {
                FileLogger.e("REPORTS_ENH", "⚠️ pagerAdapter est NULL", null);
            }

            // Mettre à jour l'état vide
            FileLogger.d("REPORTS_ENH", "Appel updateEmptyState()...");
            updateEmptyState();
            FileLogger.d("REPORTS_ENH", "✅ updateEmptyState() OK");

            FileLogger.d("REPORTS_ENH", ">>> refreshDisplay() END ✅");
        } catch (Exception e) {
            FileLogger.e("REPORTS_ENH", "❌ CRASH dans refreshDisplay", e);
        }
    }

    /**
     * Met à jour les statistiques globales
     */
    private void updateStatistics() {
        try {
            FileLogger.d("REPORTS_ENH", ">>> updateStatistics() START");
            FileLogger.d("REPORTS_ENH", "Appel ReportGrouper.calculateGlobalStats()...");
            ReportGrouper.GlobalStats stats = ReportGrouper.calculateGlobalStats(filteredReports);
            FileLogger.d("REPORTS_ENH", "✅ Stats calculées: " + stats.reportCount + " rapports, " + stats.getFormattedTotalHours() + " heures");

            tvTotalHours.setText(stats.getFormattedTotalHours());
            tvReportCount.setText(String.valueOf(stats.reportCount));
            tvAverageHours.setText(stats.getFormattedAverageHours());

            FileLogger.d("REPORTS_ENH", ">>> updateStatistics() END ✅");
        } catch (Exception e) {
            FileLogger.e("REPORTS_ENH", "❌ CRASH dans updateStatistics", e);
        }
    }

    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            tvNoReports.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.GONE);
        } else {
            tvNoReports.setVisibility(View.GONE);
            viewPager.setVisibility(View.VISIBLE);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        viewPager.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    // ==================== FILTRES ====================

    private void showPeriodFilter() {
        String[] periods = {
            "Aujourd'hui",
            "Cette semaine",
            "Ce mois",
            "30 derniers jours",
            "90 derniers jours (défaut)",
            "Tout"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filtrer par période")
            .setItems(periods, (dialog, which) -> {
                filterByPeriod(which);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void filterByPeriod(int periodIndex) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
        String today = dateFormat.format(calendar.getTime());
        String fromDate = null;

        switch (periodIndex) {
            case 0: // Aujourd'hui
                fromDate = today;
                break;
            case 1: // Cette semaine
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                fromDate = dateFormat.format(calendar.getTime());
                break;
            case 2: // Ce mois
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                fromDate = dateFormat.format(calendar.getTime());
                break;
            case 3: // 30 derniers jours
                calendar.add(Calendar.DAY_OF_YEAR, -30);
                fromDate = dateFormat.format(calendar.getTime());
                break;
            case 4: // 90 derniers jours
                calendar.add(Calendar.DAY_OF_YEAR, -90);
                fromDate = dateFormat.format(calendar.getTime());
                break;
            case 5: // Tout
                filteredReports.clear();
                filteredReports.addAll(allReports);
                refreshDisplay();
                return;
        }

        // Filtrer par date
        final String finalFromDate = fromDate;
        filteredReports.clear();
        for (TimeReport report : allReports) {
            if (report.getReportDate() != null && report.getReportDate().compareTo(finalFromDate) >= 0) {
                filteredReports.add(report);
            }
        }
        refreshDisplay();
        Toast.makeText(this, "Filtre appliqué: " + filteredReports.size() + " rapports", Toast.LENGTH_SHORT).show();
    }

    private void showProjectFilter() {
        // Récupérer la liste unique des projets
        java.util.Set<String> projectSet = new java.util.HashSet<>();
        for (TimeReport report : allReports) {
            if (report.getProjectName() != null && !report.getProjectName().isEmpty()) {
                projectSet.add(report.getProjectName());
            }
        }

        if (projectSet.isEmpty()) {
            Toast.makeText(this, "Aucun projet disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] projects = projectSet.toArray(new String[0]);
        java.util.Arrays.sort(projects);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filtrer par projet")
            .setItems(projects, (dialog, which) -> {
                filterByProject(projects[which]);
            })
            .setNegativeButton("Annuler", null)
            .setNeutralButton("Tout afficher", (dialog, which) -> {
                filteredReports.clear();
                filteredReports.addAll(allReports);
                refreshDisplay();
            })
            .show();
    }

    private void filterByProject(String projectName) {
        filteredReports.clear();
        for (TimeReport report : allReports) {
            if (projectName.equals(report.getProjectName())) {
                filteredReports.add(report);
            }
        }
        refreshDisplay();
        Toast.makeText(this, "Projet: " + projectName + " (" + filteredReports.size() + " rapports)", Toast.LENGTH_SHORT).show();
    }

    private void showStatusFilter() {
        String[] statuses = {
            "En attente",
            "Approuvé",
            "Rejeté",
            "Synchronisé",
            "Non synchronisé",
            "Tout"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filtrer par statut")
            .setItems(statuses, (dialog, which) -> {
                filterByStatus(which);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void filterByStatus(int statusIndex) {
        filteredReports.clear();

        switch (statusIndex) {
            case 0: // En attente
                for (TimeReport report : allReports) {
                    if ("pending".equalsIgnoreCase(report.getValidationStatus())) {
                        filteredReports.add(report);
                    }
                }
                break;
            case 1: // Approuvé
                for (TimeReport report : allReports) {
                    if ("approved".equalsIgnoreCase(report.getValidationStatus())) {
                        filteredReports.add(report);
                    }
                }
                break;
            case 2: // Rejeté
                for (TimeReport report : allReports) {
                    if ("rejected".equalsIgnoreCase(report.getValidationStatus())) {
                        filteredReports.add(report);
                    }
                }
                break;
            case 3: // Synchronisé
                for (TimeReport report : allReports) {
                    if (report.isSynced()) {
                        filteredReports.add(report);
                    }
                }
                break;
            case 4: // Non synchronisé
                for (TimeReport report : allReports) {
                    if (!report.isSynced()) {
                        filteredReports.add(report);
                    }
                }
                break;
            case 5: // Tout
                filteredReports.addAll(allReports);
                break;
        }

        refreshDisplay();
        Toast.makeText(this, "Filtre appliqué: " + filteredReports.size() + " rapports", Toast.LENGTH_SHORT).show();
    }

    // ==================== MENU ====================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_menu, menu);
        this.optionsMenu = menu;

        // Vérifier l'état de synchronisation initial
        SyncStateManager syncStateManager = SyncStateManager.getInstance(this);
        updateSyncButtonState(syncStateManager.isSyncing());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sync_files) {
            // Ouvrir l'interface de synchronisation unifiée
            openSyncFilesActivity();
            return true;
        } else if (item.getItemId() == R.id.action_open_agenda) {
            try {
                FileLogger.d("REPORTS_ENH", "→ Ouverture AgendaActivity");
                startActivity(new Intent(this, TimelineActivity.class));
                FileLogger.d("REPORTS_ENH", "→ AgendaActivity lancée");
            } catch (Exception e) {
                FileLogger.e("REPORTS_ENH", "❌ Erreur lancement AgendaActivity", e);
            }
            return true;
        } else if (item.getItemId() == R.id.action_reconcile_reports) {
            loadReports(); // Recharger les données
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void syncPendingReports() {
        try {
            int pendingCount = syncManager.getPendingSyncCount();

            if (pendingCount == 0) {
                Toast.makeText(this, "ℹ️ Aucun rapport en attente de synchronisation", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "🔄 Synchronisation de " + pendingCount + " rapport(s)...", Toast.LENGTH_SHORT).show();

            // ✅ Utiliser BidirectionalSyncManager.syncFull()
            syncManager.syncFull(new BidirectionalSyncManager.SyncCallback() {
                @Override
                public void onSyncStarted(String phase) {
                    android.util.Log.d("REPORTS_ENH", "Phase de synchronisation: " + phase);
                }

                @Override
                public void onSyncProgress(String message, int current, int total) {
                    android.util.Log.d("REPORTS_ENH", "Progrès: " + message + " (" + current + "/" + total + ")");
                }

                @Override
                public void onSyncCompleted(BidirectionalSyncManager.SyncResult result) {
                    android.util.Log.d("REPORTS_ENH", "Sync success: " + result.getSummary());
                    runOnUiThread(() -> {
                        Toast.makeText(TimeReportsActivity.this,
                            "✅ Synchronisation réussie",
                            Toast.LENGTH_SHORT).show();
                        // Recharger les rapports
                        reconcileMissingReports();
                    });
                }

                @Override
                public void onSyncError(String error) {
                    android.util.Log.e("REPORTS_ENH", "Erreur sync: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(TimeReportsActivity.this,
                            "❌ Erreur de synchronisation: " + error,
                            Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openSyncFilesActivity() {
        try {
            FileLogger.d("REPORTS_ENH", "→ openSyncFilesActivity() appelé");
            // Intent intent = new Intent(this, SyncManagementActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "⚠️ Fonction temporairement désactivée", Toast.LENGTH_SHORT).show();
            FileLogger.d("REPORTS_ENH", "→ openSyncFilesActivity() terminé");
        } catch (Exception e) {
            FileLogger.e("REPORTS_ENH", "❌ Erreur openSyncFilesActivity", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Télécharge les rapports des 90 derniers jours (comme l'ancienne interface)
     */
    private void reconcileMissingReports() {
        try {
            setLoading(true);

            // D'abord charger les rapports locaux
            loadLocalReports();

            String token = prefs.getString("auth_token", "");
            if (token == null || token.isEmpty()) {
                setLoading(false);
                Toast.makeText(this, "Session expirée - Utilisation des données locales uniquement", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérifier que l'API service est disponible
            if (apiService == null) {
                android.util.Log.w("REPORTS_ENH", "API Service non disponible - Mode offline uniquement");
                setLoading(false);
                Toast.makeText(this, "⚠️ Mode offline - Affichage des rapports locaux uniquement", Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("REPORTS_ENH", "Chargement des rapports (90 derniers jours)...");

            // Charger les rapports des 90 derniers jours
            Calendar calendar = Calendar.getInstance();
            // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            String dateTo = dateFormat.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_YEAR, -90);
            String dateFrom = dateFormat.format(calendar.getTime());

            android.util.Log.d("REPORTS_ENH", "Période: " + dateFrom + " à " + dateTo);

            Call<List<TimeReport>> call = apiService.getReports(token, dateFrom, dateTo, null);
            call.enqueue(new Callback<List<TimeReport>>() {
                @Override
                public void onResponse(Call<List<TimeReport>> call, Response<List<TimeReport>> response) {
                    FileLogger.d("REPORTS_ENH", "========== CALLBACK onResponse ==========");

                    try {
                        setLoading(false);
                        FileLogger.d("REPORTS_ENH", "Response code: " + response.code());
                        FileLogger.d("REPORTS_ENH", "Response successful: " + response.isSuccessful());

                        if (response.isSuccessful() && response.body() != null) {
                            List<TimeReport> serverReports = response.body();
                            FileLogger.d("REPORTS_ENH", "Nombre de rapports reçus: " + serverReports.size());

                            // ✅ FIX: Sauvegarder les rapports locaux pending AVANT de les écraser
                            List<TimeReport> pendingLocalReports = new ArrayList<>();
                            for (TimeReport localReport : allReports) {
                                if (localReport != null && localReport.isLocal() && !localReport.isSynced()) {
                                    pendingLocalReports.add(localReport);
                                }
                            }
                            FileLogger.d("REPORTS_ENH", "Rapports locaux pending sauvegardés: " + pendingLocalReports.size());

                            // Remplacer la liste par les rapports serveur
                            FileLogger.d("REPORTS_ENH", "Effacement allReports...");
                            allReports.clear();
                            FileLogger.d("REPORTS_ENH", "Ajout des rapports serveur...");
                            allReports.addAll(serverReports);

                            // Marquer tous les rapports serveur comme synchronisés
                            FileLogger.d("REPORTS_ENH", "Marquage des rapports serveur comme synchronisés...");
                            for (int i = 0; i < allReports.size(); i++) {
                                TimeReport report = allReports.get(i);
                                try {
                                    if (report != null) {
                                        report.setSynced(true);
                                        report.setLocal(false);
                                    } else {
                                        FileLogger.e("REPORTS_ENH", "⚠️ Report null à l'index " + i, null);
                                    }
                                } catch (Exception e) {
                                    FileLogger.e("REPORTS_ENH", "Erreur marquage report " + i, e);
                                }
                            }

                            // ✅ FIX: Réajouter les rapports locaux pending qui ne sont pas sur le serveur
                            // (évite de perdre les saisies non-synchronisées)
                            java.util.Set<String> serverReportKeys = new java.util.HashSet<>();
                            for (TimeReport sr : serverReports) {
                                // Créer une clé unique basée sur date + projet + heures + description
                                String key = sr.getReportDate() + "_" + sr.getProjectId() + "_" + sr.getHours();
                                if (sr.getDescription() != null) {
                                    key += "_" + sr.getDescription().hashCode();
                                }
                                serverReportKeys.add(key);
                            }

                            int addedPendingCount = 0;
                            for (TimeReport pendingReport : pendingLocalReports) {
                                String key = pendingReport.getReportDate() + "_" + pendingReport.getProjectId() + "_" + pendingReport.getHours();
                                if (pendingReport.getDescription() != null) {
                                    key += "_" + pendingReport.getDescription().hashCode();
                                }

                                if (!serverReportKeys.contains(key)) {
                                    // Ce rapport pending n'est pas sur le serveur, le rajouter
                                    allReports.add(pendingReport);
                                    addedPendingCount++;
                                }
                            }
                            FileLogger.d("REPORTS_ENH", "Rapports pending réajoutés: " + addedPendingCount);

                            FileLogger.d("REPORTS_ENH", "Total rapports: " + allReports.size());

                            // Mettre à jour l'affichage
                            FileLogger.d("REPORTS_ENH", "Mise à jour filteredReports...");
                            filteredReports.clear();
                            filteredReports.addAll(allReports);

                            FileLogger.d("REPORTS_ENH", "Appel refreshDisplay()...");
                            refreshDisplay();
                            FileLogger.d("REPORTS_ENH", "✅ refreshDisplay() terminé");

                            Toast.makeText(TimeReportsActivity.this, "✅ Données rechargées (90j)", Toast.LENGTH_SHORT).show();
                            FileLogger.d("REPORTS_ENH", "========== onResponse SUCCÈS ==========");
                        } else {
                            FileLogger.e("REPORTS_ENH", "❌ Erreur API: " + response.code(), null);
                            if (response.errorBody() != null) {
                                try {
                                    String errorBody = response.errorBody().string();
                                    FileLogger.e("REPORTS_ENH", "Error body: " + errorBody, null);
                                } catch (Exception e) {
                                    FileLogger.e("REPORTS_ENH", "Erreur lecture error body", e);
                                }
                            }
                            Toast.makeText(TimeReportsActivity.this, "❌ Erreur chargement rapports serveur", Toast.LENGTH_SHORT).show();
                            refreshDisplay();
                        }
                    } catch (Exception e) {
                        FileLogger.e("REPORTS_ENH", "❌ CRASH dans onResponse", e);
                        Toast.makeText(TimeReportsActivity.this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        refreshDisplay();
                    }
                }

                @Override
                public void onFailure(Call<List<TimeReport>> call, Throwable t) {
                    FileLogger.e("REPORTS_ENH", "❌ onFailure - Échec chargement rapports serveur", t);
                    setLoading(false);
                    Toast.makeText(TimeReportsActivity.this, "⚠️ Erreur réseau - Affichage des rapports locaux uniquement", Toast.LENGTH_LONG).show();
                    refreshDisplay();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("REPORTS_ENH", "Erreur reconcileMissingReports", e);
            setLoading(false);
            Toast.makeText(this, "❌ Erreur chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            refreshDisplay();
        }
    }

    /**
     * Met à jour l'état du bouton de synchronisation
     */
    private void updateSyncButtonState(boolean isSyncing) {
        if (optionsMenu != null) {
            MenuItem syncItem = optionsMenu.findItem(R.id.action_sync_files);
            if (syncItem != null) {
                syncItem.setEnabled(!isSyncing);
                // Optionnel: Changer le titre pour indiquer l'état
                if (isSyncing) {
                    syncItem.setTitle("Synchronisation...");
                } else {
                    syncItem.setTitle("Synchronisation");
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Enregistrer le receiver pour les changements d'état de synchronisation
        IntentFilter filter = new IntentFilter(SyncStateManager.ACTION_SYNC_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(syncStateReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Désenregistrer le receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(syncStateReceiver);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
