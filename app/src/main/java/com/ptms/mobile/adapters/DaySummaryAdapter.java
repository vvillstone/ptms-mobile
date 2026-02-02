package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ReportGroup;

import java.util.List;

/**
 * Adapter pour afficher un résumé de jour (dans une semaine)
 */
public class DaySummaryAdapter extends RecyclerView.Adapter<DaySummaryAdapter.DaySummaryViewHolder> {

    private Context context;
    private List<ReportGroup> dayGroups;

    public DaySummaryAdapter(Context context, List<ReportGroup> dayGroups) {
        this.context = context;
        this.dayGroups = dayGroups;
    }

    @NonNull
    @Override
    public DaySummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_day_summary, parent, false);
        return new DaySummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DaySummaryViewHolder holder, int position) {
        ReportGroup dayGroup = dayGroups.get(position);

        holder.tvDayName.setText(dayGroup.getTitle());
        holder.tvDayDate.setText(dayGroup.getSubtitle());
        holder.tvDayReportCount.setText(dayGroup.getFormattedReportCount());
        holder.tvDayHours.setText(dayGroup.getFormattedTotalHours());

        // Clic pour voir détails
        holder.itemView.setOnClickListener(v -> {
            android.widget.Toast.makeText(context,
                dayGroup.getTitle() + " : " + dayGroup.getFormattedReportCount(),
                android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return dayGroups.size();
    }

    static class DaySummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName, tvDayDate, tvDayReportCount, tvDayHours;

        DaySummaryViewHolder(View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvDayDate = itemView.findViewById(R.id.tv_day_date);
            tvDayReportCount = itemView.findViewById(R.id.tv_day_report_count);
            tvDayHours = itemView.findViewById(R.id.tv_day_hours);
        }
    }
}
