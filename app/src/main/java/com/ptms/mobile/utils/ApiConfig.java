package com.ptms.mobile.utils;

/**
 * Configuration de l'API PTMS Unifiée
 *
 * ⚠️ NOTE: Ces URLs sont des FALLBACKS uniquement.
 * L'URL réelle est configurée via SettingsManager (paramètres app).
 * Pour changer l'URL: Menu > Paramètres > URL du serveur
 */
public class ApiConfig {
    // ⚠️ URL de base du serveur PTMS - FALLBACK (configuré via SettingsManager)
    public static final String BASE_URL = "https://office.protti.group/hours/api/"; // Serveur PTMS avec anciens endpoints (fallback)
    public static final String UNIFIED_BASE_URL = "https://office.protti.group/hours/api/unified.php"; // API unifiée (si disponible) - PAS de slash final!
    
    // Endpoints de l'API unifiée
    public static final String LOGIN_ENDPOINT = "auth/login";
    public static final String PROJECTS_ENDPOINT = "ptms/projects";
    public static final String WORK_TYPES_ENDPOINT = "ptms/work-types";
    public static final String TIME_ENTRY_ENDPOINT = "ptms/time-entry";
    public static final String REPORTS_ENDPOINT = "ptms/reports";
    public static final String PROFILE_ENDPOINT = "ptms/profile";
    
    // Endpoints de fallback (anciens)
    public static final String LOGIN_ENDPOINT_FALLBACK = "login.php";
    public static final String PROJECTS_ENDPOINT_FALLBACK = "projects.php";
    public static final String WORK_TYPES_ENDPOINT_FALLBACK = "work-types.php";
    public static final String TIME_ENTRY_ENDPOINT_FALLBACK = "time-entry.php";
    public static final String REPORTS_ENDPOINT_FALLBACK = "reports.php";
    public static final String PROFILE_ENDPOINT_FALLBACK = "profile.php";
    
    // Endpoints système (nouvelles fonctionnalités)
    public static final String SYSTEM_STATUS_ENDPOINT = "system/status";
    public static final String SYSTEM_SEARCH_ENDPOINT = "system/search";
    public static final String HELP_ENDPOINT = "help";
    
    // Endpoints de chat PTMS
    public static final String CHAT_ROOMS_ENDPOINT = "chat/rooms";
    public static final String CHAT_MESSAGES_ENDPOINT = "chat/messages";
    public static final String CHAT_SEND_MESSAGE_ENDPOINT = "chat/send";
    public static final String CHAT_USERS_ENDPOINT = "chat/users";
    public static final String CHAT_TYPING_ENDPOINT = "chat/typing";
    public static final String CHAT_MARK_READ_ENDPOINT = "chat/mark-read";
    
    // Endpoints de fallback pour le chat (compatibles Android)
    public static final String CHAT_ROOMS_ENDPOINT_FALLBACK = "chat-rooms.php";
    public static final String CHAT_MESSAGES_ENDPOINT_FALLBACK = "chat-messages.php";
    public static final String CHAT_SEND_MESSAGE_ENDPOINT_FALLBACK = "chat-send.php";
    public static final String CHAT_USERS_ENDPOINT_FALLBACK = "chat-users.php";
    public static final String CHAT_TYPING_ENDPOINT_FALLBACK = "chat-typing.php";
    public static final String CHAT_MARK_READ_ENDPOINT_FALLBACK = "chat-mark-read.php";
    
    // Timeouts
    public static final int CONNECT_TIMEOUT = 30; // secondes
    public static final int READ_TIMEOUT = 30; // secondes
    public static final int WRITE_TIMEOUT = 30; // secondes
    
    // Configuration SSL
    // ✅ SÉCURITÉ: SSL validation activée par défaut en production
    // Pour dev local avec certificat self-signed: activer dans Paramètres
    public static final boolean DEFAULT_IGNORE_SSL = false; // SSL valide en production
}


