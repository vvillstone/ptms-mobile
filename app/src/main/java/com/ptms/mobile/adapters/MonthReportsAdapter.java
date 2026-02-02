package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ReportGroup;

import java.util.List;

/**
 * Adapter pour afficher les rapports groupés par MOIS
 */
public class MonthReportsAdapter extends RecyclerView.Adapter<MonthReportsAdapter.MonthViewHolder> {

    private Context context;
    private List<ReportGroup> monthGroups;

    public MonthReportsAdapter(Context context, List<ReportGroup> monthGroups) {
        this.context = context;
        this.monthGroups = monthGroups;
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report_month, parent, false);
        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        ReportGroup monthGroup = monthGroups.get(position);

        holder.tvMonthTitle.setText(monthGroup.getTitle());
        holder.tvMonthSubtitle.setText(monthGroup.getSubtitle());
        holder.tvMonthHours.setText(monthGroup.getFormattedTotalHours());
        holder.tvMonthCount.setText(monthGroup.getFormattedReportCount());

        // Statistiques du mois
        holder.tvMonthDaysWorked.setText(String.valueOf(monthGroup.getUniqueDaysCount()));
        holder.tvMonthAverage.setText(String.format(java.util.Locale.FRANCE, "%.2fh", monthGroup.getAverageHoursPerDay()));
        holder.tvMonthProjects.setText(String.valueOf(monthGroup.getUniqueProjectsCount()));

        // Afficher les semaines du mois
        if (monthGroup.getSubGroups() != null && !monthGroup.getSubGroups().isEmpty()) {
            WeekSummaryAdapter weekAdapter = new WeekSummaryAdapter(context, monthGroup.getSubGroups());
            holder.rvWeeksInMonth.setLayoutManager(new LinearLayoutManager(context));
            holder.rvWeeksInMonth.setAdapter(weekAdapter);
            holder.rvWeeksInMonth.setNestedScrollingEnabled(false);
        }

        // Bouton détails
        holder.btnMonthDetails.setOnClickListener(v -> {
            // TODO: Ouvrir une vue détaillée avec tous les rapports du mois
            android.widget.Toast.makeText(context, "Détails de " + monthGroup.getTitle(), android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return monthGroups.size();
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthTitle, tvMonthSubtitle, tvMonthHours, tvMonthCount;
        TextView tvMonthDaysWorked, tvMonthAverage, tvMonthProjects;
        RecyclerView rvWeeksInMonth;
        Button btnMonthDetails;

        MonthViewHolder(View itemView) {
            super(itemView);
            tvMonthTitle = itemView.findViewById(R.id.tv_month_title);
            tvMonthSubtitle = itemView.findViewById(R.id.tv_month_subtitle);
            tvMonthHours = itemView.findViewById(R.id.tv_month_hours);
            tvMonthCount = itemView.findViewById(R.id.tv_month_count);
            tvMonthDaysWorked = itemView.findViewById(R.id.tv_month_days_worked);
            tvMonthAverage = itemView.findViewById(R.id.tv_month_average);
            tvMonthProjects = itemView.findViewById(R.id.tv_month_projects);
            rvWeeksInMonth = itemView.findViewById(R.id.rv_weeks_in_month);
            btnMonthDetails = itemView.findViewById(R.id.btn_month_details);
        }
    }
}
