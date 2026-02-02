package com.ptms.mobile.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ptms.mobile.R;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.models.WorkType;
import com.ptms.mobile.sync.BidirectionalSyncManager;
import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.managers.OfflineModeManager;
import com.ptms.mobile.utils.NetworkUtils;
import com.ptms.mobile.auth.InitialAuthManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité de saisie des heures de travail avec support hors ligne
 */
public class TimeEntryActivity extends AppCompatActivity {
    
    private Spinner spinnerProject, spinnerWorkType;
    private EditText etDate, etTimeFrom, etTimeTo, etDescription;
    private Button btnSave, btnCancel;
    private Button btnToday, btnAdd15min, btnAdd30min, btnAdd1h; // Quick action buttons
    private ProgressBar progressBar;
    // Removed: private TextView tvConnectionStatus, tvSyncStatus;
    // Removed: private ImageView ivConnectionIcon;
    private TextView tvPendingCount, tvTotalHours;

    // ✅ PHOTO SUPPORT
    private Button btnAddPhoto, btnRemovePhoto;
    private ImageView ivPhotoPreview;
    private androidx.cardview.widget.CardView cardPhotoPreview;
    private String selectedPhotoPath = null;
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_IMAGE_PICK = 1002;
    private static final int REQUEST_CAMERA_PERMISSION = 1003;
    
    private SharedPreferences prefs;
    private ApiService apiService;
    private ApiClient apiClient;
    private BidirectionalSyncManager syncManager;
    private OfflineModeManager offlineModeManager;
    private OfflineDatabaseHelper dbHelper;
    private InitialAuthManager initialAuthManager;
    
    private List<Project> projects = new ArrayList<>();
    private List<WorkType> workTypes = new ArrayList<>();
    private Calendar selectedDate = Calendar.getInstance();
    private Calendar timeFrom = Calendar.getInstance();
    private Calendar timeTo = Calendar.getInstance();
    
