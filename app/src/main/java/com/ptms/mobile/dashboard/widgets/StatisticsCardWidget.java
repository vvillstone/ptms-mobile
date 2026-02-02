package com.ptms.mobile.dashboard.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.ptms.mobile.R;

/**
 * Widget carte pour afficher une statistique simple
 */
public class StatisticsCardWidget extends CardView {
    private TextView tvTitle;
    private TextView tvValue;
    private TextView tvSubtitle;
    private LinearLayout iconContainer;

    public StatisticsCardWidget(Context context) {
        super(context);
        init(context);
    }

    public StatisticsCardWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_statistics_card, this, true);

        tvTitle = findViewById(R.id.tv_stat_title);
        tvValue = findViewById(R.id.tv_stat_value);
        tvSubtitle = findViewById(R.id.tv_stat_subtitle);
        iconContainer = findViewById(R.id.icon_container);

        // Style Material Design
        setRadius(16f);
        setCardElevation(4f);
        setUseCompatPadding(true);
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    public void setValue(String value) {
        tvValue.setText(value);
    }

    public void setSubtitle(String subtitle) {
        tvSubtitle.setText(subtitle);
        tvSubtitle.setVisibility(subtitle != null && !subtitle.isEmpty() ? VISIBLE : GONE);
    }

    public void setIcon(int iconResId) {
        iconContainer.removeAllViews();
        // Implémenter l'ajout d'icône si nécessaire
    }

    public void setValueColor(int colorResId) {
        tvValue.setTextColor(getContext().getResources().getColor(colorResId));
    }
}
