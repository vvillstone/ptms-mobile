package com.ptms.mobile.api;

import com.ptms.mobile.models.Employee;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.models.WorkType;
import com.ptms.mobile.models.ChatMessage;
import com.ptms.mobile.models.ChatRoom;
import com.ptms.mobile.models.ChatUser;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * Service API pour communiquer avec le serveur PTMS Unifié
 */
public interface ApiService {
    
    // Authentification
    @POST("login.php")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    // Projets - Nouveau format avec wrapper
    @GET("projects.php")
    Call<ProjectsResponse> getProjects(@Header("Authorization") String token);

    // Types de travail
    @GET("work-types.php")
    Call<List<WorkType>> getWorkTypes(@Header("Authorization") String token);

    // Types de notes (catégories personnalisées)
    @GET("note-types.php")
    Call<NoteTypesResponse> getNoteTypes(@Header("Authorization") String token);

    // Saisie d'heures
    @POST("time-entry.php")
    Call<ApiResponse> saveTimeEntry(@Header("Authorization") String token, @Body TimeReport report);
    
    // Rapports de l'employé
    @GET("reports.php")
    Call<List<TimeReport>> getReports(
        @Header("Authorization") String token,
        @Query("date_from") String dateFrom,
        @Query("date_to") String dateTo,
        @Query("project_id") Integer projectId
    );
    
    // Profil employé
    @GET("profile.php")
    Call<Employee> getProfile(@Header("Authorization") String token);
    
    // Système - Nouvelles fonctionnalités
    @GET("system/status")
    Call<SystemStatusResponse> getSystemStatus(@Header("Authorization") String token);
    
    @GET("system/search")
    Call<Map<String, Object>> globalSearch(
        @Header("Authorization") String token,
        @Query("q") String query,
        @Query("services") String services
    );
    
    // Chat PTMS - Endpoints compatibles Android
    @GET("chat-rooms.php")
    Call<ChatRoomsResponse> getChatRooms(@Header("Authorization") String token);
    
    @GET("chat-messages.php")
    Call<ChatMessagesResponse> getChatMessages(
        @Header("Authorization") String token,
        @Query("room_id") int roomId,
        @Query("limit") Integer limit,
        @Query("offset") Integer offset
    );
    
    @POST("chat-send.php")
    Call<ChatSendResponse> sendChatMessage(@Header("Authorization") String token, @Body SendMessageRequest request);
    
    @GET("chat-users.php")
    Call<ChatUsersResponse> getChatUsers(@Header("Authorization") String token);
    
    @POST("chat-typing.php")
    Call<ChatTypingResponse> setTypingStatus(
        @Header("Authorization") String token,
        @Body TypingRequest request
    );
    
    @POST("chat-mark-read.php")
    Call<ChatMarkReadResponse> markMessagesAsRead(
        @Header("Authorization") String token,
        @Body MarkReadRequest request
    );
    
    @POST("chat/conversations")
    Call<ChatConversationResponse> createOrGetConversation(
        @Header("Authorization") String token,
        @Query("otherUserId") int otherUserId
    );

    // Notes de projet (avec tous les champs requis par l'API)
    @Multipart
    @POST("project-notes.php")
    Call<CreateNoteResponse> createProjectNote(
        @Header("Authorization") String token,
        @Part("project_id") RequestBody projectId,
        @Part("note_type") RequestBody noteType,
        @Part("note_type_id") RequestBody noteTypeId,
        @Part("note_group") RequestBody noteGroup,
        @Part("title") RequestBody title,
        @Part("content") RequestBody content,
        @Part("transcription") RequestBody transcription,
        @Part("is_important") RequestBody isImportant,
        @Part("tags") RequestBody tags,
        @Part MultipartBody.Part audioFile,
        @Part MultipartBody.Part imageFile
    );

    // Mise à jour d'une note de projet existante
    @Multipart
    @POST("project-notes.php")
    Call<CreateNoteResponse> updateProjectNote(
        @Header("Authorization") String token,
        @Part("note_id") RequestBody noteId,
        @Part("project_id") RequestBody projectId,
        @Part("note_type") RequestBody noteType,
        @Part("note_type_id") RequestBody noteTypeId,
        @Part("note_group") RequestBody noteGroup,
        @Part("title") RequestBody title,
        @Part("content") RequestBody content,
        @Part("transcription") RequestBody transcription,
        @Part("is_important") RequestBody isImportant,
        @Part("tags") RequestBody tags,
        @Part MultipartBody.Part audioFile,
        @Part MultipartBody.Part imageFile
    );

