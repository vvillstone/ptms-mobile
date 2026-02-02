package com.ptms.mobile.models;

/**
 * Modèle Rapport de Temps
 */
public class TimeReport {
    private int id;
    private int projectId;
    private int employeeId;
    private int workTypeId;
    private String reportDate;
    private String datetimeFrom;
    private String datetimeTo;
    private double hours;
    private String description;
    private String validationStatus;
    private String projectName;
    private String workTypeName;
    private String dateCreated;
    private String dateUpdated;

    // Champ pour indiquer le statut de synchronisation (local uniquement)
    private boolean isSynced = true;  // Par défaut true pour les rapports serveur
    private boolean isLocal = false;   // true si provient du cache JSON local

    // ✅ NOUVEAUX CHAMPS - Synchronisation bidirectionnelle
    private Integer serverId;          // ID sur le serveur (null si pas encore synchronisé)
    private String syncStatus;         // pending, synced, failed
    private String syncError;          // Message d'erreur si échec
    private int syncAttempts;          // Nombre de tentatives de synchronisation
    private String lastUpdated;        // Date de dernière modification (timestamp)

    // ✅ TIMEZONE SUPPORT - International operations
    private String timezone;           // User's timezone when report was created (e.g., "Europe/Paris")

    // ✅ IMAGE SUPPORT - Photo attachments
    private String imagePath;          // Path to attached image (e.g., "uploads/reports/image_12345.jpg")

    // Constructeurs
    public TimeReport() {}

    public TimeReport(int projectId, int employeeId, int workTypeId, 
                     String reportDate, String datetimeFrom, String datetimeTo, 
                     double hours, String description) {
        this.projectId = projectId;
        this.employeeId = employeeId;
        this.workTypeId = workTypeId;
        this.reportDate = reportDate;
        this.datetimeFrom = datetimeFrom;
        this.datetimeTo = datetimeTo;
        this.hours = hours;
        this.description = description;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public int getWorkTypeId() { return workTypeId; }
    public void setWorkTypeId(int workTypeId) { this.workTypeId = workTypeId; }

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public String getDatetimeFrom() { return datetimeFrom; }
    public void setDatetimeFrom(String datetimeFrom) { this.datetimeFrom = datetimeFrom; }

    public String getDatetimeTo() { return datetimeTo; }
    public void setDatetimeTo(String datetimeTo) { this.datetimeTo = datetimeTo; }

    public double getHours() { return hours; }
    public void setHours(double hours) { this.hours = hours; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getWorkTypeName() { return workTypeName; }
    public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }

    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }

    public String getDateUpdated() { return dateUpdated; }
    public void setDateUpdated(String dateUpdated) { this.dateUpdated = dateUpdated; }

    // ✅ NOUVEAUX GETTERS/SETTERS - Synchronisation bidirectionnelle
    public Integer getServerId() { return serverId; }
    public void setServerId(Integer serverId) { this.serverId = serverId; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getSyncError() { return syncError; }
    public void setSyncError(String syncError) { this.syncError = syncError; }

    public int getSyncAttempts() { return syncAttempts; }
    public void setSyncAttempts(int syncAttempts) { this.syncAttempts = syncAttempts; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    // Méthodes utilitaires
    public boolean hasImage() { return imagePath != null && !imagePath.isEmpty(); }

    public String getStatusText() {
        if (validationStatus == null) return "En attente";
        switch (validationStatus.toLowerCase()) {
            case "approved": return "Approuvé";
            case "rejected": return "Rejeté";
            default: return "En attente";
        }
    }

    public String getFormattedHours() {
        return String.format("%.2f", hours) + "h";
    }

    // Getters et setters pour les nouveaux champs
    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public boolean isLocal() { return isLocal; }
    public void setLocal(boolean local) { isLocal = local; }

    /**
     * Retourne le statut de synchronisation pour l'affichage
     * @return "Synchronisé" (vert) ou "En attente de synchronisation" (orange)
     */
    public String getSyncStatusText() {
        if (!isLocal) return "Synchronisé"; // Rapport serveur
        return isSynced ? "Synchronisé" : "En attente de synchronisation";
    }

    /**
     * Retourne la couleur pour l'indicateur de synchronisation
     * @return Code couleur Android (vert ou orange)
     */
    public int getSyncStatusColor() {
        if (!isLocal || isSynced) {
            return android.graphics.Color.parseColor("#4CAF50"); // Vert
        } else {
            return android.graphics.Color.parseColor("#FF9800"); // Orange
        }
    }
}














