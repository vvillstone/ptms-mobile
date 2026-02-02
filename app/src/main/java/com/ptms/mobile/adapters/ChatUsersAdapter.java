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
import com.ptms.mobile.models.ChatUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptateur pour la liste des utilisateurs de chat
 */
public class ChatUsersAdapter extends RecyclerView.Adapter<ChatUsersAdapter.UserViewHolder> {
    
    private Context context;
    private List<ChatUser> users;
    private List<ChatUser> selectedUsers;
    private OnUserClickListener listener;
    private boolean selectableMode = false;

    public interface OnUserClickListener {
        void onUserClick(ChatUser user);
    }
    
    public ChatUsersAdapter(Context context) {
        this.context = context;
        this.users = new ArrayList<>();
        this.selectedUsers = new ArrayList<>();
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setSelectable(boolean selectable) {
        this.selectableMode = selectable;
        notifyDataSetChanged();
    }

    public List<ChatUser> getSelectedUsers() {
        return new ArrayList<>(selectedUsers);
    }

    public void updateUsers(List<ChatUser> newUsers) {
        this.users.clear();
        if (newUsers != null) {
            this.users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_user, parent, false);
        return new UserViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        ChatUser user = users.get(position);
        holder.bind(user);
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }
    
    class UserViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvUserName;
        private TextView tvUserStatus;
        private ImageView imgOnlineStatus;
        private View avatarContainer;
        private TextView tvInitials;
        
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserStatus = itemView.findViewById(R.id.tv_user_status);
            imgOnlineStatus = itemView.findViewById(R.id.img_online_status);
            avatarContainer = itemView.findViewById(R.id.avatar_container);
            tvInitials = itemView.findViewById(R.id.tv_initials);
        }
        
        public void bind(final ChatUser user) {
            // Nom de l'utilisateur (utiliser getDisplayName pour prioriser pseudo > nom complet > username)
            tvUserName.setText(user.getDisplayName());
            
            // Statut
            if (user.isOnline()) {
                tvUserStatus.setText("En ligne");
                tvUserStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                if (imgOnlineStatus != null) {
                    imgOnlineStatus.setVisibility(View.VISIBLE);
                    imgOnlineStatus.setImageResource(R.drawable.ic_circle_green);
                }
            } else {
                tvUserStatus.setText("Hors ligne");
                tvUserStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                if (imgOnlineStatus != null) {
                    imgOnlineStatus.setVisibility(View.GONE);
                }
            }
            
            // Initiales pour l'avatar
            if (tvInitials != null) {
                tvInitials.setText(user.getInitials());
            }
            
            // Clic sur l'utilisateur
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectableMode) {
                        // Mode sélection multiple
                        if (selectedUsers.contains(user)) {
                            selectedUsers.remove(user);
                            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                        } else {
                            selectedUsers.add(user);
                            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
                        }
                    } else {
                        // Mode simple clic
                        if (listener != null) {
                            listener.onUserClick(user);
                        }
                    }
                }
            });

            // Mettre à jour l'apparence selon l'état de sélection
            if (selectableMode && selectedUsers.contains(user)) {
                itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
            } else {
                itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            }
        }
    }
}

