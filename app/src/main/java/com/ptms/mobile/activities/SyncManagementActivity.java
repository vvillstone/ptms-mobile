package com.ptms.mobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ptms.mobile.R;
import com.ptms.mobile.adapters.SyncItemsAdapter;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.models.SyncItem;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.sync.BidirectionalSyncManager;
import com.ptms.mobile.sync.SyncStateManager;
import com.ptms.mobile.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Activité pour afficher et gérer les fichiers de synchronisation JSON
 * Affiche les fichiers ToSync (orange) et AlreadySync (vert)
 */
public class SyncManagementActivity extends AppCompatActivity {

    private static final String TAG = "SyncFilesActivity";

    private BidirectionalSyncManager syncManager;
    private ListView listViewSyncFiles;
    private TextView tvSyncStats;
    private Button btnSyncAll;
    private Button btnRefresh;
    private SharedPreferences prefs;
    private ApiService apiService;
    private String authToken;
    private SyncItemsAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_management);

        // Initialisation
        syncManager = new BidirectionalSyncManager(this);
        prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
        apiService = ApiClient.getInstance(this).getApiService();
        authToken = prefs.getString("auth_token", "");

        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupListeners();
        loadSyncFiles();

        Log.d(TAG, "SyncFilesActivity créée");
    }
    
    private void initViews() {
        listViewSyncFiles = findViewById(R.id.list_view_sync_files);
        tvSyncStats = findViewById(R.id.tv_sync_stats);
        btnSyncAll = findViewById(R.id.btn_sync_all);
        btnRefresh = findViewById(R.id.btn_refresh);

        // Initialiser l'adapter pour la liste
        adapter = new SyncItemsAdapter(this);
        listViewSyncFiles.setAdapter(adapter);

        // Configurer le listener pour la suppression
        adapter.setOnItemActionListener(new SyncItemsAdapter.OnItemActionListener() {
            @Override
            public void onDeleteItem(SyncItem item, int position) {
                deleteItemWithConfirmation(item);
            }
        });
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Gestion de Synchronisation");
            }
        }
    }
    
    private void setupListeners() {
        // Bouton synchroniser tout
        if (btnSyncAll != null) {
            btnSyncAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    syncAllPendingFiles();
                }
            });
        }
        
        // Bouton rafraîchir
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadSyncFiles();
                }
            });
        }
    }
    
    private void loadSyncFiles() {
        try {
            Log.d(TAG, "Chargement des éléments de synchronisation...");

            // Charger les éléments depuis SQLite
            List<SyncItem> syncItems = new ArrayList<>();

            // 1. Charger les rapports de temps en attente
            List<TimeReport> pendingReports = syncManager.getOfflineDatabaseHelper().getAllPendingTimeReports();
            for (TimeReport report : pendingReports) {
                syncItems.add(SyncItem.fromTimeReport(report));
            }

            // 2. Charger les notes de projet en attente
            try {
                List<ProjectNote> pendingNotes = syncManager.getOfflineDatabaseHelper().getAllPendingProjectNotes();
                for (ProjectNote note : pendingNotes) {
                    syncItems.add(SyncItem.fromProjectNote(note));
                }
            } catch (Exception e) {
                Log.w(TAG, "Erreur chargement notes (normal si table n'existe pas)", e);
            }

            // Mettre à jour l'adapter
            adapter.setItems(syncItems);

            // Mettre à jour les statistiques
            updateSyncStats();

            Log.d(TAG, "Éléments de synchronisation chargés: " + syncItems.size());

        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement éléments de synchronisation", e);
            Toast.makeText(this, "Erreur chargement: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void updateSyncStats() {
        try {
            // ✅ Utiliser BidirectionalSyncManager
            int pendingCount = syncManager.getPendingSyncCount();

            String statsText = String.format(
                "En attente: %d éléments\nMode: %s",
                pendingCount,
                NetworkUtils.isOnline(this) ? "Online" : "Offline"
            );

            if (tvSyncStats != null) {
                tvSyncStats.setText(statsText);
            }

            // Activer/désactiver le bouton de synchronisation
            boolean isOnline = NetworkUtils.isOnline(this);
            if (btnSyncAll != null) {
                btnSyncAll.setEnabled(pendingCount > 0 && isOnline);

                // Texte du bouton selon l'état
                if (!isOnline) {
                    btnSyncAll.setText("⚠️ Hors ligne");
                } else if (pendingCount == 0) {
                    btnSyncAll.setText("Aucun élément à synchroniser");
                } else {
                    btnSyncAll.setText(String.format("Synchroniser (%d)", pendingCount));
                }
            }

            Log.d(TAG, "Statistiques mises à jour: " + statsText);

        } catch (Exception e) {
            Log.e(TAG, "Erreur mise à jour statistiques", e);
        }
    }
    
    private void syncAllPendingFiles() {
        // Vérifier si une sync est déjà en cours
        SyncStateManager syncStateManager = SyncStateManager.getInstance(this);
        if (syncStateManager.isSyncing()) {
            Toast.makeText(this, "⚠️ Synchronisation déjà en cours", Toast.LENGTH_SHORT).show();
            return;
        }

        // Désactiver le bouton immédiatement pour éviter les clics multiples
        if (btnSyncAll != null) {
            btnSyncAll.setEnabled(false);
            btnSyncAll.setText("Synchronisation...");
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Connexion réseau requise pour la synchronisation", Toast.LENGTH_LONG).show();
            // Réactiver le bouton en cas d'erreur
            if (btnSyncAll != null) {
                btnSyncAll.setEnabled(true);
                updateSyncButtonText();
            }
            return;
        }

        // Obtenir le nombre total d'éléments à synchroniser
        int totalItems = syncManager.getPendingSyncCount();

        Log.d(TAG, "Début synchronisation de tous les fichiers en attente: " + totalItems + " items");
        Toast.makeText(this, "Synchronisation en cours...", Toast.LENGTH_SHORT).show();

        // Notifier le début de la synchronisation globale
        syncStateManager.startSync(totalItems);

        // ✅ Utiliser BidirectionalSyncManager.syncFull()
        syncManager.syncFull(new BidirectionalSyncManager.SyncCallback() {
            @Override
            public void onSyncStarted(String phase) {
                Log.d(TAG, "Phase de synchronisation: " + phase);
            }

            @Override
            public void onSyncProgress(String message, int current, int total) {
                Log.d(TAG, "Progrès: " + message + " (" + current + "/" + total + ")");
                // Mettre à jour la progression globale
                syncStateManager.updateProgress(current);
            }

            @Override
            public void onSyncCompleted(BidirectionalSyncManager.SyncResult result) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Synchronisation réussie: " + result.getSummary());
                    Toast.makeText(SyncManagementActivity.this, "✅ " + result.getSummary(), Toast.LENGTH_LONG).show();

                    // Notifier la fin de la synchronisation
                    syncStateManager.endSync();

                    // Recharger les fichiers et statistiques
                    loadSyncFiles();

                    // Réactiver le bouton
                    if (btnSyncAll != null) {
                        btnSyncAll.setEnabled(true);
                        updateSyncButtonText();
                    }
                });
            }

            @Override
            public void onSyncError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Erreur synchronisation: " + error);
                    Toast.makeText(SyncManagementActivity.this, "❌ Erreur synchronisation: " + error, Toast.LENGTH_LONG).show();

                    // Notifier la fin de la synchronisation même en cas d'erreur
                    syncStateManager.endSync();

                    // Recharger les fichiers et statistiques
                    loadSyncFiles();

                    // Réactiver le bouton
                    if (btnSyncAll != null) {
                        btnSyncAll.setEnabled(true);
                        updateSyncButtonText();
                    }
                });
            }
        });
    }

    /**
     * Met à jour le texte du bouton de synchronisation selon le nombre d'éléments en attente
     */
    private void updateSyncButtonText() {
        try {
            int pendingCount = syncManager.getPendingSyncCount();
            boolean isOnline = NetworkUtils.isOnline(this);

            if (btnSyncAll != null) {
                // Activer/désactiver selon connexion ET éléments en attente
                btnSyncAll.setEnabled(pendingCount > 0 && isOnline);

                // Texte selon l'état
                if (!isOnline) {
                    btnSyncAll.setText("⚠️ Hors ligne");
                } else if (pendingCount == 0) {
                    btnSyncAll.setText("Aucun élément à synchroniser");
                } else {
                    btnSyncAll.setText(String.format("Synchroniser (%d)", pendingCount));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur updateSyncButtonText", e);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sync_files_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            // Bouton retour
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            loadSyncFiles();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Supprime un élément en attente ou en erreur après confirmation
     */
    private void deleteItemWithConfirmation(final SyncItem item) {
        String statusText = item.getStatus() == SyncItem.SyncStatus.ERROR ?
            "en erreur" : "en attente de synchronisation";

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Supprimer l'élément")
            .setMessage("Voulez-vous vraiment supprimer cet élément " + statusText + " ?\n\n" +
                    item.getTypeLabel() + ": " + item.getTitle())
            .setPositiveButton("Supprimer", (dialog, which) -> deleteItem(item))
            .setNegativeButton("Annuler", null)
            .show();
    }

    /**
     * Supprime un élément de la base de données SQLite
     */
    private void deleteItem(SyncItem item) {
        try {
            boolean deleted = false;

            switch (item.getType()) {
                case TIME_REPORT:
                    // Supprimer le rapport de temps
                    deleted = syncManager.getOfflineDatabaseHelper().deleteTimeReport((int) item.getId());
                    break;
                case PROJECT_NOTE:
                    // Supprimer la note de projet
                    deleted = syncManager.getOfflineDatabaseHelper().deleteProjectNote((int) item.getId());
                    break;
                case MEDIA:
                    // À implémenter si nécessaire
                    break;
            }

            if (deleted) {
                Toast.makeText(this, "✅ Élément supprimé", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Élément supprimé: " + item.getType() + " ID=" + item.getId());
                // Recharger la liste
                loadSyncFiles();
            } else {
                Toast.makeText(this, "❌ Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Échec suppression: " + item.getType() + " ID=" + item.getId());
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur suppression élément", e);
            Toast.makeText(this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les fichiers quand on revient sur l'activité
        loadSyncFiles();
        Log.d(TAG, "SyncFilesActivity onResume - rechargement des fichiers");
    }
}
