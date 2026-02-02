package com.ptms.mobile.dashboard.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.ptms.mobile.R;
import com.ptms.mobile.dashboard.DashboardWidgetManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget graphique en camembert pour afficher la distribution des projets
 */
public class ProjectChartWidget extends View {
    private List<ChartSegment> segments = new ArrayList<>();
    private Paint paint;
    private Paint textPaint;
    private RectF bounds;

    private static final int[] COLORS = {
        0xFF2196F3, // Blue
        0xFF4CAF50, // Green
        0xFFF44336, // Red
        0xFFFF9800, // Orange
        0xFF9C27B0, // Purple
        0xFFFFEB3B, // Yellow
        0xFF00BCD4, // Cyan
        0xFFE91E63  // Pink
    };

    public ProjectChartWidget(Context context) {
        super(context);
        init();
    }

    public ProjectChartWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(getResources().getColor(R.color.textPrimary));
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        bounds = new RectF();
    }

    public void setData(List<DashboardWidgetManager.ProjectStats> projects, double totalHours) {
        segments.clear();

        if (projects == null || projects.isEmpty() || totalHours == 0) {
            invalidate();
            return;
        }

        float startAngle = 0f;

        for (int i = 0; i < projects.size(); i++) {
            DashboardWidgetManager.ProjectStats project = projects.get(i);
            float sweepAngle = (float) ((project.totalHours / totalHours) * 360);

            ChartSegment segment = new ChartSegment();
            segment.startAngle = startAngle;
            segment.sweepAngle = sweepAngle;
            segment.color = COLORS[i % COLORS.length];
            segment.projectName = project.projectName;
            segment.hours = project.totalHours;
            segment.percentage = project.getPercentage(totalHours);

            segments.add(segment);
            startAngle += sweepAngle;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (segments.isEmpty()) {
            // Afficher un message "Aucune donnée"
            textPaint.setTextSize(40f);
            canvas.drawText("Aucune donnée",
                getWidth() / 2f,
                getHeight() / 2f,
                textPaint);
            return;
        }

        // Calculer les bounds du cercle
        int size = Math.min(getWidth(), getHeight());
        int padding = 40;
        bounds.set(padding, padding, size - padding, size - padding);

        // Dessiner les segments
        for (ChartSegment segment : segments) {
            paint.setColor(segment.color);
            canvas.drawArc(bounds, segment.startAngle, segment.sweepAngle, true, paint);
        }

        // Dessiner le cercle central blanc (effet donut)
        paint.setColor(0xFFFFFFFF);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int innerRadius = (size - padding * 2) / 4;
        canvas.drawCircle(centerX, centerY, innerRadius, paint);

        // Afficher le total au centre
        textPaint.setTextSize(32f);
        canvas.drawText("Total", centerX, centerY - 10, textPaint);

        textPaint.setTextSize(24f);
        textPaint.setColor(getResources().getColor(R.color.textSecondary));
        String totalText = String.format("%.1fh", getTotalHours());
        canvas.drawText(totalText, centerX, centerY + 25, textPaint);
    }

    private double getTotalHours() {
        double total = 0;
        for (ChartSegment segment : segments) {
            total += segment.hours;
        }
        return total;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // Forcer un aspect carré
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    public List<ChartSegment> getSegments() {
        return segments;
    }

    /**
     * Représente un segment du graphique
     */
    public static class ChartSegment {
        public float startAngle;
        public float sweepAngle;
        public int color;
        public String projectName;
        public double hours;
        public double percentage;
    }
}