    private boolean isOnline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_time_entry);

            // ✅ Initialisation unifiée - BidirectionalSyncManager uniquement
            prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
            dbHelper = new OfflineDatabaseHelper(this);
            syncManager = new BidirectionalSyncManager(this);
            offlineModeManager = OfflineModeManager.getInstance(this);
            initialAuthManager = new InitialAuthManager(this);
            
            // Vérifier l'authentification initiale (de manière sécurisée)
            // TEMPORAIREMENT DÉSACTIVÉ POUR DEBUG
            /*
            try {
                if (!initialAuthManager.canUseOffline()) {
                    Toast.makeText(this, "Authentification initiale requise", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, FirstLaunchAuthActivity.class));
                    finish();
                    return;
                }
            } catch (Exception e) {
                android.util.Log.e("OfflineTimeEntry", "Erreur vérification auth initiale", e);
                // En cas d'erreur, continuer normalement pour éviter le blocage
                android.util.Log.w("OfflineTimeEntry", "Continuation sans vérification auth initiale");
            }
            */
            android.util.Log.d("OfflineTimeEntry", "Système d'authentification initiale temporairement désactivé pour debug");
            
            // Configuration de la toolbar
            try {
                Toolbar toolbar = findViewById(R.id.toolbar);
                if (toolbar != null) {
                    setSupportActionBar(toolbar);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Saisie d'heures");
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur configuration toolbar", e);
            }

            initViews();
            setupApiService();
            setupListeners();
            loadData();
            setDefaultTimes();
            
            // ✅ Mode online : synchroniser automatiquement via OfflineModeManager
            if (NetworkUtils.isOnline(this)) {
                android.util.Log.d("OFFLINE_TIME_ENTRY", "Mode online - Sync automatique via OfflineModeManager");
                offlineModeManager.detectConnectionMode((isOnline, message) -> {
                    android.util.Log.d("OFFLINE_TIME_ENTRY", "Connexion: " + isOnline + " - " + message);
                    runOnUiThread(() -> updateConnectionStatus());
                });
            } else {
                android.util.Log.d("OFFLINE_TIME_ENTRY", "Mode offline - Utilisation du cache SQLite local");
            }
            
            updateConnectionStatus();
            
            // Démarrer le monitoring de connectivité
            startConnectivityMonitoring();
            
        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur dans onCreate", e);
            Toast.makeText(this, "Erreur initialisation: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void initViews() {
        try {
            spinnerProject = findViewById(R.id.spinner_project);
            spinnerWorkType = findViewById(R.id.spinner_work_type);
            etDate = findViewById(R.id.et_date);
            etTimeFrom = findViewById(R.id.et_time_from);
            etTimeTo = findViewById(R.id.et_time_to);
            etDescription = findViewById(R.id.et_description);
            btnSave = findViewById(R.id.btn_save);
            btnCancel = findViewById(R.id.btn_cancel);

            // Quick action buttons
            btnToday = findViewById(R.id.btn_today);
            btnAdd15min = findViewById(R.id.btn_add_15min);
            btnAdd30min = findViewById(R.id.btn_add_30min);
            btnAdd1h = findViewById(R.id.btn_add_1h);

            progressBar = findViewById(R.id.progress_bar);
            // Removed: tvConnectionStatus = findViewById(R.id.tv_connection_status);
            // Removed: tvSyncStatus = findViewById(R.id.tv_sync_status);
            tvPendingCount = findViewById(R.id.tv_pending_count);
            tvTotalHours = findViewById(R.id.tv_total_hours);

            // ✅ PHOTO SUPPORT
            btnAddPhoto = findViewById(R.id.btn_add_photo);
            btnRemovePhoto = findViewById(R.id.btn_remove_photo);
            ivPhotoPreview = findViewById(R.id.iv_photo_preview);
            cardPhotoPreview = findViewById(R.id.card_photo_preview);
            // Removed: ivConnectionIcon = findViewById(R.id.iv_connection_icon);

            android.util.Log.d("OFFLINE_TIME_ENTRY", "Views initialisées");
        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur initViews", e);
            Toast.makeText(this, "Erreur initialisation interface: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupApiService() {
        try {
            apiClient = ApiClient.getInstance(this);
            apiService = apiClient.getApiService();
            android.util.Log.d("OFFLINE_TIME_ENTRY", "API Service configuré");
        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur setupApiService", e);
            Toast.makeText(this, "Erreur configuration API: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        // Sélection de date
        etDate.setOnClickListener(v -> showDatePicker());

        // Sélection d'heure de début
        etTimeFrom.setOnClickListener(v -> showTimePicker(timeFrom, etTimeFrom));

        // Sélection d'heure de fin
        etTimeTo.setOnClickListener(v -> showTimePicker(timeTo, etTimeTo));

        // Bouton sauvegarder
        btnSave.setOnClickListener(v -> saveTimeEntry());

        // Bouton annuler
        btnCancel.setOnClickListener(v -> finish());

        // ✅ PHOTO SUPPORT - Bouton ajouter photo
        if (btnAddPhoto != null) {
            btnAddPhoto.setOnClickListener(v -> showPhotoPickerDialog());
        }

        // ✅ PHOTO SUPPORT - Bouton retirer photo
        if (btnRemovePhoto != null) {
            btnRemovePhoto.setOnClickListener(v -> removePhoto());
        }

        // Removed: Bouton synchronisation manuelle
        // if (findViewById(R.id.btn_sync_now) != null) {
        //     findViewById(R.id.btn_sync_now).setOnClickListener(v -> triggerManualSync());
        // }

        // Quick action: Aujourd'hui
        if (btnToday != null) {
            btnToday.setOnClickListener(v -> setToday());
        }

        // Quick action: +15 minutes
        if (btnAdd15min != null) {
            btnAdd15min.setOnClickListener(v -> addMinutesToEndTime(15));
        }

        // Quick action: +30 minutes
        if (btnAdd30min != null) {
            btnAdd30min.setOnClickListener(v -> addMinutesToEndTime(30));
        }

        // Quick action: +1 heure
        if (btnAdd1h != null) {
            btnAdd1h.setOnClickListener(v -> addMinutesToEndTime(60));
        }

        // Listener pour recalculer les heures automatiquement
        android.text.TextWatcher timeWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateAndDisplayTotalHours();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };

        etTimeFrom.addTextChangedListener(timeWatcher);
        etTimeTo.addTextChangedListener(timeWatcher);
    }

    /**
     * ✅ UNIFIÉ: Charge les données depuis SQLite uniquement
     * - Mode online: Les données sont synchronisées automatiquement par OfflineModeManager
     * - Mode offline: Utilise le cache SQLite local
     */
    private void loadData() {
        try {
            android.util.Log.d("OFFLINE_TIME_ENTRY", "📦 Chargement des données depuis SQLite...");

            // ✅ Charger UNIQUEMENT depuis SQLite (source de vérité unique)
            projects = dbHelper.getAllProjects();
            workTypes = dbHelper.getAllWorkTypes();

            // Mettre à jour les spinners
            setupProjectSpinner();
            setupWorkTypeSpinner();

            android.util.Log.d("OFFLINE_TIME_ENTRY", "✅ Données chargées: " + projects.size() + " projets, " + workTypes.size() + " types de travail");

            // Si aucune donnée et mode online, synchroniser
            if ((projects.isEmpty() || workTypes.isEmpty()) && NetworkUtils.isOnline(this)) {
                android.util.Log.d("OFFLINE_TIME_ENTRY", "⚠️ Cache SQLite vide - Synchronisation requise");
                Toast.makeText(this, "Synchronisation des données...", Toast.LENGTH_SHORT).show();

                syncManager.syncDownload(new BidirectionalSyncManager.SyncCallback() {
                    @Override
                    public void onSyncStarted(String phase) {
                        android.util.Log.d("OFFLINE_TIME_ENTRY", "Sync: " + phase);
                    }

                    @Override
                    public void onSyncProgress(String message, int current, int total) {
                        android.util.Log.d("OFFLINE_TIME_ENTRY", "Sync: " + message);
                    }

                    @Override
                    public void onSyncCompleted(BidirectionalSyncManager.SyncResult result) {
                        android.util.Log.d("OFFLINE_TIME_ENTRY", "Sync terminée: " + result.getSummary());
                        runOnUiThread(() -> {
                            Toast.makeText(TimeEntryActivity.this, "✅ Données synchronisées", Toast.LENGTH_SHORT).show();
                            loadData(); // Recharger après sync
                        });
                    }

                    @Override
                    public void onSyncError(String error) {
                        android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur sync: " + error);
                        runOnUiThread(() -> Toast.makeText(TimeEntryActivity.this, "❌ Erreur sync: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
            } else if (projects.isEmpty() || workTypes.isEmpty()) {
                // Mode offline sans données
                android.util.Log.w("OFFLINE_TIME_ENTRY", "⚠️ Mode offline sans données en cache");
                Toast.makeText(this, "⚠️ Aucune donnée disponible - Connexion requise", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "❌ Erreur loadData", e);
            // En cas d'erreur, initialiser avec des listes vides
            projects = new ArrayList<>();
            workTypes = new ArrayList<>();
            setupProjectSpinner();
            setupWorkTypeSpinner();
        }
    }
    
    private void loadDataFromServer() {
        try {
            String token = prefs.getString("auth_token", "");
            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "Session expirée - Mode hors ligne uniquement", Toast.LENGTH_LONG).show();
                return;
            }
            
            android.util.Log.d("OFFLINE_TIME_ENTRY", "Chargement des données depuis le serveur...");

            // Charger les projets
            Call<ApiService.ProjectsResponse> projectsCall = apiService.getProjects(token);
            projectsCall.enqueue(new Callback<ApiService.ProjectsResponse>() {
                @Override
                public void onResponse(Call<ApiService.ProjectsResponse> call, Response<ApiService.ProjectsResponse> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null && response.body().success) {
                            projects = response.body().projects;
                            setupProjectSpinner();

                            // ✅ Les projets sont automatiquement sauvegardés dans SQLite par BidirectionalSyncManager
                            android.util.Log.d("OFFLINE_TIME_ENTRY", "Projets chargés depuis le serveur: " + projects.size());
                        } else {
                            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur projets serveur: " + response.code());
                        }
                    } catch (Exception e) {
                        android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur dans onResponse projets", e);
                    }
                }

                @Override
                public void onFailure(Call<ApiService.ProjectsResponse> call, Throwable t) {
                    android.util.Log.e("OFFLINE_TIME_ENTRY", "Échec chargement projets serveur", t);
                    // En cas d'échec, continuer avec les données en cache
                }
            });

            // Charger les types de travail
            Call<List<WorkType>> workTypesCall = apiService.getWorkTypes(token);
            workTypesCall.enqueue(new Callback<List<WorkType>>() {
                @Override
                public void onResponse(Call<List<WorkType>> call, Response<List<WorkType>> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            workTypes = response.body();
                            setupWorkTypeSpinner();

                            // ✅ Les types de travail sont automatiquement sauvegardés dans SQLite par BidirectionalSyncManager
                            android.util.Log.d("OFFLINE_TIME_ENTRY", "Types de travail chargés: " + workTypes.size());

                            // Afficher un message de succès
                            Toast.makeText(TimeEntryActivity.this,
                                "✅ Données mises à jour - " + projects.size() + " projets, " + workTypes.size() + " types",
                                Toast.LENGTH_SHORT).show();
                        } else {
                            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur work types serveur: " + response.code());
                        }
                    } catch (Exception e) {
                        android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur dans onResponse work types", e);
                    }
                }

                @Override
                public void onFailure(Call<List<WorkType>> call, Throwable t) {
                    android.util.Log.e("OFFLINE_TIME_ENTRY", "Échec chargement work types serveur", t);
                    // En cas d'échec, continuer avec les données en cache
                }
            });
            
        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur loadDataFromServer", e);
        }
    }

    private void setupProjectSpinner() {
        try {
            List<String> projectNames = new ArrayList<>();
            projectNames.add("Sélectionner un projet...");
            
            for (Project project : projects) {
                if (project != null && project.getName() != null) {
                    projectNames.add(project.getName());
                }
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, projectNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (spinnerProject != null) {
                spinnerProject.setAdapter(adapter);
                spinnerProject.setSelection(0);
            }
            
            android.util.Log.d("OFFLINE_TIME_ENTRY", "Spinner projets configuré avec " + projects.size() + " projets");
        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur setupProjectSpinner", e);
        }
    }

    private void setupWorkTypeSpinner() {
        try {
            List<String> workTypeNames = new ArrayList<>();
            workTypeNames.add("Sélectionner un type de travail...");
            
            for (WorkType workType : workTypes) {
                if (workType != null && workType.getName() != null) {
                    workTypeNames.add(workType.getName());
                }
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, workTypeNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (spinnerWorkType != null) {
                spinnerWorkType.setAdapter(adapter);
                spinnerWorkType.setSelection(0);
            }
            
            android.util.Log.d("OFFLINE_TIME_ENTRY", "Spinner types de travail configuré avec " + workTypes.size() + " types");
        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur setupWorkTypeSpinner", e);
        }
    }

    private void setDefaultTimes() {
        try {
            // Date du jour
            if (etDate != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
                etDate.setText(dateFormat.format(selectedDate.getTime()));
                etDate.setHint("Date");
            }

            // Heure de début (9h00)
            timeFrom.set(Calendar.HOUR_OF_DAY, 9);
            timeFrom.set(Calendar.MINUTE, 0);
            if (etTimeFrom != null) {
                updateTimeDisplay(etTimeFrom, timeFrom);
                etTimeFrom.setHint("Heure de début");
            }

            // Heure de fin (17h00)
            timeTo.set(Calendar.HOUR_OF_DAY, 17);
            timeTo.set(Calendar.MINUTE, 0);
            if (etTimeTo != null) {
                updateTimeDisplay(etTimeTo, timeTo);
                etTimeTo.setHint("Heure de fin");
            }
            
            android.util.Log.d("OFFLINE_TIME_ENTRY", "Valeurs par défaut définies");
        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur setDefaultTimes", e);
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
                    etDate.setText(dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker(Calendar calendar, EditText editText) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    updateTimeDisplay(editText, calendar);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void updateTimeDisplay(EditText editText, Calendar calendar) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.FRANCE);
        editText.setText(timeFormat.format(calendar.getTime()));
    }

    private void saveTimeEntry() {
        try {
            android.util.Log.d("OFFLINE_TIME_ENTRY", "Validation et sauvegarde des heures...");

            // Désactiver le bouton pour éviter les clics multiples
            setLoading(true);

            // Validation
            if (spinnerProject == null || spinnerProject.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Veuillez sélectionner un projet", Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }

            if (spinnerWorkType == null || spinnerWorkType.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Veuillez sélectionner un type de travail", Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }

            if (projects.isEmpty() || workTypes.isEmpty()) {
                Toast.makeText(this, "Données en cours de chargement, veuillez patienter", Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }

            // Calcul des heures
            long timeDiff = timeTo.getTimeInMillis() - timeFrom.getTimeInMillis();
            double hours = timeDiff / (1000.0 * 60 * 60);

            if (hours <= 0) {
                Toast.makeText(this, "L'heure de fin doit être après l'heure de début", Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }

            if (hours > 24) {
                Toast.makeText(this, "Les heures ne peuvent pas dépasser 24h", Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }

            // Création du rapport avec support timezone
            int selectedProjectIndex = spinnerProject.getSelectedItemPosition();
            int selectedWorkTypeIndex = spinnerWorkType.getSelectedItemPosition();

            Project selectedProject = projects.get(selectedProjectIndex - 1);
            WorkType selectedWorkType = workTypes.get(selectedWorkTypeIndex - 1);

            // ✅ FIX: Use Locale.US for ISO dates (prevents locale-specific crashes)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
            SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

            String reportDate = dateFormat.format(selectedDate.getTime());

            // ✅ FIX: Combine date + time for datetime fields (server expects full datetime)
            // Create full datetime by combining selectedDate with timeFrom/timeTo
            Calendar dateTimeFrom = Calendar.getInstance();
            dateTimeFrom.setTime(selectedDate.getTime());
            dateTimeFrom.set(Calendar.HOUR_OF_DAY, timeFrom.get(Calendar.HOUR_OF_DAY));
            dateTimeFrom.set(Calendar.MINUTE, timeFrom.get(Calendar.MINUTE));
            dateTimeFrom.set(Calendar.SECOND, 0);

            Calendar dateTimeTo = Calendar.getInstance();
            dateTimeTo.setTime(selectedDate.getTime());
            dateTimeTo.set(Calendar.HOUR_OF_DAY, timeTo.get(Calendar.HOUR_OF_DAY));
            dateTimeTo.set(Calendar.MINUTE, timeTo.get(Calendar.MINUTE));
            dateTimeTo.set(Calendar.SECOND, 0);

            String datetimeFromStr = datetimeFormat.format(dateTimeFrom.getTime());
            String datetimeToStr = datetimeFormat.format(dateTimeTo.getTime());

            // Get user's timezone
            String userTimezone = com.ptms.mobile.utils.TimezoneHelper.getUserTimezone(this);

            TimeReport report = new TimeReport(
                    selectedProject.getId(),
                    prefs.getInt("employee_id", 0),
                    selectedWorkType.getId(),
                    reportDate,
                    datetimeFromStr,  // Now includes date + time
                    datetimeToStr,    // Now includes date + time
                    hours,
                    etDescription.getText().toString().trim()
            );

            // Add timezone info to report
            report.setTimezone(userTimezone);

            // ✅ PHOTO SUPPORT - Local-First: TOUJOURS sauvegarder en local
            if (selectedPhotoPath != null && !selectedPhotoPath.isEmpty()) {
                // Sauvegarder le chemin local de la photo
                report.setImagePath(selectedPhotoPath);
                android.util.Log.d("PHOTO", "Photo attachée au rapport: " + selectedPhotoPath);

                // Marquer comme "pending" pour upload ultérieur
                report.setSyncStatus("pending_media");
            }

            // Local-First: TOUJOURS sauvegarder localement d'abord
            // BidirectionalSyncManager s'occupe de la sync et de l'upload des photos
            if (isOnline) {
                // Mode ONLINE: Sauvegarder local + sync immédiate
                android.util.Log.d("OFFLINE_TIME_ENTRY", "Mode online - Sauvegarde locale + sync");
                saveOffline(report); // Sauvegarde locale d'abord
                // La sync automatique uploadera la photo si présente
            } else {
                // Mode OFFLINE: Sauvegarder local, sync plus tard
                android.util.Log.d("OFFLINE_TIME_ENTRY", "Mode offline - Sauvegarde locale, sync différée");
                saveOffline(report);
                Toast.makeText(this, "💾 Sauvegardé localement - Synchronisation auto à la reconnexion", Toast.LENGTH_LONG).show();
            }
            
        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur saveTimeEntry", e);
            Toast.makeText(this, "❌ Erreur sauvegarde: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setLoading(false);
        }
    }
    
    private void saveOnline(TimeReport report) {
        // setLoading déjà appelé dans saveTimeEntry(), pas besoin de le rappeler
        // ✅ Utiliser TokenManager centralisé + format Bearer
        com.ptms.mobile.auth.TokenManager tokenManager = com.ptms.mobile.auth.TokenManager.getInstance(this);
        String token = tokenManager.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "❌ Token d'authentification manquant", Toast.LENGTH_LONG).show();
            setLoading(false);
            return;
        }
        String bearerToken = "Bearer " + token;

        Call<ApiService.ApiResponse> call = apiService.saveTimeEntry(bearerToken, report);
        call.enqueue(new Callback<ApiService.ApiResponse>() {
            @Override
            public void onResponse(Call<ApiService.ApiResponse> call, Response<ApiService.ApiResponse> response) {
                setLoading(false);
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiService.ApiResponse apiResponse = response.body();
                        if (apiResponse.success) {
                            android.util.Log.d("OFFLINE_TIME_ENTRY", "Sauvegarde en ligne réussie");
                            Toast.makeText(TimeEntryActivity.this, "✅ Heures sauvegardées avec succès", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur API: " + apiResponse.message);
                            Toast.makeText(TimeEntryActivity.this, "❌ " + apiResponse.message, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        android.util.Log.e("OFFLINE_TIME_ENTRY", "Réponse non réussie: " + response.code());
                        Toast.makeText(TimeEntryActivity.this, "❌ Erreur serveur: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur dans onResponse", e);
                    Toast.makeText(TimeEntryActivity.this, "❌ Erreur traitement réponse: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.ApiResponse> call, Throwable t) {
                setLoading(false);
                android.util.Log.e("OFFLINE_TIME_ENTRY", "Échec sauvegarde en ligne", t);
                
                // En cas d'échec de sauvegarde en ligne, sauvegarder hors ligne
                android.util.Log.d("OFFLINE_TIME_ENTRY", "Sauvegarde hors ligne après échec réseau");
                saveOffline(report);
            }
        });
    }
    
    private void saveOffline(TimeReport report) {
        // setLoading déjà appelé dans saveTimeEntry(), pas besoin de le rappeler
        android.util.Log.d("OFFLINE_TIME_ENTRY", "Sauvegarde hors ligne en cours...");

        // ✅ Sauvegarde via BidirectionalSyncManager (Local-First)
        if (syncManager != null) {
            android.util.Log.d("OFFLINE_TIME_ENTRY", "Sauvegarde via BidirectionalSyncManager");
            syncManager.saveTimeReport(report, new BidirectionalSyncManager.SaveCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        android.util.Log.d("OFFLINE_TIME_ENTRY", "Sauvegarde réussie: " + message);
                        setLoading(false);
                        String msg = isOnline ?
                            "✅ Heures sauvegardées localement (synchronisation en cours)" :
                            "✅ Heures sauvegardées hors ligne (synchronisation automatique à la reconnexion)";

                        Toast.makeText(TimeEntryActivity.this, msg, Toast.LENGTH_LONG).show();

                        // Mettre à jour l'affichage du nombre de rapports en attente
                        updatePendingCount();

                        // Fermer l'activité après un court délai
                        new android.os.Handler().postDelayed(() -> finish(), 2000);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        android.util.Log.e("OFFLINE_TIME_ENTRY", "Échec sauvegarde: " + error);
                        setLoading(false);
                        Toast.makeText(TimeEntryActivity.this, "❌ Erreur sauvegarde: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            setLoading(false);
            android.util.Log.w("OFFLINE_TIME_ENTRY", "Aucune sauvegarde possible - syncManager null et JSON échoué");
            Toast.makeText(this, "❌ Erreur sauvegarde", Toast.LENGTH_LONG).show();
        }
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(!loading);
        }
        if (btnCancel != null) {
            btnCancel.setEnabled(!loading);
        }
    }
    
    private void updateConnectionStatus() {
        // Utiliser NetworkUtils directement
        isOnline = NetworkUtils.isOnline(this);
        
        // Removed: Connection status UI update
        // if (tvConnectionStatus != null) {
        //     tvConnectionStatus.setText(isOnline ? "Connecté" : "Hors ligne");
        // }
        //
        // if (ivConnectionIcon != null) {
        //     ivConnectionIcon.setImageResource(isOnline ?
        //         android.R.drawable.ic_menu_share :
        //         android.R.drawable.ic_menu_close_clear_cancel);
        // }
        //
        // // Mettre à jour le statut de synchronisation
        // updateSyncStatus();
        
        // Mettre à jour le nombre de rapports en attente
        updatePendingCount();
    }
    
    // Removed: updateSyncStatus() method - no longer needed
    // private void updateSyncStatus() {
    //     if (tvSyncStatus == null) return;
    //
    //     if (syncManager != null) {
    //         long lastSync = syncManager.getLastSyncTime();
    //         boolean syncInProgress = syncManager.isSyncInProgress();
    //
    //         if (syncInProgress) {
    //             tvSyncStatus.setText("Synchronisation en cours...");
    //         } else if (lastSync > 0) {
    //             tvSyncStatus.setText("Dernière sync: " + OfflineSyncManager.formatLastSyncTime(lastSync));
    //         } else {
    //             tvSyncStatus.setText("Aucune synchronisation");
    //         }
    //     } else {
    //         tvSyncStatus.setText("Mode hors ligne activé");
    //     }
    // }
    
    private void updatePendingCount() {
        if (tvPendingCount == null) return;
        
        if (syncManager != null) {
            int pendingCount = syncManager.getPendingSyncCount();
            if (pendingCount > 0) {
                tvPendingCount.setText(pendingCount + " rapport(s) en attente de synchronisation");
                tvPendingCount.setVisibility(View.VISIBLE);
            } else {
                tvPendingCount.setVisibility(View.GONE);
            }
        } else {
            tvPendingCount.setVisibility(View.GONE);
        }
    }
    
    private void startConnectivityMonitoring() {
        // ✅ Supprimé - BidirectionalSyncManager gère automatiquement la connectivité
        // via OfflineModeManager qui détecte les changements de connexion
        android.util.Log.d("OFFLINE_TIME_ENTRY", "Connectivity monitoring géré automatiquement par OfflineModeManager");
    }
    
    private void triggerManualSync() {
        if (!isOnline) {
            Toast.makeText(this, "Pas de connexion internet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (syncManager != null) {
            Toast.makeText(this, "Synchronisation manuelle lancée...", Toast.LENGTH_SHORT).show();

            syncManager.syncFull(new BidirectionalSyncManager.SyncCallback() {
                @Override
                public void onSyncStarted(String phase) {
                    runOnUiThread(() -> {
                        Toast.makeText(TimeEntryActivity.this, "Synchronisation: " + phase, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onSyncProgress(String message, int current, int total) {
                    runOnUiThread(() -> {
                        android.util.Log.d("OFFLINE_TIME_ENTRY", "Sync progress: " + message + " (" + current + "/" + total + ")");
                    });
                }

                @Override
                public void onSyncCompleted(BidirectionalSyncManager.SyncResult result) {
                    runOnUiThread(() -> {
                        updatePendingCount();
                        String message = "Synchronisation terminée: " + result.getSummary();
                        Toast.makeText(TimeEntryActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onSyncError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(TimeEntryActivity.this, "Erreur sync: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            Toast.makeText(this, "Synchronisation non disponible (mode offline)", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
    }

    /**
     * Charge les dernières données de projets et types de travail depuis le serveur
     * pour s'assurer que l'utilisateur a toujours les données les plus récentes
     */
    private void loadLatestProjectsAndWorkTypes() {
        try {
            android.util.Log.d("OFFLINE_TIME_ENTRY", "Chargement des dernières données en ligne...");
            
            String token = prefs.getString("auth_token", "");
            if (token == null || token.isEmpty()) {
                android.util.Log.w("OFFLINE_TIME_ENTRY", "Token d'authentification manquant");
                return;
            }

            // Charger les projets
            Call<ApiService.ProjectsResponse> projectsCall = apiService.getProjects(token);
            projectsCall.enqueue(new Callback<ApiService.ProjectsResponse>() {
                @Override
                public void onResponse(Call<ApiService.ProjectsResponse> call, Response<ApiService.ProjectsResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        List<Project> projects = response.body().projects;
                        android.util.Log.d("OFFLINE_TIME_ENTRY", "Projets chargés: " + projects.size());
                        
                        // ✅ Les données sont automatiquement mises à jour dans SQLite par BidirectionalSyncManager
                        android.util.Log.d("OFFLINE_TIME_ENTRY", "Données déjà synchronisées dans SQLite");

                        // Recharger les données dans l'interface
                        loadData();
                        
                        Toast.makeText(TimeEntryActivity.this, 
                            "✅ Données mises à jour depuis le serveur", 
                            Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiService.ProjectsResponse> call, Throwable t) {
                    android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur chargement projets", t);
                }
            });

            // Charger les types de travail
            Call<List<WorkType>> workTypesCall = apiService.getWorkTypes(token);
            workTypesCall.enqueue(new Callback<List<WorkType>>() {
                @Override
                public void onResponse(Call<List<WorkType>> call, Response<List<WorkType>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<WorkType> workTypes = response.body();
                        android.util.Log.d("OFFLINE_TIME_ENTRY", "Types de travail chargés: " + workTypes.size());
                    }
                }

                @Override
                public void onFailure(Call<List<WorkType>> call, Throwable t) {
                    android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur chargement types de travail", t);
                }
            });

        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur chargement données en ligne", e);
        }
    }

    // ==================== NOUVELLES FONCTIONNALITÉS ====================

    /**
     * Définit la date à aujourd'hui
     */
    private void setToday() {
        selectedDate = Calendar.getInstance();
        updateTimeDisplay(etDate, selectedDate);
        Toast.makeText(this, "Date définie à aujourd'hui", Toast.LENGTH_SHORT).show();
    }

    /**
     * Ajoute des minutes à l'heure de fin
     */
    private void addMinutesToEndTime(int minutes) {
        timeTo.add(Calendar.MINUTE, minutes);
        updateTimeDisplay(etTimeTo, timeTo);
        calculateAndDisplayTotalHours();
        Toast.makeText(this, "+" + minutes + " minutes ajoutées", Toast.LENGTH_SHORT).show();
    }

    /**
     * Calcule et affiche le total d'heures
     */
    private void calculateAndDisplayTotalHours() {
        if (tvTotalHours == null) return;

        try {
            // Calculer la différence en millisecondes
            long diff = timeTo.getTimeInMillis() - timeFrom.getTimeInMillis();

            if (diff < 0) {
                tvTotalHours.setText("0.00h");
                tvTotalHours.setTextColor(android.graphics.Color.parseColor("#E53935")); // Rouge si négatif
                return;
            }

            // Convertir en heures décimales
            double hours = diff / (1000.0 * 60 * 60);

            // Afficher avec 2 décimales
            tvTotalHours.setText(String.format(java.util.Locale.US, "%.2fh", hours));

            // Colorer selon la durée
            if (hours > 0 && hours <= 8) {
                tvTotalHours.setTextColor(android.graphics.Color.parseColor("#1B5E20")); // Vert
            } else if (hours > 8 && hours <= 12) {
                tvTotalHours.setTextColor(android.graphics.Color.parseColor("#F57C00")); // Orange
            } else if (hours > 12) {
                tvTotalHours.setTextColor(android.graphics.Color.parseColor("#E53935")); // Rouge
            } else {
                tvTotalHours.setTextColor(android.graphics.Color.parseColor("#757575")); // Gris
            }

        } catch (Exception e) {
            android.util.Log.e("OFFLINE_TIME_ENTRY", "Erreur calcul total heures", e);
            tvTotalHours.setText("0.00h");
        }
    }

    // ==================== PHOTO SUPPORT ====================

    /**
     * Affiche le dialogue de sélection de photo (caméra ou galerie)
     */
    private void showPhotoPickerDialog() {
        String[] options = {"📷 Prendre une photo", "🖼️ Choisir depuis la galerie"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Ajouter une photo")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Prendre une photo avec la caméra
                    takePhoto();
                } else {
                    // Choisir depuis la galerie
                    pickFromGallery();
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    /**
     * Prendre une photo avec la caméra
     */
    private void takePhoto() {
        // ✅ Vérifier permission caméra
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Demander la permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
            return;
        }

        // Permission accordée, lancer la caméra
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Aucune application caméra disponible", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Choisir une image depuis la galerie
     */
    private void pickFromGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
    }

    /**
     * Retirer la photo sélectionnée
     */
    private void removePhoto() {
        selectedPhotoPath = null;
        cardPhotoPreview.setVisibility(View.GONE);
        ivPhotoPreview.setImageBitmap(null);
        Toast.makeText(this, "Photo retirée", Toast.LENGTH_SHORT).show();
    }

    /**
     * Afficher l'aperçu de la photo
     */
    private void displayPhotoPreview(android.graphics.Bitmap bitmap) {
        if (bitmap != null) {
            ivPhotoPreview.setImageBitmap(bitmap);
            cardPhotoPreview.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sauvegarder la photo dans le stockage local
     */
    private String savePhotoToStorage(android.graphics.Bitmap bitmap) {
        try {
            // Créer le dossier pour les photos de rapports
            java.io.File photosDir = new java.io.File(getFilesDir(), "report_photos");
            if (!photosDir.exists()) {
                photosDir.mkdirs();
            }

            // Nom de fichier unique avec timestamp
            String filename = "report_" + System.currentTimeMillis() + ".jpg";
            java.io.File photoFile = new java.io.File(photosDir, filename);

            // Compresser et sauvegarder
            java.io.FileOutputStream out = new java.io.FileOutputStream(photoFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            out.close();

            android.util.Log.d("PHOTO", "Photo sauvegardée: " + photoFile.getAbsolutePath());
            return photoFile.getAbsolutePath();

        } catch (Exception e) {
            android.util.Log.e("PHOTO", "Erreur sauvegarde photo", e);
            Toast.makeText(this, "Erreur sauvegarde photo", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * ✅ Gestion des permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, relancer la caméra
                Toast.makeText(this, "Permission caméra accordée", Toast.LENGTH_SHORT).show();
                takePhoto();
            } else {
                // Permission refusée
                Toast.makeText(this, "Permission caméra refusée - Impossible de prendre une photo", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) return;

        try {
            android.graphics.Bitmap bitmap = null;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Photo prise avec la caméra
                android.os.Bundle extras = data.getExtras();
                if (extras != null) {
                    bitmap = (android.graphics.Bitmap) extras.get("data");
                }
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                // Photo choisie depuis la galerie
                android.net.Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                        getContentResolver(), selectedImage);
                }
            }

            if (bitmap != null) {
                // Sauvegarder la photo et afficher l'aperçu
                selectedPhotoPath = savePhotoToStorage(bitmap);
                displayPhotoPreview(bitmap);
                Toast.makeText(this, "Photo ajoutée ✓", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            android.util.Log.e("PHOTO", "Erreur traitement photo", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
