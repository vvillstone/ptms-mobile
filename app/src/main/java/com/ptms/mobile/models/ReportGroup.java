package com.ptms.mobile.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe de regroupement de rapports (par jour, semaine ou mois)
 * Permet d'agréger des TimeReport selon différentes périodes
 */
public class ReportGroup {

    // Type de regroupement
    public enum GroupType {
        DAY,        // Regroupement par jour
        WEEK,       // Regroupement par semaine
        MONTH       // Regroupement par mois
    }

    private GroupType groupType;
    private String title;               // Ex: "Lundi 16 Oct", "Semaine 42", "Octobre 2025"
    private String subtitle;            // Ex: "16/10/2025", "16-22 Oct", "4 semaines"
    private List<TimeReport> reports;   // Liste des rapports dans ce groupe
    private double totalHours;          // Total des heures
    private int reportCount;            // Nombre de rapports

    // Pour les regroupements hiérarchiques
    private List<ReportGroup> subGroups;  // Ex: jours dans une semaine, semaines dans un mois

    // Constructeur
    public ReportGroup(GroupType groupType, String title, String subtitle) {
        this.groupType = groupType;
        this.title = title;
        this.subtitle = subtitle;
        this.reports = new ArrayList<>();
        this.subGroups = new ArrayList<>();
        this.totalHours = 0.0;
        this.reportCount = 0;
    }

    // ==================== AJOUT DE RAPPORTS ====================

    /**
     * Ajoute un rapport au groupe et recalcule les statistiques
     */
    public void addReport(TimeReport report) {
        reports.add(report);
        totalHours += report.getHours();
        reportCount++;
    }

    /**
     * Ajoute plusieurs rapports
     */
    public void addReports(List<TimeReport> reports) {
        for (TimeReport report : reports) {
            addReport(report);
        }
    }

    /**
     * Ajoute un sous-groupe (pour hiérarchie)
     */
    public void addSubGroup(ReportGroup subGroup) {
        subGroups.add(subGroup);
        totalHours += subGroup.getTotalHours();
        reportCount += subGroup.getReportCount();
    }

    // ==================== GETTERS ====================

    public GroupType getGroupType() {
        return groupType;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public List<TimeReport> getReports() {
        return reports;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public int getReportCount() {
        return reportCount;
    }

    public List<ReportGroup> getSubGroups() {
        return subGroups;
    }

    // ==================== SETTERS ====================

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    // ==================== STATISTIQUES ====================

    /**
     * Calcule la moyenne d'heures par jour
     */
    public double getAverageHoursPerDay() {
        int uniqueDays = getUniqueDaysCount();
        return uniqueDays > 0 ? totalHours / uniqueDays : 0.0;
    }

    /**
     * Compte le nombre de jours uniques dans les rapports
     */
    public int getUniqueDaysCount() {
        java.util.Set<String> uniqueDates = new java.util.HashSet<>();
        for (TimeReport report : reports) {
            uniqueDates.add(report.getReportDate());
        }
        return uniqueDates.size();
    }

    /**
     * Compte le nombre de projets distincts
     */
    public int getUniqueProjectsCount() {
        java.util.Set<Integer> uniqueProjects = new java.util.HashSet<>();
        for (TimeReport report : reports) {
            if (report.getProjectId() > 0) {
                uniqueProjects.add(report.getProjectId());
            }
        }
        return uniqueProjects.size();
    }

    /**
     * Compte le nombre de types de travail distincts
     */
    public int getUniqueWorkTypesCount() {
        java.util.Set<Integer> uniqueWorkTypes = new java.util.HashSet<>();
        for (TimeReport report : reports) {
            uniqueWorkTypes.add(report.getWorkTypeId());
        }
        return uniqueWorkTypes.size();
    }

    /**
     * Retourne le total des heures formaté (ex: "35.50h")
     */
    public String getFormattedTotalHours() {
        return String.format(java.util.Locale.FRANCE, "%.2fh", totalHours);
    }

    /**
     * Retourne le compteur de rapports (ex: "12 rapports")
     */
    public String getFormattedReportCount() {
        return reportCount + (reportCount > 1 ? " rapports" : " rapport");
    }

    // ==================== UTILITAIRES ====================

    /**
     * Vérifie si le groupe est vide
     */
    public boolean isEmpty() {
        return reportCount == 0;
    }

    /**
     * Trie les rapports par date (croissant)
     */
    public void sortReportsByDate(boolean ascending) {
        reports.sort((r1, r2) -> {
            int comparison = r1.getReportDate().compareTo(r2.getReportDate());
            return ascending ? comparison : -comparison;
        });
    }

    /**
     * Trie les rapports par heures
     */
    public void sortReportsByHours(boolean ascending) {
        reports.sort((r1, r2) -> {
            int comparison = Double.compare(r1.getHours(), r2.getHours());
            return ascending ? comparison : -comparison;
        });
    }

    @Override
    public String toString() {
        return "ReportGroup{" +
                "type=" + groupType +
                ", title='" + title + '\'' +
                ", reports=" + reportCount +
                ", hours=" + getFormattedTotalHours() +
                ", subGroups=" + subGroups.size() +
                '}';
    }
}
