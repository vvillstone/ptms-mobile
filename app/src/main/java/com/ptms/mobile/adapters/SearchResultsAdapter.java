package com.ptms.mobile.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.utils.SearchManager.SearchResults;
import com.ptms.mobile.activities.NoteDetailActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ Adapter pour afficher les résultats de recherche groupés
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PROJECT = 1;
    private static final int TYPE_NOTE = 2;
    private static final int TYPE_REPORT = 3;

    private final Context context;
    private List<SearchResultItem> items;

    public SearchResultsAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    /**
     * Définit les résultats à afficher
     */
    public void setResults(SearchResults results) {
        items.clear();

        // Ajouter les projets
        if (!results.projects.isEmpty()) {
            items.add(new SearchResultItem(TYPE_HEADER, "Projets (" + results.projects.size() + ")"));
            for (Project project : results.projects) {
                items.add(new SearchResultItem(TYPE_PROJECT, project));
            }
        }

        // Ajouter les notes
        if (!results.notes.isEmpty()) {
            items.add(new SearchResultItem(TYPE_HEADER, "Notes (" + results.notes.size() + ")"));
            for (ProjectNote note : results.notes) {
                items.add(new SearchResultItem(TYPE_NOTE, note));
            }
        }

        // Ajouter les rapports
        if (!results.reports.isEmpty()) {
            items.add(new SearchResultItem(TYPE_HEADER, "Rapports (" + results.reports.size() + ")"));
            for (TimeReport report : results.reports) {
                items.add(new SearchResultItem(TYPE_REPORT, report));
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        switch (viewType) {
            case TYPE_HEADER:
                View headerView = inflater.inflate(R.layout.item_search_header, parent, false);
                return new HeaderViewHolder(headerView);

            case TYPE_PROJECT:
                View projectView = inflater.inflate(R.layout.item_search_project, parent, false);
                return new ProjectViewHolder(projectView);

            case TYPE_NOTE:
                View noteView = inflater.inflate(R.layout.item_search_note, parent, false);
                return new NoteViewHolder(noteView);

            case TYPE_REPORT:
                View reportView = inflater.inflate(R.layout.item_search_report, parent, false);
                return new ReportViewHolder(reportView);

            default:
                throw new IllegalArgumentException("Type inconnu: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchResultItem item = items.get(position);

        switch (item.type) {
            case TYPE_HEADER:
                ((HeaderViewHolder) holder).bind((String) item.data);
                break;

            case TYPE_PROJECT:
                ((ProjectViewHolder) holder).bind((Project) item.data);
                break;

            case TYPE_NOTE:
                ((NoteViewHolder) holder).bind((ProjectNote) item.data);
                break;

            case TYPE_REPORT:
                ((ReportViewHolder) holder).bind((TimeReport) item.data);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ==================== VIEW HOLDERS ====================

    /**
     * Header (titre de section)
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textHeader;

        HeaderViewHolder(View itemView) {
            super(itemView);
            textHeader = itemView.findViewById(R.id.textHeader);
        }

        void bind(String title) {
            textHeader.setText(title);
        }
    }

    /**
     * Projet
     */
    class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textDescription;

        ProjectViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textProjectName);
            textDescription = itemView.findViewById(R.id.textProjectDescription);
        }

        void bind(Project project) {
            textName.setText(project.getName());

            String description = project.getDescription();
            if (description != null && !description.isEmpty()) {
                textDescription.setText(description);
                textDescription.setVisibility(View.VISIBLE);
            } else {
                textDescription.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                // TODO: Ouvrir ProjectDetailsActivity
            });
        }
    }

    /**
     * Note
     */
    class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textContent;
        TextView textProject;
        TextView textDate;
        TextView textCategory;

        NoteViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textNoteTitle);
            textContent = itemView.findViewById(R.id.textNoteContent);
            textProject = itemView.findViewById(R.id.textNoteProject);
            textDate = itemView.findViewById(R.id.textNoteDate);
            textCategory = itemView.findViewById(R.id.textNoteCategory);
        }

        void bind(ProjectNote note) {
            textTitle.setText(note.getTitle());

            String content = note.getFullContent();
            if (content != null && !content.isEmpty()) {
                // Limiter à 100 caractères
                if (content.length() > 100) {
                    content = content.substring(0, 100) + "...";
                }
                textContent.setText(content);
                textContent.setVisibility(View.VISIBLE);
            } else {
                textContent.setVisibility(View.GONE);
            }

            // Projet
            String projectName = note.getProjectName();
            if (projectName != null && !projectName.isEmpty()) {
                textProject.setText("📁 " + projectName);
                textProject.setVisibility(View.VISIBLE);
            } else {
                textProject.setText("👤 Note personnelle");
                textProject.setVisibility(View.VISIBLE);
            }

            // Date
            if (note.getCreatedAt() != null) {
                textDate.setText(formatDate(note.getCreatedAt()));
            }

            // Catégorie
            if (note.getNoteTypeName() != null) {
                textCategory.setText(note.getCategoryEmoji() + " " + note.getNoteTypeName());
                textCategory.setVisibility(View.VISIBLE);
            } else {
                textCategory.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, NoteDetailActivity.class);
                intent.putExtra("NOTE_ID", note.getId());
                context.startActivity(intent);
            });
        }
    }

    /**
     * Rapport
     */
    class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView textProject;
        TextView textWorkType;
        TextView textDate;
        TextView textDuration;
        TextView textComment;

        ReportViewHolder(View itemView) {
            super(itemView);
            textProject = itemView.findViewById(R.id.textReportProject);
            textWorkType = itemView.findViewById(R.id.textReportWorkType);
            textDate = itemView.findViewById(R.id.textReportDate);
            textDuration = itemView.findViewById(R.id.textReportDuration);
            textComment = itemView.findViewById(R.id.textReportComment);
        }

        void bind(TimeReport report) {
            textProject.setText("📁 " + report.getProjectName());
            textWorkType.setText(report.getWorkTypeName());
            textDate.setText(formatDate(report.getReportDate()));
            textDuration.setText(formatDuration(report.getHours()));

            String comment = report.getDescription();
            if (comment != null && !comment.isEmpty()) {
                textComment.setText(comment);
                textComment.setVisibility(View.VISIBLE);
            } else {
                textComment.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                // TODO: Ouvrir ReportDetailsActivity
            });
        }
    }

    // ==================== UTILITAIRES ====================

    /**
     * Formate une date
     */
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }

        // Format simple pour l'instant
        // TODO: Formatter correctement avec SimpleDateFormat
        if (dateStr.length() >= 10) {
            return dateStr.substring(0, 10);
        }

        return dateStr;
    }

    /**
     * Formate une durée
     */
    private String formatDuration(double hours) {
        int h = (int) hours;
        int m = (int) ((hours - h) * 60);
        return h + "h" + (m > 0 ? String.format("%02d", m) : "");
    }

    // ==================== ITEM WRAPPER ====================

    /**
     * Wrapper pour les items de recherche
     */
    private static class SearchResultItem {
        int type;
        Object data;

        SearchResultItem(int type, Object data) {
            this.type = type;
            this.data = data;
        }
    }
}
