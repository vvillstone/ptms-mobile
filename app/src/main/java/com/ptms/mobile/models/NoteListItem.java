package com.ptms.mobile.models;

/**
 * Item générique pour la liste des notes (peut être un header de date ou une note)
 */
public class NoteListItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_NOTE = 1;

    private int type;
    private String dateHeader; // Pour TYPE_HEADER
    private ProjectNote note;  // Pour TYPE_NOTE

    private NoteListItem(int type) {
        this.type = type;
    }

    public static NoteListItem createHeader(String dateHeader) {
        NoteListItem item = new NoteListItem(TYPE_HEADER);
        item.dateHeader = dateHeader;
        return item;
    }

    public static NoteListItem createNote(ProjectNote note) {
        NoteListItem item = new NoteListItem(TYPE_NOTE);
        item.note = note;
        return item;
    }

    public int getType() {
        return type;
    }

    public String getDateHeader() {
        return dateHeader;
    }

    public ProjectNote getNote() {
        return note;
    }
}
