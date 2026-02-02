package com.ptms.mobile.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ptms.mobile.R;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.sync.BidirectionalSyncManager;
import com.ptms.mobile.utils.ApiManager;
import com.ptms.mobile.utils.SessionManager;
import com.ptms.mobile.utils.SettingsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activité de diagnostic pour le système de notes
 * Affiche les erreurs réseau et les problèmes de synchronisation
 */
public class NotesDiagnosticsActivity extends AppCompatActivity {

    private static final String TAG = "NotesDiagnostic";

    private TextView tvConnectionStatus, tvApiUrl, tvNotesStats, tvErrorLog, tvLastSync, tvErrorStats;
    private Button btnTestApi, btnTestSync, btnRefresh, btnClearLogs;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private BidirectionalSyncManager syncManager;
    private StringBuilder errorLog = new StringBuilder();

    // Compteurs d'erreurs
    private int errorCountNetwork = 0;
    private int errorCountHttp = 0;
    private int errorCountAuth = 0;
    private int errorCountServer = 0;
    private int errorCountTimeout = 0;
    private int errorCountJson = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_diagnostics);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("🔍 Diagnostic Notes");
        }

        // Initialiser
        sessionManager = new SessionManager(this);
        syncManager = new BidirectionalSyncManager(this);

        // Initialiser les vues
        initViews();

        // Configurer les listeners
        setupListeners();

        // Lancer le diagnostic initial
        runDiagnostic();
    }

    private void initViews() {
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        tvApiUrl = findViewById(R.id.tv_api_url);
        tvNotesStats = findViewById(R.id.tv_notes_stats);
        tvErrorLog = findViewById(R.id.tv_error_log);
        tvLastSync = findViewById(R.id.tv_last_sync);
        tvErrorStats = findViewById(R.id.tv_error_stats);
        btnTestApi = findViewById(R.id.btn_test_api);
        btnTestSync = findViewById(R.id.btn_test_sync);
        btnRefresh = findViewById(R.id.btn_refresh_diagnostic);
        btnClearLogs = findViewById(R.id.btn_clear_logs);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnTestApi.setOnClickListener(v -> testApiConnection());
        btnTestSync.setOnClickListener(v -> testSyncManager());
        btnRefresh.setOnClickListener(v -> runDiagnostic());
        btnClearLogs.setOnClickListener(v -> clearLogs());
    }

    /**
     * Lance le diagnostic complet
     */
    private void runDiagnostic() {
        logEvent("=== DÉBUT DU DIAGNOSTIC ===");
        checkAuthentication();
        checkConnection();
        checkApiUrl();
        loadNotesStats();
        checkLastSync();
    }

    /**
     * Vérifie l'authentification
     */
    private void checkAuthentication() {
        boolean isLoggedIn = sessionManager.isLoggedIn();
        int userId = sessionManager.getUserId();
        String token = sessionManager.getAuthToken();
        String userName = sessionManager.getUserName();

        logEvent("--- AUTHENTIFICATION ---");
        logEvent("Utilisateur connecté: " + (isLoggedIn ? "OUI" : "NON"));
        logEvent("User ID: " + userId);
        logEvent("Nom: " + (userName != null ? userName : "N/A"));
        logEvent("Token présent: " + (token != null && !token.isEmpty() ? "OUI" : "NON"));
        if (token != null && !token.isEmpty()) {
            logEvent("Token (premiers chars): " + token.substring(0, Math.min(20, token.length())) + "...");

            // Décoder le token pour afficher son contenu
            try {
                String decoded = new String(android.util.Base64.decode(token, android.util.Base64.DEFAULT));
                logEvent("Token décodé: " + decoded);

                String[] parts = decoded.split(":");
                logEvent("Token parts count: " + parts.length);
                if (parts.length >= 1) logEvent("  Part 0 (userID): " + parts[0]);
                if (parts.length >= 2) logEvent("  Part 1 (timestamp): " + parts[1]);
                if (parts.length >= 3) logEvent("  Part 2 (email): " + parts[2]);

            } catch (Exception e) {
                logEvent("ERREUR décodage token: " + e.getMessage());
            }
        } else {
            logEvent("⚠️ PROBLÈME: Aucun token d'authentification trouvé!");
        }
    }

    /**
     * Vérifie la connexion réseau
     */
    private void checkConnection() {
        boolean isOnline = com.ptms.mobile.utils.NetworkUtils.isOnline(this);
        if (isOnline) {
            tvConnectionStatus.setText("✅ Connecté au réseau");
            tvConnectionStatus.setTextColor(0xFF4CAF50); // Vert
            logEvent("Connexion réseau: OK");
        } else {
            tvConnectionStatus.setText("❌ Hors ligne");
            tvConnectionStatus.setTextColor(0xFFF44336); // Rouge
            logEvent("ERREUR: Pas de connexion réseau");
        }
    }

    /**
     * Affiche l'URL de l'API
     */
    private void checkApiUrl() {
        String apiUrl = ApiManager.getBaseUrl();
        tvApiUrl.setText("URL API: " + apiUrl);
        logEvent("--- CONFIGURATION URL & SSL ---");
        logEvent("URL de base (getBaseUrl): " + apiUrl);

        // Afficher aussi l'URL brute configurée
        SettingsManager settingsManager = new SettingsManager(this);
        String rawUrl = settingsManager.getServerUrlRaw();
        String normalizedUrl = settingsManager.getServerUrl();
        boolean ignoreSsl = settingsManager.isIgnoreSsl();
        int timeout = settingsManager.getTimeout();

        logEvent("URL brute (raw): " + rawUrl);
        logEvent("URL normalisée: " + normalizedUrl);
        logEvent("Ignore SSL: " + (ignoreSsl ? "OUI (certificat autosigné accepté)" : "NON"));
        logEvent("Timeout: " + timeout + " secondes");

        // Afficher l'URL complète qui sera utilisée pour les notes
        String fullNotesUrl = apiUrl + "/api/project-notes.php";
        logEvent("URL complète notes: " + fullNotesUrl);

        // Avertissement si HTTPS sans ignore SSL
        if (apiUrl.startsWith("https://") && !ignoreSsl) {
            logEvent("⚠️ ATTENTION: HTTPS sans ignore SSL - Le certificat doit être valide!");
        } else if (apiUrl.startsWith("https://") && ignoreSsl) {
            logEvent("✓ HTTPS avec ignore SSL activé - Certificats autosignés acceptés");
        } else if (apiUrl.startsWith("http://")) {
            logEvent("✓ HTTP - Pas de SSL (connexion non cryptée)");
        }
    }

    /**
     * Charge les statistiques des notes
     */
    private void loadNotesStats() {
        try {
            // Statistiques locales (notes non synchronisées)
            int notSyncedCount = syncManager.getPendingSyncCount();

            String stats = "Notes en attente: " + notSyncedCount + "\n";

            tvNotesStats.setText(stats);
            logEvent("Statistiques chargées: " + notSyncedCount + " notes en attente de sync");

        } catch (Exception e) {
            tvNotesStats.setText("Erreur chargement statistiques");
            logEvent("ERREUR chargement stats: " + e.getMessage());
            Log.e(TAG, "Erreur stats", e);
        }
    }

    /**
     * Vérifie la dernière synchronisation
     */
    private void checkLastSync() {
        // TODO: Implémenter un système de suivi de la dernière sync
        tvLastSync.setText("Information non disponible");
        logEvent("Dernière sync: N/A");
    }

    /**
     * Test la connexion à l'API des notes
     */
    private void testApiConnection() {
        progressBar.setVisibility(View.VISIBLE);
        btnTestApi.setEnabled(false);
        logEvent("--- TEST API NOTES ---");

        // Utiliser all_notes=1 pour récupérer toutes les notes (pas de project_id requis)
        String url = ApiManager.getBaseUrl() + "/api/project-notes.php?all_notes=1";
        logEvent("Test URL: " + url);

        String token = sessionManager.getAuthToken();
        if (token != null && !token.isEmpty()) {
            logEvent("Authorization: Bearer " + token.substring(0, Math.min(30, token.length())) + "...");
        } else {
            logEvent("⚠️ ATTENTION: Pas de token!");
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    btnTestApi.setEnabled(true);
                    try {
                        logEvent("✅ Réponse reçue du serveur");
                        logEvent("Response keys: " + response.keys().toString());

                        if (response.getBoolean("success")) {
                            JSONArray notes = response.getJSONArray("notes");
                            String message = "✅ API OK: " + notes.length() + " notes reçues";
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            logEvent(message);

                            // Afficher le nombre de notes par groupe
                            if (response.has("groupCounts")) {
                                JSONObject groupCounts = response.getJSONObject("groupCounts");
                                logEvent("Groupes: " + groupCounts.toString());
                            }

                            // Afficher les détails des premières notes
                            for (int i = 0; i < Math.min(3, notes.length()); i++) {
                                JSONObject note = notes.getJSONObject(i);
                                logEvent("  Note " + (i+1) + ": " + note.optString("noteType", "?") +
                                        " - " + note.optString("noteGroup", "?") +
                                        " (projet " + note.optInt("projectId", 0) + ")");
                            }

                        } else {
                            String error = response.optString("message", "Erreur inconnue");
                            String message = "⚠️ API réponse: " + error;
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            logEvent("ERREUR API: " + error);
                            logEvent("Réponse complète: " + response.toString());
                        }
                    } catch (JSONException e) {
                        errorCountJson++;
                        logEvent("📄 ERREUR PARSING JSON: " + e.getMessage());
                        logEvent("Response string: " + response.toString().substring(0, Math.min(300, response.toString().length())));
                        logEvent("   → Le serveur n'a pas renvoyé du JSON valide");
                        logEvent("   → Vérifiez qu'il n'y a pas d'erreurs PHP avant le JSON");
                        updateErrorStats();
                        Toast.makeText(this, "Erreur parsing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    btnTestApi.setEnabled(true);
                    logEvent("❌ Erreur lors de la requête API");
                    handleVolleyError(error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                    logEvent("✓ Header Authorization ajouté avec Bearer token");
                } else {
                    logEvent("✗ ATTENTION: Pas de token d'authentification - requête va échouer!");
                }
                return headers;
            }
        };

        ApiManager.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Test le gestionnaire de synchronisation
     */
    private void testSyncManager() {
        progressBar.setVisibility(View.VISIBLE);
        btnTestSync.setEnabled(false);
        logEvent("--- TEST SYNC MANAGER ---");

        try {
            boolean isOnline = com.ptms.mobile.utils.NetworkUtils.isOnline(this);
            logEvent("Sync Manager - isOnline: " + isOnline);

            int pendingCount = syncManager.getPendingSyncCount();
            logEvent("Notes en attente de sync: " + pendingCount);

            Toast.makeText(this, "✅ Sync Manager OK - " + pendingCount + " notes en attente", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            logEvent("ERREUR Sync Manager: " + e.getMessage());
            Toast.makeText(this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Erreur test sync", e);
        }

        progressBar.setVisibility(View.GONE);
        btnTestSync.setEnabled(true);
    }

    /**
     * Gère les erreurs Volley avec détails complets
     */
    private void handleVolleyError(VolleyError error) {
        String errorMessage;
        String errorType = "UNKNOWN";

        if (error.networkResponse != null) {
            // Erreur HTTP avec réponse du serveur
            int statusCode = error.networkResponse.statusCode;
            errorType = "HTTP_" + statusCode;
            errorMessage = "❌ Erreur HTTP " + statusCode;

            errorCountHttp++;

            // Analyser le code de statut
            switch (statusCode) {
                case 400:
                    errorMessage += " - Requête invalide (Bad Request)";
                    logEvent("⚠️ ERREUR 400: Vérifiez les paramètres de la requête");
                    break;
                case 401:
                    errorMessage += " - Non autorisé (Unauthorized)";
                    errorCountAuth++;
                    logEvent("🔒 ERREUR 401: Token d'authentification invalide ou expiré");
                    logEvent("   → Vérifiez que le token est présent et valide");
                    logEvent("   → Essayez de vous reconnecter");
                    break;
                case 403:
                    errorMessage += " - Accès interdit (Forbidden)";
                    errorCountAuth++;
                    logEvent("🚫 ERREUR 403: Accès refusé même avec authentification");
                    logEvent("   → Vérifiez vos permissions utilisateur");
                    break;
                case 404:
                    errorMessage += " - Endpoint non trouvé (Not Found)";
                    logEvent("❓ ERREUR 404: L'URL demandée n'existe pas sur le serveur");
                    logEvent("   → Vérifiez l'URL de base: " + ApiManager.getBaseUrl());
                    logEvent("   → Endpoint attendu: /api/project-notes.php");
                    break;
                case 408:
                    errorMessage += " - Timeout requête (Request Timeout)";
                    errorCountTimeout++;
                    logEvent("⏱️ ERREUR 408: Le serveur n'a pas répondu à temps");
                    break;
                case 500:
                    errorMessage += " - Erreur serveur interne (Internal Server Error)";
                    errorCountServer++;
                    logEvent("💥 ERREUR 500: Erreur côté serveur (PHP, base de données, etc.)");
                    logEvent("   → Vérifiez les logs du serveur");
                    break;
                case 502:
                    errorMessage += " - Bad Gateway";
                    errorCountServer++;
                    logEvent("🚧 ERREUR 502: Problème de proxy ou serveur intermédiaire");
                    break;
                case 503:
                    errorMessage += " - Service indisponible";
                    errorCountServer++;
                    logEvent("🔧 ERREUR 503: Le serveur est temporairement indisponible");
                    logEvent("   → Serveur en maintenance ou surchargé");
                    break;
                case 504:
                    errorMessage += " - Gateway Timeout";
                    errorCountServer++;
                    errorCountTimeout++;
                    logEvent("⏰ ERREUR 504: Le serveur backend n'a pas répondu à temps");
                    break;
                default:
                    errorMessage += " - Erreur non standard";
                    logEvent("❌ ERREUR HTTP " + statusCode + " (code non standard)");
                    break;
            }

            // Afficher le corps de la réponse
            try {
                String responseBody = new String(error.networkResponse.data);
                logEvent("📄 CORPS DE LA RÉPONSE:");

                // Essayer de parser comme JSON pour un affichage plus lisible
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    logEvent("   success: " + jsonResponse.optBoolean("success", false));
                    logEvent("   message: " + jsonResponse.optString("message", "N/A"));
                    if (jsonResponse.has("error")) {
                        logEvent("   error: " + jsonResponse.optString("error"));
                    }
                    if (jsonResponse.has("details")) {
                        logEvent("   details: " + jsonResponse.optString("details"));
                    }
                } catch (JSONException je) {
                    // Pas du JSON, afficher brut
                    String preview = responseBody.substring(0, Math.min(500, responseBody.length()));
                    logEvent("   " + preview);
                    if (responseBody.length() > 500) {
                        logEvent("   ... (tronqué, " + responseBody.length() + " caractères au total)");
                    }
                }
            } catch (Exception e) {
                logEvent("   Impossible de lire le corps de la réponse: " + e.getMessage());
            }

            // Afficher les headers de réponse
            if (error.networkResponse.headers != null && !error.networkResponse.headers.isEmpty()) {
                logEvent("📋 HEADERS DE RÉPONSE:");
                for (Map.Entry<String, String> entry : error.networkResponse.headers.entrySet()) {
                    logEvent("   " + entry.getKey() + ": " + entry.getValue());
                }
            }

        } else if (error.getCause() != null) {
            // Erreur réseau (pas de réponse du serveur)
            errorType = "NETWORK";
            errorCountNetwork++;
            String causeMessage = error.getCause().getMessage();
            errorMessage = "❌ Erreur réseau: " + causeMessage;

            logEvent("🌐 ERREUR RÉSEAU (pas de réponse du serveur)");
            logEvent("   Cause: " + causeMessage);

            // Analyser le type d'erreur réseau
            if (causeMessage != null) {
                if (causeMessage.contains("Unable to resolve host")) {
                    logEvent("   → DNS: Impossible de résoudre le nom de domaine");
                    logEvent("   → Vérifiez l'URL du serveur");
                    logEvent("   → Vérifiez votre connexion internet");
                } else if (causeMessage.contains("Connection refused")) {
                    logEvent("   → Le serveur refuse la connexion");
                    logEvent("   → Vérifiez que le serveur est démarré");
                    logEvent("   → Vérifiez le port et l'URL");
                } else if (causeMessage.contains("Connection timed out")) {
                    errorCountTimeout++;
                    logEvent("   → Timeout: Le serveur ne répond pas");
                    logEvent("   → Serveur trop lent ou réseau lent");
                } else if (causeMessage.contains("SSL") || causeMessage.contains("Certificate")) {
                    logEvent("   → Problème de certificat SSL");
                    logEvent("   → Activez 'Ignorer SSL' dans les paramètres si certificat autosigné");
                } else if (causeMessage.contains("Network is unreachable")) {
                    logEvent("   → Réseau inaccessible");
                    logEvent("   → Vérifiez votre connexion WiFi/Mobile");
                }
            }

        } else {
            // Erreur inconnue
            errorType = "UNKNOWN";
            errorMessage = "❌ Erreur inconnue";

            logEvent("❓ ERREUR INCONNUE");
            logEvent("   toString: " + error.toString());
            logEvent("   class: " + error.getClass().getName());

            if (error.getMessage() != null) {
                logEvent("   message: " + error.getMessage());
            }
        }

        // Log de l'erreur avec type
        logEvent("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logEvent("TYPE D'ERREUR: " + errorType);
        logEvent("MESSAGE: " + errorMessage);
        logEvent("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Mettre à jour les statistiques
        updateErrorStats();

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Met à jour les statistiques d'erreurs
     */
    private void updateErrorStats() {
        int totalErrors = errorCountNetwork + errorCountHttp + errorCountAuth + errorCountServer + errorCountTimeout + errorCountJson;

        StringBuilder stats = new StringBuilder();
        stats.append("📊 STATISTIQUES DES ERREURS\n\n");
        stats.append("Total: ").append(totalErrors).append(" erreur(s)\n\n");

        if (totalErrors > 0) {
            if (errorCountNetwork > 0) {
                stats.append("🌐 Réseau: ").append(errorCountNetwork).append("\n");
            }
            if (errorCountHttp > 0) {
                stats.append("📡 HTTP: ").append(errorCountHttp).append("\n");
            }
            if (errorCountAuth > 0) {
                stats.append("🔒 Authentification: ").append(errorCountAuth).append("\n");
            }
            if (errorCountServer > 0) {
                stats.append("💥 Serveur: ").append(errorCountServer).append("\n");
            }
            if (errorCountTimeout > 0) {
                stats.append("⏱️ Timeout: ").append(errorCountTimeout).append("\n");
            }
            if (errorCountJson > 0) {
                stats.append("📄 JSON/Parsing: ").append(errorCountJson).append("\n");
            }

            // Suggestions basées sur les erreurs
            stats.append("\n💡 SUGGESTIONS:\n");
            if (errorCountAuth > 0) {
                stats.append("→ Reconnectez-vous\n");
            }
            if (errorCountTimeout > 0) {
                stats.append("→ Vérifiez la vitesse réseau\n");
            }
            if (errorCountServer > 0) {
                stats.append("→ Contactez l'administrateur\n");
            }
            if (errorCountNetwork > 0) {
                stats.append("→ Vérifiez connexion internet\n");
            }
        } else {
            stats.append("✅ Aucune erreur détectée");
        }

        if (tvErrorStats != null) {
            tvErrorStats.setText(stats.toString());
        }
    }

    /**
     * Ajoute un événement au journal
     */
    private void logEvent(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logLine = "[" + timestamp + "] " + message + "\n";

        errorLog.append(logLine);
        tvErrorLog.setText(errorLog.toString());

        Log.d(TAG, message);
    }

    /**
     * Efface le journal et réinitialise les compteurs
     */
    private void clearLogs() {
        errorLog = new StringBuilder();
        tvErrorLog.setText("Journal effacé\n");

        // Réinitialiser les compteurs
        errorCountNetwork = 0;
        errorCountHttp = 0;
        errorCountAuth = 0;
        errorCountServer = 0;
        errorCountTimeout = 0;
        errorCountJson = 0;

        // Mettre à jour l'affichage
        updateErrorStats();

        logEvent("=== JOURNAL EFFACÉ ===");
        logEvent("Compteurs d'erreurs réinitialisés");

        Toast.makeText(this, "Journal et compteurs effacés", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
