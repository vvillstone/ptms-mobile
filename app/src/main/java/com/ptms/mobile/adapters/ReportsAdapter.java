package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.TimeReport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptateur pour la liste des rapports de temps
 */
public class ReportsAdapter extends BaseAdapter {

    private Context context;
    private List<TimeReport> reports;
    private LayoutInflater inflater;

    public ReportsAdapter(Context context, List<TimeReport> reports) {
        this.context = context;
        this.reports = reports;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return reports.size();
    }

    @Override
    public Object getItem(int position) {
        return reports.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_report, parent, false);
            holder = new ViewHolder();
            holder.sectionHeader = convertView.findViewById(R.id.section_header);
            holder.tvSectionDate = convertView.findViewById(R.id.tv_section_date);
            holder.tvSectionTotal = convertView.findViewById(R.id.tv_section_total);
            holder.tvProject = convertView.findViewById(R.id.tv_project);
            holder.tvDate = convertView.findViewById(R.id.tv_date);
            holder.tvHours = convertView.findViewById(R.id.tv_hours);
            holder.tvStatus = convertView.findViewById(R.id.tv_status);
            holder.tvDescription = convertView.findViewById(R.id.tv_description);
            holder.ivSyncStatus = convertView.findViewById(R.id.iv_sync_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TimeReport report = reports.get(position);

        // Remplir les données avec vérification des null
        String reportDate = report.getReportDate() != null ? report.getReportDate() : "";
        holder.tvProject.setText(report.getProjectName() != null ? report.getProjectName() : "Projet inconnu");
        holder.tvDate.setText(formatDate(!reportDate.isEmpty() ? reportDate : "2024-01-01"));
        holder.tvHours.setText(report.getFormattedHours());
        holder.tvStatus.setText(report.getStatusText());
        holder.tvDescription.setText(report.getDescription() != null ? report.getDescription() : "Aucune description");

        // Couleur du statut
        int statusColor = getStatusColor(report.getValidationStatus());
        holder.tvStatus.setTextColor(statusColor);

        // Icône de statut de synchronisation
        setSyncStatusIcon(holder.ivSyncStatus, report);

        // Afficher l'en-tête de section lorsque la date change par rapport à l'item précédent
        if (holder.sectionHeader != null && holder.tvSectionDate != null && holder.tvSectionTotal != null) {
            String current = formatDate(reportDate);
            String prev = null;
            if (position > 0) {
                TimeReport prevReport = reports.get(position - 1);
                prev = formatDate(prevReport.getReportDate() != null ? prevReport.getReportDate() : "");
            }
            if (position == 0 || (current != null && !current.equals(prev))) {
                holder.tvSectionDate.setText(current != null ? current : "");
                holder.tvSectionTotal.setText(String.format(Locale.FRANCE, "%.2fh", sumHoursForDate(report.getReportDate())));
                holder.sectionHeader.setVisibility(View.VISIBLE);
            } else {
                holder.sectionHeader.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    private String formatDate(String dateString) {
        try {
            // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateString;
        }
    }

    private int getStatusColor(String status) {
        if (status == null || status.isEmpty()) {
            return context.getResources().getColor(android.R.color.darker_gray);
        }
        
        switch (status.toLowerCase()) {
            case "approved":
                return context.getResources().getColor(android.R.color.holo_green_dark);
            case "rejected":
                return context.getResources().getColor(android.R.color.holo_red_dark);
            case "pending":
                return context.getResources().getColor(android.R.color.holo_orange_dark);
            default:
                return context.getResources().getColor(android.R.color.darker_gray);
        }
    }

    private void setSyncStatusIcon(ImageView imageView, TimeReport report) {
        if (imageView == null) return;

        // Utiliser les nouveaux champs de synchronisation
        if (!report.isLocal() || report.isSynced()) {
            // Rapport synchronisé (serveur OU local synchronisé) - VERT
            imageView.setImageResource(android.R.drawable.presence_online);
            imageView.setColorFilter(report.getSyncStatusColor());
            imageView.setContentDescription("Synchronisé");
        } else {
            // Rapport local non synchronisé - ORANGE
            imageView.setImageResource(android.R.drawable.presence_away);
            imageView.setColorFilter(report.getSyncStatusColor());
            imageView.setContentDescription("En attente de synchronisation");
        }

        imageView.setVisibility(View.VISIBLE);
    }

    static class ViewHolder {
        View sectionHeader;
        TextView tvSectionDate;
        TextView tvSectionTotal;
        TextView tvProject;
        TextView tvDate;
        TextView tvHours;
        TextView tvStatus;
        TextView tvDescription;
        ImageView ivSyncStatus;
    }

    private double sumHoursForDate(String dateIso) {
        if (dateIso == null) return 0.0;
        double total = 0.0;
        for (TimeReport r : reports) {
            if (dateIso.equals(r.getReportDate())) {
                total += r.getHours();
            }
        }
        return total;
    }
}
