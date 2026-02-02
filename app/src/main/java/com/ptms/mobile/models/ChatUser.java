package com.ptms.mobile.models;

import java.util.Date;

/**
 * Modèle pour les utilisateurs de chat PTMS
 */
public class ChatUser {
    private int id;
    private String name;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private String avatar;
    private String chatPseudo;
    private String displayName;
    private String role; // Rôle de l'utilisateur (Administrateur, Employé, etc.)
    private String department;
    private String position;
    private boolean isOnline;
    private Long lastSeen; // Timestamp Unix en secondes (ou null)
    private boolean isTyping;
    private String status; // "available", "busy", "away", "offline"
    
    // Constructeurs
    public ChatUser() {}
    
    public ChatUser(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.isOnline = false;
        this.isTyping = false;
        this.status = "offline";
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public void setOnline(boolean online) {
        isOnline = online;
    }
    
    public Long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Long lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Convertit le timestamp en Date pour affichage
     */
    public Date getLastSeenAsDate() {
        if (lastSeen == null) {
            return null;
        }
        return new Date(lastSeen * 1000); // Convertir secondes en millisecondes
    }
    
    public boolean isTyping() {
        return isTyping;
    }
    
    public void setTyping(boolean typing) {
        isTyping = typing;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getChatPseudo() {
        return chatPseudo;
    }

    public void setChatPseudo(String chatPseudo) {
        this.chatPseudo = chatPseudo;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Méthodes utilitaires
    public String getDisplayName() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        if (chatPseudo != null && !chatPseudo.isEmpty()) {
            return chatPseudo;
        }
        String fullName = "";
        if (firstname != null && !firstname.isEmpty()) {
            fullName = firstname;
        }
        if (lastname != null && !lastname.isEmpty()) {
            fullName += (fullName.isEmpty() ? "" : " ") + lastname;
        }
        if (!fullName.isEmpty()) {
            return fullName;
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        if (username != null && !username.isEmpty()) {
            return username;
        }
        return email != null ? email : "Utilisateur";
    }
    
    public String getInitials() {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, 1).toUpperCase();
    }
    
    public boolean isAvailable() {
        return "available".equals(status);
    }
    
    public boolean isBusy() {
        return "busy".equals(status);
    }
    
    public boolean isAway() {
        return "away".equals(status);
    }
    
    public boolean isOffline() {
        return "offline".equals(status);
    }
    
    public String getStatusText() {
        if (isOnline) {
            if (isTyping) {
                return "En train d'écrire...";
            }
            switch (status) {
                case "available":
                    return "Disponible";
                case "busy":
                    return "Occupé";
                case "away":
                    return "Absent";
                default:
                    return "En ligne";
            }
        } else {
            return "Hors ligne";
        }
    }
    
    @Override
    public String toString() {
        return "ChatUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", isOnline=" + isOnline +
                ", status='" + status + '\'' +
                '}';
    }
}
