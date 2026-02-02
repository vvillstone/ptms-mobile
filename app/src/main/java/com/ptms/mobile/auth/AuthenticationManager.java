package com.ptms.mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ptms.mobile.models.Employee;
import com.ptms.mobile.utils.SessionManager;

/**
 * GESTIONNAIRE D'AUTHENTIFICATION UNIFIÉ
 *
 * Centralise TOUTE la logique d'authentification pour éviter les doublons entre:
 * - ptms_prefs (SharedPreferences)
 * - PTMSSession (SessionManager)
 * - initial_auth_prefs (InitialAuthManager)
 *
 * PRINCIPE: Une seule source de vérité pour l'état d'authentification
 *
 * @version 2.0
 * @since 2025-10-16
 */
public class AuthenticationManager {

    private static final String TAG = "AuthManager";
    private static final String PREFS_NAME = "ptms_prefs";

    // Singleton
    private static AuthenticationManager instance;

    private Context context;
    private SharedPreferences prefs;
    private SessionManager sessionManager;
    private InitialAuthManager initialAuthManager;

    // ==================== SINGLETON ====================

    private AuthenticationManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.sessionManager = new SessionManager(context);
        this.initialAuthManager = new InitialAuthManager(context);
    }

    public static synchronized AuthenticationManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthenticationManager(context);
        }
        return instance;
    }

    // ==================== VÉRIFICATIONS D'ÉTAT ====================

    /**
     * Vérifie si l'utilisateur est connecté (online OU offline)
     * UNIFIÉ: Vérifie les deux sources (SessionManager + ptms_prefs)
     */
    public boolean isLoggedIn() {
        // Vérification 1: SessionManager (session active)
        boolean sessionActive = sessionManager.isLoggedIn();

        // Vérification 2: ptms_prefs (token présent)
        String token = prefs.getString("auth_token", null);
        boolean hasToken = token != null && !token.isEmpty();

        // Vérification 3: Données utilisateur présentes
        int userId = prefs.getInt("user_id", -1);
        boolean hasUserData = userId > 0;

        Log.d(TAG, "État connexion: session=" + sessionActive +
              ", token=" + hasToken + ", userData=" + hasUserData);

        return sessionActive || (hasToken && hasUserData);
    }

    /**
     * Vérifie si l'authentification initiale a été effectuée
     */
    public boolean hasInitialAuth() {
        return initialAuthManager.hasInitialAuthentication();
    }

    /**
     * Vérifie si le mode offline est disponible
     */
    public boolean canUseOffline() {
        boolean hasAuth = hasInitialAuth();
        boolean hasCache = initialAuthManager.hasValidDataCache();
        boolean offlineEnabled = prefs.getBoolean("offline_login_enabled", false);

        Log.d(TAG, "Mode offline: auth=" + hasAuth + ", cache=" + hasCache +
              ", enabled=" + offlineEnabled);

        return hasAuth && hasCache;
    }

    /**
     * Vérifie si les credentials offline sont sauvegardés
     */
    public boolean hasOfflineCredentials() {
        String email = prefs.getString("offline_email", null);
        String passwordHash = prefs.getString("offline_password_hash", null);

        return email != null && passwordHash != null;
    }

    // ==================== SAUVEGARDE DE DONNÉES ====================

    /**
     * Sauvegarde les données de connexion (UNIFIÉ)
     * Remplace saveUserData() éparpillé dans plusieurs classes
     */
    public void saveLoginData(String token, Employee employee) {
        try {
            Log.d(TAG, "💾 Sauvegarde unifiée des données de connexion");

            if (employee == null) {
                Log.e(TAG, "❌ Employee null - impossible de sauvegarder");
                return;
            }

            // Données à sauvegarder
            int userId = employee.getId();
            String email = employee.getEmail() != null ? employee.getEmail() : "";
            String fullName = employee.getFullName() != null ? employee.getFullName() : "Utilisateur";
            int userType = employee.getType();

            // ========================================
            // SAUVEGARDE 1: SessionManager (session active)
            // ========================================
            sessionManager.createLoginSession(token, userId, email, fullName);
            Log.d(TAG, "✓ Sauvegarde SessionManager (PTMSSession)");

            // ========================================
            // SAUVEGARDE 2: ptms_prefs (persistance offline)
            // ========================================
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("auth_token", token);
            editor.putInt("user_id", userId);
            editor.putString("user_email", email);
            editor.putString("user_name", fullName);
            editor.putInt("user_type", userType);
            boolean success = editor.commit();

            Log.d(TAG, "✓ Sauvegarde ptms_prefs: " + (success ? "réussie" : "échouée"));

            // Vérification
            if (success) {
                String savedToken = prefs.getString("auth_token", null);
                Log.d(TAG, "✓ Vérification token: " + (savedToken != null ? "présent" : "absent"));
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur sauvegarde données", e);
        }
    }

    /**
     * Sauvegarde les credentials pour le mode offline
     */
    public void saveOfflineCredentials(String email, String password) {
        try {
            Log.d(TAG, "💾 Sauvegarde credentials offline");

            // Hash du mot de passe (SHA-256)
            String passwordHash = hashPassword(password);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("offline_email", email);
            editor.putString("offline_password_hash", passwordHash);
            editor.putBoolean("offline_login_enabled", true);
            boolean success = editor.commit();

            Log.d(TAG, "✓ Credentials offline: " + (success ? "sauvegardés" : "échec"));

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur sauvegarde credentials offline", e);
        }
    }

    /**
     * Valide les credentials offline
     */
    public boolean validateOfflineCredentials(String email, String password) {
        try {
            String savedEmail = prefs.getString("offline_email", null);
            String savedPasswordHash = prefs.getString("offline_password_hash", null);

            if (savedEmail == null || savedPasswordHash == null) {
                Log.d(TAG, "Pas de credentials offline");
                return false;
            }

            if (!email.equals(savedEmail)) {
                Log.d(TAG, "Email ne correspond pas");
                return false;
            }

            String enteredPasswordHash = hashPassword(password);
            boolean match = enteredPasswordHash.equals(savedPasswordHash);

            Log.d(TAG, "Validation credentials: " + (match ? "✓ OK" : "✗ Erreur"));

            return match;

        } catch (Exception e) {
            Log.e(TAG, "Erreur validation credentials", e);
            return false;
        }
    }

    // ==================== RÉCUPÉRATION DE DONNÉES ====================

    /**
     * Récupère l'ID utilisateur
     */
    public int getUserId() {
        // Essayer SessionManager
        int sessionUserId = sessionManager.getUserId();
        if (sessionUserId > 0) {
            return sessionUserId;
        }

        // Fallback sur ptms_prefs
        int prefsUserId = prefs.getInt("user_id", -1);

        // Compatibilité: ancienne clé employee_id
        if (prefsUserId == -1) {
            prefsUserId = prefs.getInt("employee_id", -1);
        }

        return prefsUserId;
    }

    /**
     * Récupère l'email utilisateur
     */
    public String getUserEmail() {
        // Essayer SessionManager
        String sessionEmail = sessionManager.getUserEmail();
        if (sessionEmail != null && !sessionEmail.isEmpty()) {
            return sessionEmail;
        }

        // Fallback sur ptms_prefs
        String prefsEmail = prefs.getString("user_email", null);

        // Compatibilité: ancienne clé employee_email
        if (prefsEmail == null) {
            prefsEmail = prefs.getString("employee_email", null);
        }

        return prefsEmail;
    }

    /**
     * Récupère le nom complet utilisateur
     */
    public String getUserName() {
        // Essayer SessionManager
        String sessionName = sessionManager.getUserName();
        if (sessionName != null && !sessionName.isEmpty()) {
            return sessionName;
        }

        // Fallback sur ptms_prefs
        String prefsName = prefs.getString("user_name", null);

        // Compatibilité: ancienne clé employee_name
        if (prefsName == null) {
            prefsName = prefs.getString("employee_name", null);
        }

        return prefsName != null ? prefsName : "Utilisateur";
    }

    /**
     * Récupère le token d'authentification
     */
    public String getAuthToken() {
        // Essayer SessionManager
        String sessionToken = sessionManager.getAuthToken();
        if (sessionToken != null && !sessionToken.isEmpty()) {
            return sessionToken;
        }

        // Fallback sur ptms_prefs
        return prefs.getString("auth_token", null);
    }

    /**
     * Récupère le type utilisateur
     */
    public int getUserType() {
        return prefs.getInt("user_type", 4); // Par défaut: EMPLOYEE (4)
    }

    // ==================== DÉCONNEXION ====================

    /**
     * Déconnecte l'utilisateur (TOUTES les sources)
     * NE supprime PAS les credentials offline ni l'auth initiale
     */
    public void logout() {
        Log.d(TAG, "🚪 Déconnexion utilisateur");

        // Supprimer la session active
        sessionManager.logoutUser();

        // Supprimer le token (mais garder user_id, user_name pour offline)
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("auth_token");
        editor.commit();

        Log.d(TAG, "✓ Déconnexion complète (credentials offline préservés)");
    }

    /**
     * RÉINITIALISATION COMPLÈTE (pour debug/tests)
     * Supprime TOUT y compris auth initiale et credentials offline
     */
    public void fullReset() {
        Log.d(TAG, "🔄 RÉINITIALISATION COMPLÈTE");

        // Supprimer session
        sessionManager.logoutUser();

        // Supprimer ptms_prefs
        prefs.edit().clear().commit();

        // Réinitialiser auth initiale
        initialAuthManager.resetInitialAuth();

        Log.d(TAG, "✓ Réinitialisation complète effectuée");
    }

    // ==================== UTILITAIRES ====================

    /**
     * Hash un mot de passe avec SHA-256
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convertir en hexadécimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Erreur hash password", e);
            return "";
        }
    }

    /**
     * Récupère l'état complet de l'authentification (pour debug)
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== ÉTAT AUTHENTIFICATION ===\n");
        info.append("Connecté: ").append(isLoggedIn()).append("\n");
        info.append("Auth initiale: ").append(hasInitialAuth()).append("\n");
        info.append("Mode offline: ").append(canUseOffline()).append("\n");
        info.append("User ID: ").append(getUserId()).append("\n");
        info.append("Email: ").append(getUserEmail()).append("\n");
        info.append("Nom: ").append(getUserName()).append("\n");
        info.append("Type: ").append(getUserType()).append("\n");
        info.append("Token présent: ").append(getAuthToken() != null).append("\n");
        info.append("Credentials offline: ").append(hasOfflineCredentials()).append("\n");

        return info.toString();
    }
}
