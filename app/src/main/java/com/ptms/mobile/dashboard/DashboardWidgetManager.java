package com.ptms.mobile.dashboard;

import android.content.Context;
import android.util.Log;

import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.TimeReport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestionnaire centralisé des widgets du dashboard
 * Fournit les données et statistiques pour les différents widgets
 */
public class DashboardWidgetManager {
    private static final String TAG = "DashboardWidgetManager";
    private static DashboardWidgetManager instance;

    private Context context;
    private OfflineDatabaseHelper dbHelper;
    private SimpleDateFormat dateFormat;

    // Cache des statistiques
    private DashboardStats cachedStats;
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    private DashboardWidgetManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new OfflineDatabaseHelper(context);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    public static synchronized DashboardWidgetManager getInstance(Context context) {
        if (instance == null) {
            instance = new DashboardWidgetManager(context);
        }
        return instance;
    }

    /**
     * Récupère toutes les statistiques du dashboard
     */
    public DashboardStats getDashboardStats(boolean forceRefresh) {
        long currentTime = System.currentTimeMillis();

        if (!forceRefresh && cachedStats != null &&
            (currentTime - lastCacheUpdate) < CACHE_DURATION) {
            return cachedStats;
        }

        try {
            DashboardStats stats = new DashboardStats();

            // Statistiques du jour
            stats.todayStats = getTodayStats();

            // Statistiques de la semaine
            stats.weekStats = getWeekStats();

            // Statistiques du mois
            stats.monthStats = getMonthStats();

            // Top projets
            stats.topProjects = getTopProjects(5);

            // Distribution par type de travail
            stats.workTypeDistribution = getWorkTypeDistribution();

            // Tendance hebdomadaire
            stats.weeklyTrend = getWeeklyTrend();

            // Mise en cache
            cachedStats = stats;
            lastCacheUpdate = currentTime;

            return stats;

        } catch (Exception e) {
            Log.e(TAG, "Error getting dashboard stats", e);
            return new DashboardStats();
        }
    }

    /**
     * Statistiques du jour actuel
     */
    private TimeStats getTodayStats() {
        Calendar calendar = Calendar.getInstance();
        String today = dateFormat.format(calendar.getTime());

        return getStatsForPeriod(today, today);
    }

    /**
     * Statistiques de la semaine courante
     */
    private TimeStats getWeekStats() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        String weekStart = dateFormat.format(calendar.getTime());

        calendar.add(Calendar.DAY_OF_YEAR, 6);
        String weekEnd = dateFormat.format(calendar.getTime());

