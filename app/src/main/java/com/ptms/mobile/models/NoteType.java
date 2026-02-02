package com.ptms.mobile.models;

import java.io.Serializable;

/**
 * Modèle pour un type de note (catégorie)
 * Correspond à la table note_types
 */
public class NoteType implements Serializable {
    private int id;
    private Integer userId; // null = type système
    private String name;
    private String slug;
    private String icon; // Classe FontAwesome (ex: fa-fire)
    private String color; // Couleur hexadécimale (ex: #ff0000)
    private String description;
    private boolean isSystem;
    private int sortOrder;
    private String createdAt;
    private String updatedAt;

    public NoteType() {
        this.isSystem = false;
        this.sortOrder = 0;
        this.color = "#6c757d";
        this.icon = "fa-folder";
    }

    // Getters
    public int getId() { return id; }
    public Integer getUserId() { return userId; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public String getDescription() { return description; }
    public boolean isSystem() { return isSystem; }
    public int getSortOrder() { return sortOrder; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setColor(String color) { this.color = color; }
    public void setDescription(String description) { this.description = description; }
    public void setSystem(boolean system) { isSystem = system; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Retourne une icône emoji selon le slug
     */
    public String getEmoji() {
        if (slug == null) return "📁";
        switch (slug) {
            case "project": return "📊";
            case "personal": return "👤";
            case "meeting": return "👥";
            case "todo": return "✅";
            case "idea": return "💡";
            case "issue": return "⚠️";
            case "urgent": return "🔥";
            case "client": return "🤝";
            case "documentation": return "📚";
            case "other": return "📁";
            default: return "📁";
        }
    }

    /**
     * Parse la couleur pour Android (format #AARRGGBB ou #RRGGBB)
     */
    public int getColorInt() {
        try {
            if (color != null && color.startsWith("#")) {
                return android.graphics.Color.parseColor(color);
            }
        } catch (IllegalArgumentException e) {
            // Couleur invalide, retourner gris par défaut
        }
        return android.graphics.Color.parseColor("#6c757d");
    }

    /**
     * Retourne true si c'est un type personnalisé (non système)
     */
    public boolean isCustom() {
        return !isSystem && userId != null;
    }

    @Override
    public String toString() {
        return name != null ? name : "Sans catégorie";
    }
}
