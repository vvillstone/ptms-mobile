package com.ptms.mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.WorkType;
import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.api.ApiService.LoginRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Gestionnaire d'authentification initiale obligatoire
 * Garantit qu'un utilisateur s'est connecté au moins une fois avec des identifiants valides
 * et a téléchargé les données de référence nécessaires pour le mode hors ligne
 */
public class InitialAuthManager {
    
    private static final String TAG = "InitialAuthManager";
    private static final String PREFS_NAME = "initial_auth_prefs";
    private static final String KEY_HAS_INITIAL_AUTH = "has_initial_auth";
    private static final String KEY_AUTH_DATE = "auth_date";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_DATA_CACHE_DATE = "data_cache_date";
    private static final String KEY_PROJECTS_COUNT = "projects_count";
    private static final String KEY_WORK_TYPES_COUNT = "work_types_count";
    
    private Context context;
    private ApiService apiService;
    private OfflineDatabaseHelper dbHelper;
    private SharedPreferences prefs;

    // ✅ FIX: Stockage temporaire du mot de passe pour le hash offline
    private String pendingPassword;
    
    public InitialAuthManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialisation sécurisée des services
        try {
            this.apiService = ApiClient.getInstance(context).getApiService();
        } catch (Exception e) {
            Log.e(TAG, "Erreur initialisation API service", e);
            this.apiService = null;
        }
        
