package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ReportGroup;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.utils.ReportGrouper;
import com.ptms.mobile.utils.FileLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter pour le ViewPager2 des rapports
 * Gère 3 pages : Jour, Semaine, Mois
 */
public class ReportsPagerAdapter extends RecyclerView.Adapter<ReportsPagerAdapter.PageViewHolder> {

    private Context context;
    private List<TimeReport> reports;

    // Les 3 pages
    private static final int PAGE_DAY = 0;
    private static final int PAGE_WEEK = 1;
    private static final int PAGE_MONTH = 2;
    private static final int PAGE_COUNT = 3;

    public ReportsPagerAdapter(Context context, List<TimeReport> reports) {
        this.context = context;
        this.reports = reports != null ? reports : new ArrayList<>();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_reports_list, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        switch (position) {
            case PAGE_DAY:
                setupDayView(holder);
                break;
            case PAGE_WEEK:
                setupWeekView(holder);
                break;
            case PAGE_MONTH:
                setupMonthView(holder);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    /**
     * Configure la vue par JOUR
     */
    private void setupDayView(PageViewHolder holder) {
        try {
            FileLogger.d("PAGER_ADAPTER", ">>> setupDayView() START - " + reports.size() + " rapports");
            List<ReportGroup> dayGroups = ReportGrouper.groupByDay(reports);
            FileLogger.d("PAGER_ADAPTER", "✅ Groupés en " + dayGroups.size() + " groupes jour");

            DayReportsAdapter dayAdapter = new DayReportsAdapter(context, dayGroups);
            holder.recyclerView.setLayoutManager(new LinearLayoutManager(context));
            holder.recyclerView.setAdapter(dayAdapter);
            FileLogger.d("PAGER_ADAPTER", ">>> setupDayView() END ✅");
        } catch (Exception e) {
            FileLogger.e("PAGER_ADAPTER", "❌ CRASH dans setupDayView", e);
        }
    }

    /**
     * Configure la vue par SEMAINE
     */
    private void setupWeekView(PageViewHolder holder) {
        try {
            FileLogger.d("PAGER_ADAPTER", ">>> setupWeekView() START - " + reports.size() + " rapports");
            List<ReportGroup> weekGroups = ReportGrouper.groupByWeekWithDays(reports);
            FileLogger.d("PAGER_ADAPTER", "✅ Groupés en " + weekGroups.size() + " groupes semaine");

            WeekReportsAdapter weekAdapter = new WeekReportsAdapter(context, weekGroups);
            holder.recyclerView.setLayoutManager(new LinearLayoutManager(context));
            holder.recyclerView.setAdapter(weekAdapter);
            FileLogger.d("PAGER_ADAPTER", ">>> setupWeekView() END ✅");
        } catch (Exception e) {
            FileLogger.e("PAGER_ADAPTER", "❌ CRASH dans setupWeekView", e);
        }
    }

    /**
     * Configure la vue par MOIS
     */
    private void setupMonthView(PageViewHolder holder) {
        try {
            FileLogger.d("PAGER_ADAPTER", ">>> setupMonthView() START - " + reports.size() + " rapports");
            List<ReportGroup> monthGroups = ReportGrouper.groupByMonthWithWeeks(reports);
            FileLogger.d("PAGER_ADAPTER", "✅ Groupés en " + monthGroups.size() + " groupes mois");

            MonthReportsAdapter monthAdapter = new MonthReportsAdapter(context, monthGroups);
            holder.recyclerView.setLayoutManager(new LinearLayoutManager(context));
            holder.recyclerView.setAdapter(monthAdapter);
            FileLogger.d("PAGER_ADAPTER", ">>> setupMonthView() END ✅");
        } catch (Exception e) {
            FileLogger.e("PAGER_ADAPTER", "❌ CRASH dans setupMonthView", e);
        }
    }

    /**
     * Met à jour les rapports et rafraîchit toutes les vues
     */
    public void updateReports(List<TimeReport> newReports) {
        try {
            FileLogger.d("PAGER_ADAPTER", ">>> updateReports() START");
            FileLogger.d("PAGER_ADAPTER", "newReports: " + (newReports != null ? newReports.size() : "NULL"));

            this.reports = newReports != null ? newReports : new ArrayList<>();
            FileLogger.d("PAGER_ADAPTER", "Appel notifyDataSetChanged()...");
            notifyDataSetChanged();
            FileLogger.d("PAGER_ADAPTER", ">>> updateReports() END ✅");
        } catch (Exception e) {
            FileLogger.e("PAGER_ADAPTER", "❌ CRASH dans updateReports", e);
        }
    }

    /**
     * ViewHolder pour une page
     */
    static class PageViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;

        PageViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view);
        }
    }
}
