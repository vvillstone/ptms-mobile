package com.ptms.mobile.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle pour une note de projet
 */
public class ProjectNote implements Serializable {
    private int id;
    private Integer projectId; // Nullable pour notes personnelles
    private String projectName; // Nom du projet (du serveur)
    private int userId;
    private String noteType; // text, audio, dictation
    private Integer noteTypeId; // ID de la catégorie (NoteType)
    private String noteTypeName; // Nom de la catégorie
    private String noteTypeSlug; // Slug de la catégorie
    private String noteTypeIcon; // Icône de la catégorie
    private String noteTypeColor; // Couleur de la catégorie
    private String noteGroup; // project, personal, meeting, todo, idea, issue, other (legacy)
    private String title;
    private String content;
    private String audioPath;
    private Integer audioDuration; // en secondes
    private String transcription;
    private boolean isImportant;
    private List<String> tags;
    private String authorName;
    private String createdAt;
    private String updatedAt;

    // Champs pour la synchronisation hors ligne
    private boolean isSynced;
    private String syncStatus; // pending, syncing, synced, failed
    private String syncError;
    private int syncAttempts;
    private String localAudioPath; // Chemin local du fichier audio (legacy)
    private long localId; // ID local avant synchronisation

    // Champs pour support multimédia (Phase 2 - Offline-First)
    private String localFilePath; // Chemin local du fichier (audio, image, vidéo)
    private String serverUrl; // URL du fichier sur le serveur après upload
    private Long fileSize; // Taille du fichier en bytes
    private String mimeType; // Type MIME (audio/m4a, image/jpeg, video/mp4)
    private String thumbnailPath; // Chemin de la miniature (images/vidéos)
    private Integer uploadProgress; // Progress upload 0-100%

    // Champs additionnels pour gestion complète des notes
    private String priority; // low, medium, high, urgent
    private String scheduledDate; // Date planifiée YYYY-MM-DD HH:MM:SS
    private String reminderDate; // Date de rappel YYYY-MM-DD HH:MM:SS
    private Integer serverId; // ID sur le serveur après sync

    public ProjectNote() {
        this.tags = new ArrayList<>();
        this.isSynced = false;
        this.syncStatus = "pending";
        this.syncAttempts = 0;
        this.noteGroup = "project"; // Valeur par défaut
    }

