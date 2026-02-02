package com.ptms.mobile.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.ptms.mobile.R;
import com.ptms.mobile.dashboard.DashboardWidgetManager;
import com.ptms.mobile.dashboard.widgets.ProjectChartWidget;
import com.ptms.mobile.dashboard.widgets.StatisticsCardWidget;
import com.ptms.mobile.dashboard.widgets.WeeklyTrendWidget;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activité Dashboard avec widgets statistiques
 * Affiche les statistiques de temps de travail avec graphiques
 */
public class StatisticsActivity extends AppCompatActivity {
    private static final String TAG = "StatisticsDashboard";

    // Widgets statistiques
    private StatisticsCardWidget widgetToday;
    private StatisticsCardWidget widgetWeek;
    private StatisticsCardWidget widgetMonth;
    private StatisticsCardWidget widgetAverage;

    // Graphiques
    private ProjectChartWidget chartProjects;
    private WeeklyTrendWidget chartWeeklyTrend;

    // Légende du graphique projets
    private RecyclerView rvProjectLegend;
    private ProjectLegendAdapter legendAdapter;

    // UI
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout contentContainer;
    private LinearLayout noDataContainer;
    private TextView tvNoData;
    private MaterialButton btnRefresh;

    // Cards des graphiques
    private View cardProjects;
    private View cardWeeklyTrend;

