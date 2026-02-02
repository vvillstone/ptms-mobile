package com.ptms.mobile.models;

/**
 * Item unifié pour l'Agenda (rapports + notes)
 */
public class AgendaItem {

    public enum Type {
        REPORT,  // Rapport de temps
        NOTE     // Note de projet
    }

    private Type type;
    private TimeReport report;  // Si type = REPORT
    private ProjectNote note;   // Si type = NOTE
    private String date;        // Date commune (yyyy-MM-dd)

    // Constructor pour rapport
    public static AgendaItem fromReport(TimeReport report) {
        AgendaItem item = new AgendaItem();
        item.type = Type.REPORT;
        item.report = report;
        item.date = report.getReportDate();
        return item;
    }

    // Constructor pour note
    public static AgendaItem fromNote(ProjectNote note) {
        AgendaItem item = new AgendaItem();
        item.type = Type.NOTE;
        item.note = note;
        // Extraire la date de createdAt (2025-10-14 12:34:56 -> 2025-10-14)
        String createdAt = note.getCreatedAt();
        if (createdAt != null && createdAt.length() >= 10) {
            item.date = createdAt.substring(0, 10);
        } else {
            item.date = "";
        }
        return item;
    }

    public Type getType() {
        return type;
    }

    public TimeReport getReport() {
        return report;
    }

    public ProjectNote getNote() {
        return note;
    }

    public String getDate() {
        return date;
    }

    public String getDisplayTitle() {
        if (type == Type.REPORT) {
            return "⏱️ " + (report.getProjectName() != null ? report.getProjectName() : "Rapport");
        } else {
            String icon = note.getGroupIcon();
            String title = note.getTitle();
            if (title == null || title.isEmpty()) {
                title = "Note " + note.getNoteType();
            }
            return icon + " " + title;
        }
    }

    public String getDisplaySubtitle() {
        if (type == Type.REPORT) {
            return String.format("%.2fh • %s", report.getHours(),
                    report.getWorkTypeName() != null ? report.getWorkTypeName() : "");
        } else {
            if (note.getProjectName() != null && !note.getProjectName().isEmpty()) {
                return "📊 " + note.getProjectName();
            } else {
                return "👤 Note personnelle";
            }
        }
    }
}
