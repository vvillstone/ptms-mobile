package com.ptms.mobile.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ProjectNote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter pour afficher les notes dans l'Agenda
 */
public class AgendaNotesAdapter extends RecyclerView.Adapter<AgendaNotesAdapter.NoteViewHolder> {

    private List<ProjectNote> notes = new ArrayList<>();
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(ProjectNote note);
    }

    public AgendaNotesAdapter(OnNoteClickListener listener) {
        this.listener = listener;
    }

    public void setNotes(List<ProjectNote> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_agenda_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        ProjectNote note = notes.get(position);
        holder.bind(note, listener);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNoteTypeBadge;
        private TextView tvNoteTitle;
        private TextView tvImportantBadge;
        private TextView tvNoteContent;
        private TextView tvNoteTime;
        private TextView tvNoteProject;
        private TextView tvNoteCategory;
        private TextView tvNoteTags;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoteTypeBadge = itemView.findViewById(R.id.tvNoteTypeBadge);
            tvNoteTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvImportantBadge = itemView.findViewById(R.id.tvImportantBadge);
            tvNoteContent = itemView.findViewById(R.id.tvNoteContent);
            tvNoteTime = itemView.findViewById(R.id.tvNoteTime);
            tvNoteProject = itemView.findViewById(R.id.tvNoteProject);
            tvNoteCategory = itemView.findViewById(R.id.tvNoteCategory);
            tvNoteTags = itemView.findViewById(R.id.tvNoteTags);
        }

        public void bind(ProjectNote note, OnNoteClickListener listener) {
            // Type de note badge
            String typeBadge = "📝"; // default text
            if (note.getNoteType() != null) {
                switch (note.getNoteType()) {
                    case "text":
                        typeBadge = "📝";
                        break;
                    case "dictation":
                        typeBadge = "🎤";
                        break;
                    case "audio":
                        typeBadge = "🔊";
                        break;
                }
            }
            tvNoteTypeBadge.setText(typeBadge);

            // Titre
            tvNoteTitle.setText(note.getTitle() != null ? note.getTitle() : "Sans titre");

            // Badge important
            if (note.isImportant()) {
                tvImportantBadge.setVisibility(View.VISIBLE);
            } else {
                tvImportantBadge.setVisibility(View.GONE);
            }

            // Contenu
            if (note.getContent() != null && !note.getContent().isEmpty()) {
                tvNoteContent.setText(note.getContent());
                tvNoteContent.setVisibility(View.VISIBLE);
            } else if (note.getTranscription() != null && !note.getTranscription().isEmpty()) {
                tvNoteContent.setText(note.getTranscription());
                tvNoteContent.setVisibility(View.VISIBLE);
            } else if ("audio".equals(note.getNoteType())) {
                tvNoteContent.setText("🔊 Note audio (" + formatDuration(note.getAudioDuration()) + ")");
                tvNoteContent.setVisibility(View.VISIBLE);
            } else {
                tvNoteContent.setVisibility(View.GONE);
            }

            // Heure de création
            if (note.getCreatedAt() != null) {
                try {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
                    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                            .parse(note.getCreatedAt());
                    if (date != null) {
                        tvNoteTime.setText("🕐 " + timeFormat.format(date));
                    } else {
                        tvNoteTime.setText("");
                    }
                } catch (Exception e) {
                    tvNoteTime.setText("");
                }
            } else {
                tvNoteTime.setText("");
            }

            // Projet
            if (note.getProjectName() != null && !note.getProjectName().isEmpty()) {
                tvNoteProject.setText("📁 " + note.getProjectName());
                tvNoteProject.setVisibility(View.VISIBLE);
            } else {
                tvNoteProject.setVisibility(View.GONE);
            }

            // Catégorie
            if (note.getNoteGroup() != null && !note.getNoteGroup().isEmpty()) {
                String categoryName = getCategoryDisplayName(note.getNoteGroup());
                tvNoteCategory.setText(categoryName);
                tvNoteCategory.setBackgroundColor(getCategoryColor(note.getNoteGroup()));
                tvNoteCategory.setVisibility(View.VISIBLE);
            } else {
                tvNoteCategory.setVisibility(View.GONE);
            }

            // Tags
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                StringBuilder tagsText = new StringBuilder();
                for (int i = 0; i < note.getTags().size(); i++) {
                    if (i > 0) tagsText.append(" ");
                    tagsText.append("#").append(note.getTags().get(i));
                }
                tvNoteTags.setText(tagsText.toString());
                tvNoteTags.setVisibility(View.VISIBLE);
            } else {
                tvNoteTags.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNoteClick(note);
                }
            });
        }

        private String formatDuration(Integer seconds) {
            if (seconds == null || seconds == 0) {
                return "0:00";
            }
            int mins = seconds / 60;
            int secs = seconds % 60;
            return String.format(Locale.getDefault(), "%d:%02d", mins, secs);
        }

        private String getCategoryDisplayName(String slug) {
            switch (slug) {
                case "project": return "Projet";
                case "personal": return "Personnel";
                case "meeting": return "Réunion";
                case "idea": return "Idée";
                case "task": return "Tâche";
                default: return slug;
            }
        }

        private int getCategoryColor(String slug) {
            switch (slug) {
                case "project": return Color.parseColor("#1976D2");
                case "personal": return Color.parseColor("#FF9800");
                case "meeting": return Color.parseColor("#9C27B0");
                case "idea": return Color.parseColor("#4CAF50");
                case "task": return Color.parseColor("#F44336");
                default: return Color.parseColor("#757575");
            }
        }
    }
}