    // Gestion des données
    private DashboardWidgetManager widgetManager;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Statistiques");
        }

        initViews();
        initManagers();
        loadDashboardData(false);
    }

    private void initViews() {
        // Widgets statistiques
        widgetToday = findViewById(R.id.widget_today);
        widgetWeek = findViewById(R.id.widget_week);
        widgetMonth = findViewById(R.id.widget_month);
        widgetAverage = findViewById(R.id.widget_average);

        // Graphiques
        chartProjects = findViewById(R.id.chart_projects);
        chartWeeklyTrend = findViewById(R.id.chart_weekly_trend);

        // Cards des graphiques
        cardProjects = findViewById(R.id.card_projects);
        cardWeeklyTrend = findViewById(R.id.card_weekly_trend);

        // Légende
        rvProjectLegend = findViewById(R.id.rv_project_legend);
        rvProjectLegend.setLayoutManager(new LinearLayoutManager(this));
        legendAdapter = new ProjectLegendAdapter();
        rvProjectLegend.setAdapter(legendAdapter);

        // UI
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        contentContainer = findViewById(R.id.content_container);
        noDataContainer = findViewById(R.id.no_data_container);
        tvNoData = findViewById(R.id.tv_no_data);
        btnRefresh = findViewById(R.id.btn_refresh);

        // Configuration des widgets
        if (widgetToday != null) widgetToday.setTitle("AUJOURD'HUI");
        if (widgetWeek != null) widgetWeek.setTitle("CETTE SEMAINE");
        if (widgetMonth != null) widgetMonth.setTitle("CE MOIS");
        if (widgetAverage != null) widgetAverage.setTitle("MOYENNE/JOUR");

        // Listeners
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> loadDashboardData(true));
        }
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> loadDashboardData(true));
        }
    }

    private void initManagers() {
        widgetManager = DashboardWidgetManager.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
    }

    private void loadDashboardData(boolean forceRefresh) {
        showLoading(true);

        executorService.execute(() -> {
            try {
                // Vérifier la connexion réseau
                boolean isOnline = com.ptms.mobile.utils.NetworkUtils.isOnline(this);

                if (!isOnline && !forceRefresh) {
                    // Mode offline - utiliser le cache uniquement
                    runOnUiThread(() -> {
                        DashboardWidgetManager.DashboardStats cachedStats =
                            widgetManager.getDashboardStats(false);

                        if (cachedStats != null && cachedStats.todayStats != null) {
                            updateUI(cachedStats);
                            showLoading(false);
                            Toast.makeText(this,
                                "📱 Données locales (mode hors ligne)",
                                Toast.LENGTH_SHORT).show();
                        } else {
                            showNoData("Aucune donnée disponible en mode hors ligne");
                            showLoading(false);
                        }

                        if (swipeRefresh != null) {
                            swipeRefresh.setRefreshing(false);
                        }
                    });
                    return;
                }

                // Charger les statistiques
                DashboardWidgetManager.DashboardStats stats =
                    widgetManager.getDashboardStats(forceRefresh);

                runOnUiThread(() -> {
                    if (stats != null && (stats.todayStats != null ||
                        stats.weekStats != null || stats.monthStats != null)) {
                        updateUI(stats);
                        showLoading(false);
                    } else {
                        showNoData("Aucune donnée de temps enregistrée");
                        showLoading(false);
                    }

                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }
                });

            } catch (Exception e) {
                android.util.Log.e(TAG, "Erreur lors du chargement des statistiques", e);
                runOnUiThread(() -> {
                    showError("Erreur lors du chargement des statistiques: " + e.getMessage());
                    showLoading(false);
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void updateUI(DashboardWidgetManager.DashboardStats stats) {
        if (stats == null) {
            showNoData("Aucune donnée disponible");
            return;
        }

        // Statistiques du jour
        if (stats.todayStats != null && widgetToday != null) {
            widgetToday.setValue(stats.todayStats.getFormattedTotalHours());
            widgetToday.setSubtitle(stats.todayStats.totalEntries + " entrée" +
                (stats.todayStats.totalEntries > 1 ? "s" : ""));
        }

        // Statistiques de la semaine
        if (stats.weekStats != null && widgetWeek != null) {
            widgetWeek.setValue(stats.weekStats.getFormattedTotalHours());
            widgetWeek.setSubtitle(stats.weekStats.totalEntries + " entrée" +
                (stats.weekStats.totalEntries > 1 ? "s" : ""));
        }

        // Statistiques du mois
        if (stats.monthStats != null && widgetMonth != null) {
            widgetMonth.setValue(stats.monthStats.getFormattedTotalHours());
            widgetMonth.setSubtitle(stats.monthStats.totalEntries + " entrée" +
                (stats.monthStats.totalEntries > 1 ? "s" : ""));
        }

        // Moyenne par jour
        if (stats.monthStats != null && widgetAverage != null) {
            widgetAverage.setValue(stats.monthStats.getFormattedAverageHours());
            widgetAverage.setSubtitle("Basé sur le mois");
        }

        // Graphique projets
        if (stats.topProjects != null && !stats.topProjects.isEmpty() &&
            chartProjects != null && cardProjects != null) {
            chartProjects.setData(stats.topProjects, stats.monthStats.totalHours);
            legendAdapter.setData(chartProjects.getSegments());
            cardProjects.setVisibility(View.VISIBLE);
        } else if (cardProjects != null) {
            cardProjects.setVisibility(View.GONE);
        }

        // Tendance hebdomadaire
        if (stats.weeklyTrend != null && !stats.weeklyTrend.isEmpty() &&
            chartWeeklyTrend != null && cardWeeklyTrend != null) {
            chartWeeklyTrend.setData(stats.weeklyTrend);
            cardWeeklyTrend.setVisibility(View.VISIBLE);
        } else if (cardWeeklyTrend != null) {
            cardWeeklyTrend.setVisibility(View.GONE);
        }

        // Afficher le contenu
        showContent();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (contentContainer != null) {
            contentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (noDataContainer != null) {
            noDataContainer.setVisibility(View.GONE);
        }
    }

    private void showContent() {
        if (contentContainer != null) {
            contentContainer.setVisibility(View.VISIBLE);
        }
        if (noDataContainer != null) {
            noDataContainer.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showNoData(String message) {
        if (contentContainer != null) {
            contentContainer.setVisibility(View.GONE);
        }
        if (noDataContainer != null) {
            noDataContainer.setVisibility(View.VISIBLE);
        }
        if (tvNoData != null) {
            tvNoData.setText(message);
        }
        if (btnRefresh != null) {
            btnRefresh.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        showNoData("Une erreur s'est produite");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * Adapter pour la légende du graphique projets
     */
    private static class ProjectLegendAdapter extends RecyclerView.Adapter<ProjectLegendAdapter.ViewHolder> {
        private java.util.List<ProjectChartWidget.ChartSegment> segments = new java.util.ArrayList<>();

        public void setData(java.util.List<ProjectChartWidget.ChartSegment> segments) {
            this.segments = segments != null ? segments : new java.util.ArrayList<>();
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_legend, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ProjectChartWidget.ChartSegment segment = segments.get(position);

            holder.colorIndicator.setBackgroundColor(segment.color);
            holder.tvProjectName.setText(segment.projectName);
            holder.tvHours.setText(String.format("%.1fh", segment.hours));
            holder.tvPercentage.setText(String.format("%.1f%%", segment.percentage));
        }

        @Override
        public int getItemCount() {
            return segments.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View colorIndicator;
            TextView tvProjectName;
            TextView tvHours;
            TextView tvPercentage;

            ViewHolder(View itemView) {
                super(itemView);
                colorIndicator = itemView.findViewById(R.id.color_indicator);
                tvProjectName = itemView.findViewById(R.id.tv_project_name);
                tvHours = itemView.findViewById(R.id.tv_hours);
                tvPercentage = itemView.findViewById(R.id.tv_percentage);
            }
        }
    }
}
