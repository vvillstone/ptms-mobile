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
 * Adapter pour afficher les rapports groupés par SEMAINE
 */
public class WeekReportsAdapter extends RecyclerView.Adapter<WeekReportsAdapter.WeekViewHolder> {

    private Context context;
    private List<ReportGroup> weekGroups;

    public WeekReportsAdapter(Context context, List<ReportGroup> weekGroups) {
        this.context = context;
        this.weekGroups = weekGroups;
    }

    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report_week, parent, false);
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        ReportGroup weekGroup = weekGroups.get(position);

        holder.tvWeekTitle.setText(weekGroup.getTitle());
        holder.tvWeekDates.setText(weekGroup.getSubtitle());
        holder.tvWeekHours.setText(weekGroup.getFormattedTotalHours());
        holder.tvWeekCount.setText(weekGroup.getFormattedReportCount());

        // Afficher les jours de la semaine
        if (weekGroup.getSubGroups() != null && !weekGroup.getSubGroups().isEmpty()) {
            DaySummaryAdapter dayAdapter = new DaySummaryAdapter(context, weekGroup.getSubGroups());
            holder.rvDaysInWeek.setLayoutManager(new LinearLayoutManager(context));
            holder.rvDaysInWeek.setAdapter(dayAdapter);
            holder.rvDaysInWeek.setNestedScrollingEnabled(false);
        }

        // Bouton détails
        holder.btnWeekDetails.setOnClickListener(v -> {
            // TODO: Ouvrir une vue détaillée avec tous les rapports de la semaine
            android.widget.Toast.makeText(context, "Détails de " + weekGroup.getTitle(), android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return weekGroups.size();
    }

    static class WeekViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeekTitle, tvWeekDates, tvWeekHours, tvWeekCount;
        RecyclerView rvDaysInWeek;
        Button btnWeekDetails;

        WeekViewHolder(View itemView) {
            super(itemView);
            tvWeekTitle = itemView.findViewById(R.id.tv_week_title);
            tvWeekDates = itemView.findViewById(R.id.tv_week_dates);
            tvWeekHours = itemView.findViewById(R.id.tv_week_hours);
            tvWeekCount = itemView.findViewById(R.id.tv_week_count);
            rvDaysInWeek = itemView.findViewById(R.id.rv_days_in_week);
            btnWeekDetails = itemView.findViewById(R.id.btn_week_details);
        }
    }
}
