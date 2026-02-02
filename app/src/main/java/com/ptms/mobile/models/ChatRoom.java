package com.ptms.mobile.models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Modèle pour les salles de chat PTMS
 */
public class ChatRoom implements Serializable {
    private int id;
    private String name;
    private String displayName; // Nom d'affichage (pour les conversations privées, contient le nom du contact)
    private String description;
    private String roomType; // "direct", "group", "project", "department"
    private int projectId; // Si c'est un chat de projet
    private String projectName; // Nom du projet associé
    private String departmentName; // Nom du département associé
    private String type; // Alias pour roomType (compatibilité API)
    private List<ChatUser> participants;
    private ChatMessage lastMessage;
    private Date lastActivity;
    private boolean isActive;
    private int unreadCount;
    private String createdBy;
    private Date createdAt;
    
    // Constructeurs
    public ChatRoom() {}
    
    public ChatRoom(String name, String description, String roomType) {
        this.name = name;
        this.description = description;
        this.roomType = roomType;
        this.isActive = true;
        this.unreadCount = 0;
        this.createdAt = new Date();
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayNameField() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRoomType() {
        return roomType;
    }
    
    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }
    
    public int getProjectId() {
        return projectId;
    }
    
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getType() {
        return type != null ? type : roomType;
    }

    public void setType(String type) {
        this.type = type;
        this.roomType = type; // Synchroniser roomType
    }

    public List<ChatUser> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<ChatUser> participants) {
        this.participants = participants;
    }
    
    public ChatMessage getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(ChatMessage lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public Date getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    // Méthodes utilitaires
    public boolean isProjectChat() {
        return "project".equals(roomType);
    }
    
    public boolean isDepartmentChat() {
        return "department".equals(roomType);
    }
    
    public boolean isGeneralChat() {
        return "general".equals(roomType);
    }
    
    public boolean isPrivateChat() {
        return "private".equals(roomType) || "direct".equals(roomType);
    }

    public boolean isGroupChat() {
        return "group".equals(roomType);
    }

    public boolean hasUnreadMessages() {
        return unreadCount > 0;
    }
    
    public String getDisplayName() {
        // ✅ CORRECTION: Utiliser displayName de l'API s'il existe (pour les conversations privées)
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }

        // Sinon, utiliser la logique existante
        if (isProjectChat() && projectName != null) {
            return projectName + " - " + name;
        }
        if (isDepartmentChat() && departmentName != null) {
            return departmentName + " - " + name;
        }
        return name != null ? name : "Conversation";
    }

    /**
     * Retourne l'icône correspondant au type de conversation
     */
    public String getTypeIcon() {
        if (isPrivateChat()) return "💬";
        if (isGroupChat()) return "👥";
        if (isProjectChat()) return "📁";
        if (isDepartmentChat()) return "🏢";
        return "💬";
    }

    /**
     * Retourne le label du type de conversation
     */
    public String getTypeLabel() {
        if (isPrivateChat()) return "Privée";
        if (isGroupChat()) return "Groupe";
        if (isProjectChat()) return "Projet";
        if (isDepartmentChat()) return "Département";
        return "Conversation";
    }
    
    @Override
    public String toString() {
        return "ChatRoom{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", roomType='" + roomType + '\'' +
                ", projectId=" + projectId +
                ", isActive=" + isActive +
                ", unreadCount=" + unreadCount +
                '}';
    }
}
