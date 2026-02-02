package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.TimeReport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView pour afficher les rapports individuels
 */
public class ReportItemsAdapter extends RecyclerView.Adapter<ReportItemsAdapter.ReportViewHolder> {

    private Context context;
    private List<TimeReport> reports;
    // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);

    public ReportItemsAdapter(Context context, List<TimeReport> reports) {
        this.context = context;
        this.reports = reports;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        TimeReport report = reports.get(position);

        // Projet
        String projectName = report.getProjectName();
        if (projectName == null || projectName.isEmpty()) {
            projectName = "Sans projet";
        }
        holder.tvProject.setText(projectName);

        // Date
        holder.tvDate.setText(formatDate(report.getReportDate()));

        // Heures
        holder.tvHours.setText(report.getFormattedHours());

        // Statut
        holder.tvStatus.setText(report.getStatusText());
        holder.tvStatus.setBackgroundColor(getStatusColor(report.getValidationStatus()));

        // Description
        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            holder.tvDescription.setText(report.getDescription());
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Icône de synchronisation
        if (report.isSynced()) {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_sync_success);
            holder.ivSyncStatus.setColorFilter(0xFF4CAF50); // Vert
        } else {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_sync_pending);
            holder.ivSyncStatus.setColorFilter(0xFFFF9800); // Orange
        }

        // Clic sur le rapport - Ouvrir l'activité de détails
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context,
                com.ptms.mobile.activities.TimeReportDetailActivity.class);

            // Passer le rapport en JSON
            com.google.gson.Gson gson = new com.google.gson.Gson();
            intent.putExtra(com.ptms.mobile.activities.TimeReportDetailActivity.EXTRA_REPORT_JSON,
                gson.toJson(report));

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    private String formatDate(String dateStr) {
        try {
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFFFF9800; // Orange par défaut

        switch (status.toLowerCase()) {
            case "approved":
                return 0xFF4CAF50; // Vert
            case "rejected":
                return 0xFFF44336; // Rouge
            default:
                return 0xFFFF9800; // Orange
        }
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvProject, tvDate, tvHours, tvStatus, tvDescription;
        ImageView ivSyncStatus;

        ReportViewHolder(View itemView) {
            super(itemView);
            tvProject = itemView.findViewById(R.id.tv_project);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvHours = itemView.findViewById(R.id.tv_hours);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDescription = itemView.findViewById(R.id.tv_description);
            ivSyncStatus = itemView.findViewById(R.id.iv_sync_status);
        }
    }
}
