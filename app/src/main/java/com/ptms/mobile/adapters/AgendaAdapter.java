package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.AgendaItem;

import java.util.List;

/**
 * Adaptateur unifié pour l'Agenda (rapports + notes)
 */
public class AgendaAdapter extends ArrayAdapter<AgendaItem> {

    private Context context;
    private List<AgendaItem> items;

    public AgendaAdapter(Context context, List<AgendaItem> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AgendaItem item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_agenda, parent, false);
        }

        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvSubtitle = convertView.findViewById(R.id.tvSubtitle);
        TextView tvTime = convertView.findViewById(R.id.tvTime);
        View indicator = convertView.findViewById(R.id.typeIndicator);

        if (item != null) {
            tvTitle.setText(item.getDisplayTitle());
            tvSubtitle.setText(item.getDisplaySubtitle());

            // Afficher l'heure ou type
            if (item.getType() == AgendaItem.Type.REPORT) {
                tvTime.setText(item.getReport().getDatetimeFrom() + " - " + item.getReport().getDatetimeTo());
                indicator.setBackgroundColor(context.getResources().getColor(R.color.primary));
            } else {
                // Note: afficher le type
                String noteType = item.getNote().getNoteType();
                if ("text".equals(noteType)) {
                    tvTime.setText("📝 Texte");
                } else if ("audio".equals(noteType)) {
                    tvTime.setText("🎤 Audio");
                } else if ("dictation".equals(noteType)) {
                    tvTime.setText("🗣️ Dictée");
                } else {
                    tvTime.setText("📝 Note");
                }
                indicator.setBackgroundColor(context.getResources().getColor(R.color.accent));
            }
        }

        return convertView;
    }
}
