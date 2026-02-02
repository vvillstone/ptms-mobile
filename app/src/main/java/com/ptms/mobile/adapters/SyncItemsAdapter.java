package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.SyncItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter pour afficher les éléments de synchronisation avec leur statut
 */
public class SyncItemsAdapter extends BaseAdapter {

    public interface OnItemActionListener {
        void onDeleteItem(SyncItem item, int position);
    }

    private Context context;
    private List<SyncItem> items;
    private LayoutInflater inflater;
    private OnItemActionListener listener;

    public SyncItemsAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SyncItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public SyncItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_sync_element, parent, false);
            holder = new ViewHolder();
            holder.ivStatusIcon = convertView.findViewById(R.id.iv_status_icon);
            holder.tvItemType = convertView.findViewById(R.id.tv_item_type);
            holder.tvItemTitle = convertView.findViewById(R.id.tv_item_title);
            holder.tvItemSubtitle = convertView.findViewById(R.id.tv_item_subtitle);
            holder.tvItemDate = convertView.findViewById(R.id.tv_item_date);
            holder.tvItemStatus = convertView.findViewById(R.id.tv_item_status);
            holder.btnDelete = convertView.findViewById(R.id.btn_delete_item);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final SyncItem item = items.get(position);
        final int pos = position;

        // Icône de statut
        switch (item.getStatus()) {
            case PENDING:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_sync_pending);
                holder.ivStatusIcon.setColorFilter(context.getResources().getColor(R.color.warning));
                break;
            case SYNCING:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_sync);
                holder.ivStatusIcon.setColorFilter(context.getResources().getColor(R.color.primary));
                break;
            case SYNCED:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_sync_success);
                holder.ivStatusIcon.setColorFilter(context.getResources().getColor(R.color.success));
                break;
            case ERROR:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_error);
                holder.ivStatusIcon.setColorFilter(context.getResources().getColor(R.color.error));
                break;
        }

        // Type d'élément
        holder.tvItemType.setText(item.getTypeLabel());

        // Titre
        holder.tvItemTitle.setText(item.getTitle());

        // Sous-titre
        if (item.getSubtitle() != null && !item.getSubtitle().isEmpty()) {
            holder.tvItemSubtitle.setVisibility(View.VISIBLE);
            holder.tvItemSubtitle.setText(item.getSubtitle());
        } else {
            holder.tvItemSubtitle.setVisibility(View.GONE);
        }

        // Date
        holder.tvItemDate.setText(item.getDateFormatted());

        // Statut
        holder.tvItemStatus.setText(item.getStatusLabel());
        switch (item.getStatus()) {
            case PENDING:
                holder.tvItemStatus.setTextColor(context.getResources().getColor(R.color.warning));
                break;
            case SYNCING:
                holder.tvItemStatus.setTextColor(context.getResources().getColor(R.color.primary));
                break;
            case SYNCED:
                holder.tvItemStatus.setTextColor(context.getResources().getColor(R.color.success));
                break;
            case ERROR:
                holder.tvItemStatus.setTextColor(context.getResources().getColor(R.color.error));
                break;
        }

        // Bouton de suppression (visible pour les éléments en erreur ou en attente)
        if (item.getStatus() == SyncItem.SyncStatus.ERROR ||
            item.getStatus() == SyncItem.SyncStatus.PENDING) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteItem(item, pos);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView ivStatusIcon;
        TextView tvItemType;
        TextView tvItemTitle;
        TextView tvItemSubtitle;
        TextView tvItemDate;
        TextView tvItemStatus;
        Button btnDelete;
    }
}
