package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ProjectNote;

import java.util.List;

/**
 * Adapter pour afficher les notes de projet
 */
public class ProjectNotesAdapter extends RecyclerView.Adapter<ProjectNotesAdapter.NoteViewHolder> {

    private Context context;
    private List<ProjectNote> notes;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(ProjectNote note);
        void onDeleteClick(ProjectNote note);
    }

    public ProjectNotesAdapter(Context context, List<ProjectNote> notes, OnNoteClickListener listener) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_project_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        ProjectNote note = notes.get(position);

        // Icône du type
        holder.tvType.setText(note.getTypeIcon());

        // Titre ou "Sans titre"
        String title = note.getTitle();
        if (title == null || title.isEmpty()) {
            title = "Sans titre";
        }
        holder.tvTitle.setText(title);

        // Contenu complet (sans limitation)
        String fullContent = note.getFullContent();
        holder.tvContent.setText(fullContent);
        holder.tvContent.setVisibility(fullContent.isEmpty() ? View.GONE : View.VISIBLE);

        // Auteur et date
        holder.tvAuthor.setText(note.getAuthorName() + " • " + formatDate(note.getCreatedAt()));

        // Important
        holder.tvImportant.setVisibility(note.isImportant() ? View.VISIBLE : View.GONE);

        // Durée audio et bouton play
        if ("audio".equals(note.getNoteType())) {
            if (note.getAudioDuration() != null && note.getAudioDuration() > 0) {
                holder.tvDuration.setText("🎵 " + note.getFormattedDuration());
                holder.tvDuration.setVisibility(View.VISIBLE);
            } else {
                holder.tvDuration.setText("🎵 Audio");
                holder.tvDuration.setVisibility(View.VISIBLE);
            }
        } else {
            holder.tvDuration.setVisibility(View.GONE);
        }

        // Sync Status Badge (Phase 3 Offline-First)
        updateSyncStatusBadge(holder, note);

        // Upload Progress Bar (Phase 3 Offline-First)
        updateUploadProgress(holder, note);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoteClick(note);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(note);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    /**
     * Met à jour le badge de statut de synchronisation
     */
    private void updateSyncStatusBadge(NoteViewHolder holder, ProjectNote note) {
        String syncStatus = note.getSyncStatus();

        // Si pas de fichier média, pas de badge sync
        if (note.getLocalFilePath() == null && note.getServerUrl() == null) {
            holder.tvSyncStatus.setVisibility(View.GONE);
            return;
        }

        // Afficher le badge avec le bon fond et texte selon le statut
        holder.tvSyncStatus.setVisibility(View.VISIBLE);

        if ("pending".equalsIgnoreCase(syncStatus)) {
            holder.tvSyncStatus.setText("📱 Local");
            holder.tvSyncStatus.setBackgroundResource(R.drawable.badge_pending);
        } else if ("syncing".equalsIgnoreCase(syncStatus) || "uploading".equalsIgnoreCase(syncStatus)) {
            Integer progress = note.getUploadProgress();
            String progressText = progress != null ? " " + progress + "%" : "";
            holder.tvSyncStatus.setText("📤 Upload" + progressText);
            holder.tvSyncStatus.setBackgroundResource(R.drawable.badge_uploading);
        } else if ("synced".equalsIgnoreCase(syncStatus)) {
            holder.tvSyncStatus.setText("☁️ Sync");
            holder.tvSyncStatus.setBackgroundResource(R.drawable.badge_synced);
        } else if ("failed".equalsIgnoreCase(syncStatus)) {
            holder.tvSyncStatus.setText("❌ Échec");
            holder.tvSyncStatus.setBackgroundResource(R.drawable.badge_failed);
        } else {
            // Statut inconnu, masquer le badge
            holder.tvSyncStatus.setVisibility(View.GONE);
        }
    }

    /**
     * Met à jour la barre de progression d'upload
     */
    private void updateUploadProgress(NoteViewHolder holder, ProjectNote note) {
        Integer uploadProgress = note.getUploadProgress();
        String syncStatus = note.getSyncStatus();

        // Afficher la progress bar seulement si en cours d'upload
        if ("syncing".equalsIgnoreCase(syncStatus) || "uploading".equalsIgnoreCase(syncStatus)) {
            if (uploadProgress != null && uploadProgress > 0 && uploadProgress < 100) {
                holder.progressUpload.setVisibility(View.VISIBLE);
                holder.progressUpload.setProgress(uploadProgress);
            } else {
                holder.progressUpload.setVisibility(View.GONE);
            }
        } else {
            holder.progressUpload.setVisibility(View.GONE);
        }
    }

    /**
     * Formatte une date simplifiée
     */
    private String formatDate(String dateStr) {
        try {
            // Format: 2025-10-11 19:30:00
            String[] parts = dateStr.split(" ");
            if (parts.length >= 2) {
                String[] dateParts = parts[0].split("-");
                String[] timeParts = parts[1].split(":");
                return dateParts[2] + "/" + dateParts[1] + " " + timeParts[0] + ":" + timeParts[1];
            }
        } catch (Exception e) {
            // Ignore
        }
        return dateStr;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTitle, tvContent, tvAuthor, tvImportant, tvDuration;
        TextView tvSyncStatus;
        ProgressBar progressUpload;
        ImageButton btnDelete;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_note_type);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvContent = itemView.findViewById(R.id.tv_note_content);
            tvAuthor = itemView.findViewById(R.id.tv_note_author);
            tvImportant = itemView.findViewById(R.id.tv_note_important);
            tvDuration = itemView.findViewById(R.id.tv_note_duration);
            tvSyncStatus = itemView.findViewById(R.id.tv_sync_status);
            progressUpload = itemView.findViewById(R.id.progress_upload);
            btnDelete = itemView.findViewById(R.id.btn_delete_note);
        }
    }
}
