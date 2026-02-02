package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ReportGroup;
import com.ptms.mobile.models.TimeReport;

import java.util.List;

/**
 * Adapter pour afficher les rapports groupés par JOUR
 */
public class DayReportsAdapter extends RecyclerView.Adapter<DayReportsAdapter.DayViewHolder> {

    private Context context;
    private List<ReportGroup> dayGroups;

    public DayReportsAdapter(Context context, List<ReportGroup> dayGroups) {
        this.context = context;
        this.dayGroups = dayGroups;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report_day_group, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        ReportGroup dayGroup = dayGroups.get(position);

        holder.tvDayTitle.setText(dayGroup.getTitle());
        holder.tvDaySubtitle.setText(dayGroup.getSubtitle());
        holder.tvDayHours.setText(dayGroup.getFormattedTotalHours());
        holder.tvDayCount.setText(dayGroup.getFormattedReportCount());

        // Afficher les rapports du jour avec un adapter RecyclerView
        ReportItemsAdapter reportsAdapter = new ReportItemsAdapter(context, dayGroup.getReports());
        holder.rvReports.setLayoutManager(new LinearLayoutManager(context));
        holder.rvReports.setAdapter(reportsAdapter);
        holder.rvReports.setNestedScrollingEnabled(false);
    }

    @Override
    public int getItemCount() {
        return dayGroups.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayTitle, tvDaySubtitle, tvDayHours, tvDayCount;
        RecyclerView rvReports;

        DayViewHolder(View itemView) {
            super(itemView);
            tvDayTitle = itemView.findViewById(R.id.tv_day_title);
            tvDaySubtitle = itemView.findViewById(R.id.tv_day_subtitle);
            tvDayHours = itemView.findViewById(R.id.tv_day_hours);
            tvDayCount = itemView.findViewById(R.id.tv_day_count);
            rvReports = itemView.findViewById(R.id.rv_reports);
        }
    }
}
