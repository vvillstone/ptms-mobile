package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ptms.mobile.R;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.auth.AuthenticationManager;
import com.ptms.mobile.models.Employee;
import com.ptms.mobile.utils.SettingsManager;
import com.ptms.mobile.auth.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Activité du profil employé
 */
public class UserProfileActivity extends AppCompatActivity {

    private TextView tvAvatar, tvFullName, tvEmailHeader;
    private TextView tvName, tvEmail, tvDepartment, tvPosition, tvEmployeeStatus, tvStatus;
    private TextView tvReportCount, tvTotalHours, tvMemberSince;
    private Button btnLogout;
    private ProgressBar progressBar;
    private SharedPreferences prefs;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_user_profile);

            // Initialisation
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            
            // Configuration de la toolbar avec vérification
            try {
                Toolbar toolbar = findViewById(R.id.toolbar);
                if (toolbar != null) {
                    setSupportActionBar(toolbar);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Mon Profil");
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("PROFILE", "Erreur configuration toolbar", e);
            }

            initViews();
            setupApiService();
            setupListeners();

            // Afficher les données en cache
            displayCachedProfile();

            android.util.Log.d("PROFILE", "Profil affiché depuis le cache - Utilisez 'Actualiser' pour synchroniser");
            
        } catch (Exception e) {
            android.util.Log.e("PROFILE", "Erreur dans onCreate", e);
            Toast.makeText(this, "Erreur initialisation profil: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        // Header views
        tvAvatar = findViewById(R.id.tv_avatar);
        tvFullName = findViewById(R.id.tv_full_name);
        tvEmailHeader = findViewById(R.id.tv_email_header);

        // Information views
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvDepartment = findViewById(R.id.tv_department);
        tvPosition = findViewById(R.id.tv_position);
        tvEmployeeStatus = findViewById(R.id.tv_employee_status);
        tvStatus = findViewById(R.id.tv_status);

        // Statistics views
        tvReportCount = findViewById(R.id.tv_report_count);
        tvTotalHours = findViewById(R.id.tv_total_hours);
        tvMemberSince = findViewById(R.id.tv_member_since);

        // Action buttons
        btnLogout = findViewById(R.id.btn_logout);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupApiService() {
        try {
            ApiClient apiClient = ApiClient.getInstance(this);
            apiService = apiClient.getApiService();
            android.util.Log.d("PROFILE", "API Service configuré avec SSL ignoré: " + apiClient.getBaseUrl());
        } catch (Exception e) {
            android.util.Log.e("PROFILE", "Erreur setupApiService", e);
            Toast.makeText(this, "Erreur configuration API: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    private void loadProfile() {
        try {
            setLoading(true);

            // Utiliser TokenManager pour obtenir le token
            TokenManager tokenManager = TokenManager.getInstance(this);
            String token = tokenManager.getToken();

            if (token == null || token.isEmpty()) {
                setLoading(false);
                Toast.makeText(this, "Session expirée - Veuillez vous reconnecter", Toast.LENGTH_LONG).show();
                displayCachedProfile(); // Fallback sur les données en cache
                return;
            }
            
            android.util.Log.d("PROFILE", "Chargement du profil...");
            
            Call<Employee> call = apiService.getProfile(token);
            call.enqueue(new Callback<Employee>() {
                @Override
                public void onResponse(Call<Employee> call, Response<Employee> response) {
                    setLoading(false);
                    
                    try {
                        android.util.Log.d("PROFILE", "Réponse profil: " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null) {
                            Employee employee = response.body();
                            android.util.Log.d("PROFILE", "Profil chargé: " + employee.getFullName());
                            displayProfile(employee);
                        } else {
                            android.util.Log.e("PROFILE", "Erreur API: " + response.code());
                            // Afficher les données en cache si l'API échoue
                            displayCachedProfile();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("PROFILE", "Erreur dans onResponse", e);
                        displayCachedProfile();
                    }
                }

                @Override
                public void onFailure(Call<Employee> call, Throwable t) {
                    setLoading(false);
                    android.util.Log.e("PROFILE", "Échec chargement profil", t);
                    // Afficher les données en cache en cas d'erreur réseau
                    displayCachedProfile();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("PROFILE", "Erreur loadProfile", e);
            setLoading(false);
            displayCachedProfile();
        }
    }

    private void displayProfile(Employee employee) {
        // ✅ CORRECTION: Afficher les informations dans le header
        String fullName = employee.getFullName();
        tvFullName.setText(fullName);
        tvEmailHeader.setText(employee.getEmail());

        // Avatar avec initiales
        String initials = getInitials(fullName);
        tvAvatar.setText(initials);

        // Informations détaillées
        tvName.setText(fullName);
        tvEmail.setText(employee.getEmail());
        tvDepartment.setText(employee.getDepartment() != null ? employee.getDepartment() : "Non défini");
        tvPosition.setText(employee.getPosition() != null ? employee.getPosition() : "Non défini");

        // Afficher le statut employé avec couleur
        String employeeStatusText = employee.getEmployeeStatusText();
        tvEmployeeStatus.setText(employeeStatusText);
        tvEmployeeStatus.setTextColor(employee.getEmployeeStatusColor(this));

        tvStatus.setText(employee.isActive() ? "Actif" : "Inactif");

        // Charger les statistiques
        loadStatistics();
    }

    /**
     * Extrait les initiales d'un nom complet
     */
    private String getInitials(String fullName) {
        try {
            if (fullName == null || fullName.trim().isEmpty()) {
                return "--";
            }

            String[] parts = fullName.trim().split("\\s+");
            if (parts.length >= 2 && parts[0].length() > 0 && parts[1].length() > 0) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            } else if (parts.length == 1 && parts[0].length() > 0) {
                return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            }
            return "--";
        } catch (Exception e) {
            android.util.Log.e("PROFILE", "Erreur getInitials", e);
            return "--";
        }
    }

    private void displayCachedProfile() {
        // ✅ CORRECTION: Charger TOUTES les données depuis le cache
        String userName = prefs.getString("user_name", null);
        if (userName == null) {
            userName = prefs.getString("employee_name", "Non disponible");
        }

        String userEmail = prefs.getString("user_email", null);
        if (userEmail == null) {
            userEmail = prefs.getString("employee_email", "Non disponible");
        }

        // ✅ NOUVEAU: Charger département, poste, statut depuis le cache
        String department = prefs.getString("user_department", "Non disponible");
        String position = prefs.getString("user_position", "Non disponible");
        String employeeStatus = prefs.getString("user_employee_status", "Non défini");
        boolean isActive = prefs.getBoolean("user_is_active", true);

        // Header
        tvFullName.setText(userName);
        tvEmailHeader.setText(userEmail);
        tvAvatar.setText(getInitials(userName));

        // Informations détaillées
        tvName.setText(userName);
        tvEmail.setText(userEmail);
        tvDepartment.setText(department);
        tvPosition.setText(position);

        // Statut employé avec couleur
        tvEmployeeStatus.setText(employeeStatus);
        // Couleur selon le statut
        if ("active".equalsIgnoreCase(employeeStatus) || "actif".equalsIgnoreCase(employeeStatus)) {
            tvEmployeeStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if ("on_leave".equalsIgnoreCase(employeeStatus) || "en_conge".equalsIgnoreCase(employeeStatus)) {
            tvEmployeeStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvEmployeeStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        tvStatus.setText(isActive ? "Actif" : "Inactif");

        // Charger les statistiques locales
        loadStatistics();

        android.util.Log.d("PROFILE", "Profil affiché depuis le cache - Utilisez 'Actualiser' pour synchroniser");
    }

    /**
     * ✅ CORRECTION: Charge les statistiques de l'utilisateur
     */
    private void loadStatistics() {
        try {
            // Charger depuis la base de données locale
            com.ptms.mobile.database.OfflineDatabaseHelper dbHelper =
                    new com.ptms.mobile.database.OfflineDatabaseHelper(this);

            int userId = prefs.getInt("user_id", -1);
            if (userId == -1) {
                userId = prefs.getInt("employee_id", -1);
            }

            // Compter les rapports de temps
            int reportCount = dbHelper.getTimeReportsCount(userId);
            tvReportCount.setText(String.valueOf(reportCount));

            // Calculer les heures totales
            double totalHours = dbHelper.getTotalHours(userId);
            tvTotalHours.setText(String.format("%.2f h", totalHours));

            // Afficher la date de création (si disponible)
            // Pour l'instant, on utilise une valeur par défaut
            tvMemberSince.setText("--");

            android.util.Log.d("PROFILE", "Statistiques chargées: " + reportCount + " rapports, " + totalHours + " heures");

        } catch (Exception e) {
            android.util.Log.e("PROFILE", "Erreur chargement statistiques", e);
            tvReportCount.setText("--");
            tvTotalHours.setText("--");
            tvMemberSince.setText("--");
        }
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

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadProfile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