        try {
            this.dbHelper = new OfflineDatabaseHelper(context);
        } catch (Exception e) {
            Log.e(TAG, "Erreur initialisation base de données", e);
            this.dbHelper = null;
        }
    }
    
    // ==================== VÉRIFICATION DE L'AUTHENTIFICATION INITIALE ====================
    
    /**
     * Vérifie si l'utilisateur a effectué une authentification initiale valide
     */
    public boolean hasInitialAuthentication() {
        boolean hasAuth = prefs.getBoolean(KEY_HAS_INITIAL_AUTH, false);
        long authDate = prefs.getLong(KEY_AUTH_DATE, 0);
        String userEmail = prefs.getString(KEY_USER_EMAIL, "");
        
        Log.d(TAG, "Vérification authentification initiale: " + hasAuth + 
              " (Date: " + authDate + ", Email: " + userEmail + ")");
        
        return hasAuth && authDate > 0 && !userEmail.isEmpty();
    }
    
    /**
     * Vérifie si les données de référence sont à jour
     */
    public boolean hasValidDataCache() {
        long cacheDate = prefs.getLong(KEY_DATA_CACHE_DATE, 0);
        int projectsCount = prefs.getInt(KEY_PROJECTS_COUNT, 0);
        int workTypesCount = prefs.getInt(KEY_WORK_TYPES_COUNT, 0);
        
        // Considérer le cache valide si il a moins de 7 jours et contient des données
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        boolean isRecent = cacheDate > sevenDaysAgo;
        boolean hasData = projectsCount > 0 && workTypesCount > 0;
        
        Log.d(TAG, "Vérification cache données: récent=" + isRecent + 
              ", projets=" + projectsCount + ", types=" + workTypesCount);
        
        return isRecent && hasData;
    }
    
    /**
     * Vérifie si l'utilisateur peut utiliser l'application hors ligne
     */
    public boolean canUseOffline() {
        try {
            boolean hasAuth = hasInitialAuthentication();
            boolean hasCache = hasValidDataCache();
            
            Log.d(TAG, "Peut utiliser hors ligne: auth=" + hasAuth + ", cache=" + hasCache);
            
            return hasAuth && hasCache;
        } catch (Exception e) {
            Log.e(TAG, "Erreur vérification mode hors ligne", e);
            return false;
        }
    }
    
    // ==================== AUTHENTIFICATION INITIALE ====================
    
    /**
     * Effectue l'authentification initiale et télécharge les données de référence
     */
    public void performInitialAuthentication(String email, String password, InitialAuthCallback callback) {
        Log.d(TAG, "Début de l'authentification initiale pour: " + email);

        // ✅ FIX: Stocker temporairement le mot de passe pour le hash offline
        this.pendingPassword = password;

        if (callback != null) {
            callback.onProgress("🔐 Vérification des identifiants...");
        }

        try {
            // 1. Tester la connexion avec les identifiants
            testCredentials(email, password, new CredentialsTestCallback() {
                @Override
                public void onCredentialsValid(String token, ApiService.LoginResponse loginResponse) {
                    Log.d(TAG, "Identifiants valides, téléchargement des données de référence...");

                    if (callback != null) {
                        callback.onProgress("✅ Authentification réussie\n📥 Téléchargement des données...");
                    }

                    // 2. Télécharger les données de référence
                    downloadReferenceData(token, new ReferenceDataCallback() {
                        @Override
                        public void onDataDownloaded(int projectsCount, int workTypesCount) {
                            // 3. Marquer l'authentification initiale comme réussie
                            markInitialAuthSuccess(email, token, loginResponse, projectsCount, workTypesCount);
                            
                            Log.d(TAG, "Authentification initiale réussie: " + projectsCount + " projets, " + workTypesCount + " types");
                            
                            if (callback != null) {
                                callback.onInitialAuthSuccess(projectsCount, workTypesCount);
                            }
                        }
                        
                        @Override
                        public void onDataError(String error) {
                            Log.e(TAG, "Erreur téléchargement données: " + error);
                            // ✅ FIX: Nettoyer le mot de passe temporaire en cas d'erreur
                            pendingPassword = null;
                            if (callback != null) {
                                callback.onInitialAuthError("Erreur téléchargement données: " + error);
                            }
                        }
                    });
                }
                
                @Override
                public void onCredentialsInvalid(String error) {
                    Log.e(TAG, "Identifiants invalides: " + error);
                    // ✅ FIX: Nettoyer le mot de passe temporaire en cas d'erreur
                    pendingPassword = null;
                    if (callback != null) {
                        callback.onInitialAuthError("Identifiants invalides: " + error);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur authentification initiale", e);
            // ✅ FIX: Nettoyer le mot de passe temporaire en cas d'erreur
            pendingPassword = null;
            if (callback != null) {
                callback.onInitialAuthError("Erreur: " + e.getMessage());
            }
        }
    }
    
    /**
     * Teste les identifiants de l'utilisateur
     */
    private void testCredentials(String email, String password, CredentialsTestCallback callback) {
        try {
            if (apiService == null) {
                callback.onCredentialsInvalid("Service API non disponible");
                return;
            }
            
            // Créer une requête de connexion de test
            LoginRequest loginRequest = new LoginRequest(email, password);
            
            Call<ApiService.LoginResponse> loginCall = apiService.login(loginRequest);
            loginCall.enqueue(new Callback<ApiService.LoginResponse>() {
                @Override
                public void onResponse(Call<ApiService.LoginResponse> call, Response<ApiService.LoginResponse> response) {
                    if (response.isSuccessful()) {
                        // Extraire le token de la réponse
                        String token = extractTokenFromResponse(response.body());
                        if (token != null && !token.isEmpty()) {
                            callback.onCredentialsValid(token, response.body());  // ✅ Passer la réponse complète
                        } else {
                            callback.onCredentialsInvalid("Token manquant dans la réponse");
                        }
                    } else {
                        callback.onCredentialsInvalid("Erreur " + response.code() + ": " + response.message());
                    }
                }
                
                @Override
                public void onFailure(Call<ApiService.LoginResponse> call, Throwable t) {
                    callback.onCredentialsInvalid("Erreur réseau: " + t.getMessage());
                }
            });
            
        } catch (Exception e) {
            callback.onCredentialsInvalid("Erreur: " + e.getMessage());
        }
    }
    
    /**
     * Télécharge les données de référence (projets et types de travail)
     */
    private void downloadReferenceData(String token, ReferenceDataCallback callback) {
        try {
            if (apiService == null) {
                callback.onDataError("Service API non disponible");
                return;
            }
            
            // Télécharger les projets
            Call<ApiService.ProjectsResponse> projectsCall = apiService.getProjects(token);
            projectsCall.enqueue(new Callback<ApiService.ProjectsResponse>() {
                @Override
                public void onResponse(Call<ApiService.ProjectsResponse> call, Response<ApiService.ProjectsResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        List<Project> projects = response.body().projects;
                        Log.d(TAG, "Projets téléchargés: " + projects.size());
                        
                        // Sauvegarder les projets dans la base locale
                        if (dbHelper != null) {
                            for (Project project : projects) {
                                dbHelper.insertProject(project);
                            }
                        } else {
                            Log.w(TAG, "Base de données non disponible pour sauvegarder les projets");
                        }
                        
                        // Télécharger les types de travail
                        downloadWorkTypes(token, projects.size(), callback);
                        
                    } else {
                        callback.onDataError("Erreur téléchargement projets: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<ApiService.ProjectsResponse> call, Throwable t) {
                    callback.onDataError("Erreur téléchargement projets: " + t.getMessage());
                }
            });
            
        } catch (Exception e) {
            callback.onDataError("Erreur: " + e.getMessage());
        }
    }
    
    /**
     * Télécharge les types de travail
     */
    private void downloadWorkTypes(String token, int projectsCount, ReferenceDataCallback callback) {
        try {
            if (apiService == null) {
                callback.onDataError("Service API non disponible");
                return;
            }
            
            Call<List<WorkType>> workTypesCall = apiService.getWorkTypes(token);
            workTypesCall.enqueue(new Callback<List<WorkType>>() {
                @Override
                public void onResponse(Call<List<WorkType>> call, Response<List<WorkType>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<WorkType> workTypes = response.body();
                        Log.d(TAG, "Types de travail téléchargés: " + workTypes.size());
                        
                        // Sauvegarder les types de travail dans la base locale
                        if (dbHelper != null) {
                            for (WorkType workType : workTypes) {
                                dbHelper.insertWorkType(workType);
                            }
                        } else {
                            Log.w(TAG, "Base de données non disponible pour sauvegarder les types de travail");
                        }
                        
                        // Succès complet
                        callback.onDataDownloaded(projectsCount, workTypes.size());
                        
                    } else {
                        callback.onDataError("Erreur téléchargement types de travail: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<List<WorkType>> call, Throwable t) {
                    callback.onDataError("Erreur téléchargement types de travail: " + t.getMessage());
                }
            });
            
        } catch (Exception e) {
            callback.onDataError("Erreur: " + e.getMessage());
        }
    }
    
    /**
     * Marque l'authentification initiale comme réussie
     */
    private void markInitialAuthSuccess(String email, String token, ApiService.LoginResponse loginResponse, int projectsCount, int workTypesCount) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_HAS_INITIAL_AUTH, true);
        editor.putLong(KEY_AUTH_DATE, System.currentTimeMillis());
        editor.putString(KEY_USER_EMAIL, email);
        editor.putLong(KEY_DATA_CACHE_DATE, System.currentTimeMillis());
        editor.putInt(KEY_PROJECTS_COUNT, projectsCount);
        editor.putInt(KEY_WORK_TYPES_COUNT, workTypesCount);
        editor.apply();

        // Sauvegarder aussi le token et les infos utilisateur dans les préférences d'authentification
        SharedPreferences authPrefs = context.getSharedPreferences("ptms_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor authEditor = authPrefs.edit();
        authEditor.putString("auth_token", token);

        // ✅ Sauvegarder les informations utilisateur depuis la réponse de login
        if (loginResponse != null && loginResponse.user != null) {
            // Utiliser user_* au lieu de employee_* (migration)
            authEditor.putInt("user_id", loginResponse.user.getId());
            authEditor.putString("user_firstname", loginResponse.user.getFirstname());
            authEditor.putString("user_lastname", loginResponse.user.getLastname());
            authEditor.putString("user_email", loginResponse.user.getEmail());
            authEditor.putInt("user_type", loginResponse.user.getType());

            // ✅ FIX: Construire et sauvegarder le nom complet pour l'affichage offline
            String fullName = "";
            if (loginResponse.user.getFirstname() != null) {
                fullName = loginResponse.user.getFirstname();
            }
            if (loginResponse.user.getLastname() != null) {
                if (!fullName.isEmpty()) fullName += " ";
                fullName += loginResponse.user.getLastname();
            }
            if (fullName.isEmpty()) {
                fullName = "Utilisateur";
            }
            authEditor.putString("user_name", fullName);

            Log.d(TAG, "Informations utilisateur sauvegardées: " + loginResponse.user.getId() + " - " + fullName);
        }

        // ✅ FIX #1: Sauvegarder le hash du mot de passe pour le login offline
        authEditor.putString("offline_email", email);
        if (pendingPassword != null && !pendingPassword.isEmpty()) {
            String passwordHash = hashPassword(pendingPassword);
            if (passwordHash != null) {
                authEditor.putString("offline_password_hash", passwordHash);
                Log.d(TAG, "✅ Hash du mot de passe sauvegardé pour login offline");
            } else {
                Log.e(TAG, "❌ Erreur lors du hash du mot de passe");
            }
            // Effacer le mot de passe temporaire pour sécurité
            pendingPassword = null;
        } else {
            Log.w(TAG, "⚠️ Mot de passe non disponible pour le hash offline");
        }

        // ✅ FIX #2: Utiliser la bonne clé de préférence (offline_login_enabled)
        authEditor.putBoolean("offline_login_enabled", true);

        authEditor.apply();

        Log.d(TAG, "Authentification initiale marquée comme réussie (offline login activé)");
    }
    
    // ==================== UTILITAIRES ====================
    
    /**
     * Hash le mot de passe pour le stockage offline sécurisé
     * Utilise SHA-256
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            Log.e(TAG, "Erreur hash mot de passe", e);
            return null;
        }
    }

    /**
     * Extrait le token de la réponse de connexion
     */
    private String extractTokenFromResponse(ApiService.LoginResponse response) {
        try {
            if (response != null && response.success) {
                return response.token;
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Erreur extraction token", e);
            return null;
        }
    }
    
    /**
     * Réinitialise l'authentification initiale (pour les tests)
     */
    public void resetInitialAuth() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Authentification initiale réinitialisée");
    }
    
    /**
     * Obtient les informations de l'authentification initiale
     */
    public InitialAuthInfo getInitialAuthInfo() {
        return new InitialAuthInfo(
            prefs.getBoolean(KEY_HAS_INITIAL_AUTH, false),
            prefs.getLong(KEY_AUTH_DATE, 0),
            prefs.getString(KEY_USER_EMAIL, ""),
            prefs.getLong(KEY_DATA_CACHE_DATE, 0),
            prefs.getInt(KEY_PROJECTS_COUNT, 0),
            prefs.getInt(KEY_WORK_TYPES_COUNT, 0)
        );
    }
    
    // ==================== INTERFACES DE CALLBACK ====================
    
    public interface InitialAuthCallback {
        void onInitialAuthSuccess(int projectsCount, int workTypesCount);
        void onInitialAuthError(String error);
        void onProgress(String message);  // ✅ AJOUT pour afficher la progression
    }
    
    private interface CredentialsTestCallback {
        void onCredentialsValid(String token, ApiService.LoginResponse loginResponse);
        void onCredentialsInvalid(String error);
    }
    
    private interface ReferenceDataCallback {
        void onDataDownloaded(int projectsCount, int workTypesCount);
        void onDataError(String error);
    }
    
    // ==================== CLASSE D'INFORMATIONS ====================
    
    public static class InitialAuthInfo {
        public final boolean hasAuth;
        public final long authDate;
        public final String userEmail;
        public final long cacheDate;
        public final int projectsCount;
        public final int workTypesCount;
        
        public InitialAuthInfo(boolean hasAuth, long authDate, String userEmail, 
                              long cacheDate, int projectsCount, int workTypesCount) {
            this.hasAuth = hasAuth;
            this.authDate = authDate;
            this.userEmail = userEmail;
            this.cacheDate = cacheDate;
            this.projectsCount = projectsCount;
            this.workTypesCount = workTypesCount;
        }
        
        public boolean isDataFresh() {
            long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
            return cacheDate > sevenDaysAgo;
        }
        
        public String getAuthDateString() {
            if (authDate == 0) return "Jamais";
            return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
                    .format(new java.util.Date(authDate));
        }
    }
}
