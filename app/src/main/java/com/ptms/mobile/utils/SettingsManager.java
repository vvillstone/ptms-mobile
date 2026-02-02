package com.ptms.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestionnaire des paramètres généraux de l'application
 */
public class SettingsManager {
    
    private static final String PREFS_NAME = "ptms_settings";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_IGNORE_SSL = "ignore_ssl";
    private static final String KEY_TIMEOUT = "timeout";
    private static final String KEY_DEBUG_MODE = "debug_mode";
    private static final String KEY_CHAT_ENABLED = "chat_enabled"; // true = Chat activé, false = Chat désactivé
    private static final String KEY_CHAT_POLLING_ENABLED = "chat_polling_enabled";
    private static final String KEY_CHAT_VERSION = "chat_version"; // 1 = Polling HTTP, 2 = WebSocket
    private static final String KEY_CHAT_GROUPED_VIEW = "chat_grouped_view"; // true = Regroupé par type

    // Valeurs par défaut
    private static final String DEFAULT_SERVER_URL = "https://office.protti.group/hours"; // URL du serveur PTMS
    private static final boolean DEFAULT_IGNORE_SSL = false; // SSL valide en production
    private static final int DEFAULT_TIMEOUT = 30;
    // ✅ SÉCURITÉ: Debug désactivé par défaut en production
    private static final boolean DEFAULT_DEBUG_MODE = false;
    private static final boolean DEFAULT_CHAT_ENABLED = true; // Chat activé par défaut
    private static final boolean DEFAULT_CHAT_POLLING_ENABLED = true; // Activé par défaut
    private static final int DEFAULT_CHAT_VERSION = 1; // Version 1 (Polling) par défaut
    private static final boolean DEFAULT_CHAT_GROUPED_VIEW = true; // Regroupé par défaut

    // Constantes pour les versions du chat
    public static final int CHAT_VERSION_POLLING = 1;
    public static final int CHAT_VERSION_WEBSOCKET = 2;

    private SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Getters
    public String getServerUrl() {
        String baseUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
        // Ajouter automatiquement /api/ si ce n'est pas déjà là
        return normalizeServerUrl(baseUrl);
    }

    /**
     * Normalise l'URL du serveur pour toujours avoir le bon format
     * Exemples:
     *   192.168.188.28 -> https://192.168.188.28/api/
     *   http://192.168.188.28 -> http://192.168.188.28/api/
     *   https://192.168.188.28/api/ -> https://192.168.188.28/api/
     */
    private String normalizeServerUrl(String url) {
        if (url == null || url.isEmpty()) {
            return DEFAULT_SERVER_URL + "/api/";
        }

        url = url.trim();

        // Si pas de protocole, ajouter https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        // Retirer le slash final s'il existe
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Ajouter /api/ si pas déjà là
        if (!url.endsWith("/api")) {
            url = url + "/api";
        }

        // Ajouter le slash final
        url = url + "/";

        return url;
    }

    /**
     * Retourne l'URL de base RAW (telle qu'entrée par l'utilisateur)
     */
    public String getServerUrlRaw() {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }
    
    public boolean isIgnoreSsl() {
        return prefs.getBoolean(KEY_IGNORE_SSL, DEFAULT_IGNORE_SSL);
    }
    
    public int getTimeout() {
        return prefs.getInt(KEY_TIMEOUT, DEFAULT_TIMEOUT);
    }
    
    public boolean isDebugMode() {
        return prefs.getBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE);
    }

    public boolean isChatEnabled() {
        return prefs.getBoolean(KEY_CHAT_ENABLED, DEFAULT_CHAT_ENABLED);
    }

    public boolean isChatPollingEnabled() {
        return prefs.getBoolean(KEY_CHAT_POLLING_ENABLED, DEFAULT_CHAT_POLLING_ENABLED);
    }

    public int getChatVersion() {
        return prefs.getInt(KEY_CHAT_VERSION, DEFAULT_CHAT_VERSION);
    }

    public boolean isChatVersionPolling() {
        return getChatVersion() == CHAT_VERSION_POLLING;
    }

    public boolean isChatVersionWebSocket() {
        return getChatVersion() == CHAT_VERSION_WEBSOCKET;
    }

    public boolean isChatGroupedView() {
        return prefs.getBoolean(KEY_CHAT_GROUPED_VIEW, DEFAULT_CHAT_GROUPED_VIEW);
    }

    // Setters
    public void setServerUrl(String url) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }
    
    public void setIgnoreSsl(boolean ignore) {
        prefs.edit().putBoolean(KEY_IGNORE_SSL, ignore).apply();
    }
    
    public void setTimeout(int timeout) {
        prefs.edit().putInt(KEY_TIMEOUT, timeout).apply();
    }
    
    public void setDebugMode(boolean debug) {
        prefs.edit().putBoolean(KEY_DEBUG_MODE, debug).apply();
    }

    public void setChatEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CHAT_ENABLED, enabled).apply();
    }

    public void setChatPollingEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CHAT_POLLING_ENABLED, enabled).apply();
    }

    public void setChatVersion(int version) {
        if (version == CHAT_VERSION_POLLING || version == CHAT_VERSION_WEBSOCKET) {
            prefs.edit().putInt(KEY_CHAT_VERSION, version).apply();
        }
    }

    public void setChatGroupedView(boolean grouped) {
        prefs.edit().putBoolean(KEY_CHAT_GROUPED_VIEW, grouped).apply();
    }

    // Méthodes utilitaires
    public void resetToDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
        editor.putBoolean(KEY_IGNORE_SSL, DEFAULT_IGNORE_SSL);
        editor.putInt(KEY_TIMEOUT, DEFAULT_TIMEOUT);
        editor.putBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE);
        editor.putBoolean(KEY_CHAT_ENABLED, DEFAULT_CHAT_ENABLED);
        editor.putBoolean(KEY_CHAT_POLLING_ENABLED, DEFAULT_CHAT_POLLING_ENABLED);
        editor.putInt(KEY_CHAT_VERSION, DEFAULT_CHAT_VERSION);
        editor.putBoolean(KEY_CHAT_GROUPED_VIEW, DEFAULT_CHAT_GROUPED_VIEW);
        editor.apply();
    }
    
    public boolean isUrlValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String trimmedUrl = url.trim();

        // Accepter http://, https://, ou juste une IP/domaine
        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            return true;
        }

        // Accepter une IP seule (ex: 192.168.188.28)
        // Pattern simple pour IPv4: xxx.xxx.xxx.xxx
        if (trimmedUrl.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            return true;
        }

        // Accepter un nom de domaine (ex: serveralpha.protti.group)
        if (trimmedUrl.matches("^[a-zA-Z0-9][a-zA-Z0-9-\\.]*[a-zA-Z0-9]$")) {
            return true;
        }

        return false;
    }
}