        return getStatsForPeriod(weekStart, weekEnd);
    }

    /**
     * Statistiques du mois courant
     */
    private TimeStats getMonthStats() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String monthStart = dateFormat.format(calendar.getTime());

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        String monthEnd = dateFormat.format(calendar.getTime());

        return getStatsForPeriod(monthStart, monthEnd);
    }

    /**
     * Calcule les statistiques pour une période donnée
     */
    private TimeStats getStatsForPeriod(String startDate, String endDate) {
        TimeStats stats = new TimeStats();
        stats.startDate = startDate;
        stats.endDate = endDate;

        try {
            // Récupérer tous les rapports de la période
            List<TimeReport> reports = dbHelper.getTimeReportsByDateRange(startDate, endDate);

            if (reports.isEmpty()) {
                return stats;
            }

            // Calculer les statistiques
            double totalHours = 0;
            int totalEntries = reports.size();
            Map<String, Double> projectHours = new HashMap<>();

            for (TimeReport report : reports) {
                double hours = report.getHours();
                totalHours += hours;

                // Accumuler par projet
                String projectName = getProjectName(report.getProjectId());
                projectHours.put(projectName,
                    projectHours.getOrDefault(projectName, 0.0) + hours);
            }

            stats.totalHours = totalHours;
            stats.totalEntries = totalEntries;
            stats.averageHoursPerDay = calculateAveragePerDay(totalHours, startDate, endDate);
            stats.projectBreakdown = projectHours;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating period stats", e);
        }

        return stats;
    }

    /**
     * Top projets par heures travaillées
     */
    private List<ProjectStats> getTopProjects(int limit) {
        List<ProjectStats> topProjects = new ArrayList<>();

        try {
            // Récupérer tous les rapports du mois
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            String monthStart = dateFormat.format(calendar.getTime());

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            String monthEnd = dateFormat.format(calendar.getTime());

            List<TimeReport> reports = dbHelper.getTimeReportsByDateRange(monthStart, monthEnd);

            // Accumuler par projet
            Map<Integer, Double> projectHours = new HashMap<>();
            for (TimeReport report : reports) {
                int projectId = report.getProjectId();
                double hours = report.getHours();
                projectHours.put(projectId, projectHours.getOrDefault(projectId, 0.0) + hours);
            }

            // Créer les stats de projet
            for (Map.Entry<Integer, Double> entry : projectHours.entrySet()) {
                Project project = dbHelper.getProjectById(entry.getKey());
                if (project != null) {
                    ProjectStats stats = new ProjectStats();
                    stats.projectId = entry.getKey();
                    stats.projectName = project.getName();
                    stats.totalHours = entry.getValue();
                    stats.entryCount = countEntriesForProject(reports, entry.getKey());
                    topProjects.add(stats);
                }
            }

            // Trier par heures décroissantes
            topProjects.sort((a, b) -> Double.compare(b.totalHours, a.totalHours));

            // Limiter le nombre de résultats
            if (topProjects.size() > limit) {
                topProjects = topProjects.subList(0, limit);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting top projects", e);
        }

        return topProjects;
    }

    /**
     * Distribution par type de travail
     */
    private Map<String, Double> getWorkTypeDistribution() {
        Map<String, Double> distribution = new HashMap<>();

        try {
            // Récupérer tous les rapports du mois
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            String monthStart = dateFormat.format(calendar.getTime());

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            String monthEnd = dateFormat.format(calendar.getTime());

            List<TimeReport> reports = dbHelper.getTimeReportsByDateRange(monthStart, monthEnd);

            // Accumuler par type de travail
            for (TimeReport report : reports) {
                String workType = getWorkTypeName(report.getWorkTypeId());
                double hours = report.getHours();
                distribution.put(workType, distribution.getOrDefault(workType, 0.0) + hours);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting work type distribution", e);
        }

        return distribution;
    }

    /**
     * Tendance hebdomadaire (7 derniers jours)
     */
    private List<DayStats> getWeeklyTrend() {
        List<DayStats> trend = new ArrayList<>();

        try {
            Calendar calendar = Calendar.getInstance();

            for (int i = 6; i >= 0; i--) {
                calendar.setTime(new Date());
                calendar.add(Calendar.DAY_OF_YEAR, -i);
                String date = dateFormat.format(calendar.getTime());

                TimeStats dayStats = getStatsForPeriod(date, date);

                DayStats day = new DayStats();
                day.date = date;
                day.dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK,
                    Calendar.SHORT, Locale.getDefault());
                day.totalHours = dayStats.totalHours;
                day.entryCount = dayStats.totalEntries;

                trend.add(day);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting weekly trend", e);
        }

        return trend;
    }

    /**
     * Calcule la moyenne d'heures par jour
     */
    private double calculateAveragePerDay(double totalHours, String startDate, String endDate) {
        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            long diffMillis = end.getTime() - start.getTime();
            int days = (int) (diffMillis / (1000 * 60 * 60 * 24)) + 1;

            return totalHours / days;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating average", e);
            return 0;
        }
    }

    /**
     * Récupère le nom d'un projet
     */
    private String getProjectName(int projectId) {
        try {
            Project project = dbHelper.getProjectById(projectId);
            return project != null ? project.getName() : "Unknown Project";
        } catch (Exception e) {
            return "Unknown Project";
        }
    }

    /**
     * Récupère le nom d'un type de travail
     */
    private String getWorkTypeName(int workTypeId) {
        try {
            // Utiliser la méthode du DatabaseHelper si elle existe
            // Sinon retourner un nom par défaut
            return "Work Type " + workTypeId;
        } catch (Exception e) {
            return "Unknown Work Type";
        }
    }

    /**
     * Compte le nombre d'entrées pour un projet
     */
    private int countEntriesForProject(List<TimeReport> reports, int projectId) {
        int count = 0;
        for (TimeReport report : reports) {
            if (report.getProjectId() == projectId) {
                count++;
            }
        }
        return count;
    }

    /**
     * Force le rafraîchissement du cache
     */
    public void refreshCache() {
        getDashboardStats(true);
    }

    /**
     * Vide le cache
     */
    public void clearCache() {
        cachedStats = null;
        lastCacheUpdate = 0;
    }

    // ==================== Classes de données ====================

    /**
     * Statistiques complètes du dashboard
     */
    public static class DashboardStats {
        public TimeStats todayStats;
        public TimeStats weekStats;
        public TimeStats monthStats;
        public List<ProjectStats> topProjects;
        public Map<String, Double> workTypeDistribution;
        public List<DayStats> weeklyTrend;

        public DashboardStats() {
            todayStats = new TimeStats();
            weekStats = new TimeStats();
            monthStats = new TimeStats();
            topProjects = new ArrayList<>();
            workTypeDistribution = new HashMap<>();
            weeklyTrend = new ArrayList<>();
        }
    }

    /**
     * Statistiques de temps pour une période
     */
    public static class TimeStats {
        public String startDate;
        public String endDate;
        public double totalHours;
        public int totalEntries;
        public double averageHoursPerDay;
        public Map<String, Double> projectBreakdown;

        public TimeStats() {
            totalHours = 0;
            totalEntries = 0;
            averageHoursPerDay = 0;
            projectBreakdown = new HashMap<>();
        }

        public String getFormattedTotalHours() {
            int hours = (int) totalHours;
            int minutes = (int) ((totalHours - hours) * 60);
            return String.format(Locale.getDefault(), "%dh %02dmin", hours, minutes);
        }

        public String getFormattedAverageHours() {
            int hours = (int) averageHoursPerDay;
            int minutes = (int) ((averageHoursPerDay - hours) * 60);
            return String.format(Locale.getDefault(), "%dh %02dmin", hours, minutes);
        }
    }

    /**
     * Statistiques par projet
     */
    public static class ProjectStats {
        public int projectId;
        public String projectName;
        public double totalHours;
        public int entryCount;

        public String getFormattedHours() {
            int hours = (int) totalHours;
            int minutes = (int) ((totalHours - hours) * 60);
            return String.format(Locale.getDefault(), "%dh %02dmin", hours, minutes);
        }

        public double getPercentage(double totalHours) {
            if (totalHours == 0) return 0;
            return (this.totalHours / totalHours) * 100;
        }
    }

    /**
     * Statistiques pour un jour
     */
    public static class DayStats {
        public String date;
        public String dayOfWeek;
        public double totalHours;
        public int entryCount;

        public String getFormattedHours() {
            int hours = (int) totalHours;
            int minutes = (int) ((totalHours - hours) * 60);
            return String.format(Locale.getDefault(), "%dh %02dmin", hours, minutes);
        }
    }
}
