package com.ptms.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptms.mobile.R;
import com.ptms.mobile.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptateur pour afficher les messages de chat dans une RecyclerView
 */
public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.MessageViewHolder> {
    
    private List<ChatMessage> messages;
    private Context context;
    private int currentUserId;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    
    public ChatMessagesAdapter(Context context, int currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.messages = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void addMessages(List<ChatMessage> newMessages) {
        int startPosition = messages.size();
        messages.addAll(newMessages);
        notifyItemRangeInserted(startPosition, newMessages.size());
    }
    
    public void updateMessages(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }
    
    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }
    
    public ChatMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout layoutMessageSent;
        private LinearLayout layoutMessageReceived;
        private LinearLayout layoutMessageSystem;
        
        private TextView tvMessageSent;
        private TextView tvTimeSent;
        private TextView tvAvatarSent;
        
        private TextView tvMessageReceived;
        private TextView tvTimeReceived;
        private TextView tvSenderName;
        private TextView tvAvatarReceived;
        
        private TextView tvMessageSystem;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            
            layoutMessageSent = itemView.findViewById(R.id.layout_message_sent);
            layoutMessageReceived = itemView.findViewById(R.id.layout_message_received);
            layoutMessageSystem = itemView.findViewById(R.id.layout_message_system);
            
            tvMessageSent = itemView.findViewById(R.id.tv_message_sent);
            tvTimeSent = itemView.findViewById(R.id.tv_time_sent);
            tvAvatarSent = itemView.findViewById(R.id.tv_avatar_sent);
            
            tvMessageReceived = itemView.findViewById(R.id.tv_message_received);
            tvTimeReceived = itemView.findViewById(R.id.tv_time_received);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvAvatarReceived = itemView.findViewById(R.id.tv_avatar_received);
            
            tvMessageSystem = itemView.findViewById(R.id.tv_message_system);
        }
        
        public void bind(ChatMessage message) {
            // Masquer tous les layouts
            layoutMessageSent.setVisibility(View.GONE);
            layoutMessageReceived.setVisibility(View.GONE);
            layoutMessageSystem.setVisibility(View.GONE);
            
            boolean isCurrentUser = message.getSenderId() == currentUserId;
            boolean isSystemMessage = message.isSystemMessage();
            
            if (isSystemMessage) {
                // Message système
                layoutMessageSystem.setVisibility(View.VISIBLE);
                tvMessageSystem.setText(message.getContent());
            } else if (isCurrentUser) {
                // Message envoyé par l'utilisateur actuel
                layoutMessageSent.setVisibility(View.VISIBLE);
                tvMessageSent.setText(message.getContent());
                tvTimeSent.setText(formatTime(message.getTimestamp()));
                tvAvatarSent.setText(getInitials(message.getSenderName()));
            } else {
                // Message reçu d'un autre utilisateur
                layoutMessageReceived.setVisibility(View.VISIBLE);
                tvMessageReceived.setText(message.getContent());
                tvTimeReceived.setText(formatTime(message.getTimestamp()));
                tvSenderName.setText(message.getSenderName());
                tvAvatarReceived.setText(getInitials(message.getSenderName()));
            }
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
                return context.getString(R.string.chat_yesterday) + " " + timeFormat.format(timestamp);
            } else {
                // Autre jour - afficher la date et l'heure
                return dateFormat.format(timestamp) + " " + timeFormat.format(timestamp);
            }
        }
        
        private String getInitials(String name) {
            if (name == null || name.isEmpty()) {
                return "?";
            }
            
            String[] parts = name.split(" ");
            if (parts.length >= 2) {
                return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
            }
            return name.substring(0, 1).toUpperCase();
        }
    }
}
