package com.ptms.mobile.dashboard.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.ptms.mobile.R;
import com.ptms.mobile.dashboard.DashboardWidgetManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget graphique pour afficher la tendance hebdomadaire
 */
public class WeeklyTrendWidget extends View {
    private List<DashboardWidgetManager.DayStats> dayStats = new ArrayList<>();
    private Paint linePaint;
    private Paint fillPaint;
    private Paint textPaint;
    private Paint gridPaint;
    private Path linePath;
    private Path fillPath;

    private double maxHours = 8.0; // Échelle par défaut

    public WeeklyTrendWidget(Context context) {
        super(context);
        init();
    }

    public WeeklyTrendWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Ligne du graphique
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(getResources().getColor(R.color.primary));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // Remplissage sous la ligne
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(getResources().getColor(R.color.primary_light));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAlpha(100);

        // Texte
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(getResources().getColor(R.color.textSecondary));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Grille
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(getResources().getColor(R.color.divider));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        linePath = new Path();
        fillPath = new Path();
    }

    public void setData(List<DashboardWidgetManager.DayStats> stats) {
        this.dayStats = stats != null ? stats : new ArrayList<>();

        // Calculer l'échelle
        maxHours = 8.0;
        for (DashboardWidgetManager.DayStats day : dayStats) {
            if (day.totalHours > maxHours) {
                maxHours = Math.ceil(day.totalHours);
            }
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dayStats.isEmpty()) {
            // Afficher un message "Aucune donnée"
            textPaint.setTextSize(40f);
            canvas.drawText("Aucune donnée",
                getWidth() / 2f,
                getHeight() / 2f,
                textPaint);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int padding = 80;
        int chartHeight = height - padding * 2;
        int chartWidth = width - padding * 2;

        // Dessiner la grille horizontale
        drawGrid(canvas, padding, chartHeight);

        // Calculer les points
        List<Point> points = calculatePoints(chartWidth, chartHeight, padding);

        // Dessiner la ligne et le remplissage
        drawChart(canvas, points, chartHeight, padding);

        // Dessiner les labels des jours
        drawLabels(canvas, points);
    }

    private void drawGrid(Canvas canvas, int padding, int chartHeight) {
        // Lignes horizontales (0h, 4h, 8h)
        int numLines = 3;
        for (int i = 0; i <= numLines; i++) {
            float y = padding + chartHeight - (chartHeight * i / (float) numLines);
            canvas.drawLine(padding, y, getWidth() - padding, y, gridPaint);

            // Label des heures
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setTextSize(24f);
            String label = String.format("%dh", (int) (maxHours * i / numLines));
            canvas.drawText(label, padding - 15, y + 8, textPaint);
        }
    }

    private List<Point> calculatePoints(int chartWidth, int chartHeight, int padding) {
        List<Point> points = new ArrayList<>();

        if (dayStats.isEmpty()) {
            return points;
        }

        float segmentWidth = chartWidth / (float) (dayStats.size() - 1);

        for (int i = 0; i < dayStats.size(); i++) {
            DashboardWidgetManager.DayStats day = dayStats.get(i);

            float x = padding + (i * segmentWidth);
            float y = padding + chartHeight - (float) ((day.totalHours / maxHours) * chartHeight);

            Point point = new Point();
            point.x = x;
            point.y = y;
            point.dayOfWeek = day.dayOfWeek;
            point.hours = day.totalHours;

            points.add(point);
        }

        return points;
    }

    private void drawChart(Canvas canvas, List<Point> points, int chartHeight, int padding) {
        if (points.isEmpty()) {
            return;
        }

        linePath.reset();
        fillPath.reset();

        // Commencer le chemin
        Point firstPoint = points.get(0);
        linePath.moveTo(firstPoint.x, firstPoint.y);
        fillPath.moveTo(firstPoint.x, padding + chartHeight);
        fillPath.lineTo(firstPoint.x, firstPoint.y);

        // Dessiner les lignes
        for (int i = 1; i < points.size(); i++) {
            Point point = points.get(i);
            linePath.lineTo(point.x, point.y);
            fillPath.lineTo(point.x, point.y);
        }

        // Fermer le chemin de remplissage
        Point lastPoint = points.get(points.size() - 1);
        fillPath.lineTo(lastPoint.x, padding + chartHeight);
        fillPath.close();

        // Dessiner
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // Dessiner les points
        Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(getResources().getColor(R.color.primary));
        pointPaint.setStyle(Paint.Style.FILL);

        for (Point point : points) {
            canvas.drawCircle(point.x, point.y, 8f, pointPaint);

            // Cercle blanc au centre
            pointPaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(point.x, point.y, 4f, pointPaint);
            pointPaint.setColor(getResources().getColor(R.color.primary));
        }
    }

    private void drawLabels(Canvas canvas, List<Point> points) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(26f);

        for (Point point : points) {
            canvas.drawText(point.dayOfWeek,
                point.x,
                getHeight() - 30,
                textPaint);
        }
    }

    /**
     * Point sur le graphique
     */
    private static class Point {
        float x;
        float y;
        String dayOfWeek;
        double hours;
    }
}