    // Getters
    public int getId() { return id; }
    public Integer getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public int getUserId() { return userId; }
    public String getNoteType() { return noteType; }
    public Integer getNoteTypeId() { return noteTypeId; }
    public String getNoteTypeName() { return noteTypeName; }
    public String getNoteTypeSlug() { return noteTypeSlug; }
    public String getNoteTypeIcon() { return noteTypeIcon; }
    public String getNoteTypeColor() { return noteTypeColor; }
    public String getNoteGroup() { return noteGroup; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAudioPath() { return audioPath; }
    public Integer getAudioDuration() { return audioDuration; }
    public String getTranscription() { return transcription; }
    public boolean isImportant() { return isImportant; }
    public List<String> getTags() { return tags; }
    public String getAuthorName() { return authorName; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public boolean isSynced() { return isSynced; }
    public String getSyncStatus() { return syncStatus; }
    public String getSyncError() { return syncError; }
    public int getSyncAttempts() { return syncAttempts; }
    public String getLocalAudioPath() { return localAudioPath; }
    public long getLocalId() { return localId; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setNoteType(String noteType) { this.noteType = noteType; }
    public void setNoteTypeId(Integer noteTypeId) { this.noteTypeId = noteTypeId; }
    public void setNoteTypeName(String noteTypeName) { this.noteTypeName = noteTypeName; }
    public void setNoteTypeSlug(String noteTypeSlug) { this.noteTypeSlug = noteTypeSlug; }
    public void setNoteTypeIcon(String noteTypeIcon) { this.noteTypeIcon = noteTypeIcon; }
    public void setNoteTypeColor(String noteTypeColor) { this.noteTypeColor = noteTypeColor; }
    public void setNoteGroup(String noteGroup) { this.noteGroup = noteGroup; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public void setAudioDuration(Integer audioDuration) { this.audioDuration = audioDuration; }
    public void setTranscription(String transcription) { this.transcription = transcription; }
    public void setImportant(boolean important) { isImportant = important; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public void setSynced(boolean synced) { isSynced = synced; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public void setSyncError(String syncError) { this.syncError = syncError; }
    public void setSyncAttempts(int syncAttempts) { this.syncAttempts = syncAttempts; }
    public void setLocalAudioPath(String localAudioPath) { this.localAudioPath = localAudioPath; }
    public void setLocalId(long localId) { this.localId = localId; }

    // Getters/Setters pour support multimédia (Phase 2)
    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public Integer getUploadProgress() { return uploadProgress; }
    public void setUploadProgress(Integer uploadProgress) { this.uploadProgress = uploadProgress; }

    // Getters/Setters pour champs additionnels
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getReminderDate() { return reminderDate; }
    public void setReminderDate(String reminderDate) { this.reminderDate = reminderDate; }

    public Integer getServerId() { return serverId; }
    public void setServerId(Integer serverId) { this.serverId = serverId; }

    /**
     * Retourne une icône selon le type de note
     */
    public String getTypeIcon() {
        switch (noteType) {
            case "audio": return "🎤";
            case "dictation": return "🗣️";
            default: return "📝";
        }
    }

    /**
     * Retourne une icône selon le groupe de note (LEGACY FALLBACK)
     * Utilisé en fallback par getCategoryEmoji()
     * @deprecated Utilisez getCategoryEmoji() à la place
     */
    @Deprecated
    public String getGroupIcon() {
        if (noteGroup == null) return "📁";
        switch (noteGroup) {
            case "personal": return "👤";
            case "meeting": return "🤝";
            case "todo": return "✅";
            case "idea": return "💡";
            case "issue": return "⚠️";
            case "other": return "📌";
            default: return "📁"; // project
        }
    }

    /**
     * Retourne l'emoji de la catégorie (nouveau système)
     */
    public String getCategoryEmoji() {
        // Si on a un slug de catégorie, l'utiliser
        if (noteTypeSlug != null && !noteTypeSlug.isEmpty()) {
            switch (noteTypeSlug) {
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
            }
        }
        // Fallback sur le nom de catégorie si disponible
        if (noteTypeName != null && !noteTypeName.isEmpty()) {
            return "🏷️"; // Icône générique pour catégories personnalisées
        }
        // Legacy fallback sur noteGroup
        return getGroupIcon();
    }

    /**
     * Retourne la couleur de la catégorie au format Android Color
     */
    public int getCategoryColor() {
        if (noteTypeColor != null && noteTypeColor.startsWith("#")) {
            try {
                return android.graphics.Color.parseColor(noteTypeColor);
            } catch (IllegalArgumentException e) {
                // Couleur invalide
            }
        }
        // Couleur par défaut (gris)
        return android.graphics.Color.parseColor("#6c757d");
    }

    /**
     * Retourne true si la note est une note personnelle (sans projet)
     */
    public boolean isPersonalNote() {
        return projectId == null || projectId == 0;
    }

    /**
     * Formatte la durée audio en mm:ss
     */
    public String getFormattedDuration() {
        if (audioDuration == null || audioDuration == 0) return "";
        int mins = audioDuration / 60;
        int secs = audioDuration % 60;
        return String.format("%02d:%02d", mins, secs);
    }


    /**
     * Retourne le contenu complet de la note (sans limitation de longueur)
     */
    public String getFullContent() {
        if ("text".equals(noteType) && content != null) {
            return content;
        } else if (("audio".equals(noteType) || "dictation".equals(noteType)) && transcription != null) {
            return transcription;
        }
        return "";
    }
}
