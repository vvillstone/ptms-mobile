package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ptms.mobile.R;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.models.Employee;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.WorkType;
import com.ptms.mobile.utils.ApiConfig;
import com.ptms.mobile.utils.SessionManager;
import com.ptms.mobile.utils.SettingsManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Activité de connexion des employés
 */
public class AuthenticationActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private SharedPreferences prefs;
    private ApiService apiService;
    private android.widget.ImageButton btnSettings;
    private SettingsManager settingsManager;
    private ApiClient apiClient;

    // Indicateur offline (NOUVEAU)
    private android.widget.LinearLayout offlineStatusContainer;
    private android.widget.TextView offlineStatusIcon;
    private android.widget.TextView tvOfflineStatusTitle;
    private android.widget.TextView tvOfflineStatusMessage;

    // Credentials pour fallback offline
    private String currentEmail;
    private String currentPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        // Initialisation
        prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
        settingsManager = new SettingsManager(this);
        apiClient = ApiClient.getInstance(this);
        initViews();
        setupApiService();
        setupListeners();

        // Afficher le statut offline
        updateOfflineStatusIndicator();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
        btnSettings = findViewById(R.id.btn_settings);

        // Indicateur offline (NOUVEAU)
        offlineStatusContainer = findViewById(R.id.offline_status_container);
        offlineStatusIcon = findViewById(R.id.offline_status_icon);
        tvOfflineStatusTitle = findViewById(R.id.tv_offline_status_title);
        tvOfflineStatusMessage = findViewById(R.id.tv_offline_status_message);
    }

    private void setupApiService() {
        Log.d("LOGIN", "Configuration du service API");
        Log.d("LOGIN", "URL du serveur: " + settingsManager.getServerUrl());
        apiService = apiClient.getApiService();
        Log.d("LOGIN", "Service API configuré");
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });
        
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d("LOGIN", "=== DÉBUT TENTATIVE DE CONNEXION ===");
        Log.d("LOGIN", "Email: " + email);
        Log.d("LOGIN", "Password length: " + password.length() + " caractères");
        Log.d("LOGIN", "URL du serveur: " + settingsManager.getServerUrl());
        Log.d("LOGIN", "Ignorer SSL: " + settingsManager.isIgnoreSsl());
        Log.d("LOGIN", "Timeout: " + settingsManager.getTimeout() + " secondes");

        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        // Plus de validation stricte de l'email car on accepte aussi username
        // if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        //     Toast.makeText(this, "Format d'email invalide", Toast.LENGTH_SHORT).show();
        //     return;
        // }

        // Afficher le loading
        setLoading(true);

        // ============================================
        // NOUVELLE LOGIQUE: Détection intelligente
        // ============================================

        // 1. Vérifier d'abord si on a une connexion réseau basique
        boolean hasNetwork = com.ptms.mobile.utils.NetworkUtils.isOnline(this);
        Log.d("LOGIN", "État réseau: " + (hasNetwork ? "Connecté" : "Hors ligne"));

        if (!hasNetwork) {
            // PAS DE RÉSEAU DU TOUT → Mode offline direct
            Log.d("LOGIN", "❌ Aucun réseau détecté - Tentative login offline immédiate");
            setLoading(false);

            if (performOfflineLogin(email, password)) {
                Toast.makeText(this,
                    "✅ Mode Hors Ligne Activé\n\n" +
                    "Vous travaillez avec vos données locales.\n" +
                    "Vos saisies seront synchronisées à la prochaine connexion.\n\n" +
                    "💡 Connectez-vous à Internet quand vous voulez synchroniser.",
                    Toast.LENGTH_LONG).show();
                Intent dashboardIntent = new Intent(AuthenticationActivity.this, HomeActivity.class);
                startActivity(dashboardIntent);
                finish();
            } else {
                Toast.makeText(this,
                    "❌ Connexion Hors Ligne Impossible\n\n" +
                    "📵 Aucune connexion réseau détectée.\n\n" +
                    "⚠️ Vous devez vous connecter UNE PREMIÈRE FOIS en ligne pour :\n" +
                    "  • Télécharger vos projets\n" +
                    "  • Télécharger les types de travail\n" +
                    "  • Activer le mode hors ligne\n\n" +
                    "💡 Connectez-vous à Internet et réessayez.",
                    Toast.LENGTH_LONG).show();
            }
            return;
        }

        // 2. On a du réseau → Vérifier si le SERVEUR est accessible
        Log.d("LOGIN", "✓ Réseau détecté - Vérification du serveur PTMS...");
        com.ptms.mobile.utils.ServerHealthCheck.quickPing(this, (status, responseTime, message) -> {
            if (status == com.ptms.mobile.utils.ServerHealthCheck.ServerStatus.ONLINE ||
                status == com.ptms.mobile.utils.ServerHealthCheck.ServerStatus.SLOW) {
                // Serveur accessible - Login online normal
                Log.d("LOGIN", "✅ Serveur PTMS accessible (" + responseTime + "ms) - Login online");
                performOnlineLogin(email, password);
            } else {
                // Réseau OK mais serveur inaccessible → Fallback offline
                setLoading(false);
                Log.d("LOGIN", "⚠️ Réseau OK mais serveur PTMS inaccessible - Fallback offline");
                Log.d("LOGIN", "Raison: " + message);

                if (performOfflineLogin(email, password)) {
                    Toast.makeText(this,
                        "✅ Mode Hors Ligne Activé\n\n" +
                        "Vous travaillez avec vos données locales.\n" +
                        "Vos saisies seront synchronisées à la prochaine connexion.\n\n" +
                        "💡 Utilisez le bouton 'Reconnecter' pour synchroniser quand vous le souhaitez.",
                        Toast.LENGTH_LONG).show();
                    Intent dashboardIntent = new Intent(AuthenticationActivity.this, HomeActivity.class);
                    startActivity(dashboardIntent);
                    finish();
                } else {
                    Toast.makeText(this,
                        "❌ Connexion Impossible\n\n" +
                        "⚠️ " + message + "\n\n" +
                        "Vérifiez vos identifiants ou l'URL du serveur dans les paramètres.\n\n" +
                        "💡 Pour vous connecter hors ligne, vous devez d'abord vous connecter une fois en ligne.",
                        Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Effectue le login online avec l'API
     */
    private void performOnlineLogin(String email, String password) {
        // Sauvegarder les credentials pour fallback offline possible
        this.currentEmail = email;
        this.currentPassword = password;

        // Appel API
        ApiService.LoginRequest request = new ApiService.LoginRequest(email, password);
        String baseUrl = ApiClient.getInstance(this).getBaseUrl();
        Log.d("LOGIN", "URL de base: " + baseUrl);
        Log.d("LOGIN", "URL complète: " + baseUrl + "login.php");
        Log.d("LOGIN", "Envoi de la requête de connexion...");
        Call<ApiService.LoginResponse> call = apiService.login(request);

        call.enqueue(new Callback<ApiService.LoginResponse>() {
            @Override
            public void onResponse(Call<ApiService.LoginResponse> call, Response<ApiService.LoginResponse> response) {
                setLoading(false);
                Log.d("LOGIN", "Réponse reçue: " + response.code() + " " + response.message());
                
                try {
                    // Essayer de lire le body brut pour debug
                    String responseBody = "";
                    if (response.errorBody() != null) {
                        responseBody = response.errorBody().string();
                    } else if (response.body() != null) {
                        responseBody = "Body présent mais pas d'erreur";
                    } else {
                        responseBody = "Pas de body";
                    }
                    Log.d("LOGIN", "Body de réponse: " + responseBody);
                    
                    // Vérifier si c'est du HTML au lieu de JSON
                    if (responseBody.contains("<html") || responseBody.contains("<!DOCTYPE") || 
                        responseBody.contains("<title>") || responseBody.contains("</html>")) {
                        Log.e("LOGIN", "Le serveur retourne du HTML au lieu de JSON!");
                        Toast.makeText(AuthenticationActivity.this, "Serveur retourne HTML - Vérifiez l'URL", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception e) {
                    Log.d("LOGIN", "Impossible de lire le body: " + e.getMessage());
                }
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.LoginResponse loginResponse = response.body();
                        
                        if (loginResponse == null) {
                            Log.e("LOGIN", "LoginResponse est null!");
                            Toast.makeText(AuthenticationActivity.this, "Réponse serveur invalide", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        Log.d("LOGIN", "Réponse login: success=" + loginResponse.success);
                        Log.d("LOGIN", "Token reçu: " + (loginResponse.token != null ? loginResponse.token.substring(0, Math.min(20, loginResponse.token.length())) + "..." : "null"));
                        Log.d("LOGIN", "User reçu: " + (loginResponse.user != null ? "oui" : "null"));
                        
                        if (loginResponse.user != null) {
                            try {
                                Log.d("LOGIN", "User ID: " + loginResponse.user.getId());
                                Log.d("LOGIN", "User Email: " + loginResponse.user.getEmail());
                                Log.d("LOGIN", "User Name: " + loginResponse.user.getFullName());
                            } catch (Exception e) {
                                Log.e("LOGIN", "Erreur lors de l'accès aux données utilisateur", e);
                            }
                        }
                        
                        if (loginResponse.success) {
                            Log.d("LOGIN", "Connexion réussie - Redirection vers dashboard...");

                            // Vérifier que nous avons les données nécessaires
                            if (loginResponse.token == null) {
                                Log.e("LOGIN", "Token manquant dans la réponse!");
                                Toast.makeText(AuthenticationActivity.this, "Token manquant", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (loginResponse.user == null) {
                                Log.e("LOGIN", "Données utilisateur manquantes!");
                                Toast.makeText(AuthenticationActivity.this, "Données utilisateur manquantes", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            try {
                                // Sauvegarder le token et les données utilisateur de base
                                saveUserData(loginResponse.token, loginResponse.user);

                                // Sauvegarder les credentials pour login hors ligne
                                saveCredentialsForOffline(email, password);

                                // ✅ NOUVEAU: Rediriger vers LoadingActivity qui gère tout le téléchargement
                                Log.d("LOGIN", "Redirection vers LoadingActivity...");
                                Intent intent = new Intent(AuthenticationActivity.this, AppLoadingActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {
                                Log.e("LOGIN", "Erreur lors de la redirection", e);
                                Toast.makeText(AuthenticationActivity.this, "Erreur de redirection: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.d("LOGIN", "Connexion échouée: " + loginResponse.message);
                            Toast.makeText(AuthenticationActivity.this, loginResponse.message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("LOGIN", "Réponse non réussie ou body null");
                        Toast.makeText(AuthenticationActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("LOGIN", "Exception dans onResponse", e);
                    Toast.makeText(AuthenticationActivity.this, "Erreur inattendue: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.LoginResponse> call, Throwable t) {
                setLoading(false);
                Log.e("LOGIN", "=== ÉCHEC DE LA CONNEXION ===", t);
                Log.e("LOGIN", "Type d'erreur: " + t.getClass().getName());
                Log.e("LOGIN", "Message d'erreur: " + t.getMessage());

                // ✅ NOUVEAU: Vérifier si on peut faire un fallback offline automatique
                boolean canFallbackOffline = false;
                String fallbackReason = "";

                if (t.getMessage() != null) {
                    if (t.getMessage().contains("timeout")) {
                        canFallbackOffline = true;
                        fallbackReason = "Timeout - Serveur trop lent";
                    } else if (t.getMessage().contains("UnknownHostException") || t.getMessage().contains("failed to connect")) {
                        canFallbackOffline = true;
                        fallbackReason = "Serveur non accessible";
                    } else if (t.getMessage().contains("Connection refused")) {
                        canFallbackOffline = true;
                        fallbackReason = "Connexion refusée par le serveur";
                    }
                }

                // ✅ TENTATIVE DE FALLBACK OFFLINE
                if (canFallbackOffline && currentEmail != null && currentPassword != null) {
                    Log.d("LOGIN", "⚠️ Échec login online (" + fallbackReason + ") - Tentative fallback offline...");

                    if (performOfflineLogin(currentEmail, currentPassword)) {
                        // ✅ Message POSITIF - Mode offline est une fonctionnalité normale
                        Toast.makeText(AuthenticationActivity.this,
                            "✅ Mode Hors Ligne Activé\n\n" +
                            "Vous travaillez avec vos données locales.\n" +
                            "Vos saisies seront synchronisées à la prochaine connexion.\n\n" +
                            "💡 Utilisez le bouton 'Reconnecter' pour synchroniser quand vous le souhaitez.",
                            Toast.LENGTH_LONG).show();
                        Intent dashboardIntent = new Intent(AuthenticationActivity.this, HomeActivity.class);
                        startActivity(dashboardIntent);
                        finish();
                        return; // Important: ne pas afficher l'erreur si le fallback a réussi
                    } else {
                        Log.d("LOGIN", "❌ Fallback offline impossible - Affichage erreur");
                    }
                }

                // ❌ AFFICHAGE ERREUR (si pas de fallback ou fallback échoué)
                String errorMessage = "Erreur de connexion";
                String debugInfo = "";

                if (t.getMessage() != null) {
                    if (t.getMessage().contains("Expected BEGIN_OBJECT but was")) {
                        errorMessage = "❌ Serveur retourne HTML au lieu de JSON";
                        debugInfo = "\n\nVérifiez l'URL de l'API dans les paramètres";
                    } else if (t.getMessage().contains("JsonReader.setLenient")) {
                        errorMessage = "❌ Format JSON invalide";
                        debugInfo = "\n\nLe serveur ne répond pas correctement";
                    } else if (t.getMessage().contains("SSL") || t.getMessage().contains("certificate")) {
                        errorMessage = "❌ Erreur SSL/Certificat";
                        debugInfo = "\n\nActivez 'Ignorer SSL' dans les paramètres";
                    } else if (t.getMessage().contains("timeout")) {
                        errorMessage = "❌ Timeout - Serveur trop lent";
                        debugInfo = "\n\nEssayez de vous connecter hors ligne si vous avez déjà téléchargé les données";
                    } else if (t.getMessage().contains("UnknownHostException") || t.getMessage().contains("failed to connect")) {
                        errorMessage = "❌ Serveur non accessible";
                        debugInfo = "\n\nURL: " + settingsManager.getServerUrl() + "\n\nVérifiez l'URL dans les paramètres";
                    } else if (t.getMessage().contains("Connection refused")) {
                        errorMessage = "❌ Connexion refusée";
                        debugInfo = "\n\nLe serveur refuse la connexion\nVérifiez que le serveur est démarré";
                    } else {
                        errorMessage = "❌ Erreur: " + t.getMessage();
                        debugInfo = "\n\nConsultez les logs pour plus de détails";
                    }
                }

                Toast.makeText(AuthenticationActivity.this, errorMessage + debugInfo, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserData(String token, Employee employee) {
        try {
            Log.d("LOGIN", "Sauvegarde des données utilisateur...");
            Log.d("LOGIN", "Token à sauvegarder: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));

            if (employee == null) {
                Log.e("LOGIN", "Employee est null - impossible de sauvegarder");
                return;
            }

            Log.d("LOGIN", "Employee ID: " + employee.getId());

            // Vérifier que les méthodes ne retournent pas null
            String fullName = employee.getFullName();
            String email = employee.getEmail();

            if (fullName == null) {
                fullName = "Utilisateur";
            }
            if (email == null) {
                email = "";
            }

            // ========================================
            // SAUVEGARDE UNIFIÉE DANS SESSIONMANAGER
            // ========================================
            SessionManager sessionManager = new SessionManager(this);
            sessionManager.createLoginSession(token, employee.getId(), email, fullName);
            Log.d("LOGIN", "✓ Token sauvegardé dans SessionManager (PTMSSession)");

            // ========================================
            // SAUVEGARDE COMPLÈTE DANS ptms_prefs POUR COMPATIBILITÉ OFFLINE
            // ========================================
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("auth_token", token);
            editor.putInt("user_id", employee.getId());
            editor.putString("user_name", fullName);
            editor.putString("user_email", email);
            editor.putInt("user_type", employee.getType());

            // ✅ CORRECTION: Sauvegarder TOUTES les données du profil pour l'affichage offline
            editor.putString("user_department", employee.getDepartment() != null ? employee.getDepartment() : "");
            editor.putString("user_position", employee.getPosition() != null ? employee.getPosition() : "");
            editor.putString("user_employee_status", employee.getEmployeeStatusText() != null ? employee.getEmployeeStatusText() : "");
            editor.putBoolean("user_is_active", employee.isActive());

            // Sauvegarder également dans les anciennes clés pour compatibilité
            editor.putInt("employee_id", employee.getId());
            editor.putString("employee_name", fullName);
            editor.putString("employee_email", email);

            // Utiliser commit() pour s'assurer que les données sont sauvegardées IMMÉDIATEMENT
            boolean success = editor.commit();
            Log.d("LOGIN", "✓ Données utilisateur complètes sauvegardées: " + (success ? "réussi" : "échoué"));

            // Vérifier que le token est bien sauvegardé dans les DEUX endroits
            String savedTokenPrefs = prefs.getString("auth_token", null);
            String savedTokenSession = sessionManager.getAuthToken();
            Log.d("LOGIN", "Vérification token:");
            Log.d("LOGIN", "  - ptms_prefs: " + (savedTokenPrefs != null ? "✓ présent" : "✗ absent"));
            Log.d("LOGIN", "  - PTMSSession: " + (savedTokenSession != null ? "✓ présent" : "✗ absent"));

        } catch (Exception e) {
            Log.e("LOGIN", "Erreur lors de la sauvegarde des données", e);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }
    
    /**
     * Sauvegarde les credentials pour permettre le login hors ligne
     * IMPORTANT : Utilise un hash pour ne pas stocker le mot de passe en clair
     */
    private void saveCredentialsForOffline(String email, String password) {
        try {
            Log.d("LOGIN", "Sauvegarde des credentials pour login hors ligne");

            // Hash du mot de passe pour sécurité (utilise SHA-256)
            String passwordHash = hashPassword(password);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("offline_email", email);
            editor.putString("offline_password_hash", passwordHash);
            editor.putBoolean("offline_login_enabled", true);
            boolean success = editor.commit();

            Log.d("LOGIN", "Credentials offline sauvegardés: " + (success ? "réussie" : "échouée"));
        } catch (Exception e) {
            Log.e("LOGIN", "Erreur sauvegarde credentials offline", e);
        }
    }

    /**
     * Tente un login hors ligne avec les credentials en cache
     * AMÉLIORATION: Vérifie aussi l'authentification initiale
     */
    private boolean performOfflineLogin(String email, String password) {
        try {
            Log.d("LOGIN", "🔄 Tentative de login hors ligne");

            // ========================================
            // VÉRIFICATION AMÉLIORÉE: Auth Initiale + Offline Enabled
            // ========================================
            boolean offlineEnabled = prefs.getBoolean("offline_login_enabled", false);
            com.ptms.mobile.auth.InitialAuthManager authManager =
                new com.ptms.mobile.auth.InitialAuthManager(this);
            boolean hasInitialAuth = authManager.hasInitialAuthentication();

            Log.d("LOGIN", "État offline: enabled=" + offlineEnabled + ", hasInitialAuth=" + hasInitialAuth);

            // Si AUCUNE authentification initiale, bloquer
            if (!hasInitialAuth && !offlineEnabled) {
                Log.d("LOGIN", "❌ Login offline impossible - Aucune authentification initiale");
                // ✅ FIX: Ne pas utiliser runOnUiThread si déjà sur UI thread
                // Cela évite les problèmes potentiels de threading
                Toast.makeText(this,
                    "⚠️ AUTHENTIFICATION INITIALE REQUISE\n\n" +
                    "Vous devez vous connecter UNE FOIS en ligne pour:\n" +
                    "• Télécharger les projets\n" +
                    "• Télécharger les types de travail\n" +
                    "• Activer le mode hors ligne\n\n" +
                    "Connectez-vous à Internet et réessayez.",
                    Toast.LENGTH_LONG).show();
                return false;
            }

            // Si auth initiale OK mais offline pas activé → activer automatiquement
            if (hasInitialAuth && !offlineEnabled) {
                prefs.edit().putBoolean("offline_login_enabled", true).commit();
                Log.d("LOGIN", "✅ Mode offline activé automatiquement (auth initiale validée)");
            }

            // Récupérer les credentials sauvegardés
            String savedEmail = prefs.getString("offline_email", null);
            String savedPasswordHash = prefs.getString("offline_password_hash", null);

            if (savedEmail == null || savedPasswordHash == null) {
                Log.d("LOGIN", "❌ Pas de credentials offline sauvegardés");
                return false;
            }

            // Vérifier que l'email correspond
            if (!email.equals(savedEmail)) {
                Log.d("LOGIN", "Email ne correspond pas: " + email + " vs " + savedEmail);
                return false;
            }

            // Hash du mot de passe saisi
            String enteredPasswordHash = hashPassword(password);

            // Vérifier que le hash correspond
            if (!enteredPasswordHash.equals(savedPasswordHash)) {
                Log.d("LOGIN", "Mot de passe incorrect");
                return false;
            }

            // Vérifier que nous avons les données utilisateur en cache
            // MIGRATION: employee_id → user_id, employee_name → user_name
            int userId = prefs.getInt("user_id", -1);
            String userName = prefs.getString("user_name", null);
            int userType = prefs.getInt("user_type", 4);  // Par défaut: EMPLOYEE

            // Compatibilité: Essayer les anciennes clés si les nouvelles sont absentes
            if (userId == -1) {
                userId = prefs.getInt("employee_id", -1);
                Log.d("LOGIN", "⚠️ Fallback sur ancienne clé 'employee_id': " + userId);
            }
            if (userName == null) {
                userName = prefs.getString("employee_name", null);
                Log.d("LOGIN", "⚠️ Fallback sur ancienne clé 'employee_name': " + userName);
            }

            // ⚠️ CORRECTION: Ne plus bloquer si les données sont absentes
            // Les credentials offline suffisent pour valider l'identité
            // Les données complètes seront chargées depuis la base locale ou synchronisées
            if (userId == -1 || userName == null) {
                Log.d("LOGIN", "⚠️ Données utilisateur partielles - login offline autorisé mais données incomplètes");
                Log.d("LOGIN", "Les données complètes seront chargées depuis la base de données offline");
                // Ne pas bloquer - continuer le login offline
            } else {
                Log.d("LOGIN", "✅ Données utilisateur complètes: ID=" + userId + ", Name=" + userName + ", Type=" + userType);
            }

            // Recréer la session même avec données partielles
            SessionManager sessionManager = new SessionManager(this);
            String token = prefs.getString("auth_token", "offline_token");

            // Si on a les données complètes, les utiliser
            if (userId != -1 && userName != null) {
                sessionManager.createLoginSession(token, userId, savedEmail, userName);
            } else {
                // Sinon créer une session minimale - les données seront chargées depuis la DB offline
                sessionManager.createLoginSession(token, 0, savedEmail, "Utilisateur");
            }

            Log.d("LOGIN", "✅ Login hors ligne réussi pour: " + savedEmail);
            return true;

        } catch (Exception e) {
            Log.e("LOGIN", "Erreur lors du login hors ligne", e);
            return false;
        }
    }

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
            Log.e("LOGIN", "Erreur hash password", e);
            return "";
        }
    }

    /**
     * ✅ NOUVEAU: Télécharge les données de référence après un login réussi
     * pour éviter le crash au prochain chargement du dashboard
     */
    /**
     * Charge le profil complet de l'utilisateur après un login réussi
     * Cela garantit que toutes les données (department, position, employeeStatus) sont en cache
     */
    private void loadFullProfileAfterLogin(String token) {
        Log.d("LOGIN", "Appel API /employee/profile...");

        Call<Employee> call = apiService.getProfile(token);
        call.enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Employee fullEmployee = response.body();
                    Log.d("LOGIN", "✅ Profil complet chargé: " + fullEmployee.getFullName());

                    // Sauvegarder le profil complet dans le cache
                    saveUserData(token, fullEmployee);

                    // ✅ CORRIGÉ: Télécharger les données ET attendre la fin AVANT d'ouvrir le dashboard
                    Log.d("LOGIN", "Téléchargement des données de référence...");
                    downloadReferenceDataAfterLogin(token, () -> {
                        // Ce callback est appelé APRÈS que les données soient téléchargées
                        Log.d("LOGIN", "✅ Données prêtes - Ouverture du dashboard...");
                        runOnUiThread(() -> {
                            Intent dashboardIntent = new Intent(AuthenticationActivity.this, HomeActivity.class);
                            startActivity(dashboardIntent);
                            finish();
                        });
                    });
                } else {
                    Log.e("LOGIN", "Erreur chargement profil: " + response.code());
                    // Fallback: continuer quand même avec les données de base
                    downloadReferenceDataAfterLogin(token, () -> {
                        runOnUiThread(() -> {
                            Intent dashboardIntent = new Intent(AuthenticationActivity.this, HomeActivity.class);
                            startActivity(dashboardIntent);
                            finish();
                        });
                    });
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                Log.e("LOGIN", "Échec chargement profil", t);
                // Fallback: continuer quand même avec les données de base
                downloadReferenceDataAfterLogin(token, () -> {
                    runOnUiThread(() -> {
                        Intent dashboardIntent = new Intent(AuthenticationActivity.this, HomeActivity.class);
                        startActivity(dashboardIntent);
                        finish();
                    });
                });
            }
        });
    }

    private void downloadReferenceDataAfterLogin(String token, Runnable callback) {
        new Thread(() -> {
            try {
                Log.d("LOGIN", "Début téléchargement données de référence...");

                // Télécharger les projets
                retrofit2.Response<ApiService.ProjectsResponse> projectsResponse =
                    apiService.getProjects(token).execute();

                if (projectsResponse.isSuccessful() && projectsResponse.body() != null
                    && projectsResponse.body().success && projectsResponse.body().projects != null) {

                    List<Project> projects = projectsResponse.body().projects;
                    Log.d("LOGIN", "✅ " + projects.size() + " projets téléchargés");

                    // Sauvegarder dans SQLite
                    com.ptms.mobile.database.OfflineDatabaseHelper dbHelper =
                        new com.ptms.mobile.database.OfflineDatabaseHelper(this);
                    dbHelper.clearProjects(); // Vider les anciens
                    for (Project project : projects) {
                        dbHelper.insertProject(project);
                    }
                    Log.d("LOGIN", "✅ Projets sauvegardés en local");
                }

                // Télécharger les types de travail
                retrofit2.Response<List<WorkType>> workTypesResponse =
                    apiService.getWorkTypes(token).execute();

                if (workTypesResponse.isSuccessful() && workTypesResponse.body() != null) {
                    List<WorkType> workTypes = workTypesResponse.body();
                    Log.d("LOGIN", "✅ " + workTypes.size() + " types de travail téléchargés");

                    // Sauvegarder dans SQLite
                    com.ptms.mobile.database.OfflineDatabaseHelper dbHelper =
                        new com.ptms.mobile.database.OfflineDatabaseHelper(this);
                    dbHelper.clearWorkTypes(); // Vider les anciens
                    for (WorkType workType : workTypes) {
                        dbHelper.insertWorkType(workType);
                    }
                    Log.d("LOGIN", "✅ Types de travail sauvegardés en local");
                }

                Log.d("LOGIN", "✅ Téléchargement données de référence terminé");

            } catch (Exception e) {
                Log.e("LOGIN", "Erreur téléchargement données de référence", e);
                // Ne pas bloquer le login si le téléchargement échoue
            } finally {
                // CRITIQUE: Exécuter le callback APRÈS le téléchargement (ou en cas d'erreur)
                if (callback != null) {
                    callback.run();
                }
            }
        }).start();
    }

    private void openSettings() {
        Intent intent = new Intent(this, AppSettingsActivity.class);
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Reconfigurer l'API avec les nouveaux paramètres
            apiClient.refreshConfiguration();
            setupApiService();
            Toast.makeText(this, "Paramètres mis à jour", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Met à jour l'indicateur visuel du statut offline
     * NOUVEAU: Affiche l'état du mode offline avec code couleur
     */
    private void updateOfflineStatusIndicator() {
        try {
            com.ptms.mobile.auth.InitialAuthManager authManager =
                new com.ptms.mobile.auth.InitialAuthManager(this);

            boolean hasInitialAuth = authManager.hasInitialAuthentication();
            boolean hasValidCache = authManager.hasValidDataCache();
            boolean canOffline = authManager.canUseOffline();

            if (offlineStatusContainer == null) {
                Log.w("LOGIN", "Offline status container not found");
                return;
            }

            if (canOffline && hasValidCache) {
                // ✅ MODE OFFLINE DISPONIBLE - VERT
                offlineStatusIcon.setText("✅");
                tvOfflineStatusTitle.setText("Mode Offline Disponible");
                tvOfflineStatusTitle.setTextColor(0xFF4CAF50); // Vert

                com.ptms.mobile.auth.InitialAuthManager.InitialAuthInfo info = authManager.getInitialAuthInfo();
                tvOfflineStatusMessage.setText(
                    "Projets: " + info.projectsCount + " | Types: " + info.workTypesCount + "\n" +
                    "Dernière sync: " + info.getAuthDateString()
                );
                tvOfflineStatusMessage.setTextColor(0xFF4CAF50); // Vert

                offlineStatusContainer.setBackgroundColor(0xFFE8F5E9); // Fond vert clair

            } else if (hasInitialAuth && !hasValidCache) {
                // ⚠️ DONNÉES EXPIRÉES - ORANGE
                offlineStatusIcon.setText("⚠️");
                tvOfflineStatusTitle.setText("Mode Offline Disponible");
                tvOfflineStatusTitle.setTextColor(0xFFFF9800); // Orange

                tvOfflineStatusMessage.setText(
                    "⚠️ Données anciennes - Synchronisation recommandée\n" +
                    "Connectez-vous en ligne pour mettre à jour"
                );
                tvOfflineStatusMessage.setTextColor(0xFFFF9800); // Orange

                offlineStatusContainer.setBackgroundColor(0xFFFFF3E0); // Fond orange clair

            } else {
                // ❌ MODE OFFLINE NON CONFIGURÉ - ROUGE
                offlineStatusIcon.setText("❌");
                tvOfflineStatusTitle.setText("Mode Offline Non Configuré");
                tvOfflineStatusTitle.setTextColor(0xFFF44336); // Rouge

                tvOfflineStatusMessage.setText(
                    "Vous devez vous connecter UNE FOIS en ligne pour:\n" +
                    "• Télécharger les projets\n" +
                    "• Télécharger les types de travail\n" +
                    "• Activer le mode hors ligne"
                );
                tvOfflineStatusMessage.setTextColor(0xFFF44336); // Rouge

                offlineStatusContainer.setBackgroundColor(0xFFFFEBEE); // Fond rouge clair
            }

            Log.d("LOGIN", "Indicateur offline mis à jour: canOffline=" + canOffline +
                  ", hasCache=" + hasValidCache);

        } catch (Exception e) {
            Log.e("LOGIN", "Erreur mise à jour indicateur offline", e);
            // En cas d'erreur, masquer l'indicateur
            if (offlineStatusContainer != null) {
                offlineStatusContainer.setVisibility(android.view.View.GONE);
            }
        }
    }
}