    // Phase 2 - Offline-First: Upload multimédia simplifié
    @Multipart
    @POST("upload-media.php")
    Call<CreateNoteResponse> uploadProjectMedia(
        @Header("Authorization") String token,
        @Part("project_id") RequestBody projectId,
        @Part("note_type") RequestBody noteType,
        @Part("title") RequestBody title,
        @Part MultipartBody.Part mediaFile
    );

    @GET("project-notes.php")
    Call<NotesResponse> getProjectNotes(
        @Header("Authorization") String token,
        @Query("project_id") int projectId
    );

    // Classes pour les réponses API
    class LoginRequest {
        public String email;
        public String password;
        
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
    
    class LoginResponse {
        public boolean success;
        public String message;
        public String token;
        public Employee user; // Le serveur retourne 'user' au lieu de 'employee'
    }
    
    class ApiResponse {
        public boolean success;
        public String message;
        public Object data;
    }
    
    class SystemStatusResponse {
        public boolean success;
        public String message;
        public Map<String, ServiceStatus> services;
        
        public static class ServiceStatus {
            public boolean enabled;
            public String status;
            public String message;
            public long last_check;
        }
    }
    
    // Classes pour les requêtes de chat
    class SendMessageRequest {
        public int roomId;
        public String content;
        public String messageType;
        public String attachmentUrl;
        public String attachmentName;
        
        public SendMessageRequest(int roomId, String content) {
            this.roomId = roomId;
            this.content = content;
            this.messageType = "text";
        }
        
        public SendMessageRequest(int roomId, String content, String messageType, String attachmentUrl, String attachmentName) {
            this.roomId = roomId;
            this.content = content;
            this.messageType = messageType;
            this.attachmentUrl = attachmentUrl;
            this.attachmentName = attachmentName;
        }
    }
    
    class TypingRequest {
        public int roomId;
        public boolean isTyping;
        
        public TypingRequest(int roomId, boolean isTyping) {
            this.roomId = roomId;
            this.isTyping = isTyping;
        }
    }
    
    class MarkReadRequest {
        public int roomId;
        public int messageId;
        
        public MarkReadRequest(int roomId, int messageId) {
            this.roomId = roomId;
            this.messageId = messageId;
        }
    }
    
    // Classes de réponse pour les endpoints de chat compatibles Android
    class ChatRoomsResponse {
        public boolean success;
        public String message;
        public List<ChatRoom> rooms;
        public int total;
    }
    
    class ChatMessagesResponse {
        public boolean success;
        public String message;
        public List<ChatMessage> messages;
        public int total;
    }
    
    class ChatSendResponse {
        public boolean success;
        public String message;
        public ChatMessage chatMessage;
    }
    
    class ChatUsersResponse {
        public boolean success;
        public String message;
        public List<ChatUser> users;
        public int total;
    }
    
    class ChatTypingResponse {
        public boolean success;
        public String message;
        public List<String> typingUsers;
    }
    
    class ChatMarkReadResponse {
        public boolean success;
        public String message;
        public int unreadCount;
    }
    
    class ChatConversationResponse {
        public boolean success;
        public String message;
        public int conversationId;
    }

    // Classe de réponse pour les projets (format avec wrapper)
    class ProjectsResponse {
        public boolean success;
        public String message;
        public List<Project> projects;
    }

    // Classes de réponse pour les notes de projet
    class CreateNoteResponse {
        public boolean success;
        public String message;
        public int noteId;
        public String fileUrl; // URL du fichier uploadé (Phase 2 - Multimédia)
    }

    class NotesResponse {
        public boolean success;
        public String message;
        public List<com.ptms.mobile.models.ProjectNote> notes;
    }

    // Classe de réponse pour les types de notes
    class NoteTypesResponse {
        public boolean success;
        public String message;
        public List<com.ptms.mobile.models.NoteType> types; // "types" dans l'API backend
        public int count;
    }
}




