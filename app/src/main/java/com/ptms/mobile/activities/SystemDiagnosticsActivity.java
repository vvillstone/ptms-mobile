package com.ptms.mobile.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ptms.mobile.R;
import com.ptms.mobile.utils.RoleCompatibilityTester;
import com.ptms.mobile.utils.SettingsManager;

/**
 * Activité de diagnostique complète
 * Regroupe tous les tests de l'application PTMS
 */
public class SystemDiagnosticsActivity extends AppCompatActivity {

    private static final String TAG = "DiagnosticActivity";
    private static final String PREFS_NAME = "ptms_prefs";
    private static final String PREF_TOKEN = "auth_token";

    private TextView testResultsTextView;
    private ScrollView scrollView;
    private Button btnTestApi;
    private Button btnTestConnection;
    private Button btnTestBaseUrl;
    private Button btnTestToken;
    private Button btnTestRoles;
    private Button btnTestChatConversations;
    private Button btnTestChatUsers;
    private Button btnTestChatSendMessage;
    private Button btnTestChatFull;
    private Button btnClearResults;
    private Button btnTestOfflineMode;
    private Button btnTestNotes;

    private SettingsManager settingsManager;
    private com.ptms.mobile.api.ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_diagnostics);

        settingsManager = new SettingsManager(this);
        apiService = com.ptms.mobile.api.ApiClient.getInstance(this).getApiService();

        setupToolbar();
        initViews();
        setupListeners();
        displayWelcomeMessage();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Diagnostique PTMS");
        }
    }

    private void initViews() {
        testResultsTextView = findViewById(R.id.test_results_text);
        scrollView = findViewById(R.id.scroll_view);
        btnTestApi = findViewById(R.id.btn_test_api);
        btnTestConnection = findViewById(R.id.btn_test_connection);
        btnTestBaseUrl = findViewById(R.id.btn_test_base_url);
        btnTestToken = findViewById(R.id.btn_test_token);
        btnTestRoles = findViewById(R.id.btn_test_roles);
        btnTestChatConversations = findViewById(R.id.btn_test_chat_conversations);
        btnTestChatUsers = findViewById(R.id.btn_test_chat_users);
        btnTestChatSendMessage = findViewById(R.id.btn_test_chat_send_message);
        btnTestChatFull = findViewById(R.id.btn_test_chat_full);
        btnClearResults = findViewById(R.id.btn_clear_results);
        btnTestOfflineMode = findViewById(R.id.btn_test_offline_mode);
        btnTestNotes = findViewById(R.id.btn_test_notes);
    }

    private void setupListeners() {
        btnTestApi.setOnClickListener(v -> testApi());
        btnTestConnection.setOnClickListener(v -> testConnection());
        btnTestBaseUrl.setOnClickListener(v -> testBaseUrl());
        btnTestToken.setOnClickListener(v -> testToken());
        btnTestRoles.setOnClickListener(v -> testRoles());
        btnTestChatConversations.setOnClickListener(v -> testChatConversations());
        btnTestChatUsers.setOnClickListener(v -> testChatUsers());
        btnTestChatSendMessage.setOnClickListener(v -> testChatSendMessage());
        btnTestChatFull.setOnClickListener(v -> testChatFull());
        btnClearResults.setOnClickListener(v -> clearResults());
        btnTestOfflineMode.setOnClickListener(v -> testOfflineMode());
        btnTestNotes.setOnClickListener(v -> testNotesDiagnostic());
    }

    private void displayWelcomeMessage() {
        appendResult("═══════════════════════════════════════════");
        appendResult("       DIAGNOSTIQUE PTMS v2.1");
        appendResult("═══════════════════════════════════════════");
        appendResult("");
        appendResult("Cette page regroupe tous les tests de diagnostic");
        appendResult("pour vérifier la communication entre l'application");
        appendResult("Android et le serveur PTMS.");
        appendResult("");
        appendResult("Cliquez sur un bouton pour lancer un test.");
        appendResult("");
    }

    /**
     * Test complet de l'API
     */
    private void testApi() {
        disableAllButtons();
        appendResult("═══════════════════════════════════════════");
        appendResult("🔍 TEST COMPLET DE L'API");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        String serverUrl = settingsManager.getServerUrl();
        appendResult("📡 URL du serveur: " + serverUrl);

        String token = getStoredToken();
        if (token == null || token.isEmpty()) {
            appendResult("❌ ERREUR: Aucun token d'authentification trouvé");
            appendResult("   Veuillez vous connecter d'abord");
            enableAllButtons();
            return;
        }
        appendResult("🔑 Token: Présent (***" + token.substring(Math.max(0, token.length() - 8)) + ")");
        appendResult("");

        // Test de connexion
        appendResult("➡️ Test 1/3: Connexion au serveur...");
        testConnectionInBackground(result -> {
            appendResult(result);
            appendResult("");

            // Test de l'URL de base
            appendResult("➡️ Test 2/3: Test de l'URL de base...");
            testBaseUrlInBackground(result2 -> {
                appendResult(result2);
                appendResult("");

                // Test des rôles
                appendResult("➡️ Test 3/3: Test de compatibilité des rôles...");
                testRolesInBackground(() -> {
                    appendResult("");
                    appendResult("✅ Tests API terminés");
                    appendResult("═══════════════════════════════════════════");
                    enableAllButtons();
                });
            });
        });
    }

    /**
     * Test de connexion au serveur
     */
    private void testConnection() {
        disableButton(btnTestConnection);
        appendResult("═══════════════════════════════════════════");
        appendResult("🔗 TEST DE CONNEXION");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        testConnectionInBackground(result -> {
            appendResult(result);
            appendResult("═══════════════════════════════════════════");
            appendResult("");
            enableButton(btnTestConnection);
        });
    }

    private void testConnectionInBackground(TestCallback callback) {
        String serverUrl = settingsManager.getServerUrl();
        appendResult("📡 URL testée: " + serverUrl + "login.php");
        appendResult("⏳ Test en cours...");
        appendResult("");

        new Thread(() -> {
            try {
                boolean ignoreSsl = settingsManager.isIgnoreSsl();
                int timeout = settingsManager.getTimeout();

                okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS);

                // Configuration SSL
                if (ignoreSsl) {
                    configureSsl(clientBuilder);
                }

                okhttp3.OkHttpClient client = clientBuilder.build();
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(serverUrl + "login.php")
                        .post(okhttp3.RequestBody.create("{}", okhttp3.MediaType.get("application/json")))
                        .addHeader("Content-Type", "application/json")
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                runOnUiThread(() -> {
                    String result = "✅ Connexion réussie (Code: " + response.code() + ")";

                    if (responseBody.contains("<html")) {
                        result += "\n⚠️ Le serveur retourne du HTML au lieu de JSON";
                        if (responseBody.contains("404")) {
                            result += "\n❌ Endpoint non trouvé (404)";
                        }
                    } else if (responseBody.contains("{")) {
                        result += "\n✅ Le serveur retourne du JSON";
                    }

                    callback.onResult(result);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    String errorMsg = "❌ Connexion échouée: ";
                    if (e.getMessage().contains("SSL")) {
                        errorMsg += "Erreur SSL";
                    } else if (e.getMessage().contains("timeout")) {
                        errorMsg += "Timeout";
                    } else {
                        errorMsg += e.getMessage();
                    }
                    callback.onResult(errorMsg);
                });
            }
        }).start();
    }

    /**
     * Test de l'URL de base
     */
    private void testBaseUrl() {
        disableButton(btnTestBaseUrl);
        appendResult("═══════════════════════════════════════════");
        appendResult("🌐 TEST URL DE BASE");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        testBaseUrlInBackground(result -> {
            appendResult(result);
            appendResult("═══════════════════════════════════════════");
            appendResult("");
            enableButton(btnTestBaseUrl);
        });
    }

    private void testBaseUrlInBackground(TestCallback callback) {
        String serverUrl = settingsManager.getServerUrl();
        appendResult("📡 URL testée: " + serverUrl);
        appendResult("⏳ Test en cours...");
        appendResult("");

        new Thread(() -> {
            try {
                boolean ignoreSsl = settingsManager.isIgnoreSsl();
                int timeout = settingsManager.getTimeout();

                okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS);

                if (ignoreSsl) {
                    configureSsl(clientBuilder);
                }

                okhttp3.OkHttpClient client = clientBuilder.build();
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(serverUrl)
                        .get()
                        .build();

                okhttp3.Response response = client.newCall(request).execute();

                runOnUiThread(() -> {
                    String result = "✅ URL de base accessible (Code: " + response.code() + ")";

                    if (response.code() == 200) {
                        result += "\n✅ Serveur accessible";
                    } else if (response.code() == 404) {
                        result += "\n⚠️ Page non trouvée";
                    } else if (response.code() == 403) {
                        result += "\n❌ Accès interdit";
                    }

                    callback.onResult(result);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    callback.onResult("❌ Test échoué: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Test du token d'authentification et des données utilisateur
     */
    private void testToken() {
        disableButton(btnTestToken);
        appendResult("═══════════════════════════════════════════");
        appendResult("🔑 DIAGNOSTIC COMPLET - SESSION & DONNÉES");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = getStoredToken();

        // ========================================
        // SECTION 1: AUTHENTIFICATION
        // ========================================
        appendResult("┌─────────────────────────────────────────┐");
        appendResult("│ 🔐 SECTION 1: AUTHENTIFICATION         │");
        appendResult("└─────────────────────────────────────────┘");
        appendResult("");

        if (token == null || token.isEmpty()) {
            appendResult("❌ Token: ABSENT");
            appendResult("   Statut: Non connecté");
            appendResult("");
            appendResult("💡 Solution:");
            appendResult("   1. Déconnectez-vous de l'application");
            appendResult("   2. Reconnectez-vous");
            appendResult("   3. Relancez ce test");
        } else {
            appendResult("✅ Token: PRÉSENT");
            appendResult("   Longueur: " + token.length() + " caractères");
            appendResult("   Préfixe: " + token.substring(0, Math.min(20, token.length())) + "...");
            appendResult("   Suffixe: ***" + token.substring(Math.max(0, token.length() - 8)));
        }
        appendResult("");

        // ========================================
        // SECTION 2: DONNÉES UTILISATEUR (NOUVELLES CLÉS v2.0)
        // ========================================
        appendResult("┌─────────────────────────────────────────┐");
        appendResult("│ 👤 SECTION 2: DONNÉES UTILISATEUR v2.0  │");
        appendResult("└─────────────────────────────────────────┘");
        appendResult("");

        int userId = prefs.getInt("user_id", -1);
        String userName = prefs.getString("user_name", null);
        String userEmail = prefs.getString("user_email", null);
        int userType = prefs.getInt("user_type", -1);

        appendResult("User ID: " + (userId != -1 ? "✅ " + userId : "❌ ABSENT"));
        appendResult("User Name: " + (userName != null ? "✅ " + userName : "❌ ABSENT"));
        appendResult("User Email: " + (userEmail != null ? "✅ " + userEmail : "❌ ABSENT"));

        if (userType != -1) {
            String typeText = getUserTypeText(userType);
            appendResult("User Type: ✅ " + userType + " (" + typeText + ")");
        } else {
            appendResult("User Type: ❌ ABSENT");
        }
        appendResult("");

        // ========================================
        // SECTION 3: DONNÉES ANCIENNES (Compatibilité v1.0)
        // ========================================
        appendResult("┌─────────────────────────────────────────┐");
        appendResult("│ 🔄 SECTION 3: COMPATIBILITÉ v1.0       │");
        appendResult("└─────────────────────────────────────────┘");
        appendResult("");

        int oldEmployeeId = prefs.getInt("employee_id", -1);
        String oldEmployeeName = prefs.getString("employee_name", null);
        String oldEmployeeEmail = prefs.getString("employee_email", null);

        if (oldEmployeeId != -1 || oldEmployeeName != null || oldEmployeeEmail != null) {
            appendResult("⚠️ Anciennes données détectées:");
            appendResult("   employee_id: " + (oldEmployeeId != -1 ? oldEmployeeId + " (obsolète)" : "Absent"));
            appendResult("   employee_name: " + (oldEmployeeName != null ? oldEmployeeName + " (obsolète)" : "Absent"));
            appendResult("   employee_email: " + (oldEmployeeEmail != null ? oldEmployeeEmail + " (obsolète)" : "Absent"));
            appendResult("");
            appendResult("💡 Ces données seront remplacées au prochain login");
        } else {
            appendResult("✅ Aucune ancienne donnée (normal pour v2.0)");
        }
        appendResult("");

        // ========================================
        // SECTION 4: MODE OFFLINE
        // ========================================
        appendResult("┌─────────────────────────────────────────┐");
        appendResult("│ 📵 SECTION 4: MODE OFFLINE              │");
        appendResult("└─────────────────────────────────────────┘");
        appendResult("");

        boolean offlineEnabled = prefs.getBoolean("offline_login_enabled", false);
        String offlineEmail = prefs.getString("offline_email", null);
        String offlinePasswordHash = prefs.getString("offline_password_hash", null);

        appendResult("Offline activé: " + (offlineEnabled ? "✅ OUI" : "❌ NON"));
        appendResult("Email offline: " + (offlineEmail != null ? "✅ " + offlineEmail : "❌ ABSENT"));
        appendResult("Password hash: " + (offlinePasswordHash != null ? "✅ Présent" : "❌ ABSENT"));
        appendResult("");

        // Déterminer si le login offline est possible
        int finalUserId = (userId != -1) ? userId : oldEmployeeId;
        String finalUserName = (userName != null) ? userName : oldEmployeeName;

        boolean canLoginOffline = offlineEnabled &&
                                  offlineEmail != null &&
                                  offlinePasswordHash != null &&
                                  finalUserId != -1 &&
                                  finalUserName != null;

        if (canLoginOffline) {
            appendResult("🎉 Login offline: ✅ POSSIBLE");
        } else {
            appendResult("⚠️ Login offline: ❌ IMPOSSIBLE");
            appendResult("");
            appendResult("Données manquantes:");
            if (!offlineEnabled) appendResult("   - offline_login_enabled");
            if (offlineEmail == null) appendResult("   - offline_email");
            if (offlinePasswordHash == null) appendResult("   - offline_password_hash");
            if (finalUserId == -1) appendResult("   - user_id / employee_id");
            if (finalUserName == null) appendResult("   - user_name / employee_name");
        }
        appendResult("");

        // ========================================
        // SECTION 5: CONFIGURATION RÉSEAU
        // ========================================
        appendResult("┌─────────────────────────────────────────┐");
        appendResult("│ 🌐 SECTION 5: CONFIGURATION RÉSEAU      │");
        appendResult("└─────────────────────────────────────────┘");
        appendResult("");

        String serverUrl = settingsManager.getServerUrl();
        boolean ignoreSsl = settingsManager.isIgnoreSsl();
        int timeout = settingsManager.getTimeout();

        appendResult("URL Serveur: " + serverUrl);
        appendResult("Ignorer SSL: " + (ignoreSsl ? "✅ Oui" : "❌ Non"));
        appendResult("Timeout: " + timeout + " secondes");
        appendResult("");

        // ========================================
        // SECTION 6: RÉSUMÉ
        // ========================================
        appendResult("┌─────────────────────────────────────────┐");
        appendResult("│ 📊 SECTION 6: RÉSUMÉ                    │");
        appendResult("└─────────────────────────────────────────┘");
        appendResult("");

        int totalChecks = 6;
        int passedChecks = 0;

        if (token != null) passedChecks++;
        if (userId != -1 || oldEmployeeId != -1) passedChecks++;
        if (userName != null || oldEmployeeName != null) passedChecks++;
        if (userEmail != null || oldEmployeeEmail != null) passedChecks++;
        if (serverUrl != null && !serverUrl.isEmpty()) passedChecks++;
        if (canLoginOffline) passedChecks++;

        appendResult("✅ Vérifications réussies: " + passedChecks + "/" + totalChecks);
        appendResult("");

        if (passedChecks == totalChecks) {
            appendResult("🎉 STATUT: EXCELLENT");
            appendResult("   Toutes les données sont présentes et valides.");
        } else if (passedChecks >= 4) {
            appendResult("✅ STATUT: BON");
            appendResult("   La plupart des données sont présentes.");
        } else if (passedChecks >= 2) {
            appendResult("⚠️ STATUT: MOYEN");
            appendResult("   Certaines données manquent.");
        } else {
            appendResult("❌ STATUT: CRITIQUE");
            appendResult("   Reconnexion nécessaire.");
        }

        appendResult("═══════════════════════════════════════════");
        appendResult("");
        enableButton(btnTestToken);
    }

    /**
     * Obtenir le texte du type utilisateur
     */
    private String getUserTypeText(int type) {
        switch (type) {
            case 1: return "Admin";
            case 2: return "Manager";
            case 3: return "Accountant";
            case 4: return "Employee";
            case 5: return "Viewer";
            default: return "Inconnu";
        }
    }

    /**
     * Test de compatibilité des rôles
     */
    private void testRoles() {
        disableButton(btnTestRoles);
        appendResult("═══════════════════════════════════════════");
        appendResult("👥 TEST DE COMPATIBILITÉ DES RÔLES");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        String token = getStoredToken();
        if (token == null || token.isEmpty()) {
            appendResult("❌ ERREUR: Aucun token d'authentification trouvé");
            appendResult("   Veuillez vous connecter d'abord");
            appendResult("═══════════════════════════════════════════");
            appendResult("");
            enableButton(btnTestRoles);
            return;
        }

        testRolesInBackground(() -> {
            appendResult("═══════════════════════════════════════════");
            appendResult("");
            enableButton(btnTestRoles);
        });
    }

    private void testRolesInBackground(Runnable onComplete) {
        String token = getStoredToken();

        RoleCompatibilityTester.quickTest(this, token, new RoleCompatibilityTester.TestResultCallback() {
            @Override
            public void onTestCompleted(boolean success, String message) {
                runOnUiThread(() -> {
                    if (success) {
                        appendResult("✅ " + message);
                        appendResult("");
                        appendResult("🎉 Tous les tests sont passés avec succès!");
                        appendResult("   L'application est compatible avec le serveur PTMS.");
                    } else {
                        appendResult("❌ " + message);
                        appendResult("");
                        appendResult("🚨 Problèmes de compatibilité détectés");
                        appendResult("   Vérifiez la configuration du serveur.");
                    }
                    onComplete.run();
                });
            }

            @Override
            public void onTestProgress(String message) {
                runOnUiThread(() -> {
                    appendResult(message);
                });
            }
        });
    }

    /**
     * Effacer les résultats
     */
    private void clearResults() {
        testResultsTextView.setText("");
        displayWelcomeMessage();
        Toast.makeText(this, "Résultats effacés", Toast.LENGTH_SHORT).show();
    }

    /**
     * Ajouter un résultat au TextView
     */
    private void appendResult(String message) {
        String currentText = testResultsTextView.getText().toString();
        String newText = currentText + message + "\n";
        testResultsTextView.setText(newText);

        // Faire défiler vers le bas
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        Log.d(TAG, message);
    }

    /**
     * Obtenir le token stocké
     */
    private String getStoredToken() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_TOKEN, null);
    }

    /**
     * Configuration SSL pour ignorer les certificats
     */
    private void configureSsl(okhttp3.OkHttpClient.Builder clientBuilder) {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                new javax.net.ssl.X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            clientBuilder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            clientBuilder.hostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            Log.e(TAG, "Erreur configuration SSL", e);
        }
    }

    /**
     * Désactiver tous les boutons
     */
    private void disableAllButtons() {
        btnTestApi.setEnabled(false);
        btnTestConnection.setEnabled(false);
        btnTestBaseUrl.setEnabled(false);
        btnTestToken.setEnabled(false);
        btnTestRoles.setEnabled(false);
        btnTestChatConversations.setEnabled(false);
        btnTestChatUsers.setEnabled(false);
        btnTestChatSendMessage.setEnabled(false);
        btnTestChatFull.setEnabled(false);
        btnTestOfflineMode.setEnabled(false);
        btnTestNotes.setEnabled(false);
    }

    /**
     * Activer tous les boutons
     */
    private void enableAllButtons() {
        btnTestApi.setEnabled(true);
        btnTestConnection.setEnabled(true);
        btnTestBaseUrl.setEnabled(true);
        btnTestToken.setEnabled(true);
        btnTestRoles.setEnabled(true);
        btnTestChatConversations.setEnabled(true);
        btnTestChatUsers.setEnabled(true);
        btnTestChatSendMessage.setEnabled(true);
        btnTestChatFull.setEnabled(true);
        btnTestOfflineMode.setEnabled(true);
        btnTestNotes.setEnabled(true);
    }

    /**
     * Désactiver un bouton spécifique
     */
    private void disableButton(Button button) {
        if (button != null) {
            button.setEnabled(false);
        }
    }

    /**
     * Activer un bouton spécifique
     */
    private void enableButton(Button button) {
        if (button != null) {
            button.setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ========================================================================
    // TESTS DU CHAT
    // ========================================================================

    /**
     * Test de récupération des conversations
     */
    private void testChatConversations() {
        disableButton(btnTestChatConversations);
        appendResult("═══════════════════════════════════════════");
        appendResult("💬 TEST: RÉCUPÉRATION DES CONVERSATIONS");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        String token = getStoredToken();
        if (token == null || token.isEmpty()) {
            appendResult("❌ ERREUR: Aucun token d'authentification");
            appendResult("   Veuillez vous connecter d'abord");
            appendResult("═══════════════════════════════════════════");
            appendResult("");
            enableButton(btnTestChatConversations);
            return;
        }

        appendResult("🔑 Token: Présent");
        appendResult("📡 Endpoint: /api/chat/conversations");
        appendResult("⏳ Envoi de la requête...");
        appendResult("");

        apiService.getChatRooms(token).enqueue(new retrofit2.Callback<com.ptms.mobile.api.ApiService.ChatRoomsResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatRoomsResponse> call, retrofit2.Response<com.ptms.mobile.api.ApiService.ChatRoomsResponse> response) {
                runOnUiThread(() -> {
                    appendResult("📥 Réponse reçue:");
                    appendResult("   Code HTTP: " + response.code());
                    appendResult("");

                    if (response.isSuccessful() && response.body() != null) {
                        com.ptms.mobile.api.ApiService.ChatRoomsResponse chatResponse = response.body();

                        if (chatResponse.success) {
                            appendResult("✅ SUCCÈS");
                            appendResult("   Message: " + chatResponse.message);
                            appendResult("   Nombre de conversations: " + (chatResponse.rooms != null ? chatResponse.rooms.size() : 0));
                            appendResult("");

                            if (chatResponse.rooms != null && !chatResponse.rooms.isEmpty()) {
                                appendResult("📋 Liste des conversations:");
                                for (int i = 0; i < Math.min(5, chatResponse.rooms.size()); i++) {
                                    com.ptms.mobile.models.ChatRoom room = chatResponse.rooms.get(i);
                                    appendResult("   " + (i+1) + ". " + room.getName());
                                    appendResult("      Type: " + room.getType());
                                    appendResult("      Messages non lus: " + room.getUnreadCount());
                                }
                                if (chatResponse.rooms.size() > 5) {
                                    appendResult("   ... et " + (chatResponse.rooms.size() - 5) + " autres");
                                }
                            }
                        } else {
                            appendResult("❌ ÉCHEC");
                            appendResult("   Message: " + chatResponse.message);
                            appendResult("   Success: " + chatResponse.success);
                        }
                    } else {
                        appendResult("❌ ERREUR HTTP");
                        appendResult("   Code: " + response.code());
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Aucun détail";
                            appendResult("   Détails: " + errorBody);
                        } catch (Exception e) {
                            appendResult("   Impossible de lire les détails");
                        }
                    }

                    appendResult("═══════════════════════════════════════════");
                    appendResult("");
                    enableButton(btnTestChatConversations);
                });
            }

            @Override
            public void onFailure(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatRoomsResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    appendResult("❌ ÉCHEC DE LA REQUÊTE");
                    appendResult("   Erreur: " + t.getClass().getSimpleName());
                    appendResult("   Message: " + t.getMessage());
                    appendResult("");
                    appendResult("💡 Causes possibles:");
                    appendResult("   - Serveur inaccessible");
                    appendResult("   - Problème réseau");
                    appendResult("   - URL incorrecte");
                    appendResult("   - Timeout");
                    appendResult("═══════════════════════════════════════════");
                    appendResult("");
                    enableButton(btnTestChatConversations);
                });
            }
        });
    }

    /**
     * Test de récupération des utilisateurs du chat
     */
    private void testChatUsers() {
        disableButton(btnTestChatUsers);
        appendResult("═══════════════════════════════════════════");
        appendResult("👥 TEST: RÉCUPÉRATION DES UTILISATEURS");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        String token = getStoredToken();
        if (token == null || token.isEmpty()) {
            appendResult("❌ ERREUR: Aucun token d'authentification");
            appendResult("═══════════════════════════════════════════");
            appendResult("");
            enableButton(btnTestChatUsers);
            return;
        }

        appendResult("🔑 Token: Présent");
        appendResult("📡 Endpoint: /api/chat/users");
        appendResult("⏳ Envoi de la requête...");
        appendResult("");

        apiService.getChatUsers(token).enqueue(new retrofit2.Callback<com.ptms.mobile.api.ApiService.ChatUsersResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatUsersResponse> call, retrofit2.Response<com.ptms.mobile.api.ApiService.ChatUsersResponse> response) {
                runOnUiThread(() -> {
                    appendResult("📥 Réponse reçue:");
                    appendResult("   Code HTTP: " + response.code());
                    appendResult("");

                    if (response.isSuccessful() && response.body() != null) {
                        com.ptms.mobile.api.ApiService.ChatUsersResponse usersResponse = response.body();

                        if (usersResponse.success) {
                            appendResult("✅ SUCCÈS");
                            appendResult("   Nombre d'utilisateurs: " + (usersResponse.users != null ? usersResponse.users.size() : 0));
                            appendResult("");

                            if (usersResponse.users != null && !usersResponse.users.isEmpty()) {
                                appendResult("👥 Liste des utilisateurs:");
                                for (int i = 0; i < Math.min(10, usersResponse.users.size()); i++) {
                                    com.ptms.mobile.models.ChatUser user = usersResponse.users.get(i);
                                    appendResult("   " + (i+1) + ". " + user.getName());
                                    appendResult("      ID: " + user.getId());
                                    appendResult("      Email: " + user.getEmail());
                                    appendResult("      En ligne: " + (user.isOnline() ? "Oui ✓" : "Non"));
                                }
                                if (usersResponse.users.size() > 10) {
                                    appendResult("   ... et " + (usersResponse.users.size() - 10) + " autres");
                                }
                            }
                        } else {
                            appendResult("❌ ÉCHEC");
                            appendResult("   Message: " + usersResponse.message);
                        }
                    } else {
                        appendResult("❌ ERREUR HTTP");
                        appendResult("   Code: " + response.code());
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Aucun détail";
                            appendResult("   Détails: " + errorBody);
                        } catch (Exception e) {
                            appendResult("   Impossible de lire les détails");
                        }
                    }

                    appendResult("═══════════════════════════════════════════");
                    appendResult("");
                    enableButton(btnTestChatUsers);
                });
            }

            @Override
            public void onFailure(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatUsersResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    appendResult("❌ ÉCHEC DE LA REQUÊTE");
                    appendResult("   Erreur: " + t.getMessage());
                    appendResult("═══════════════════════════════════════════");
                    appendResult("");
                    enableButton(btnTestChatUsers);
                });
            }
        });
    }

    /**
     * Test d'envoi de message
     */
    private void testChatSendMessage() {
        disableButton(btnTestChatSendMessage);
        appendResult("═══════════════════════════════════════════");
        appendResult("✉️ TEST: ENVOI DE MESSAGE");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        String token = getStoredToken();
        if (token == null || token.isEmpty()) {
            appendResult("❌ ERREUR: Aucun token d'authentification");
            appendResult("═══════════════════════════════════════════");
            appendResult("");
            enableButton(btnTestChatSendMessage);
            return;
        }

        appendResult("⚠️ Ce test nécessite une conversation existante");
        appendResult("   Récupération des conversations d'abord...");
        appendResult("");

        // Récupérer d'abord les conversations
        apiService.getChatRooms(token).enqueue(new retrofit2.Callback<com.ptms.mobile.api.ApiService.ChatRoomsResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatRoomsResponse> call, retrofit2.Response<com.ptms.mobile.api.ApiService.ChatRoomsResponse> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        com.ptms.mobile.api.ApiService.ChatRoomsResponse chatResponse = response.body();

                        if (chatResponse.rooms != null && !chatResponse.rooms.isEmpty()) {
                            // Prendre la première conversation
                            com.ptms.mobile.models.ChatRoom firstRoom = chatResponse.rooms.get(0);
                            appendResult("✅ Conversation trouvée: " + firstRoom.getName());
                            appendResult("   ID: " + firstRoom.getId());
                            appendResult("");

                            // Envoyer un message de test
                            String testMessage = "Message de test - " + System.currentTimeMillis();
                            appendResult("📤 Envoi du message de test...");
                            appendResult("   Message: \"" + testMessage + "\"");
                            appendResult("");

                            com.ptms.mobile.api.ApiService.SendMessageRequest messageRequest =
                                new com.ptms.mobile.api.ApiService.SendMessageRequest(firstRoom.getId(), testMessage);

                            apiService.sendChatMessage(token, messageRequest).enqueue(new retrofit2.Callback<com.ptms.mobile.api.ApiService.ChatSendResponse>() {
                                @Override
                                public void onResponse(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatSendResponse> call, retrofit2.Response<com.ptms.mobile.api.ApiService.ChatSendResponse> response) {
                                    runOnUiThread(() -> {
                                        appendResult("📥 Réponse reçue:");
                                        appendResult("   Code HTTP: " + response.code());
                                        appendResult("");

                                        if (response.isSuccessful() && response.body() != null) {
                                            com.ptms.mobile.api.ApiService.ChatSendResponse sendResponse = response.body();

                                            if (sendResponse.success) {
                                                appendResult("✅ MESSAGE ENVOYÉ AVEC SUCCÈS!");
                                                appendResult("   Message: " + sendResponse.message);
                                                if (sendResponse.chatMessage != null) {
                                                    appendResult("   ID du message: " + sendResponse.chatMessage.getId());
                                                    appendResult("   Contenu: " + sendResponse.chatMessage.getContent());
                                                    if (sendResponse.chatMessage.getTimestamp() != null) {
                                                        appendResult("   Envoyé à: " + java.text.DateFormat.getDateTimeInstance().format(sendResponse.chatMessage.getTimestamp()));
                                                    }
                                                }
                                            } else {
                                                appendResult("❌ ÉCHEC");
                                                appendResult("   Message: " + sendResponse.message);
                                            }
                                        } else {
                                            appendResult("❌ ERREUR HTTP");
                                            appendResult("   Code: " + response.code());
                                            try {
                                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Aucun détail";
                                                appendResult("   Détails: " + errorBody);
                                            } catch (Exception e) {
                                                appendResult("   Impossible de lire les détails");
                                            }
                                        }

                                        appendResult("═══════════════════════════════════════════");
                                        appendResult("");
                                        enableButton(btnTestChatSendMessage);
                                    });
                                }

                                @Override
                                public void onFailure(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatSendResponse> call, Throwable t) {
                                    runOnUiThread(() -> {
                                        appendResult("❌ ÉCHEC DE L'ENVOI");
                                        appendResult("   Erreur: " + t.getMessage());
                                        appendResult("═══════════════════════════════════════════");
                                        appendResult("");
                                        enableButton(btnTestChatSendMessage);
                                    });
                                }
                            });
                        } else {
                            appendResult("⚠️ Aucune conversation disponible");
                            appendResult("   Créez d'abord une conversation pour tester l'envoi");
                            appendResult("═══════════════════════════════════════════");
                            appendResult("");
                            enableButton(btnTestChatSendMessage);
                        }
                    } else {
                        appendResult("❌ Impossible de récupérer les conversations");
                        appendResult("═══════════════════════════════════════════");
                        appendResult("");
                        enableButton(btnTestChatSendMessage);
                    }
                });
            }

            @Override
            public void onFailure(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatRoomsResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    appendResult("❌ Échec récupération conversations");
                    appendResult("   Erreur: " + t.getMessage());
                    appendResult("═══════════════════════════════════════════");
                    appendResult("");
                    enableButton(btnTestChatSendMessage);
                });
            }
        });
    }

    /**
     * Test complet du chat
     */
    private void testChatFull() {
        disableAllButtons();
        appendResult("═══════════════════════════════════════════");
        appendResult("🚀 TEST COMPLET DU CHAT");
        appendResult("═══════════════════════════════════════════");
        appendResult("Heure: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");

        String token = getStoredToken();
        if (token == null || token.isEmpty()) {
            appendResult("❌ ERREUR: Aucun token d'authentification");
            appendResult("   Veuillez vous connecter d'abord");
            appendResult("═══════════════════════════════════════════");
            appendResult("");
            enableAllButtons();
            return;
        }

        String serverUrl = settingsManager.getServerUrl();
        appendResult("📡 URL du serveur: " + serverUrl);
        appendResult("🔑 Token: Présent");
        appendResult("");
        appendResult("Ce test va exécuter:");
        appendResult("   1. Récupération des utilisateurs");
        appendResult("   2. Récupération des conversations");
        appendResult("   3. Envoi d'un message de test (si possible)");
        appendResult("");

        // Test 1: Utilisateurs
        appendResult("➡️ Test 1/3: Récupération des utilisateurs...");
        apiService.getChatUsers(token).enqueue(new retrofit2.Callback<com.ptms.mobile.api.ApiService.ChatUsersResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatUsersResponse> call, retrofit2.Response<com.ptms.mobile.api.ApiService.ChatUsersResponse> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        appendResult("✅ Utilisateurs récupérés: " + response.body().users.size());
                    } else {
                        appendResult("❌ Échec récupération utilisateurs (Code: " + response.code() + ")");
                    }
                    appendResult("");

                    // Test 2: Conversations
                    appendResult("➡️ Test 2/3: Récupération des conversations...");
                    apiService.getChatRooms(token).enqueue(new retrofit2.Callback<com.ptms.mobile.api.ApiService.ChatRoomsResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatRoomsResponse> call, retrofit2.Response<com.ptms.mobile.api.ApiService.ChatRoomsResponse> response) {
                            runOnUiThread(() -> {
                                boolean hasConversations = false;
                                int conversationId = -1;

                                if (response.isSuccessful() && response.body() != null && response.body().success) {
                                    appendResult("✅ Conversations récupérées: " + response.body().rooms.size());
                                    if (response.body().rooms != null && !response.body().rooms.isEmpty()) {
                                        hasConversations = true;
                                        conversationId = response.body().rooms.get(0).getId();
                                    }
                                } else {
                                    appendResult("❌ Échec récupération conversations (Code: " + response.code() + ")");
                                }
                                appendResult("");

                                // Test 3: Envoi de message (si conversation disponible)
                                appendResult("➡️ Test 3/3: Envoi d'un message...");
                                if (hasConversations) {
                                    final int convId = conversationId;
                                    String testMessage = "Test automatique - " + System.currentTimeMillis();

                                    com.ptms.mobile.api.ApiService.SendMessageRequest messageRequest =
                                        new com.ptms.mobile.api.ApiService.SendMessageRequest(convId, testMessage);

                                    apiService.sendChatMessage(token, messageRequest).enqueue(new retrofit2.Callback<com.ptms.mobile.api.ApiService.ChatSendResponse>() {
                                        @Override
                                        public void onResponse(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatSendResponse> call, retrofit2.Response<com.ptms.mobile.api.ApiService.ChatSendResponse> response) {
                                            runOnUiThread(() -> {
                                                if (response.isSuccessful() && response.body() != null && response.body().success) {
                                                    appendResult("✅ Message envoyé avec succès");
                                                } else {
                                                    appendResult("❌ Échec envoi message (Code: " + response.code() + ")");
                                                }
                                                appendResult("");
                                                appendResult("═══════════════════════════════════════════");
                                                appendResult("✅ TEST COMPLET TERMINÉ");
                                                appendResult("═══════════════════════════════════════════");
                                                appendResult("");
                                                enableAllButtons();
                                            });
                                        }

                                        @Override
                                        public void onFailure(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatSendResponse> call, Throwable t) {
                                            runOnUiThread(() -> {
                                                appendResult("❌ Échec envoi: " + t.getMessage());
                                                appendResult("");
                                                appendResult("═══════════════════════════════════════════");
                                                appendResult("⚠️ TEST COMPLET TERMINÉ AVEC ERREURS");
                                                appendResult("═══════════════════════════════════════════");
                                                appendResult("");
                                                enableAllButtons();
                                            });
                                        }
                                    });
                                } else {
                                    appendResult("⚠️ Aucune conversation pour tester l'envoi");
                                    appendResult("");
                                    appendResult("═══════════════════════════════════════════");
                                    appendResult("⚠️ TEST COMPLET TERMINÉ (PARTIEL)");
                                    appendResult("═══════════════════════════════════════════");
                                    appendResult("");
                                    enableAllButtons();
                                }
                            });
                        }

                        @Override
                        public void onFailure(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatRoomsResponse> call, Throwable t) {
                            runOnUiThread(() -> {
                                appendResult("❌ Échec: " + t.getMessage());
                                appendResult("");
                                appendResult("═══════════════════════════════════════════");
                                appendResult("❌ TEST COMPLET INTERROMPU");
                                appendResult("═══════════════════════════════════════════");
                                appendResult("");
                                enableAllButtons();
                            });
                        }
                    });
                });
            }

            @Override
            public void onFailure(retrofit2.Call<com.ptms.mobile.api.ApiService.ChatUsersResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    appendResult("❌ Échec: " + t.getMessage());
                    appendResult("");
                    appendResult("═══════════════════════════════════════════");
                    appendResult("❌ TEST COMPLET INTERROMPU");
                    appendResult("═══════════════════════════════════════════");
                    appendResult("");
                    enableAllButtons();
                });
            }
        });
    }

    /**
     * Test du mode offline - Affiche toutes les données sauvegardées
     */
    private void testOfflineMode() {
        appendResult("═══════════════════════════════════════════");
        appendResult("🔍 TEST MODE OFFLINE");
        appendResult("═══════════════════════════════════════════");
        appendResult("");

        SharedPreferences prefs = getSharedPreferences("ptms_prefs", Context.MODE_PRIVATE);

        // 1. Vérifier offline_login_enabled
        boolean offlineEnabled = prefs.getBoolean("offline_login_enabled", false);
        appendResult("📌 Login offline: " + (offlineEnabled ? "✅ ACTIVÉ" : "❌ DÉSACTIVÉ"));
        appendResult("");

        // 2. Vérifier credentials offline
        String offlineEmail = prefs.getString("offline_email", null);
        String offlinePasswordHash = prefs.getString("offline_password_hash", null);

        appendResult("📧 Email offline: " + (offlineEmail != null ? "✅ " + offlineEmail : "❌ ABSENT"));
        appendResult("🔐 Hash mot de passe: " + (offlinePasswordHash != null ?
            "✅ " + offlinePasswordHash.substring(0, Math.min(16, offlinePasswordHash.length())) + "..." :
            "❌ ABSENT"));
        appendResult("");

        // 3. Vérifier données utilisateur
        int userId = prefs.getInt("user_id", -1);
        String userName = prefs.getString("user_name", null);
        String userEmail = prefs.getString("user_email", null);
        int userType = prefs.getInt("user_type", -1);

        appendResult("👤 User ID: " + (userId != -1 ? "✅ " + userId : "❌ ABSENT"));
        appendResult("👤 User Name: " + (userName != null ? "✅ " + userName : "❌ ABSENT"));
        appendResult("📧 User Email: " + (userEmail != null ? "✅ " + userEmail : "❌ ABSENT"));

        if (userType != -1) {
            String typeText = "";
            switch (userType) {
                case 1: typeText = "Admin"; break;
                case 2: typeText = "Manager"; break;
                case 3: typeText = "Accountant"; break;
                case 4: typeText = "Employee"; break;
                case 5: typeText = "Viewer"; break;
                default: typeText = "Inconnu"; break;
            }
            appendResult("🔰 User Type: ✅ " + userType + " (" + typeText + ")");
        } else {
            appendResult("🔰 User Type: ❌ ABSENT");
        }
        appendResult("");

        // 4. Vérifier token
        String authToken = prefs.getString("auth_token", null);
        appendResult("🔑 Auth Token: " + (authToken != null ?
            "✅ " + authToken.substring(0, Math.min(20, authToken.length())) + "..." :
            "❌ ABSENT"));
        appendResult("");

        // 5. Conclusion
        appendResult("═══ CONCLUSION ═══");
        boolean canLoginOffline = offlineEnabled && offlineEmail != null &&
                                  offlinePasswordHash != null && userId != -1 && userName != null;

        if (canLoginOffline) {
            appendResult("✅ Login offline POSSIBLE");
            appendResult("Toutes les données nécessaires sont présentes");
        } else {
            appendResult("❌ Login offline IMPOSSIBLE");
            appendResult("Données manquantes:");
            if (!offlineEnabled) appendResult("  - offline_login_enabled = false");
            if (offlineEmail == null) appendResult("  - offline_email absent");
            if (offlinePasswordHash == null) appendResult("  - offline_password_hash absent");
            if (userId == -1) appendResult("  - user_id absent");
            if (userName == null) appendResult("  - user_name absent");
            appendResult("");
            appendResult("⚠️ Connectez-vous EN LIGNE une fois");
        }
        appendResult("");
        appendResult("═══════════════════════════════════════════");
    }

    /**
     * Test du système de notes - Diagnostic complet
     */
    private void testNotesDiagnostic() {
        appendResult("═══════════════════════════════════════════");
        appendResult("📝 TEST SYSTÈME DE NOTES");
        appendResult("═══════════════════════════════════════════");
        appendResult("");

        try {
            // Vérifier BidirectionalSyncManager
            com.ptms.mobile.sync.BidirectionalSyncManager syncManager =
                new com.ptms.mobile.sync.BidirectionalSyncManager(this);

            appendResult("✅ BidirectionalSyncManager initialisé");

            // Vérifier le nombre de notes en attente
            int pendingCount = syncManager.getPendingSyncCount();
            appendResult("📊 Notes en attente: " + pendingCount);
            appendResult("");

            // Vérifier l'accès à la base de données SQLite
            com.ptms.mobile.database.OfflineDatabaseHelper dbHelper = syncManager.getOfflineDatabaseHelper();
            appendResult("✅ OfflineDatabaseHelper accessible");

            // Tester la récupération des notes
            try {
                java.util.List<com.ptms.mobile.models.ProjectNote> notes = dbHelper.getAllPendingProjectNotes();
                appendResult("📝 Notes trouvées: " + notes.size());

                if (notes.size() > 0) {
                    appendResult("");
                    appendResult("Aperçu des notes:");
                    for (int i = 0; i < Math.min(3, notes.size()); i++) {
                        com.ptms.mobile.models.ProjectNote note = notes.get(i);
                        appendResult("  " + (i+1) + ". " + note.getNoteType() + " - " +
                                   (note.getProjectId() != null ? "Projet " + note.getProjectId() : "Personnel"));
                    }
                    if (notes.size() > 3) {
                        appendResult("  ... et " + (notes.size() - 3) + " autre(s)");
                    }
                }
            } catch (Exception e) {
                appendResult("⚠️ Erreur récupération notes: " + e.getMessage());
            }
            appendResult("");

            // Vérifier la connectivité API
            boolean isOnline = com.ptms.mobile.utils.NetworkUtils.isOnline(this);
            appendResult("🌐 Connexion réseau: " + (isOnline ? "✅ ONLINE" : "❌ OFFLINE"));

            if (isOnline) {
                appendResult("💡 Vous pouvez synchroniser les notes en attente");
            } else {
                appendResult("💡 Les notes seront synchronisées à la prochaine connexion");
            }
            appendResult("");

            // Résumé
            appendResult("═══ RÉSUMÉ ═══");
            appendResult("✅ Système de notes opérationnel");
            appendResult("📊 " + pendingCount + " élément(s) en attente de sync");
            appendResult("🌐 Mode: " + (isOnline ? "ONLINE" : "OFFLINE"));

        } catch (Exception e) {
            appendResult("❌ ERREUR: " + e.getMessage());
            Log.e(TAG, "Erreur test notes", e);
        }

        appendResult("");
        appendResult("═══════════════════════════════════════════");
    }

    /**
     * Interface pour les callbacks de test
     */
    private interface TestCallback {
        void onResult(String result);
    }
}
