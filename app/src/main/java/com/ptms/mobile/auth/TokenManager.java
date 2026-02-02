package com.ptms.mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Gestionnaire centralisé des tokens d'authentification
 * Gère le stockage, la validation et l'expiration des tokens
 */
public class TokenManager {

    private static final String TAG = "TokenManager";
    private static final String PREFS_NAME = "ptms_prefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_TOKEN_TIMESTAMP = "token_timestamp";
    private static final String KEY_OFFLINE_MODE_ENABLED = "offline_mode_enabled";

    // Durée de validité du token (24 heures par défaut)
    private static final long TOKEN_VALIDITY_DURATION = 24 * 60 * 60 * 1000; // 24h en ms

    private static TokenManager instance;
    private final SharedPreferences prefs;
    private final Context context;

    /**
     * Constructeur privé (Singleton)
     */
    private TokenManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Obtenir l'instance singleton
     */
    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        }
        return instance;
    }

    /**
     * Récupérer le token d'authentification
     * @return Le token ou null s'il n'existe pas
     */
    public String getToken() {
        String token = prefs.getString(KEY_AUTH_TOKEN, null);

        if (token == null || token.isEmpty()) {
            Log.w(TAG, "Aucun token d'authentification trouvé");
            return null;
        }

        // Vérifier si le token est expiré
        if (isTokenExpired()) {
            Log.w(TAG, "Token expiré (âge: " + getTokenAge() + "ms)");

            // Ne pas supprimer automatiquement le token expiré
            // L'utilisateur peut toujours l'utiliser en mode offline
            // Retourner quand même le token
            return token;
        }

        return token;
    }

    /**
     * Vérifier si un token valide existe
     * @return true si un token existe (même expiré)
     */
    public boolean hasToken() {
        String token = prefs.getString(KEY_AUTH_TOKEN, null);
        return token != null && !token.isEmpty();
    }

    /**
     * Vérifier si le token est valide (non expiré)
     * @return true si le token existe et n'est pas expiré
     */
    public boolean hasValidToken() {
        return hasToken() && !isTokenExpired();
    }

    /**
     * Sauvegarder un nouveau token
     * @param token Le token à sauvegarder
     */
    public void saveToken(String token) {
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "Tentative de sauvegarde d'un token vide");
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "Token sauvegardé avec succès (longueur: " + token.length() + ")");
    }

    /**
     * Supprimer le token
     */
    public void clearToken() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_AUTH_TOKEN);
        editor.remove(KEY_TOKEN_TIMESTAMP);
        editor.apply();

        Log.d(TAG, "Token supprimé");
    }

    /**
     * Vérifier si le token est expiré
     * @return true si le token est expiré
     */
    public boolean isTokenExpired() {
        if (!hasToken()) {
            return true;
        }

        long tokenAge = getTokenAge();
        boolean expired = tokenAge > TOKEN_VALIDITY_DURATION;

        if (expired) {
            Log.w(TAG, "Token expiré - Âge: " + (tokenAge / 1000 / 60 / 60) + "h");
        }

        return expired;
    }

    /**
     * Obtenir l'âge du token en millisecondes
     * @return L'âge du token
     */
    public long getTokenAge() {
        long timestamp = prefs.getLong(KEY_TOKEN_TIMESTAMP, 0);

        if (timestamp == 0) {
            Log.w(TAG, "Aucun timestamp trouvé pour le token");
            return Long.MAX_VALUE; // Token très ancien
        }

        return System.currentTimeMillis() - timestamp;
    }

    /**
     * Obtenir le temps restant avant expiration du token (en millisecondes)
     * @return Le temps restant, ou 0 si expiré
     */
    public long getTokenTimeRemaining() {
        long age = getTokenAge();
        long remaining = TOKEN_VALIDITY_DURATION - age;
        return Math.max(0, remaining);
    }

    /**
     * Vérifier si le mode offline est disponible
     * @return true si le mode offline est activé
     */
    public boolean canUseOfflineMode() {
        boolean enabled = prefs.getBoolean(KEY_OFFLINE_MODE_ENABLED, false);

        // Vérifier également si les credentials offline existent
        boolean hasOfflineEmail = prefs.getString("offline_email", null) != null;
        boolean hasOfflinePassword = prefs.getString("offline_password_hash", null) != null;

        return enabled && hasOfflineEmail && hasOfflinePassword;
    }

    /**
     * Activer le mode offline
     */
    public void enableOfflineMode() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_OFFLINE_MODE_ENABLED, true);
        editor.apply();

        Log.d(TAG, "Mode offline activé");
    }

    /**
     * Désactiver le mode offline
     */
    public void disableOfflineMode() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_OFFLINE_MODE_ENABLED, false);
        editor.apply();

        Log.d(TAG, "Mode offline désactivé");
    }

    /**
     * Obtenir le token avec gestion d'erreur
     * @param fallbackValue Valeur de fallback si le token n'existe pas
     * @return Le token ou la valeur de fallback
     */
    public String getTokenOrDefault(String fallbackValue) {
        String token = getToken();
        return (token != null) ? token : fallbackValue;
    }

    /**
     * Vérifier si le token est valide pour une utilisation en ligne
     * @return true si le token est valide et non expiré
     */
    public boolean isValidForOnlineUse() {
        return hasValidToken();
    }

    /**
     * Vérifier si le token est valide pour une utilisation hors ligne
     * @return true si un token existe (même expiré) et que le mode offline est activé
     */
    public boolean isValidForOfflineUse() {
        return hasToken() && canUseOfflineMode();
    }

    /**
     * Rafraîchir le timestamp du token (sans changer le token)
     * Utile après une vérification réussie avec le serveur
     */
    public void refreshTokenTimestamp() {
        if (hasToken()) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis());
            editor.apply();

            Log.d(TAG, "Timestamp du token rafraîchi");
        }
    }

    /**
     * Obtenir des informations de diagnostic sur le token
     * @return String avec les informations de diagnostic
     */
    public String getDiagnosticInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TOKEN MANAGER DIAGNOSTIC ===\n\n");

        sb.append("Token présent: ");
        sb.append(hasToken() ? "✅ OUI" : "❌ NON");
        sb.append("\n");

        if (hasToken()) {
            String token = prefs.getString(KEY_AUTH_TOKEN, "");
            sb.append("Token (10 premiers caractères): ");
            sb.append(token.substring(0, Math.min(10, token.length())));
            sb.append("...\n");

            sb.append("Longueur du token: ");
            sb.append(token.length());
            sb.append(" caractères\n");

            sb.append("Âge du token: ");
            long ageHours = getTokenAge() / (1000 * 60 * 60);
            sb.append(ageHours);
            sb.append(" heures\n");

            sb.append("Token expiré: ");
            sb.append(isTokenExpired() ? "❌ OUI" : "✅ NON");
            sb.append("\n");

            if (!isTokenExpired()) {
                sb.append("Temps restant: ");
                long remainingHours = getTokenTimeRemaining() / (1000 * 60 * 60);
                sb.append(remainingHours);
                sb.append(" heures\n");
            }
        }

        sb.append("\nMode offline disponible: ");
        sb.append(canUseOfflineMode() ? "✅ OUI" : "❌ NON");
        sb.append("\n");

        sb.append("Valide pour utilisation en ligne: ");
        sb.append(isValidForOnlineUse() ? "✅ OUI" : "❌ NON");
        sb.append("\n");

        sb.append("Valide pour utilisation hors ligne: ");
        sb.append(isValidForOfflineUse() ? "✅ OUI" : "❌ NON");
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Logger les informations de diagnostic
     */
    public void logDiagnostic() {
        String diagnostic = getDiagnosticInfo();
        String[] lines = diagnostic.split("\n");
        for (String line : lines) {
            Log.d(TAG, line);
        }
    }
}
