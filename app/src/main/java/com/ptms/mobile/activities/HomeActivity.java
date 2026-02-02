package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ptms.mobile.R;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.auth.AuthenticationManager;
import com.ptms.mobile.managers.OfflineModeManager;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.WorkType;
import com.ptms.mobile.services.AutoSyncService;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Import pour ReportsEnhancedActivity
import com.ptms.mobile.activities.TimeReportsActivity;

/**
 * Dashboard principal de l'employé
 */
public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnTimeEntry, btnReports, btnProfile, btnChat, btnNotes, btnStatistics, btnSyncManagement;
    private SharedPreferences prefs;
    private ApiService apiService;
    private String authToken;
    private AppUpdateManager appUpdateManager;
    private static final int REQUEST_CODE_IMMEDIATE_UPDATE = 1001;

    // Offline mode manager
    private OfflineModeManager offlineModeManager;
    private LinearLayout connectionStatusBar;
    private TextView tvConnectionStatus;
    private TextView tvPendingSync;
    private Button btnRetryConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_home);

            // Initialisation
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            apiService = ApiClient.getInstance(this).getApiService();
            authToken = prefs.getString("auth_token", null);

                    // Démarrer le service de synchronisation automatique maintenant que l'utilisateur est connecté
                    // TEMPORAIREMENT DÉSACTIVÉ POUR ÉVITER LE CRASH
                    // AutoSyncService.startService(this);

            // Vérifier l'authentification
            if (authToken == null) {
                Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AuthenticationActivity.class));
                finish();
                return;
            }

            // Configuration de la toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("PTMS - Dashboard");
                }
            }

            initViews();
            setupListeners();
            displayUserInfo();
            setupOfflineMode();
            loadDashboardData();
            checkForAppUpdate();

            // ✅ NOUVEAU: Informer l'utilisateur s'il est en mode offline
            checkAndNotifyOfflineMode();

        } catch (Exception e) {
            android.util.Log.e("DASHBOARD", "Erreur onCreate", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, AuthenticationActivity.class));
            finish();
        }
    }

    private void initViews() {
        try {
            tvWelcome = findViewById(R.id.tv_welcome);
            btnTimeEntry = findViewById(R.id.btn_time_entry);
            btnReports = findViewById(R.id.btn_reports);
            btnProfile = findViewById(R.id.btn_profile);
            btnChat = findViewById(R.id.btn_chat);
            btnNotes = findViewById(R.id.btn_notes);
            btnStatistics = findViewById(R.id.btn_statistics);
            btnSyncManagement = findViewById(R.id.btn_sync_management);
            // btnRoleTest masqué - déplacé vers page diagnostique
            // btnRoleTest = findViewById(R.id.btn_role_test);

            // Vues du bandeau de connexion
            connectionStatusBar = findViewById(R.id.connection_status_bar);
            tvConnectionStatus = findViewById(R.id.tv_connection_status);
            tvPendingSync = findViewById(R.id.tv_pending_sync);
            btnRetryConnection = findViewById(R.id.btn_retry_connection);

            android.util.Log.d("DASHBOARD", "Views initialisées");
        } catch (Exception e) {
            android.util.Log.e("DASHBOARD", "Erreur initViews", e);
        }
    }

    private void setupListeners() {
        try {
            if (btnTimeEntry != null) {
                btnTimeEntry.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ✅ CORRECTION Phase 1 Offline-First: Utiliser OfflineTimeEntryActivity (Local-First)
                        // OfflineTimeEntryActivity sauvegarde toujours en local d'abord, puis sync en arrière-plan
                        startActivity(new Intent(HomeActivity.this, TimeEntryActivity.class));
                    }
                });
            }

            if (btnReports != null) {
                btnReports.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(HomeActivity.this, TimeReportsActivity.class));
                    }
                });
            }

            if (btnProfile != null) {
                btnProfile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(HomeActivity.this, UserProfileActivity.class));
                    }
                });
            }

            if (btnChat != null) {
                btnChat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(HomeActivity.this, ConversationsActivity.class));
                    }
                });
            }

            if (btnNotes != null) {
                btnNotes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // INTERFACE COMPLÈTE DES NOTES (D002)
                        startActivity(new Intent(HomeActivity.this, NotesActivity.class));
                    }
                });
            }

            if (btnStatistics != null) {
                btnStatistics.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Ouvrir le dashboard statistiques avec widgets
                        startActivity(new Intent(HomeActivity.this, StatisticsActivity.class));
                    }
                });
            }

            if (btnSyncManagement != null) {
                btnSyncManagement.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Ouvrir l'activité de gestion de synchronisation
                        startActivity(new Intent(HomeActivity.this, SyncManagementActivity.class));
                    }
                });
            }

            // btnRoleTest masqué - fonctionnalité déplacée vers page diagnostique
            /*
            if (btnRoleTest != null) {
                btnRoleTest.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(HomeActivity.this, RoleCompatibilityTestActivity.class));
                    }
                });
            }
            */

            android.util.Log.d("DASHBOARD", "Listeners configurés");
        } catch (Exception e) {
            android.util.Log.e("DASHBOARD", "Erreur setupListeners", e);
        }
    }

    private void displayUserInfo() {
        try {
            // ✅ CORRECTION: Utiliser user_name (nouvelle clé) avec fallback sur employee_name (ancienne clé)
            String userName = prefs.getString("user_name", null);
            if (userName == null) {
                // Fallback sur l'ancienne clé pour compatibilité
                userName = prefs.getString("employee_name", "Utilisateur");
            }

            // ✅ AMÉLIORATION: Extraire le prénom (premier mot du nom complet)
            String firstName = userName;
            if (userName != null && userName.contains(" ")) {
                firstName = userName.split(" ")[0];
            }

            if (tvWelcome != null) {
                tvWelcome.setText("Bienvenue, " + firstName + " !");
            }
            android.util.Log.d("DASHBOARD", "Info utilisateur affichée: " + firstName);
        } catch (Exception e) {
            android.util.Log.e("DASHBOARD", "Erreur displayUserInfo", e);
            // Fallback en cas d'erreur
            if (tvWelcome != null) {
                tvWelcome.setText("Bienvenue !");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_diagnostic) {
            startActivity(new Intent(this, SystemDiagnosticsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, AppSettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        } else if (item.getItemId() == R.id.action_agenda) {
            startActivity(new Intent(this, TimelineActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // ✅ CORRIGÉ: Utiliser AuthenticationManager pour préserver les credentials offline
        AuthenticationManager authManager = AuthenticationManager.getInstance(this);
        authManager.logout(); // Supprime SEULEMENT le token, préserve les credentials offline

        // Rediriger vers la page de connexion
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Empêcher le retour vers la page de connexion
        moveTaskToBack(true);
    }

    /**
     * ✅ NOUVEAU: Vérifie et notifie l'utilisateur s'il est en mode offline
     */
    private void checkAndNotifyOfflineMode() {
        try {
            boolean isOnline = com.ptms.mobile.utils.NetworkUtils.isOnline(this);

            if (!isOnline) {
                // ✅ NOUVEAU: Afficher le bandeau de connexion offline avec icône
                if (connectionStatusBar != null && tvConnectionStatus != null) {
                    connectionStatusBar.setVisibility(View.VISIBLE);
                    connectionStatusBar.setBackgroundColor(0xFFF44336); // Rouge
                    tvConnectionStatus.setText("📱 Données Locales • Hors ligne");
                    if (btnRetryConnection != null) {
                        btnRetryConnection.setVisibility(View.VISIBLE);
                        btnRetryConnection.setText("Reconnecter");
                    }
                }

                // Vérifier que l'utilisateur a bien une authentification initiale
                com.ptms.mobile.auth.InitialAuthManager authManager =
                    new com.ptms.mobile.auth.InitialAuthManager(this);
                boolean hasInitialAuth = authManager.hasInitialAuthentication();

                String userName = prefs.getString("user_name", null);
                if (userName == null) {
                    userName = prefs.getString("employee_name", "Utilisateur");
                }

                // Extraire le prénom
                String firstName = userName;
                if (userName != null && userName.contains(" ")) {
                    firstName = userName.split(" ")[0];
                }

                if (hasInitialAuth) {
                    Toast.makeText(this,
                        "📵 Mode Hors Ligne\n\n" +
                        "Bonjour " + firstName + " !\n" +
                        "Vous êtes authentifié(e) et pouvez travailler hors ligne.\n\n" +
                        "✅ Vos données ont été téléchargées lors de votre dernière connexion\n" +
                        "✅ Vos saisies seront synchronisées à la prochaine connexion\n\n" +
                        "💡 Utilisez le bouton 'Reconnecter' pour vérifier la connexion",
                        Toast.LENGTH_LONG).show();

                    android.util.Log.d("DASHBOARD", "Mode offline - Utilisateur informé + bandeau affiché");
                } else {
                    Toast.makeText(this,
                        "⚠️ Mode Hors Ligne\n\nAucune authentification initiale détectée",
                        Toast.LENGTH_LONG).show();
                }
            } else {
                // ✅ Mode online: Afficher le bandeau vert avec icône
                if (connectionStatusBar != null && tvConnectionStatus != null) {
                    connectionStatusBar.setVisibility(View.VISIBLE);
                    connectionStatusBar.setBackgroundColor(0xFF4CAF50); // Vert
                    tvConnectionStatus.setText("☁️ Données Serveur • Connecté");
                    if (btnRetryConnection != null) {
                        btnRetryConnection.setVisibility(View.GONE);
                    }
                }
                android.util.Log.d("DASHBOARD", "Mode online - Bandeau affiché avec icône");
            }
        } catch (Exception e) {
            android.util.Log.e("DASHBOARD", "Erreur checkAndNotifyOfflineMode", e);
        }
    }

    /**
     * ✅ NOUVEAU: Tente une reconnexion manuelle au serveur
     */
    private void attemptReconnection() {
        android.util.Log.d("DASHBOARD", "🔄 Tentative de reconnexion manuelle...");
        Toast.makeText(this, "🔄 Vérification de la connexion...", Toast.LENGTH_SHORT).show();

        // 1. Vérifier d'abord si on a une connexion réseau
        boolean hasNetwork = com.ptms.mobile.utils.NetworkUtils.isOnline(this);

        if (!hasNetwork) {
            android.util.Log.d("DASHBOARD", "❌ Aucun réseau détecté");
            Toast.makeText(this,
                "❌ Aucune connexion réseau\n\n" +
                "Vérifiez votre WiFi ou vos données mobiles",
                Toast.LENGTH_LONG).show();
            return;
        }

        android.util.Log.d("DASHBOARD", "✓ Réseau détecté - Vérification du serveur PTMS...");

        // 2. Vérifier si le serveur PTMS est accessible
        com.ptms.mobile.utils.ServerHealthCheck.quickPing(this, (status, responseTime, message) -> {
            runOnUiThread(() -> {
                if (status == com.ptms.mobile.utils.ServerHealthCheck.ServerStatus.ONLINE ||
                    status == com.ptms.mobile.utils.ServerHealthCheck.ServerStatus.SLOW) {

                    android.util.Log.d("DASHBOARD", "✅ Serveur PTMS accessible (" + responseTime + "ms)");

                    // Serveur accessible - Proposer de recharger
                    Toast.makeText(this,
                        "✅ Connexion rétablie!\n\n" +
                        "Serveur accessible (" + responseTime + "ms)\n" +
                        "Redémarrage pour synchroniser les données...",
                        Toast.LENGTH_LONG).show();

                    // Redémarrer l'activité pour recharger en mode online
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }, 2000); // Délai de 2s pour que l'utilisateur voie le message

                } else {
                    android.util.Log.d("DASHBOARD", "⚠️ Réseau OK mais serveur PTMS inaccessible");
                    Toast.makeText(this,
                        "⚠️ Serveur inaccessible\n\n" +
                        message + "\n\n" +
                        "Vous restez en mode hors ligne",
                        Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Configure le gestionnaire de mode offline
     */
    private void setupOfflineMode() {
        try {
            android.util.Log.d("DASHBOARD", "Configuration du mode offline");

            // Initialiser le gestionnaire
            offlineModeManager = OfflineModeManager.getInstance(this);

            // Configurer le listener de retry
            if (btnRetryConnection != null) {
                btnRetryConnection.setOnClickListener(v -> {
                    android.util.Log.d("DASHBOARD", "🔄 Retry manuel demandé");
                    attemptReconnection();
                });
            }

            // Ajouter un listener pour les changements de mode
            offlineModeManager.addListener(new OfflineModeManager.ModeChangeListener() {
                @Override
                public void onModeChanged(OfflineModeManager.ConnectionMode oldMode,
                                        OfflineModeManager.ConnectionMode newMode, String reason) {
                    runOnUiThread(() -> updateConnectionStatus(newMode));
                }

                @Override
                public void onSyncStarted() {
                    runOnUiThread(() -> {
                        if (tvConnectionStatus != null) {
                            tvConnectionStatus.setText("🔄 Synchronisation...");
                        }
                    });
                }

                @Override
                public void onSyncProgress(String message) {
                }

                @Override
                public void onSyncCompleted(int syncedCount, int failedCount) {
                    runOnUiThread(() -> {
                        String message = "✅ " + syncedCount + " synchronisé" + (syncedCount > 1 ? "s" : "");
                        if (failedCount > 0) {
                            message += " (" + failedCount + " échec" + (failedCount > 1 ? "s" : "") + ")";
                        }
                        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onSyncError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeActivity.this, "❌ Erreur sync: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

            // Lancer la détection automatique
            offlineModeManager.detectConnectionMode((online, message) -> {
                runOnUiThread(() -> updateConnectionStatus(offlineModeManager.getCurrentMode()));
            });

            // Démarrer le monitoring continu
            offlineModeManager.startMonitoring();

            android.util.Log.d("DASHBOARD", "Mode offline configuré");
        } catch (Exception e) {
            android.util.Log.e("DASHBOARD", "Erreur setupOfflineMode", e);
        }
    }

    /**
     * Met à jour l'affichage du statut de connexion
     */
    private void updateConnectionStatus(OfflineModeManager.ConnectionMode mode) {
        try {
            if (connectionStatusBar == null || tvConnectionStatus == null) {
                return;
            }

            // ✅ CORRECTION: S'assurer que le bandeau est toujours visible
            connectionStatusBar.setVisibility(View.VISIBLE);

            int pendingCount = offlineModeManager.getPendingSyncCount();

            switch (mode) {
                case ONLINE:
                    connectionStatusBar.setBackgroundColor(0xFF4CAF50); // Vert
                    tvConnectionStatus.setText("☁️ Données Serveur • Connecté");
                    if (btnRetryConnection != null) {
                        btnRetryConnection.setVisibility(View.GONE);
                    }
                    if (tvPendingSync != null) {
                        tvPendingSync.setVisibility(View.GONE);
                    }
                    break;

                case OFFLINE:
                    connectionStatusBar.setBackgroundColor(0xFFF44336); // Rouge
                    if (pendingCount > 0) {
                        tvConnectionStatus.setText("📱 Données Locales • Hors ligne");
                        if (tvPendingSync != null) {
                            tvPendingSync.setText(pendingCount + " à sync");
                            tvPendingSync.setVisibility(View.VISIBLE);
                        }
                    } else {
                        tvConnectionStatus.setText("📱 Données Locales • Hors ligne");
                        if (tvPendingSync != null) {
                            tvPendingSync.setVisibility(View.GONE);
                        }
                    }
                    if (btnRetryConnection != null) {
                        btnRetryConnection.setVisibility(View.VISIBLE);
                    }
                    break;

                case SYNCING:
                    connectionStatusBar.setBackgroundColor(0xFF2196F3); // Bleu
                    tvConnectionStatus.setText("🔄 Synchronisation Serveur ↔ Local...");
                    if (btnRetryConnection != null) {
                        btnRetryConnection.setVisibility(View.GONE);
                    }
                    if (tvPendingSync != null) {
                        if (pendingCount > 0) {
                            tvPendingSync.setText(pendingCount + " restant" + (pendingCount > 1 ? "s" : ""));
                            tvPendingSync.setVisibility(View.VISIBLE);
                        } else {
                            tvPendingSync.setVisibility(View.GONE);
                        }
                    }
                    break;

                case UNKNOWN:
                default:
                    connectionStatusBar.setBackgroundColor(0xFF9E9E9E); // Gris
                    tvConnectionStatus.setText("❔ Vérification...");
                    if (btnRetryConnection != null) {
                        btnRetryConnection.setVisibility(View.GONE);
                    }
                    if (tvPendingSync != null) {
                        tvPendingSync.setVisibility(View.GONE);
                    }
                    break;
            }
        } catch (Exception e) {
            android.util.Log.e("DASHBOARD", "Erreur updateConnectionStatus", e);
        }
    }
    
    private void loadDashboardData() {
        if (apiService == null || authToken == null) {
            return;
        }

        // ✅ CORRECTION: Ne charger depuis le serveur QUE si on est en ligne
        boolean isOnline = com.ptms.mobile.utils.NetworkUtils.isOnline(this);
        android.util.Log.d("DASHBOARD", "loadDashboardData - Mode: " + (isOnline ? "ONLINE" : "OFFLINE"));

        if (!isOnline) {
            android.util.Log.d("DASHBOARD", "Mode offline - Pas de chargement depuis le serveur (données en cache local)");
            return; // Les données sont déjà en cache local, pas besoin de charger depuis le serveur
        }

        // Mode ONLINE - Charger les projets
        Call<ApiService.ProjectsResponse> projectsCall = apiService.getProjects(authToken);
        projectsCall.enqueue(new Callback<ApiService.ProjectsResponse>() {
            @Override
            public void onResponse(Call<ApiService.ProjectsResponse> call, Response<ApiService.ProjectsResponse> response) {
                // Projets chargés
                android.util.Log.d("DASHBOARD", "Projets chargés depuis le serveur");
            }

            @Override
            public void onFailure(Call<ApiService.ProjectsResponse> call, Throwable t) {
                android.util.Log.e("DASHBOARD", "Échec chargement projets", t);
            }
        });

        // Mode ONLINE - Charger les types de travail
        Call<List<WorkType>> workTypesCall = apiService.getWorkTypes(authToken);
        workTypesCall.enqueue(new Callback<List<WorkType>>() {
            @Override
            public void onResponse(Call<List<WorkType>> call, Response<List<WorkType>> response) {
                // Types de travail chargés
                android.util.Log.d("DASHBOARD", "Types de travail chargés depuis le serveur");
            }

            @Override
            public void onFailure(Call<List<WorkType>> call, Throwable t) {
                android.util.Log.e("DASHBOARD", "Échec chargement types de travail", t);
            }
        });
    }

    private void checkForAppUpdate() {
        try {
            // ✅ CORRECTION: Ne vérifier les mises à jour QUE si on est en ligne
            boolean isOnline = com.ptms.mobile.utils.NetworkUtils.isOnline(this);
            if (!isOnline) {
                android.util.Log.d("DASHBOARD", "Mode offline - Pas de vérification des mises à jour");
                return;
            }

            appUpdateManager = AppUpdateManagerFactory.create(this);
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                REQUEST_CODE_IMMEDIATE_UPDATE
                        );
                    } catch (Exception e) {
                        android.util.Log.e("DASHBOARD", "Erreur démarrage in-app update", e);
                    }
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // Reprendre une mise à jour interrompue
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                REQUEST_CODE_IMMEDIATE_UPDATE
                        );
                    } catch (Exception e) {
                        android.util.Log.e("DASHBOARD", "Erreur reprise in-app update", e);
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("DASHBOARD", "Erreur checkForAppUpdate", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMMEDIATE_UPDATE) {
            if (resultCode != RESULT_OK) {
                android.util.Log.w("DASHBOARD", "Mise à jour annulée ou échouée, code=" + resultCode);
                // Optionnel: relancer le check
                // checkForAppUpdate();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appUpdateManager != null) {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                REQUEST_CODE_IMMEDIATE_UPDATE
                        );
                    } catch (Exception e) {
                        android.util.Log.e("DASHBOARD", "Erreur reprise onResume", e);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrêter le monitoring lors de la destruction de l'activité
        if (offlineModeManager != null) {
            offlineModeManager.stopMonitoring();
        }
    }
}
