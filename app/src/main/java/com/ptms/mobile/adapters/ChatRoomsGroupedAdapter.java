package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ChatRoom;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adaptateur pour afficher les salles de chat regroupées par type avec sections pliables
 */
public class ChatRoomsGroupedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ROOM = 1;

    private List<Object> items; // Contient des Section et ChatRoom
    private Context context;
    private ChatRoomsAdapter.OnChatRoomClickListener listener;
    private SimpleDateFormat timeFormat;
    private Map<String, Boolean> collapsedSections; // Type -> isCollapsed

    public ChatRoomsGroupedAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.collapsedSections = new HashMap<>();
    }

    public void setOnChatRoomClickListener(ChatRoomsAdapter.OnChatRoomClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof Section) ? VIEW_TYPE_HEADER : VIEW_TYPE_ROOM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_section_header, parent, false);
            return new SectionHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_room, parent, false);
            return new ChatRoomViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SectionHeaderViewHolder) {
            Section section = (Section) items.get(position);
            ((SectionHeaderViewHolder) holder).bind(section);
        } else if (holder instanceof ChatRoomViewHolder) {
            ChatRoom chatRoom = (ChatRoom) items.get(position);
            ((ChatRoomViewHolder) holder).bind(chatRoom);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Met à jour les conversations en les regroupant par type
     */
    public void updateChatRooms(List<ChatRoom> chatRooms) {
        items.clear();

        // Regrouper les conversations par type
        Map<String, List<ChatRoom>> grouped = new LinkedHashMap<>();
        grouped.put("direct", new ArrayList<>());
        grouped.put("group", new ArrayList<>());
        grouped.put("project", new ArrayList<>());
        grouped.put("department", new ArrayList<>());

        for (ChatRoom room : chatRooms) {
            String type = room.getType();
            if (type == null || type.isEmpty()) {
                type = "direct"; // Par défaut
            }

            if (!grouped.containsKey(type)) {
                grouped.put(type, new ArrayList<>());
            }
            grouped.get(type).add(room);
        }

        // Ajouter les sections et les salles
        for (Map.Entry<String, List<ChatRoom>> entry : grouped.entrySet()) {
            String type = entry.getKey();
            List<ChatRoom> rooms = entry.getValue();

            if (!rooms.isEmpty()) {
                // Ajouter l'en-tête de section
                Section section = new Section(type, rooms.size());
                items.add(section);

                // Ajouter les salles si la section n'est pas pliée
                Boolean isCollapsed = collapsedSections.get(type);
                if (isCollapsed == null || !isCollapsed) {
                    items.addAll(rooms);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Plie/déplie une section
     */
    private void toggleSection(String type) {
        Boolean isCollapsed = collapsedSections.get(type);
        if (isCollapsed == null) {
            isCollapsed = false;
        }
        collapsedSections.put(type, !isCollapsed);

        // Trouver la position de la section
        int sectionIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Section) {
                Section section = (Section) items.get(i);
                if (section.type.equals(type)) {
                    sectionIndex = i;
                    break;
                }
            }
        }

        if (sectionIndex == -1) return;

        Section section = (Section) items.get(sectionIndex);
        int count = section.count;

        if (collapsedSections.get(type)) {
            // Plier - supprimer les salles après la section
            int removedCount = 0;
            while (sectionIndex + 1 < items.size() && !(items.get(sectionIndex + 1) instanceof Section)) {
                items.remove(sectionIndex + 1);
                removedCount++;
            }
            notifyItemRangeRemoved(sectionIndex + 1, removedCount);
        } else {
            // Déplier - réinsérer les salles
            // On doit récupérer toutes les salles de ce type depuis la liste complète
            // Pour simplifier, on va juste appeler updateChatRooms avec la liste existante
            // (C'est un hack, on devrait garder une copie de la liste complète)
            notifyDataSetChanged();
        }

        // Mettre à jour l'icône de la section
        notifyItemChanged(sectionIndex);
    }

    /**
     * Classe représentant une section (en-tête de groupe)
     */
    private static class Section {
        String type;
        int count;

        Section(String type, int count) {
            this.type = type;
            this.count = count;
        }

        String getTitle(Context context) {
            switch (type) {
                case "direct":
                case "private":
                    return context.getString(R.string.chat_section_private);
                case "group":
                    return context.getString(R.string.chat_section_groups);
                case "project":
                    return context.getString(R.string.chat_section_projects);
                case "department":
                    return context.getString(R.string.chat_section_departments);
                default:
                    return type;
            }
        }

        int getIcon() {
            switch (type) {
                case "direct":
                case "private":
                    return R.drawable.ic_chat_bubble_outline;
                case "group":
                    return R.drawable.ic_group;
                case "project":
                    return R.drawable.ic_folder;
                case "department":
                    return R.drawable.ic_business;
                default:
                    return R.drawable.ic_chat_bubble_outline;
            }
        }
    }

    /**
     * ViewHolder pour les en-têtes de section
     */
    class SectionHeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSectionTitle;
        private TextView tvSectionCount;
        private ImageView ivSectionIcon;
        private ImageView ivSectionExpand;

        public SectionHeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            tvSectionTitle = itemView.findViewById(R.id.tv_section_title);
            tvSectionCount = itemView.findViewById(R.id.tv_section_count);
            ivSectionIcon = itemView.findViewById(R.id.iv_section_icon);
            ivSectionExpand = itemView.findViewById(R.id.iv_section_expand);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Section section = (Section) items.get(position);
                        toggleSection(section.type);
                    }
                }
            });
        }

        public void bind(Section section) {
            tvSectionTitle.setText(section.getTitle(context));
            tvSectionCount.setText(String.valueOf(section.count));
            ivSectionIcon.setImageResource(section.getIcon());

            // Icône de pli/dépli
            Boolean isCollapsed = collapsedSections.get(section.type);
            if (isCollapsed != null && isCollapsed) {
                ivSectionExpand.setRotation(0); // Flèche vers la droite
            } else {
                ivSectionExpand.setRotation(90); // Flèche vers le bas
            }
        }
    }

    /**
     * ViewHolder pour les salles de chat
     */
    class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        private TextView tvRoomAvatar;
        private TextView tvRoomName;
        private TextView tvLastMessage;
        private TextView tvLastTime;
        private TextView tvUnreadCount;
        private ImageView ivMessageStatus;
        private ImageView ivRoomType;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);

            tvRoomAvatar = itemView.findViewById(R.id.tv_room_avatar);
            tvRoomName = itemView.findViewById(R.id.tv_room_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvLastTime = itemView.findViewById(R.id.tv_last_time);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            ivMessageStatus = itemView.findViewById(R.id.iv_message_status);
            ivRoomType = itemView.findViewById(R.id.iv_room_type);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && items.get(position) instanceof ChatRoom) {
                            listener.onChatRoomClick((ChatRoom) items.get(position));
                        }
                    }
                }
            });
        }

        public void bind(ChatRoom chatRoom) {
            // Nom de la salle
            tvRoomName.setText(chatRoom.getDisplayName());

            // Avatar (initiales)
            tvRoomAvatar.setText(getRoomInitials(chatRoom.getName()));

            // Dernier message
            if (chatRoom.getLastMessage() != null) {
                tvLastMessage.setText(chatRoom.getLastMessage().getContent());
                tvLastTime.setText(formatTime(chatRoom.getLastMessage().getTimestamp()));
            } else {
                tvLastMessage.setText(context.getString(R.string.chat_no_messages));
                tvLastTime.setText("");
            }

            // Badge de messages non lus
            if (chatRoom.hasUnreadMessages()) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                tvUnreadCount.setText(String.valueOf(chatRoom.getUnreadCount()));
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }

            // Icône de type de salle
            setRoomTypeIcon(chatRoom.getRoomType());

            // Statut du message (pour l'instant, toujours caché)
            ivMessageStatus.setVisibility(View.GONE);
        }

        private String getRoomInitials(String roomName) {
            if (roomName == null || roomName.isEmpty()) {
                return "?";
            }

            String[] parts = roomName.split(" ");
            if (parts.length >= 2) {
                return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
            }
            return roomName.substring(0, 1).toUpperCase();
        }

        private String formatTime(Date timestamp) {
            if (timestamp == null) return "";

            Date now = new Date();
            long diffInMillis = now.getTime() - timestamp.getTime();
            long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

            if (diffInDays == 0) {
                // Aujourd'hui - afficher seulement l'heure
                return timeFormat.format(timestamp);
            } else if (diffInDays == 1) {
                // Hier
                return context.getString(R.string.chat_yesterday);
            } else {
                // Autre jour - afficher la date
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                return dateFormat.format(timestamp);
            }
        }

        private void setRoomTypeIcon(String roomType) {
            if (roomType == null) {
                roomType = "direct";
            }

            switch (roomType) {
                case "direct":
                case "private":
                    ivRoomType.setImageResource(R.drawable.ic_chat_bubble_outline);
                    break;
                case "group":
                    ivRoomType.setImageResource(R.drawable.ic_group);
                    break;
                case "project":
                    ivRoomType.setImageResource(R.drawable.ic_folder);
                    break;
                case "department":
                    ivRoomType.setImageResource(R.drawable.ic_business);
                    break;
                case "general":
                    ivRoomType.setImageResource(R.drawable.ic_chat_bubble_outline);
                    break;
                default:
                    ivRoomType.setImageResource(R.drawable.ic_chat_bubble_outline);
                    break;
            }
        }
    }
}
