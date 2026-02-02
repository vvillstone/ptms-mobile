package com.ptms.mobile.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.ptms.mobile.R;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.services.TimerService;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.WorkType;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Gestionnaire du widget flottant de timer
 * Affiche un widget flottant par-dessus toutes les applications
 * pour faciliter le suivi du temps sans avoir à ouvrir l'app
 */
public class FloatingTimerWidgetManager {

    private static FloatingTimerWidgetManager instance;
    private Context context;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    // États du widget
    private CardView widgetStopped;
    private CardView widgetRunning;
    private CardView widgetMinimized;

    // Éléments UI du widget en cours
    private TextView tvTimerDisplay;
    private TextView tvProjectName;
    private Button btnPause;
    private Button btnStop;

    // Données du timer
    private List<Project> projectList = new ArrayList<>();
    private List<WorkType> workTypeList = new ArrayList<>();
    private String currentProjectName = "";
    private int currentProjectId = 0;

    // Position du widget pour le drag
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    private TimerUpdateReceiver timerReceiver;
    private boolean isWidgetVisible = false;

    private FloatingTimerWidgetManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initializeReceiver();
    }

    public static synchronized FloatingTimerWidgetManager getInstance(Context context) {
        if (instance == null) {
            instance = new FloatingTimerWidgetManager(context);
        }
        return instance;
    }

    /**
     * Affiche le widget flottant
     */
    @SuppressLint("ClickableViewAccessibility")
    public void showWidget() {
        if (isWidgetVisible) {
            return;
        }

        // Charger le layout du widget
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.widget_timer_floating, null);

        // Configurer les paramètres de la fenêtre flottante
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        // Référencer les vues
        widgetStopped = floatingView.findViewById(R.id.widget_stopped);
        widgetRunning = floatingView.findViewById(R.id.widget_running);
        widgetMinimized = floatingView.findViewById(R.id.widget_minimized);

        tvTimerDisplay = floatingView.findViewById(R.id.tv_timer_display);
        tvProjectName = floatingView.findViewById(R.id.tv_project_name);
        btnPause = floatingView.findViewById(R.id.btn_pause_resume);
        btnStop = floatingView.findViewById(R.id.btn_stop);

        // Configurer les listeners
        setupListeners();

        // Configurer le drag du widget
        setupDragListener();

        // Ajouter le widget à l'écran
        windowManager.addView(floatingView, params);
        isWidgetVisible = true;

        // Charger les projets et types de travail
        loadProjects();
        loadWorkTypes();

        // Vérifier l'état actuel du timer
        checkTimerStatus();
    }

    /**
     * Masque le widget flottant
     */
    public void hideWidget() {
        if (isWidgetVisible && floatingView != null) {
            windowManager.removeView(floatingView);
            isWidgetVisible = false;
        }
    }

    /**
     * Configure les listeners des boutons
     */
    private void setupListeners() {
        // Bouton Start (widget arrêté)
        widgetStopped.setOnClickListener(v -> showStartDialog());

        // Bouton Pause/Resume
        btnPause.setOnClickListener(v -> {
            String state = btnPause.getText().toString();
            if (state.equals("Pause")) {
                pauseTimer();
            } else {
                resumeTimer();
            }
        });

        // Bouton Stop
        btnStop.setOnClickListener(v -> showStopConfirmationDialog());

        // Clic sur widget minimisé pour agrandir
        widgetMinimized.setOnClickListener(v -> expandWidget());
    }

    /**
     * Configure le drag & drop du widget
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupDragListener() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isDragging = false;
            private long touchStartTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStartTime = System.currentTimeMillis();
                        isDragging = false;

                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;

                        // Si déplacement > 10px, considérer comme drag
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true;
                        }

                        params.x = initialX + (int) deltaX;
                        params.y = initialY + (int) deltaY;
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        long touchDuration = System.currentTimeMillis() - touchStartTime;

                        // Si clic court et pas de drag, laisser passer l'événement
                        if (touchDuration < 200 && !isDragging) {
                            v.performClick();
                            return false;
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Affiche la boîte de dialogue pour démarrer le timer
     */
    private void showStartDialog() {
        if (projectList.isEmpty()) {
            Toast.makeText(context, "Chargement des projets...", Toast.LENGTH_SHORT).show();
            loadProjects();
            return;
        }

        // Créer la vue du dialogue
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_timer_start, null);
        Spinner spinnerProject = dialogView.findViewById(R.id.spinner_project);

        // Adapter pour les projets
        List<String> projectNames = new ArrayList<>();
        for (Project project : projectList) {
            projectNames.add(project.getName());
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            projectNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(adapter);

        // Créer et afficher le dialogue
        new AlertDialog.Builder(getActivityContext())
            .setTitle("Démarrer le timer")
            .setView(dialogView)
            .setPositiveButton("Start", (dialog, which) -> {
                int position = spinnerProject.getSelectedItemPosition();
                if (position >= 0 && position < projectList.size()) {
                    Project selectedProject = projectList.get(position);
                    startTimer(selectedProject.getId(), selectedProject.getName());
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    /**
     * Affiche la confirmation avant d'arrêter le timer
     */
    private void showStopConfirmationDialog() {
        String timeElapsed = tvTimerDisplay.getText().toString();
        String projectName = tvProjectName.getText().toString();

        String message = String.format(
            "Êtes-vous sûr de vouloir terminer le timer?\n\n" +
            "Projet: %s\n" +
            "Temps écoulé: %s\n\n" +
            "Cette action créera un rapport de temps.",
            projectName,
            timeElapsed
        );

        new AlertDialog.Builder(getActivityContext())
            .setTitle("⚠️ Confirmer l'arrêt")
            .setMessage(message)
            .setPositiveButton("Oui, terminer", (dialog, which) -> showStopDialog())
            .setNegativeButton("Continuer", null)
            .show();
    }

    /**
     * Affiche la boîte de dialogue pour arrêter le timer
     */
    private void showStopDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_timer_stop, null);
        Spinner spinnerWorkType = dialogView.findViewById(R.id.spinner_work_type);
        EditText etDescription = dialogView.findViewById(R.id.et_description);

        // Adapter pour les types de travail
        List<String> workTypeNames = new ArrayList<>();
        for (WorkType workType : workTypeList) {
            workTypeNames.add(workType.getName());
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            workTypeNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWorkType.setAdapter(adapter);

        new AlertDialog.Builder(getActivityContext())
            .setTitle("Terminer le timer")
            .setView(dialogView)
            .setPositiveButton("Stop", (dialog, which) -> {
                int position = spinnerWorkType.getSelectedItemPosition();
                String description = etDescription.getText().toString();

                if (position >= 0 && position < workTypeList.size()) {
                    WorkType selectedWorkType = workTypeList.get(position);
                    stopTimer(selectedWorkType.getId(), description);
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    /**
     * Démarre le timer via l'API
     */
    private void startTimer(int projectId, String projectName) {
        currentProjectId = projectId;
        currentProjectName = projectName;

        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(TimerService.ACTION_START);
        intent.putExtra("project_id", projectId);
        intent.putExtra("project_name", projectName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Mettre à jour l'UI
        updateWidgetState(TimerService.STATE_RUNNING, 0);
        tvProjectName.setText(projectName);
    }

    /**
     * Met en pause le timer
     */
    private void pauseTimer() {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(TimerService.ACTION_PAUSE);
        context.startService(intent);
    }

    /**
     * Reprend le timer
     */
    private void resumeTimer() {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(TimerService.ACTION_RESUME);
        context.startService(intent);
    }

    /**
     * Arrête le timer via l'API
     */
    private void stopTimer(int workTypeId, String description) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(TimerService.ACTION_STOP);
        intent.putExtra("work_type_id", workTypeId);
        intent.putExtra("description", description);
        context.startService(intent);
    }

    /**
     * Met à jour l'état visuel du widget
     */
    private void updateWidgetState(int state, int elapsedSeconds) {
        if (floatingView == null) return;

        switch (state) {
            case TimerService.STATE_STOPPED:
                widgetStopped.setVisibility(View.VISIBLE);
                widgetRunning.setVisibility(View.GONE);
                widgetMinimized.setVisibility(View.GONE);
                break;

            case TimerService.STATE_RUNNING:
                widgetStopped.setVisibility(View.GONE);
                widgetRunning.setVisibility(View.VISIBLE);
                widgetMinimized.setVisibility(View.GONE);
                btnPause.setText("Pause");
                updateTimerDisplay(elapsedSeconds);
                break;

            case TimerService.STATE_PAUSED:
                widgetStopped.setVisibility(View.GONE);
                widgetRunning.setVisibility(View.VISIBLE);
                widgetMinimized.setVisibility(View.GONE);
                btnPause.setText("Resume");
                updateTimerDisplay(elapsedSeconds);
                break;
        }
    }

    /**
     * Met à jour l'affichage du temps
     */
    private void updateTimerDisplay(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, secs);
        tvTimerDisplay.setText(timeString);

        // Mettre à jour aussi le widget minimisé
        TextView tvMinimized = floatingView.findViewById(R.id.tv_timer_minimized);
        tvMinimized.setText(timeString);
    }

    /**
     * Minimise le widget
     */
    private void minimizeWidget() {
        widgetRunning.setVisibility(View.GONE);
        widgetMinimized.setVisibility(View.VISIBLE);
    }

    /**
     * Agrandit le widget
     */
    private void expandWidget() {
        widgetMinimized.setVisibility(View.GONE);
        widgetRunning.setVisibility(View.VISIBLE);
    }

    /**
     * Charge la liste des projets depuis l'API avec Retrofit
     * ✅ IMPLÉMENTÉ: Fallback sur cache offline si échec
     */
    private void loadProjects() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("ptms_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("auth_token", "");

            if (token == null || token.isEmpty()) {
                android.util.Log.w("WIDGET_TIMER", "⚠️ Pas de token - Chargement depuis cache offline");
                loadProjectsFromCache();
                return;
            }

            android.util.Log.d("WIDGET_TIMER", "Chargement des projets depuis l'API...");

            ApiClient apiClient = ApiClient.getInstance(context);
            ApiService apiService = apiClient.getApiService();

            Call<ApiService.ProjectsResponse> call = apiService.getProjects(token);
            call.enqueue(new Callback<ApiService.ProjectsResponse>() {
                @Override
                public void onResponse(Call<ApiService.ProjectsResponse> call, Response<ApiService.ProjectsResponse> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null && response.body().success) {
                            projectList.clear();
                            projectList.addAll(response.body().projects);
                            android.util.Log.d("WIDGET_TIMER", "✅ Projets chargés depuis API: " + projectList.size());
                        } else {
                            android.util.Log.e("WIDGET_TIMER", "❌ Erreur API projets: " + response.code());
                            loadProjectsFromCache();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WIDGET_TIMER", "❌ Erreur parsing projets", e);
                        loadProjectsFromCache();
                    }
                }

                @Override
                public void onFailure(Call<ApiService.ProjectsResponse> call, Throwable t) {
                    android.util.Log.e("WIDGET_TIMER", "❌ Échec chargement projets", t);
                    loadProjectsFromCache();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("WIDGET_TIMER", "❌ Erreur loadProjects", e);
            loadProjectsFromCache();
        }
    }

    /**
     * Charge les projets depuis le cache local (fallback offline)
     */
    private void loadProjectsFromCache() {
        try {
            OfflineDatabaseHelper dbHelper = new OfflineDatabaseHelper(context);
            projectList.clear();
            List<Project> cachedProjects = dbHelper.getAllProjects();
            if (cachedProjects != null) {
                projectList.addAll(cachedProjects);
                android.util.Log.d("WIDGET_TIMER", "✅ Projets chargés depuis cache: " + projectList.size());
            }
        } catch (Exception e) {
            android.util.Log.e("WIDGET_TIMER", "❌ Erreur chargement cache projets", e);
        }
    }

    /**
     * Charge la liste des types de travail depuis l'API avec Retrofit
     * ✅ IMPLÉMENTÉ: Fallback sur cache offline si échec
     */
    private void loadWorkTypes() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("ptms_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("auth_token", "");

            if (token == null || token.isEmpty()) {
                android.util.Log.w("WIDGET_TIMER", "⚠️ Pas de token - Chargement depuis cache offline");
                loadWorkTypesFromCache();
                return;
            }

            android.util.Log.d("WIDGET_TIMER", "Chargement des types de travail depuis l'API...");

            ApiClient apiClient = ApiClient.getInstance(context);
            ApiService apiService = apiClient.getApiService();

            Call<List<WorkType>> call = apiService.getWorkTypes(token);
            call.enqueue(new Callback<List<WorkType>>() {
                @Override
                public void onResponse(Call<List<WorkType>> call, Response<List<WorkType>> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            workTypeList.clear();
                            workTypeList.addAll(response.body());
                            android.util.Log.d("WIDGET_TIMER", "✅ Types de travail chargés depuis API: " + workTypeList.size());
                        } else {
                            android.util.Log.e("WIDGET_TIMER", "❌ Erreur API types de travail: " + response.code());
                            loadWorkTypesFromCache();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WIDGET_TIMER", "❌ Erreur parsing types de travail", e);
                        loadWorkTypesFromCache();
                    }
                }

                @Override
                public void onFailure(Call<List<WorkType>> call, Throwable t) {
                    android.util.Log.e("WIDGET_TIMER", "❌ Échec chargement types de travail", t);
                    loadWorkTypesFromCache();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("WIDGET_TIMER", "❌ Erreur loadWorkTypes", e);
            loadWorkTypesFromCache();
        }
    }

    /**
     * Charge les types de travail depuis le cache local (fallback offline)
     */
    private void loadWorkTypesFromCache() {
        try {
            OfflineDatabaseHelper dbHelper = new OfflineDatabaseHelper(context);
            workTypeList.clear();
            List<WorkType> cachedWorkTypes = dbHelper.getAllWorkTypes();
            if (cachedWorkTypes != null) {
                workTypeList.addAll(cachedWorkTypes);
                android.util.Log.d("WIDGET_TIMER", "✅ Types de travail chargés depuis cache: " + workTypeList.size());
            }
        } catch (Exception e) {
            android.util.Log.e("WIDGET_TIMER", "❌ Erreur chargement cache types de travail", e);
        }
    }

    /**
     * Vérifie le statut actuel du timer au démarrage
     * ✅ IMPLÉMENTÉ: Lecture depuis SharedPreferences du TimerService
     */
    private void checkTimerStatus() {
        try {
            SharedPreferences timerPrefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE);

            boolean isRunning = timerPrefs.getBoolean("is_running", false);
            boolean isPaused = timerPrefs.getBoolean("is_paused", false);
            long elapsedSeconds = timerPrefs.getLong("elapsed_seconds", 0);
            int projectId = timerPrefs.getInt("project_id", 0);
            String projectName = timerPrefs.getString("project_name", "");

            if (isRunning) {
                // Le timer est en cours
                currentProjectId = projectId;
                currentProjectName = projectName;

                if (isPaused) {
                    android.util.Log.d("WIDGET_TIMER", "⏸️ Timer en pause - Projet: " + projectName + " (" + elapsedSeconds + "s)");
                    // Afficher l'état en pause si le widget a des vues pour cela
                    showPausedState(projectName, elapsedSeconds);
                } else {
                    android.util.Log.d("WIDGET_TIMER", "▶️ Timer actif - Projet: " + projectName + " (" + elapsedSeconds + "s)");
                    // Afficher l'état actif
                    showRunningState(projectName, elapsedSeconds);
                }
            } else {
                android.util.Log.d("WIDGET_TIMER", "⏹️ Timer arrêté");
                // Afficher l'état arrêté
                showStoppedState();
            }
        } catch (Exception e) {
            android.util.Log.e("WIDGET_TIMER", "❌ Erreur checkTimerStatus", e);
        }
    }

    /**
     * Affiche le widget dans l'état "timer en cours"
     */
    private void showRunningState(String projectName, long elapsedSeconds) {
        if (widgetRunning != null) {
            widgetRunning.setVisibility(View.VISIBLE);
            widgetStopped.setVisibility(View.GONE);
            widgetMinimized.setVisibility(View.GONE);

            if (tvProjectName != null) {
                tvProjectName.setText(projectName);
            }
            if (tvTimerDisplay != null) {
                tvTimerDisplay.setText(formatTime(elapsedSeconds));
            }
        }
    }

    /**
     * Affiche le widget dans l'état "timer en pause"
     */
    private void showPausedState(String projectName, long elapsedSeconds) {
        // Pour l'instant, afficher comme l'état running (peut être amélioré)
        showRunningState(projectName, elapsedSeconds);
    }

    /**
     * Affiche le widget dans l'état "timer arrêté"
     */
    private void showStoppedState() {
        if (widgetStopped != null) {
            widgetStopped.setVisibility(View.VISIBLE);
            widgetRunning.setVisibility(View.GONE);
            widgetMinimized.setVisibility(View.GONE);
        }
    }

    /**
     * Formate le temps en secondes en format HH:MM:SS
     */
    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Initialise le receiver pour les mises à jour du timer
     */
    private void initializeReceiver() {
        timerReceiver = new TimerUpdateReceiver();
        IntentFilter filter = new IntentFilter(TimerService.ACTION_TIMER_UPDATE);
        context.registerReceiver(timerReceiver, filter);
    }

    /**
     * Nettoie les ressources
     */
    public void destroy() {
        hideWidget();
        if (timerReceiver != null) {
            context.unregisterReceiver(timerReceiver);
        }
    }

    /**
     * Obtient le contexte d'activité pour les dialogues
     */
    private Context getActivityContext() {
        // Si on a une activité active, l'utiliser
        if (context instanceof Activity) {
            return context;
        }
        // Sinon, utiliser le contexte application mais avec le flag nécessaire
        return context;
    }

    /**
     * BroadcastReceiver pour recevoir les mises à jour du TimerService
     */
    private class TimerUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", TimerService.STATE_STOPPED);
            int elapsedSeconds = intent.getIntExtra("elapsed_seconds", 0);
            String projectName = intent.getStringExtra("project_name");

            if (projectName != null && !projectName.isEmpty()) {
                currentProjectName = projectName;
                tvProjectName.setText(projectName);
            }

            updateWidgetState(state, elapsedSeconds);
        }
    }
}
