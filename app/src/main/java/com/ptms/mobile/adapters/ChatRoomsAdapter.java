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
import java.util.List;
import java.util.Locale;

/**
 * Adaptateur pour afficher les salles de chat dans une RecyclerView
 */
public class ChatRoomsAdapter extends RecyclerView.Adapter<ChatRoomsAdapter.ChatRoomViewHolder> {
    
    private List<ChatRoom> chatRooms;
    private Context context;
    private OnChatRoomClickListener listener;
    private SimpleDateFormat timeFormat;
    
    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }
    
    public ChatRoomsAdapter(Context context) {
        this.context = context;
        this.chatRooms = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    public void setOnChatRoomClickListener(OnChatRoomClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        holder.bind(chatRoom);
    }
    
    @Override
    public int getItemCount() {
        return chatRooms.size();
    }
    
    public void addChatRoom(ChatRoom chatRoom) {
        chatRooms.add(chatRoom);
        notifyItemInserted(chatRooms.size() - 1);
    }
    
    public void addChatRooms(List<ChatRoom> newChatRooms) {
        int startPosition = chatRooms.size();
        chatRooms.addAll(newChatRooms);
        notifyItemRangeInserted(startPosition, newChatRooms.size());
    }
    
    public void updateChatRooms(List<ChatRoom> newChatRooms) {
        chatRooms.clear();
        chatRooms.addAll(newChatRooms);
        notifyDataSetChanged();
    }
    
    public void clearChatRooms() {
        chatRooms.clear();
        notifyDataSetChanged();
    }
    
    public List<ChatRoom> getChatRooms() {
        return new ArrayList<>(chatRooms);
    }
    
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
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onChatRoomClick(chatRooms.get(position));
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
                    // 💬 Conversation privée
                    ivRoomType.setImageResource(R.drawable.ic_chat_bubble_outline);
                    break;
                case "group":
                    // 👥 Groupe
                    ivRoomType.setImageResource(R.drawable.ic_group);
                    break;
                case "project":
                    // 📁 Projet
                    ivRoomType.setImageResource(R.drawable.ic_folder);
                    break;
                case "department":
                    // 🏢 Département
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
