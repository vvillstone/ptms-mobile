package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ReportGroup;

import java.util.List;

/**
 * Adapter pour afficher un résumé de semaine (dans un mois)
 */
public class WeekSummaryAdapter extends RecyclerView.Adapter<WeekSummaryAdapter.WeekSummaryViewHolder> {

    private Context context;
    private List<ReportGroup> weekGroups;

    public WeekSummaryAdapter(Context context, List<ReportGroup> weekGroups) {
        this.context = context;
        this.weekGroups = weekGroups;
    }

    @NonNull
    @Override
    public WeekSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_week_summary, parent, false);
        return new WeekSummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekSummaryViewHolder holder, int position) {
        ReportGroup weekGroup = weekGroups.get(position);

        holder.tvWeekNumber.setText(weekGroup.getTitle());
        holder.tvWeekRange.setText(weekGroup.getSubtitle());
        holder.tvWeekTotal.setText(weekGroup.getFormattedTotalHours());

        // Barre de progression (objectif 40h)
        int maxHours = 40;
        int progress = (int) Math.min(weekGroup.getTotalHours(), maxHours);
        holder.progressWeek.setMax(maxHours);
        holder.progressWeek.setProgress(progress);

        // Texte progression
        holder.tvWeekProgress.setText(String.format(java.util.Locale.FRANCE, "%.0f/%dh",
            weekGroup.getTotalHours(), maxHours));

        // Clic pour voir détails
        holder.itemView.setOnClickListener(v -> {
            android.widget.Toast.makeText(context,
                weekGroup.getTitle() + " : " + weekGroup.getFormattedTotalHours(),
                android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return weekGroups.size();
    }

    static class WeekSummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeekNumber, tvWeekRange, tvWeekProgress, tvWeekTotal;
        ProgressBar progressWeek;

        WeekSummaryViewHolder(View itemView) {
            super(itemView);
            tvWeekNumber = itemView.findViewById(R.id.tv_week_number);
            tvWeekRange = itemView.findViewById(R.id.tv_week_range);
            tvWeekProgress = itemView.findViewById(R.id.tv_week_progress);
            tvWeekTotal = itemView.findViewById(R.id.tv_week_total);
            progressWeek = itemView.findViewById(R.id.progress_week);
        }
    }
}
