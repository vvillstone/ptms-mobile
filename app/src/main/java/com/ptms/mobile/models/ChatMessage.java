package com.ptms.mobile.models;

import java.util.Date;

/**
 * Modèle pour les messages de chat PTMS
 */
public class ChatMessage {
    private int id;
    private int chatRoomId;
    private int senderId;
    private String senderName;
    private String content;
    private Date timestamp;
    private String messageType; // "text", "image", "file", "system"
    private boolean isRead;
    private String attachmentUrl;
    private String attachmentName;
    
    // Constructeurs
    public ChatMessage() {}
    
    public ChatMessage(int chatRoomId, int senderId, String senderName, String content) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = new Date();
        this.messageType = "text";
        this.isRead = false;
    }
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getChatRoomId() {
        return chatRoomId;
    }
    
    public void setChatRoomId(int chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
    
    public int getSenderId() {
        return senderId;
    }
    
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public String getAttachmentUrl() {
        return attachmentUrl;
    }
    
    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
    
    public String getAttachmentName() {
        return attachmentName;
    }
    
    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }
    
    // Méthodes utilitaires
    public boolean isSystemMessage() {
        return "system".equals(messageType);
    }
    
    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.isEmpty();
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", chatRoomId=" + chatRoomId +
                ", senderId=" + senderId +
                ", senderName='" + senderName + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", messageType='" + messageType + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}
