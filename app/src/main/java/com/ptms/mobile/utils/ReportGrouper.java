package com.ptms.mobile.utils;

import com.ptms.mobile.models.ReportGroup;
import com.ptms.mobile.models.TimeReport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utilitaire pour regrouper des TimeReport par jour/semaine/mois
 */
public class ReportGrouper {

    // ✅ FIX: Use Locale.US for ISO dates (prevents crashes), Locale.FRANCE for display
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.FRANCE);
    private static final SimpleDateFormat DAY_NAME_FORMAT = new SimpleDateFormat("EEEE", Locale.FRANCE);

    // ==================== REGROUPEMENT PAR JOUR ====================

    /**
     * Regroupe les rapports par jour
     * @param reports Liste de rapports
     * @return Liste de ReportGroup (un par jour)
     */
    public static List<ReportGroup> groupByDay(List<TimeReport> reports) {
        try {
            FileLogger.d("GROUPER", ">>> groupByDay() START - " + (reports != null ? reports.size() : "NULL") + " rapports");
            Map<String, ReportGroup> dayGroups = new HashMap<>();

            if (reports == null) {
                FileLogger.d("GROUPER", "⚠️ reports est NULL");
                return new ArrayList<>();
            }

            for (int i = 0; i < reports.size(); i++) {
                try {
                    TimeReport report = reports.get(i);
                    if (report == null) {
                        FileLogger.e("GROUPER", "⚠️ Report NULL à l'index " + i, null);
                        continue;
                    }

                    String date = report.getReportDate();
                    if (date == null || date.isEmpty()) {
                        FileLogger.d("GROUPER", "⚠️ Date vide/null pour report " + i);
                        date = "unknown";
                    }

                    if (!dayGroups.containsKey(date)) {
                        // Créer un nouveau groupe pour ce jour
                        String title = formatDayTitle(date);
                        String subtitle = formatDisplayDate(date);
                        ReportGroup dayGroup = new ReportGroup(ReportGroup.GroupType.DAY, title, subtitle);
                        dayGroups.put(date, dayGroup);
                    }

                    dayGroups.get(date).addReport(report);
                } catch (Exception e) {
                    FileLogger.e("GROUPER", "Erreur traitement report " + i, e);
                }
            }

            // Convertir en liste et trier par date (décroissant = plus récent d'abord)
            List<ReportGroup> result = new ArrayList<>(dayGroups.values());
            result.sort((g1, g2) -> g2.getTitle().compareTo(g1.getTitle()));

            FileLogger.d("GROUPER", ">>> groupByDay() END ✅ - " + result.size() + " groupes");
            return result;
        } catch (Exception e) {
            FileLogger.e("GROUPER", "❌ CRASH dans groupByDay", e);
            return new ArrayList<>();
        }
    }

    // ==================== REGROUPEMENT PAR SEMAINE ====================

    /**
     * Regroupe les rapports par semaine (ISO 8601)
     * @param reports Liste de rapports
     * @return Liste de ReportGroup (un par semaine)
     */
    public static List<ReportGroup> groupByWeek(List<TimeReport> reports) {
        Map<String, ReportGroup> weekGroups = new HashMap<>();

        if (reports == null) {
            return new ArrayList<>();
        }

        for (TimeReport report : reports) {
            if (report == null) {
                continue;
            }

            try {
                String dateStr = report.getReportDate();
                if (dateStr == null || dateStr.isEmpty()) {
                    continue;
                }

                Date date = DATE_FORMAT.parse(dateStr);
                if (date == null) {
                    continue;
                }

                Calendar cal = Calendar.getInstance(Locale.FRANCE);
                cal.setTime(date);

                // Récupérer le numéro de semaine et l'année
                int weekNumber = cal.get(Calendar.WEEK_OF_YEAR);
                int year = cal.get(Calendar.YEAR);
                String weekKey = year + "-W" + weekNumber;

                if (!weekGroups.containsKey(weekKey)) {
                    // Calculer les dates de début et fin de semaine
                    String[] weekRange = getWeekDateRange(cal);

                    String title = "Semaine " + weekNumber;
                    String subtitle = weekRange[0] + " - " + weekRange[1];
                    ReportGroup weekGroup = new ReportGroup(ReportGroup.GroupType.WEEK, title, subtitle);
                    weekGroups.put(weekKey, weekGroup);
                }

                weekGroups.get(weekKey).addReport(report);

            } catch (ParseException e) {
                android.util.Log.e("ReportGrouper", "Erreur parsing date: " + report.getReportDate(), e);
            } catch (Exception e) {
                android.util.Log.e("ReportGrouper", "Erreur groupByWeek", e);
            }
        }

        // Convertir en liste et trier par semaine (décroissant)
        List<ReportGroup> result = new ArrayList<>(weekGroups.values());
        result.sort((g1, g2) -> g2.getTitle().compareTo(g1.getTitle()));

        return result;
    }

    // ==================== REGROUPEMENT PAR MOIS ====================

    /**
     * Regroupe les rapports par mois
     * @param reports Liste de rapports
     * @return Liste de ReportGroup (un par mois)
     */
    public static List<ReportGroup> groupByMonth(List<TimeReport> reports) {
        Map<String, ReportGroup> monthGroups = new HashMap<>();

        if (reports == null) {
            return new ArrayList<>();
        }

        for (TimeReport report : reports) {
            if (report == null) {
                continue;
            }

            try {
                String dateStr = report.getReportDate();
                if (dateStr == null || dateStr.isEmpty()) {
                    continue;
                }

                Date date = DATE_FORMAT.parse(dateStr);
                if (date == null) {
                    continue;
                }

                Calendar cal = Calendar.getInstance(Locale.FRANCE);
                cal.setTime(date);

                // Récupérer le mois et l'année
                int month = cal.get(Calendar.MONTH);
                int year = cal.get(Calendar.YEAR);
                String monthKey = year + "-" + String.format("%02d", month + 1);

                if (!monthGroups.containsKey(monthKey)) {
                    String title = MONTH_FORMAT.format(date);
                    String subtitle = getMonthSubtitle(cal);
                    ReportGroup monthGroup = new ReportGroup(ReportGroup.GroupType.MONTH, title, subtitle);
                    monthGroups.put(monthKey, monthGroup);
                }

                monthGroups.get(monthKey).addReport(report);

            } catch (ParseException e) {
                android.util.Log.e("ReportGrouper", "Erreur parsing date: " + report.getReportDate(), e);
            } catch (Exception e) {
                android.util.Log.e("ReportGrouper", "Erreur groupByMonth", e);
            }
        }

        // Convertir en liste et trier par mois (décroissant)
        List<ReportGroup> result = new ArrayList<>(monthGroups.values());
        result.sort((g1, g2) -> g2.getTitle().compareTo(g1.getTitle()));

        return result;
    }

    // ==================== REGROUPEMENT HIÉRARCHIQUE ====================

    /**
     * Regroupe les rapports par semaine avec détail des jours
     * @param reports Liste de rapports
     * @return Liste de ReportGroup (semaines) contenant des sub-groups (jours)
     */
    public static List<ReportGroup> groupByWeekWithDays(List<TimeReport> reports) {
        List<ReportGroup> weekGroups = groupByWeek(reports);

        // Pour chaque semaine, créer des sous-groupes par jour
        for (ReportGroup weekGroup : weekGroups) {
            List<ReportGroup> dayGroups = groupByDay(weekGroup.getReports());
            for (ReportGroup dayGroup : dayGroups) {
                weekGroup.addSubGroup(dayGroup);
            }
        }

        return weekGroups;
    }

    /**
     * Regroupe les rapports par mois avec détail des semaines
     * @param reports Liste de rapports
     * @return Liste de ReportGroup (mois) contenant des sub-groups (semaines)
     */
    public static List<ReportGroup> groupByMonthWithWeeks(List<TimeReport> reports) {
        List<ReportGroup> monthGroups = groupByMonth(reports);

        // Pour chaque mois, créer des sous-groupes par semaine
        for (ReportGroup monthGroup : monthGroups) {
            List<ReportGroup> weekGroups = groupByWeek(monthGroup.getReports());
            for (ReportGroup weekGroup : weekGroups) {
                monthGroup.addSubGroup(weekGroup);
            }
        }

        return monthGroups;
    }

    // ==================== UTILITAIRES DE FORMATAGE ====================

    /**
     * Formate un titre de jour (ex: "Lundi 16 Oct")
     */
    private static String formatDayTitle(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty()) {
                return "Date inconnue";
            }

            Date date = DATE_FORMAT.parse(dateStr);
            if (date == null) {
                return dateStr;
            }

            Calendar cal = Calendar.getInstance(Locale.FRANCE);
            cal.setTime(date);

            String dayName = DAY_NAME_FORMAT.format(date);

            // Protection contre chaîne vide
            if (dayName == null || dayName.isEmpty()) {
                return dateStr;
            }

            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            SimpleDateFormat dayFormat = new SimpleDateFormat("dd MMM", Locale.FRANCE);
            String dayMonth = dayFormat.format(date);

            return dayName + " " + dayMonth;

        } catch (ParseException e) {
            android.util.Log.e("ReportGrouper", "Erreur parsing date: " + dateStr, e);
            return dateStr != null ? dateStr : "Date invalide";
        } catch (StringIndexOutOfBoundsException e) {
            android.util.Log.e("ReportGrouper", "Erreur substring sur date: " + dateStr, e);
            return dateStr != null ? dateStr : "Date invalide";
        }
    }

    /**
     * Formate une date pour affichage (dd/MM/yyyy)
     */
    private static String formatDisplayDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty()) {
                return "Date inconnue";
            }

            Date date = DATE_FORMAT.parse(dateStr);
            if (date == null) {
                return dateStr;
            }

            return DISPLAY_DATE_FORMAT.format(date);
        } catch (ParseException e) {
            android.util.Log.e("ReportGrouper", "Erreur parsing date: " + dateStr, e);
            return dateStr != null ? dateStr : "Date invalide";
        }
    }

    /**
     * Calcule la plage de dates d'une semaine
     * @return [dateDebut, dateFin] formatées "dd MMM"
     */
    private static String[] getWeekDateRange(Calendar weekCalendar) {
        try {
            if (weekCalendar == null) {
                return new String[]{"--/--", "--/--"};
            }

            Calendar cal = (Calendar) weekCalendar.clone();

            // Début de semaine (Lundi)
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            SimpleDateFormat shortFormat = new SimpleDateFormat("dd MMM", Locale.FRANCE);
            String startDate = shortFormat.format(cal.getTime());

            // Fin de semaine (Dimanche)
            cal.add(Calendar.DAY_OF_WEEK, 6);
            String endDate = shortFormat.format(cal.getTime());

            return new String[]{startDate, endDate};
        } catch (Exception e) {
            android.util.Log.e("ReportGrouper", "Erreur calcul plage semaine", e);
            return new String[]{"--/--", "--/--"};
        }
    }

    /**
     * Génère un sous-titre pour un mois (ex: "4 semaines • 22 jours ouvrés")
     */
    private static String getMonthSubtitle(Calendar monthCalendar) {
        Calendar cal = (Calendar) monthCalendar.clone();
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);

        // Compter les semaines du mois
        cal.set(year, month, 1);
        int firstWeek = cal.get(Calendar.WEEK_OF_YEAR);
        cal.set(year, month, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        int lastWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int weekCount = lastWeek - firstWeek + 1;

        // Compter les jours ouvrés (Lundi-Vendredi)
        cal.set(year, month, 1);
        int workDays = 0;
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= maxDay; day++) {
            cal.set(year, month, day);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY) {
                workDays++;
            }
        }

        return weekCount + " semaines • " + workDays + " jours ouvrés";
    }

    // ==================== STATISTIQUES GLOBALES ====================

    /**
     * Calcule les statistiques globales pour une liste de rapports
     */
    public static class GlobalStats {
        public double totalHours;
        public int reportCount;
        public double averageHoursPerDay;
        public int uniqueDaysCount;
        public int uniqueProjectsCount;

        public String getFormattedTotalHours() {
            return String.format(Locale.FRANCE, "%.2fh", totalHours);
        }

        public String getFormattedAverageHours() {
            return String.format(Locale.FRANCE, "%.2fh", averageHoursPerDay);
        }
    }

    /**
     * Calcule les statistiques globales
     */
    public static GlobalStats calculateGlobalStats(List<TimeReport> reports) {
        try {
            FileLogger.d("GROUPER", ">>> calculateGlobalStats() START - " + (reports != null ? reports.size() : "NULL") + " rapports");
            GlobalStats stats = new GlobalStats();

            if (reports == null || reports.isEmpty()) {
                FileLogger.d("GROUPER", "⚠️ Liste vide/null");
                return stats;
            }

            java.util.Set<String> uniqueDates = new java.util.HashSet<>();
            java.util.Set<Integer> uniqueProjects = new java.util.HashSet<>();

            for (int i = 0; i < reports.size(); i++) {
                try {
                    TimeReport report = reports.get(i);
                    if (report == null) {
                        FileLogger.e("GROUPER", "⚠️ Report NULL à l'index " + i, null);
                        continue;
                    }

                    // Ligne par ligne pour capturer le crash exact
                    try {
                        double hours = report.getHours();
                        FileLogger.d("GROUPER", "Report " + i + " - hours: " + hours);
                        stats.totalHours += hours;
                    } catch (Exception e) {
                        FileLogger.e("GROUPER", "❌ CRASH getHours() pour report " + i, e);
                        throw e;
                    }

                    stats.reportCount++;

                    try {
                        String date = report.getReportDate();
                        if (date != null && !date.isEmpty()) {
                            uniqueDates.add(date);
                        }
                    } catch (Exception e) {
                        FileLogger.e("GROUPER", "❌ CRASH getReportDate() pour report " + i, e);
                        throw e;
                    }

                    try {
                        if (report.getProjectId() > 0) {
                            uniqueProjects.add(report.getProjectId());
                        }
                    } catch (Exception e) {
                        FileLogger.e("GROUPER", "❌ CRASH getProjectId() pour report " + i, e);
                        throw e;
                    }
                } catch (Exception e) {
                    FileLogger.e("GROUPER", "Erreur traitement report " + i + " dans stats", e);
                }
            }

            stats.uniqueDaysCount = uniqueDates.size();
            stats.uniqueProjectsCount = uniqueProjects.size();
            stats.averageHoursPerDay = stats.uniqueDaysCount > 0 ? stats.totalHours / stats.uniqueDaysCount : 0.0;

            FileLogger.d("GROUPER", ">>> calculateGlobalStats() END ✅");
            return stats;
        } catch (Exception e) {
            FileLogger.e("GROUPER", "❌ CRASH dans calculateGlobalStats", e);
            return new GlobalStats();
        }
    }
}
