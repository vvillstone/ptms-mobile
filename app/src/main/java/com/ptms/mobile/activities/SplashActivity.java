package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ptms.mobile.R;
import com.ptms.mobile.utils.SettingsManager;
import com.ptms.mobile.utils.ServerHealthCheck;
import com.ptms.mobile.services.AutoSyncService;
import com.ptms.mobile.auth.InitialAuthManager;

/**
 * Activité principale - Point d'entrée de l'application
 * Avec détection automatique de connexion
 */
public class SplashActivity extends AppCompatActivity {

    private Button btnLogin;
    private Button btnResetSettings;
    private Button btnRetryPing;
    private SharedPreferences prefs;
    private SettingsManager settingsManager;
    private InitialAuthManager initialAuthManager;

    // Vues pour l'indicateur de connexion
    private LinearLayout connectionIndicator;
    private ProgressBar progressIndicator;
    private TextView tvConnectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_splash);

            // Initialisation sécurisée
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            settingsManager = new SettingsManager(this);

            // ========================================
            // AUTHENTIFICATION INITIALE OBLIGATOIRE
            // ========================================
            try {
                initialAuthManager = new InitialAuthManager(this);

                // Vérifier si l'utilisateur a déjà effectué l'auth initiale
                if (!initialAuthManager.hasInitialAuthentication()) {
                    android.util.Log.d("MainActivity", "⚠️ Authentification initiale requise - Redirection vers InitialAuthActivity");

                    // ✅ FIX: Anti-loop protection - éviter les redirections infinies
                    long lastRedirect = prefs.getLong("last_first_launch_redirect", 0);
                    int redirectCount = prefs.getInt("first_launch_redirect_count", 0);
                    long now = System.currentTimeMillis();

                    // Réinitialiser le compteur si plus de 30 secondes depuis la dernière redirection
                    if (now - lastRedirect > 30000) {
                        redirectCount = 0;
                    }

                    // Si plus de 5 redirections en 30 secondes, c'est une boucle
                    if (redirectCount > 5) {
                        android.util.Log.e("MainActivity", "❌ BOUCLE DÉTECTÉE - Arrêt des redirections");
                        Toast.makeText(this,
                            "❌ Erreur de démarrage\n\n" +
                            "L'application ne peut pas démarrer correctement.\n" +
                            "Réinstallez l'application ou contactez le support.",
                            Toast.LENGTH_LONG).show();

                        // Réinitialiser le compteur et ne pas rediriger
                        prefs.edit()
                            .putInt("first_launch_redirect_count", 0)
                            .apply();
                        return;
                    }

                    // Sauvegarder le compteur
                    prefs.edit()
                        .putLong("last_first_launch_redirect", now)
                        .putInt("first_launch_redirect_count", redirectCount + 1)
                        .apply();

                    // Rediriger vers l'authentification initiale
                    startActivity(new Intent(this, FirstLaunchAuthActivity.class));
                    finish();
                    return;
                } else {
                    android.util.Log.d("MainActivity", "✅ Authentification initiale validée");

                    // Vérifier si les données sont fraîches
                    if (!initialAuthManager.hasValidDataCache()) {
                        android.util.Log.w("MainActivity", "⚠️ Cache de données expiré - Synchronisation recommandée");
                        Toast.makeText(this, "⚠️ Vos données sont anciennes - Synchronisation recommandée", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Erreur vérification auth initiale", e);
                // En cas d'erreur critique, permettre l'accès pour éviter le blocage total
                android.util.Log.w("MainActivity", "⚠️ Erreur auth initiale - Continuation avec précaution");
                Toast.makeText(this, "⚠️ Erreur vérification authentification", Toast.LENGTH_SHORT).show();
            }

            // ========================================
            // SERVICE DE SYNCHRONISATION AUTOMATIQUE
            // ========================================
            if (isUserLoggedIn()) {
                try {
                    android.util.Log.d("MainActivity", "Démarrage du service de synchronisation automatique");
                    startAutoSyncService();
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Erreur démarrage AutoSyncService - Continuation sans sync auto", e);
                    // Ne pas bloquer l'application si le service échoue
                }
            }
            
            // Initialiser les vues de l'indicateur de connexion
            connectionIndicator = findViewById(R.id.connection_indicator);
            progressIndicator = findViewById(R.id.progress_indicator);
            tvConnectionStatus = findViewById(R.id.tv_connection_status);
            btnRetryPing = findViewById(R.id.btn_retry_ping);

            // Lancer le test de connexion au démarrage
            performStartupPing();

            // Configurer le bouton de réessai
            if (btnRetryPing != null) {
                btnRetryPing.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        performStartupPing();
                    }
                });
            }

            // Vérifier si l'utilisateur est déjà connecté
            if (isUserLoggedIn()) {
                // Rediriger vers le dashboard
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return;
            }

            // Configuration des boutons avec vérification
            btnLogin = findViewById(R.id.btn_login);
            if (btnLogin != null) {
                btnLogin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(SplashActivity.this, AuthenticationActivity.class));
                    }
                });
            }

            btnResetSettings = findViewById(R.id.btn_reset_settings);
            if (btnResetSettings != null) {
                btnResetSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            // Réinitialiser les paramètres
                            settingsManager.resetToDefaults();
                            Toast.makeText(SplashActivity.this, "Paramètres réinitialisés !", Toast.LENGTH_SHORT).show();

                            // Afficher l'URL actuelle
                            String currentUrl = settingsManager.getServerUrl();
                            Toast.makeText(SplashActivity.this, "URL: " + currentUrl, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(SplashActivity.this, "Erreur reset: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            // Afficher l'URL du serveur pour debug
            try {
                String currentUrl = settingsManager.getServerUrl();
                Toast.makeText(this, "Serveur: " + currentUrl, Toast.LENGTH_LONG).show();
                
                // Test de l'URL complète
                String testUrl = currentUrl + "login.php";
                Toast.makeText(this, "Test URL: " + testUrl, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Erreur config: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            
        } catch (Exception e) {
            // En cas d'erreur, rediriger directement vers LoginActivity
            Toast.makeText(this, "Erreur initialisation: " + e.getMessage(), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, AuthenticationActivity.class));
            finish();
        }
    }

    /**
     * Vérifie si l'utilisateur est connecté
     */
    private boolean isUserLoggedIn() {
        String token = prefs.getString("auth_token", null);
        return token != null && !token.isEmpty();
    }
    
    /**
     * Démarrer le service de synchronisation automatique
     */
    private void startAutoSyncService() {
        try {
            android.util.Log.d("MainActivity", "Démarrage du service de synchronisation automatique");
            AutoSyncService.startService(this);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Erreur démarrage service sync", e);
        }
    }

    /**
     * Test de connexion au démarrage
     */
    private void performStartupPing() {
        if (connectionIndicator == null || progressIndicator == null ||
            tvConnectionStatus == null || btnRetryPing == null) {
            android.util.Log.w("MainActivity", "Les vues de l'indicateur de connexion ne sont pas initialisées");
            return;
        }

        // Afficher le chargement
        progressIndicator.setVisibility(View.VISIBLE);
        btnRetryPing.setVisibility(View.GONE);
        tvConnectionStatus.setText("Vérification de la connexion...");
        connectionIndicator.setBackgroundColor(0xFF9E9E9E); // Gris

        android.util.Log.d("MainActivity", "Démarrage du ping de démarrage...");

        // Lancer le ping rapide (3 secondes)
        ServerHealthCheck.quickPing(this, new ServerHealthCheck.HealthCheckCallback() {
            @Override
            public void onHealthCheckComplete(ServerHealthCheck.ServerStatus status, long responseTime, String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressIndicator.setVisibility(View.GONE);

                        switch (status) {
                            case ONLINE:
                                // Serveur accessible - Vert
                                connectionIndicator.setBackgroundColor(0xFF4CAF50); // Vert
                                tvConnectionStatus.setText("✅ Serveur accessible (" + responseTime + "ms)");
                                btnRetryPing.setVisibility(View.GONE);
                                android.util.Log.d("MainActivity", "Serveur ONLINE: " + responseTime + "ms");
                                break;

                            case SLOW:
                                // Serveur lent - Orange
                                connectionIndicator.setBackgroundColor(0xFFFF9800); // Orange
                                tvConnectionStatus.setText("⚠️ Serveur lent (" + responseTime + "ms)");
                                btnRetryPing.setVisibility(View.VISIBLE);
                                android.util.Log.d("MainActivity", "Serveur SLOW: " + responseTime + "ms");
                                break;

                            case OFFLINE:
                            case ERROR:
                            default:
                                // Serveur inaccessible - Rouge
                                connectionIndicator.setBackgroundColor(0xFFF44336); // Rouge
                                tvConnectionStatus.setText("❌ " + message);
                                btnRetryPing.setVisibility(View.VISIBLE);
                                android.util.Log.d("MainActivity", "Serveur OFFLINE/ERROR: " + message);
                                break;
                        }
                    }
                });
            }
        });
    }
}
