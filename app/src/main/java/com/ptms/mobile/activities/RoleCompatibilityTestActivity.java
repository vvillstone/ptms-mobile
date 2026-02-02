package com.ptms.mobile.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ptms.mobile.R;
import com.ptms.mobile.utils.RoleCompatibilityTester;
import com.ptms.mobile.auth.TokenManager;

/**
 * Activité de test pour vérifier la compatibilité des rôles
 * Permet de tester rapidement la communication Android-API après mise à jour des rôles
 */
public class RoleCompatibilityTestActivity extends AppCompatActivity {
    
    private static final String TAG = "RoleTestActivity";
    private static final String PREFS_NAME = "ptms_mobile_prefs";
    private static final String PREF_TOKEN = "auth_token";
    
    private TextView testResultsTextView;
    private ScrollView scrollView;
    private Button runTestsButton;
    private Button clearResultsButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_compatibility_test);
        
        // Initialiser les vues
        testResultsTextView = findViewById(R.id.test_results_text);
        scrollView = findViewById(R.id.scroll_view);
        runTestsButton = findViewById(R.id.run_tests_button);
        clearResultsButton = findViewById(R.id.clear_results_button);
        
        // Configuration des boutons
        runTestsButton.setOnClickListener(v -> runCompatibilityTests());
        clearResultsButton.setOnClickListener(v -> clearResults());
        
        // Charger les résultats précédents si disponibles
        loadPreviousResults();
    }
    
    /**
     * Exécuter les tests de compatibilité
     */
    private void runCompatibilityTests() {
        runTestsButton.setEnabled(false);
        runTestsButton.setText("Tests en cours...");
        
        String token = getStoredToken();
        if (token == null || token.isEmpty()) {
            appendResult("❌ ERREUR: Aucun token d'authentification trouvé");
            appendResult("   Veuillez vous connecter d'abord dans l'application");
            runTestsButton.setEnabled(true);
            runTestsButton.setText("Lancer les tests");
            return;
        }
        
        appendResult("🚀 Démarrage des tests de compatibilité des rôles...");
        appendResult("🕐 " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        appendResult("");
        
        RoleCompatibilityTester.quickTest(this, token, new RoleCompatibilityTester.TestResultCallback() {
            @Override
            public void onTestCompleted(boolean success, String message) {
                runOnUiThread(() -> {
                    if (success) {
                        appendResult("🎉 " + message);
                        appendResult("");
                        appendResult("✅ Tous les tests sont passés avec succès!");
                        appendResult("   L'application Android est compatible avec la nouvelle gestion des rôles PTMS.");
                    } else {
                        appendResult("❌ " + message);
                        appendResult("");
                        appendResult("🚨 Des problèmes de compatibilité ont été détectés.");
                        appendResult("   Vérifiez la configuration des rôles côté serveur.");
                    }
                    
                    appendResult("");
                    appendResult("🏁 Tests terminés à " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
                    appendResult("=" + "=".repeat(60));
                    
                    runTestsButton.setEnabled(true);
                    runTestsButton.setText("Relancer les tests");
                    
                    // Sauvegarder les résultats
                    saveResults();
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
     * Ajouter un résultat au TextView
     */
    private void appendResult(String message) {
        String currentText = testResultsTextView.getText().toString();
        String newText = currentText + message + "\n";
        testResultsTextView.setText(newText);
        
        // Faire défiler vers le bas
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        
        Log.d(TAG, "Test result: " + message);
    }
    
    /**
     * Effacer les résultats
     */
    private void clearResults() {
        testResultsTextView.setText("");
    }
    
    /**
     * Obtenir le token stocké via TokenManager
     */
    private String getStoredToken() {
        // ✅ CORRECTION: Utiliser TokenManager au lieu de SharedPreferences directement
        TokenManager tokenManager = TokenManager.getInstance(this);
        String token = tokenManager.getToken();

        // Afficher le diagnostic du token
        appendResult("");
        appendResult("📋 DIAGNOSTIC TOKEN:");
        appendResult(tokenManager.getDiagnosticInfo());
        appendResult("");

        return token;
    }
    
    /**
     * Sauvegarder les résultats
     */
    private void saveResults() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("test_results", testResultsTextView.getText().toString());
        editor.putLong("test_timestamp", System.currentTimeMillis());
        editor.apply();
    }
    
    /**
     * Charger les résultats précédents
     */
    private void loadPreviousResults() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedResults = prefs.getString("test_results", null);
        long timestamp = prefs.getLong("test_timestamp", 0);
        
        if (savedResults != null && !savedResults.isEmpty()) {
            testResultsTextView.setText(savedResults);
            
            if (timestamp > 0) {
                java.util.Date date = new java.util.Date(timestamp);
                appendResult("");
                appendResult("📅 Dernière exécution: " + java.text.DateFormat.getDateTimeInstance().format(date));
                appendResult("");
            }
        } else {
            appendResult("🧪 Tests de compatibilité des rôles PTMS");
            appendResult("");
            appendResult("Cette activité permet de tester la communication entre");
            appendResult("l'application Android et l'API PTMS après une mise à jour");
            appendResult("des rôles utilisateur.");
            appendResult("");
            appendResult("Cliquez sur 'Lancer les tests' pour commencer.");
            appendResult("");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveResults();
    }
}
