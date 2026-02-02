package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ptms.mobile.R;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.auth.InitialAuthManager;
import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.models.Employee;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.WorkType;
import com.ptms.mobile.utils.NetworkUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Écran de chargement après authentification
 * Télécharge toutes les données nécessaires avant d'ouvrir le Dashboard
 * Supporte le mode offline
 */
public class AppLoadingActivity extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";

    private ProgressBar progressBar;
    private TextView tvLoadingStatus;
    private TextView tvProgress;
    private TextView tvOfflineMode;
    private Button btnRetry;
    private Button btnContinueOffline;

    private SharedPreferences prefs;
    private ApiService apiService;
    private String authToken;
    private int currentProgress = 0;
    private boolean isOfflineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_loading);

        // Initialisation
        prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
        apiService = ApiClient.getInstance(this).getApiService();
        authToken = prefs.getString("auth_token", null);

        // Vérifier le token
        if (authToken == null) {
            Toast.makeText(this, "Erreur: Token manquant", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        // Init views
        progressBar = findViewById(R.id.progress_bar);
        tvLoadingStatus = findViewById(R.id.tv_loading_status);
        tvProgress = findViewById(R.id.tv_progress);
        tvOfflineMode = findViewById(R.id.tv_offline_mode);
        btnRetry = findViewById(R.id.btn_retry);
        btnContinueOffline = findViewById(R.id.btn_continue_offline);

        // Boutons
        btnRetry.setOnClickListener(v -> startDataLoading());
        btnContinueOffline.setOnClickListener(v -> goToDashboard());

        // Vérifier la connectivité
        if (!NetworkUtils.isOnline(this)) {
            Log.d(TAG, "Mode hors ligne détecté");
            showOfflineMode();
        } else {
            // Démarrer le chargement des données
            startDataLoading();
        }
    }

    /**
     * Affiche le mode offline et permet de continuer
     */
    private void showOfflineMode() {
        isOfflineMode = true;
        tvLoadingStatus.setText("Mode hors ligne");
        tvOfflineMode.setVisibility(View.VISIBLE);
        progressBar.setProgress(100);
        tvProgress.setText("100%");

        // Attendre 1 seconde puis aller au dashboard automatiquement
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            goToDashboard();
        }, 1000);
    }

    /**
     * Démarre le chargement des données (mode online)
     */
    private void startDataLoading() {
        btnRetry.setVisibility(View.GONE);
        btnContinueOffline.setVisibility(View.GONE);
        currentProgress = 0;
        updateProgress(0, "Démarrage...");

        // Étape 1: Charger le profil complet (33%)
        loadProfile();
    }

    /**
     * Étape 1: Charge le profil complet
     */
    private void loadProfile() {
        updateProgress(10, "Chargement du profil...");

        Call<Employee> call = apiService.getProfile(authToken);
        call.enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Employee employee = response.body();
                    Log.d(TAG, "✅ Profil chargé: " + employee.getFullName());

                    // Sauvegarder le profil complet
                    saveProfileData(employee);

                    updateProgress(33, "Profil chargé ✓");

                    // Passer à l'étape suivante
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        loadProjects();
                    }, 500);
                } else {
                    Log.e(TAG, "Erreur chargement profil: " + response.code());
                    handleError("Erreur chargement du profil");
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                Log.e(TAG, "Échec chargement profil", t);
                handleError("Erreur réseau: " + t.getMessage());
            }
        });
    }

    /**
     * Étape 2: Charge les projets (66%)
     */
    private void loadProjects() {
        updateProgress(40, "Chargement des projets...");

        apiService.getProjects(authToken).enqueue(new Callback<ApiService.ProjectsResponse>() {
            @Override
            public void onResponse(Call<ApiService.ProjectsResponse> call, Response<ApiService.ProjectsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<Project> projects = response.body().projects;
                    Log.d(TAG, "✅ " + projects.size() + " projets chargés");

                    // Sauvegarder en cache local
                    saveProjectsToCache(projects);

                    updateProgress(66, "Projets chargés ✓");

                    // Passer à l'étape suivante
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        loadWorkTypes();
                    }, 500);
                } else {
                    Log.e(TAG, "Erreur chargement projets");
                    handleError("Erreur chargement des projets");
                }
            }

            @Override
            public void onFailure(Call<ApiService.ProjectsResponse> call, Throwable t) {
                Log.e(TAG, "Échec chargement projets", t);
                handleError("Erreur réseau: " + t.getMessage());
            }
        });
    }

    /**
     * Étape 3: Charge les types de travail (100%)
     */
    private void loadWorkTypes() {
        updateProgress(75, "Chargement des types de travail...");

        apiService.getWorkTypes(authToken).enqueue(new Callback<List<WorkType>>() {
            @Override
            public void onResponse(Call<List<WorkType>> call, Response<List<WorkType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WorkType> workTypes = response.body();
                    Log.d(TAG, "✅ " + workTypes.size() + " types de travail chargés");

                    // Sauvegarder en cache local
                    saveWorkTypesToCache(workTypes);

                    // ✅ CORRECTION: Marquer l'authentification initiale comme complète
                    markInitialAuthenticationComplete(workTypes.size());

                    updateProgress(100, "Données chargées ✓");

                    // Attendre 500ms puis aller au dashboard
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        goToDashboard();
                    }, 500);
                } else {
                    Log.e(TAG, "Erreur chargement types de travail");
                    handleError("Erreur chargement des types de travail");
                }
            }

            @Override
            public void onFailure(Call<List<WorkType>> call, Throwable t) {
                Log.e(TAG, "Échec chargement types de travail", t);
                handleError("Erreur réseau: " + t.getMessage());
            }
        });
    }

    /**
     * Sauvegarde les données du profil
     */
    private void saveProfileData(Employee employee) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("user_id", employee.getId());
        editor.putString("user_name", employee.getFullName());
        editor.putString("user_email", employee.getEmail());
        editor.putInt("user_type", employee.getType());
        editor.putString("user_department", employee.getDepartment() != null ? employee.getDepartment() : "");
        editor.putString("user_position", employee.getPosition() != null ? employee.getPosition() : "");
        editor.putString("user_employee_status", employee.getEmployeeStatusText() != null ? employee.getEmployeeStatusText() : "");
        editor.putBoolean("user_is_active", employee.isActive());
        editor.commit();
        Log.d(TAG, "Profil sauvegardé");
    }

    /**
     * Sauvegarde les projets en cache
     */
    private void saveProjectsToCache(List<Project> projects) {
        try {
            OfflineDatabaseHelper dbHelper = new OfflineDatabaseHelper(this);
            dbHelper.clearProjects();
            for (Project project : projects) {
                dbHelper.insertProject(project);
            }
            Log.d(TAG, projects.size() + " projets sauvegardés en cache");
        } catch (Exception e) {
            Log.e(TAG, "Erreur sauvegarde projets", e);
        }
    }

    /**
     * Sauvegarde les types de travail en cache
     */
    private void saveWorkTypesToCache(List<WorkType> workTypes) {
        try {
            OfflineDatabaseHelper dbHelper = new OfflineDatabaseHelper(this);
            dbHelper.clearWorkTypes();
            for (WorkType workType : workTypes) {
                dbHelper.insertWorkType(workType);
            }
            Log.d(TAG, workTypes.size() + " types de travail sauvegardés en cache");
        } catch (Exception e) {
            Log.e(TAG, "Erreur sauvegarde types de travail", e);
        }
    }

    /**
     * Met à jour la barre de progression
     */
    private void updateProgress(int progress, String status) {
        runOnUiThread(() -> {
            currentProgress = progress;
            progressBar.setProgress(progress);
            tvProgress.setText(progress + "%");
            tvLoadingStatus.setText(status);
        });
    }

    /**
     * Gère les erreurs de chargement
     */
    private void handleError(String message) {
        runOnUiThread(() -> {
            tvLoadingStatus.setText(message);
            btnRetry.setVisibility(View.VISIBLE);
            btnContinueOffline.setVisibility(View.VISIBLE);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Redirige vers le Dashboard
     */
    private void goToDashboard() {
        Log.d(TAG, "🚀 Redirection vers le Dashboard");
        Intent intent = new Intent(AppLoadingActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Redirige vers le Login
     */
    private void goToLogin() {
        Intent intent = new Intent(AppLoadingActivity.this, AuthenticationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Marque l'authentification initiale comme complète
     * Sauvegarde l'information que l'utilisateur a téléchargé les données nécessaires pour le mode offline
     */
    private void markInitialAuthenticationComplete(int workTypesCount) {
        try {
            // Récupérer le nombre de projets depuis la base locale
            OfflineDatabaseHelper dbHelper = new OfflineDatabaseHelper(this);
            int projectsCount = dbHelper.getProjectCount();

            // Récupérer l'email de l'utilisateur
            String userEmail = prefs.getString("user_email", "");

            // Créer l'instance de InitialAuthManager
            InitialAuthManager authManager = new InitialAuthManager(this);

            // Marquer l'authentification initiale comme réussie
            // On utilise la méthode privée via réflexion ou on crée une méthode publique
            // Pour l'instant, on va sauvegarder directement dans les SharedPreferences
            SharedPreferences initialAuthPrefs = getSharedPreferences("initial_auth_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = initialAuthPrefs.edit();
            editor.putBoolean("has_initial_auth", true);
            editor.putLong("auth_date", System.currentTimeMillis());
            editor.putString("user_email", userEmail);
            editor.putLong("data_cache_date", System.currentTimeMillis());
            editor.putInt("projects_count", projectsCount);
            editor.putInt("work_types_count", workTypesCount);
            editor.commit();

            Log.d(TAG, "✅ Authentification initiale marquée comme complète:");
            Log.d(TAG, "   - Projets: " + projectsCount);
            Log.d(TAG, "   - Types de travail: " + workTypesCount);
            Log.d(TAG, "   - Email: " + userEmail);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du marquage de l'authentification initiale", e);
            // Ne pas bloquer le flux si cette opération échoue
        }
    }

    @Override
    public void onBackPressed() {
        // Empêcher le retour arrière pendant le chargement
        // L'utilisateur peut utiliser les boutons si erreur
    }
}
