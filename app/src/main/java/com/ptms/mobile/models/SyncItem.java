package com.ptms.mobile.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Modèle représentant un élément de synchronisation
 */
public class SyncItem {

    public enum SyncType {
        TIME_REPORT("Rapport de temps"),
        PROJECT_NOTE("Note de projet"),
        MEDIA("Média");

        private final String label;

        SyncType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum SyncStatus {
        PENDING("En attente"),
        SYNCING("Synchronisation..."),
        SYNCED("Synchronisé"),
        ERROR("Erreur");

        private final String label;

        SyncStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private long id;
    private SyncType type;
    private SyncStatus status;
    private String title;
    private String subtitle;
    private Date date;
    private String errorMessage;

    public SyncItem(long id, SyncType type, SyncStatus status, String title, String subtitle, Date date) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.title = title;
        this.subtitle = subtitle;
        this.date = date;
    }

    // Getters
    public long getId() {
        return id;
    }

    public SyncType getType() {
        return type;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Date getDate() {
        return date;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // Setters
    public void setStatus(SyncStatus status) {
        this.status = status;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Helpers
    public String getTypeLabel() {
        return type.getLabel();
    }

    public String getStatusLabel() {
        return status.getLabel();
    }

    public String getDateFormatted() {
        if (date == null) {
            return "";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return dateFormat.format(date);
    }

    /**
     * Crée un SyncItem depuis un TimeReport
     */
    public static SyncItem fromTimeReport(TimeReport report) {
        SyncStatus status;
        if (report.isSynced()) {
            status = SyncStatus.SYNCED;
        } else {
            status = SyncStatus.PENDING;
        }

        String title = String.format(Locale.getDefault(), "%.2fh - %s",
            report.getHours(), report.getProjectName());
        String subtitle = report.getDescription();

        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            date = sdf.parse(report.getReportDate());
        } catch (Exception e) {
            date = new Date();
        }

        // Utiliser serverId si disponible, sinon utiliser id
        long itemId = (report.getServerId() != null && report.getServerId() > 0)
            ? report.getServerId()
            : report.getId();

        return new SyncItem(
            itemId,
            SyncType.TIME_REPORT,
            status,
            title,
            subtitle,
            date
        );
    }

    /**
     * Crée un SyncItem depuis une ProjectNote
     */
    public static SyncItem fromProjectNote(ProjectNote note) {
        SyncStatus status;
        if (note.isSynced()) {
            status = SyncStatus.SYNCED;
        } else {
            status = SyncStatus.PENDING;
        }

        String title = note.getTitle() != null ? note.getTitle() : "Note sans titre";
        String subtitle = note.getProjectName();

        // Convertir createdAt (String) en Date
        Date date = new Date();
        if (note.getCreatedAt() != null && !note.getCreatedAt().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                date = sdf.parse(note.getCreatedAt());
            } catch (Exception e) {
                // Garder la date par défaut en cas d'erreur
            }
        }

        return new SyncItem(
            note.getLocalId(),
            SyncType.PROJECT_NOTE,
            status,
            title,
            subtitle,
            date
        );
    }
}
