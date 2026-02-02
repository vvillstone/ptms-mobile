package com.ptms.mobile.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.ptms.mobile.R;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.TimeReport;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité de visualisation détaillée et d'édition d'un rapport de temps
 */
public class TimeReportDetailActivity extends AppCompatActivity {

    private static final String TAG = "TimeReportDetail";
    public static final String EXTRA_REPORT_ID = "report_id";
    public static final String EXTRA_REPORT_JSON = "report_json";  // Pour passer l'objet complet

    // Views
    private Toolbar toolbar;
    private ImageView ivReportImage;
    private Button btnViewImage, btnEdit, btnDelete;
    private TextView tvProject, tvWorkType, tvDate, tvHours, tvTimeRange, tvDescription, tvStatus, tvSyncStatus;
    private ProgressBar progressBar;
    private View dividerAfterImage;

    // Data
    private TimeReport report;
    private SharedPreferences prefs;
    private ApiService apiService;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_report_detail);

        // Initialisation
        prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
        apiService = ApiClient.getInstance(this).getApiService();
        authToken = prefs.getString("auth_token", null);

        initViews();
        setupToolbar();
        loadReportData();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivReportImage = findViewById(R.id.iv_report_image);
        btnViewImage = findViewById(R.id.btn_view_image);
        dividerAfterImage = findViewById(R.id.divider_after_image);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        tvProject = findViewById(R.id.tv_project);
        tvWorkType = findViewById(R.id.tv_work_type);
        tvDate = findViewById(R.id.tv_date);
        tvHours = findViewById(R.id.tv_hours);
        tvTimeRange = findViewById(R.id.tv_time_range);
        tvDescription = findViewById(R.id.tv_description);
        tvStatus = findViewById(R.id.tv_status);
        tvSyncStatus = findViewById(R.id.tv_sync_status);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Détails du rapport");
        }
    }

    private void loadReportData() {
        // Récupérer le rapport depuis l'intent
        String reportJson = getIntent().getStringExtra(EXTRA_REPORT_JSON);

        if (reportJson != null) {
            // Désérialiser le JSON (utiliser Gson)
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                report = gson.fromJson(reportJson, TimeReport.class);
                displayReport();
            } catch (Exception e) {
                Log.e(TAG, "Erreur désérialisation rapport", e);
                Toast.makeText(this, "Erreur de chargement du rapport", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            // Fallback: charger depuis l'ID
            int reportId = getIntent().getIntExtra(EXTRA_REPORT_ID, -1);
            if (reportId != -1) {
                loadReportFromServer(reportId);
            } else {
                Toast.makeText(this, "Rapport introuvable", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void loadReportFromServer(int reportId) {
        setLoading(true);

        // TODO: Appeler l'API pour récupérer le rapport
        // Pour l'instant, on affiche juste une erreur
        setLoading(false);
        Toast.makeText(this, "Fonctionnalité non implémentée", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void displayReport() {
        if (report == null) return;

        // Projet
        tvProject.setText(report.getProjectName() != null ? report.getProjectName() : "Sans projet");

        // Type de travail
        tvWorkType.setText(report.getWorkTypeName() != null ? report.getWorkTypeName() : "Non spécifié");

        // Date
        tvDate.setText(formatDate(report.getReportDate()));

        // Heures
        tvHours.setText(report.getFormattedHours());

        // Horaires
        String timeRange = formatTime(report.getDatetimeFrom()) + " - " + formatTime(report.getDatetimeTo());
        tvTimeRange.setText(timeRange);

        // Description
        tvDescription.setText(report.getDescription() != null && !report.getDescription().isEmpty()
            ? report.getDescription()
            : "Aucune description");

        // Statut
        tvStatus.setText(report.getStatusText());
        tvStatus.setBackgroundColor(getStatusColor(report.getValidationStatus()));

        // Statut de synchronisation
        tvSyncStatus.setText(report.getSyncStatusText());
        tvSyncStatus.setTextColor(report.getSyncStatusColor());

        // Image
        if (report.hasImage()) {
            displayImage(report.getImagePath());
        } else {
            ivReportImage.setVisibility(View.GONE);
            btnViewImage.setVisibility(View.GONE);
            dividerAfterImage.setVisibility(View.GONE);
        }

        // Désactiver l'édition si le rapport est approuvé ou rejeté
        if ("approved".equalsIgnoreCase(report.getValidationStatus()) ||
            "rejected".equalsIgnoreCase(report.getValidationStatus())) {
            btnEdit.setEnabled(false);
            btnEdit.setAlpha(0.5f);
            btnDelete.setEnabled(false);
            btnDelete.setAlpha(0.5f);
        }
    }

    private void displayImage(String imagePath) {
        ivReportImage.setVisibility(View.VISIBLE);
        btnViewImage.setVisibility(View.VISIBLE);
        dividerAfterImage.setVisibility(View.VISIBLE);

        // Charger l'image depuis le stockage local ou URL
        File imageFile = new File(imagePath);

        if (imageFile.exists()) {
            // Image locale
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            ivReportImage.setImageBitmap(bitmap);
        } else if (imagePath.startsWith("http")) {
            // Image distante (TODO: charger avec Glide ou Picasso)
            Toast.makeText(this, "Chargement image distante non implémenté", Toast.LENGTH_SHORT).show();
        } else {
            ivReportImage.setVisibility(View.GONE);
            btnViewImage.setVisibility(View.GONE);
            dividerAfterImage.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnViewImage.setOnClickListener(v -> {
            if (report != null && report.hasImage()) {
                Intent intent = new Intent(this, MediaViewerActivity.class);
                intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_URL, report.getImagePath());
                intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, "image");
                intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TITLE, "Photo du rapport");
                startActivity(intent);
            }
        });

        btnEdit.setOnClickListener(v -> {
            if (report != null) {
                // Ouvrir TimeEntryActivity en mode édition
                Intent intent = new Intent(this, TimeEntryActivity.class);
                intent.putExtra("EDIT_MODE", true);
                intent.putExtra("REPORT_ID", report.getId());

                // Passer les données du rapport
                com.google.gson.Gson gson = new com.google.gson.Gson();
                intent.putExtra("REPORT_JSON", gson.toJson(report));

                startActivity(intent);
                finish();  // Fermer l'activité de détails
            }
        });

        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmation();
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Confirmer la suppression")
            .setMessage("Êtes-vous sûr de vouloir supprimer ce rapport ?")
            .setPositiveButton("Supprimer", (dialog, which) -> deleteReport())
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void deleteReport() {
        if (report == null) return;

        setLoading(true);

        // TODO: Appeler l'API pour supprimer le rapport
        // Pour l'instant, simuler la suppression

        new android.os.Handler().postDelayed(() -> {
            setLoading(false);
            Toast.makeText(this, "Rapport supprimé", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }, 1000);
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String formatTime(String datetimeStr) {
        if (datetimeStr == null || datetimeStr.isEmpty()) return "";

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.FRANCE);
            Date date = inputFormat.parse(datetimeStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // Essayer de retourner juste l'heure si c'est déjà au format HH:mm
            if (datetimeStr.contains(":")) {
                return datetimeStr.substring(datetimeStr.lastIndexOf(" ") + 1, datetimeStr.length());
            }
            return datetimeStr;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFFFF9800; // Orange par défaut

        switch (status.toLowerCase()) {
            case "approved":
                return 0xFF4CAF50; // Vert
            case "rejected":
                return 0xFFF44336; // Rouge
            default:
                return 0xFFFF9800; // Orange
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnEdit.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
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
