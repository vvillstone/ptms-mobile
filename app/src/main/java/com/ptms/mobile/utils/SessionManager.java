package com.ptms.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestionnaire de session pour le chat (wrapper autour de SharedPreferences)
 */
public class SessionManager {
    private static final String PREF_NAME = "PTMSSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_SESSION_COOKIE = "session_cookie";

    // Firebase Cloud Messaging
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_CHAT_NOTIF_ENABLED = "chat_notif_enabled";
    private static final String KEY_PROJECT_NOTIF_ENABLED = "project_notif_enabled";
    private static final String KEY_REMINDER_NOTIF_ENABLED = "reminder_notif_enabled";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Créer une session de connexion
     */
    public void createLoginSession(String token, int userId, String email, String name) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.commit();
    }

    /**
     * Vérifier si l'utilisateur est connecté
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Obtenir le token
     */
    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    /**
     * Obtenir le token d'authentification (alias pour getToken)
     */
    public String getAuthToken() {
        return getToken();
    }

    /**
     * Obtenir le cookie de session
     */
    public String getSessionCookie() {
        return pref.getString(KEY_SESSION_COOKIE, null);
    }

    /**
     * Définir le cookie de session
     */
    public void setSessionCookie(String cookie) {
        editor.putString(KEY_SESSION_COOKIE, cookie);
        editor.commit();
    }

    /**
     * Obtenir l'ID utilisateur
     */
    public int getUserId() {
        return pref.getInt(KEY_USER_ID, 0);
    }

    /**
     * Obtenir l'email utilisateur
     */
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Obtenir le nom utilisateur
     */
    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    /**
     * Déconnecter l'utilisateur (garde les données offline)
     * Efface seulement la session active, pas les données d'authentification initiale
     */
    public void logoutUser() {
        // ⚠️ NE PAS utiliser clear() qui efface TOUT
        // On efface seulement les données de session, pas l'auth initiale
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_SESSION_COOKIE);
        // On GARDE: user_id, user_email, user_name, fcm_token, notif settings
        // pour permettre la reconnexion offline
        editor.commit();
    }

    /**
     * Déconnecter complètement l'utilisateur (efface TOUTES les données)
     * Utilisé pour changer de compte ou reset complet
     */
    public void logoutUserCompletely() {
        editor.clear();
        editor.commit();
    }

    // ==================== FIREBASE CLOUD MESSAGING ====================

    /**
     * Sauvegarder le token FCM
     */
    public void saveFcmToken(String token) {
        editor.putString(KEY_FCM_TOKEN, token);
        editor.commit();
    }

    /**
     * Récupérer le token FCM
     */
    public String getFcmToken() {
        return pref.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * Supprimer le token FCM
     */
    public void clearFcmToken() {
        editor.remove(KEY_FCM_TOKEN);
        editor.commit();
    }

    /**
     * Activer les notifications chat
     */
    public void setChatNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_CHAT_NOTIF_ENABLED, enabled);
        editor.commit();
    }

    /**
     * Vérifier si les notifications chat sont activées
     */
    public boolean isChatNotificationsEnabled() {
        return pref.getBoolean(KEY_CHAT_NOTIF_ENABLED, true); // Activé par défaut
    }

    /**
     * Activer les notifications projet
     */
    public void setProjectNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_PROJECT_NOTIF_ENABLED, enabled);
        editor.commit();
    }

    /**
     * Vérifier si les notifications projet sont activées
     */
    public boolean isProjectNotificationsEnabled() {
        return pref.getBoolean(KEY_PROJECT_NOTIF_ENABLED, true); // Activé par défaut
    }

    /**
     * Activer les rappels
     */
    public void setReminderNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_REMINDER_NOTIF_ENABLED, enabled);
        editor.commit();
    }

    /**
     * Vérifier si les rappels sont activés
     */
    public boolean isReminderNotificationsEnabled() {
        return pref.getBoolean(KEY_REMINDER_NOTIF_ENABLED, true); // Activé par défaut
    }
}
