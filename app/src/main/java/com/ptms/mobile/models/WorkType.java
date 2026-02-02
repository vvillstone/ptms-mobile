package com.ptms.mobile.models;

/**
 * Modèle Type de Travail
 */
public class WorkType {
    private int id;
    private String name;
    private String description;
    private int status;
    private String dateCreated;
    private String dateUpdated;

    // Constructeurs
    public WorkType() {}

    public WorkType(int id, String name, String description) {
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

    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }

    public String getDateUpdated() { return dateUpdated; }
    public void setDateUpdated(String dateUpdated) { this.dateUpdated = dateUpdated; }

    // Méthodes utilitaires
    public boolean isActive() {
        return status == 1;
    }
}





