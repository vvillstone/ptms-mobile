package com.ptms.mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ptms.mobile.R;
import com.ptms.mobile.auth.InitialAuthManager;

/**
 * Activité d'authentification initiale obligatoire
 * L'utilisateur doit se connecter au moins une fois pour valider ses identifiants
 * et télécharger les données de référence nécessaires pour le mode hors ligne
 */
public class FirstLaunchAuthActivity extends AppCompatActivity {
    
    private EditText etEmail, etPassword;
    private Button btnConnect;
    private ProgressBar progressBar;
    private TextView tvInfo, tvStatus;
    private InitialAuthManager authManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_launch_auth);
        
        // Configuration de la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Authentification Initiale");
        }
        
        // Initialisation
        authManager = new InitialAuthManager(this);
        
        initViews();
        setupListeners();
        checkInitialAuthStatus();
    }
    
    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnConnect = findViewById(R.id.btn_connect);
        progressBar = findViewById(R.id.progress_bar);
        tvInfo = findViewById(R.id.tv_info);
        tvStatus = findViewById(R.id.tv_status);
        
        // Masquer la barre de progression au début
        progressBar.setVisibility(View.GONE);
    }
    
    private void setupListeners() {
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performInitialAuthentication();
            }
        });
    }
    
    private void checkInitialAuthStatus() {
        InitialAuthManager.InitialAuthInfo info = authManager.getInitialAuthInfo();
        
        if (info.hasAuth) {
            tvInfo.setText("✅ Authentification initiale déjà effectuée");
            tvStatus.setText("Email: " + info.userEmail + "\n" +
                           "Date: " + info.getAuthDateString() + "\n" +
                           "Projets: " + info.projectsCount + "\n" +
                           "Types de travail: " + info.workTypesCount);
            
            // Permettre de se reconnecter si les données sont anciennes
            if (!info.isDataFresh()) {
                tvStatus.append("\n⚠️ Données anciennes - Reconnexion recommandée");
            }
        } else {
            tvInfo.setText("🔐 Authentification initiale requise");
            tvStatus.setText("Vous devez vous connecter au moins une fois pour:\n" +
                           "• Valider vos identifiants\n" +
                           "• Télécharger les projets disponibles\n" +
                           "• Télécharger les types de travail\n" +
                           "• Activer le mode hors ligne");
        }
    }
    
    private void performInitialAuthentication() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // Validation des champs
        if (email.isEmpty()) {
            etEmail.setError("Email requis");
            etEmail.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            etPassword.setError("Mot de passe requis");
            etPassword.requestFocus();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format d'email invalide");
            etEmail.requestFocus();
            return;
        }
        
        // Démarrer l'authentification
        setLoading(true);
        tvStatus.setText("🔄 Connexion en cours...");
        
        authManager.performInitialAuthentication(email, password, new InitialAuthManager.InitialAuthCallback() {
            @Override
            public void onInitialAuthSuccess(int projectsCount, int workTypesCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        tvStatus.setText("✅ Authentification réussie !\n" +
                                       "Projets téléchargés: " + projectsCount + "\n" +
                                       "Types de travail: " + workTypesCount + "\n\n" +
                                       "Vous pouvez maintenant utiliser l'application hors ligne.");

                        Toast.makeText(FirstLaunchAuthActivity.this,
                            "Authentification initiale réussie !", Toast.LENGTH_LONG).show();

                        // Rediriger vers l'application principale après un délai
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startMainApplication();
                            }
                        }, 2000);
                    }
                });
            }

            @Override
            public void onInitialAuthError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        tvStatus.setText("❌ Erreur d'authentification: " + error);

                        Toast.makeText(FirstLaunchAuthActivity.this,
                            "Erreur: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onProgress(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText(message);  // ✅ Mettre à jour le statut avec le message de progression
                    }
                });
            }
        });
    }
    
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnConnect.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        
        if (loading) {
            btnConnect.setText("Connexion...");
        } else {
            btnConnect.setText("Se connecter");
        }
    }
    
    private void startMainApplication() {
        // Rediriger vers l'écran de connexion normal
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // Empêcher de revenir en arrière sans authentification
        Toast.makeText(this, "Authentification initiale obligatoire", Toast.LENGTH_SHORT).show();
    }
}
