package com.ptms.mobile.models;

/**
 * Modèle Projet
 */
public class Project {
    private int id;
    private String name;
    private String description;
    private int status;
    private boolean isPlaceholder;
    private String assignedUserId;
    private String client;
    private String priority;
    private double progress;
    private String dateCreated;
    private String dateUpdated;

    // Constructeurs
    public Project() {}

    public Project(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public boolean isPlaceholder() { return isPlaceholder; }
    public void setPlaceholder(boolean placeholder) { isPlaceholder = placeholder; }

    public String getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(String assignedUserId) { this.assignedUserId = assignedUserId; }

    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }

    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }

    public String getDateUpdated() { return dateUpdated; }
    public void setDateUpdated(String dateUpdated) { this.dateUpdated = dateUpdated; }

    // Méthodes utilitaires
    public boolean isActive() {
        return status == 1;
    }

    public String getStatusText() {
        return status == 1 ? "Actif" : "Inactif";
    }
}














