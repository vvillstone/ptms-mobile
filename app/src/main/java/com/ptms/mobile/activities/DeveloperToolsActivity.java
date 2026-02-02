package com.ptms.mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ptms.mobile.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Mode Développeur - Liste de TOUTES les activités de l'app
 * Pour faciliter la navigation et le debug
 */
public class DeveloperToolsActivity extends AppCompatActivity {

    private static final String TAG = "DevModeActivity";

    // Liste COMPLÈTE de toutes les activités avec numéros
    private static class ActivityItem {
        String id;
        String name;
        String description;
        Class<?> activityClass;

        ActivityItem(String id, String name, String description, Class<?> activityClass) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.activityClass = activityClass;
        }

        @Override
        public String toString() {
            return id + " - " + name + "\n    " + description;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer_tools);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("🛠️ Mode Développeur");
        }

        ListView listView = findViewById(R.id.listActivities);

        // TOUTES les activités de l'app avec ID unique
        List<ActivityItem> activities = new ArrayList<>();

        // ===== CORE =====
        activities.add(new ActivityItem("A001", "SplashActivity", "Point d'entrée principal de l'app", SplashActivity.class));
        activities.add(new ActivityItem("A002", "AuthenticationActivity", "Écran de connexion principal", AuthenticationActivity.class));
        activities.add(new ActivityItem("A003", "FirstLaunchAuthActivity", "Authentification au premier lancement", FirstLaunchAuthActivity.class));
        activities.add(new ActivityItem("A004", "HomeActivity", "Tableau de bord principal après login", HomeActivity.class));
        activities.add(new ActivityItem("A005", "AppLoadingActivity", "Écran de chargement de l'app", AppLoadingActivity.class));

        // ===== TIME ENTRY & REPORTS =====
        activities.add(new ActivityItem("B001", "TimeEntryActivity", "Saisie d'heures (Local-First)", TimeEntryActivity.class));
        activities.add(new ActivityItem("B002", "TimeReportsActivity", "Rapports de temps (Groupement jour/semaine/mois)", TimeReportsActivity.class));
        activities.add(new ActivityItem("B003", "StatisticsActivity", "Statistiques et tableaux de bord", StatisticsActivity.class));

        // ===== USER MANAGEMENT =====
        activities.add(new ActivityItem("C001", "UserProfileActivity", "Profil utilisateur", UserProfileActivity.class));
        activities.add(new ActivityItem("C002", "AppSettingsActivity", "Paramètres de l'app", AppSettingsActivity.class));

        // ===== NOTES =====
        activities.add(new ActivityItem("D001", "NotesActivity", "Menu principal des notes", NotesActivity.class));
        activities.add(new ActivityItem("D002", "NotesListActivity", "Liste de toutes les notes avec filtres", NotesListActivity.class));
        activities.add(new ActivityItem("D003", "ProjectSelectorForNotesActivity", "Sélection projet pour notes", ProjectSelectorForNotesActivity.class));
        activities.add(new ActivityItem("D004", "NoteEditorActivity", "Création/édition note unifiée (texte/audio/dictée)", NoteEditorActivity.class));
        activities.add(new ActivityItem("D005", "NoteDetailActivity", "Détail d'une note", NoteDetailActivity.class));
        activities.add(new ActivityItem("D006", "NotesTimelineActivity", "Timeline/Agenda des notes", NotesTimelineActivity.class));
        activities.add(new ActivityItem("D007", "NoteCategoriesActivity", "Gestion des catégories de notes", NoteCategoriesActivity.class));

        // ===== CHAT =====
        activities.add(new ActivityItem("E001", "ConversationsActivity", "Liste des conversations/salons", ConversationsActivity.class));
        activities.add(new ActivityItem("E002", "ChatWebSocketActivity", "Interface de chat WebSocket (V2)", ChatWebSocketActivity.class));
        activities.add(new ActivityItem("E003", "ChatActivity", "Interface de chat classique (V1)", ChatActivity.class));
        activities.add(new ActivityItem("E004", "ChatUsersActivity", "Liste des utilisateurs pour chat", ChatUsersActivity.class));
        activities.add(new ActivityItem("E005", "ChatParticipantsActivity", "Participants d'une conversation", ChatParticipantsActivity.class));
        activities.add(new ActivityItem("E006", "NewConversationActivity", "Créer une nouvelle conversation", NewConversationActivity.class));

        // ===== DIAGNOSTIC & TESTS =====
        activities.add(new ActivityItem("F001", "SystemDiagnosticsActivity", "Diagnostic complet du système", SystemDiagnosticsActivity.class));
        activities.add(new ActivityItem("F002", "OfflineModeDiagnosticsActivity", "Diagnostic mode offline/sync", OfflineModeDiagnosticsActivity.class));
        activities.add(new ActivityItem("F003", "NotesDiagnosticsActivity", "Diagnostic du système de notes", NotesDiagnosticsActivity.class));
        activities.add(new ActivityItem("F004", "RoleCompatibilityTestActivity", "Test de compatibilité des rôles", RoleCompatibilityTestActivity.class));

        // ===== SYNC & UTILITIES =====
        activities.add(new ActivityItem("G001", "SyncManagementActivity", "Gestion de la synchronisation", SyncManagementActivity.class));
        activities.add(new ActivityItem("G002", "TimelineActivity", "Timeline/Agenda général", TimelineActivity.class));
        activities.add(new ActivityItem("G003", "GlobalSearchActivity", "Recherche globale dans l'app", GlobalSearchActivity.class));

        // Adapter
        ArrayAdapter<ActivityItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                activities
        );
        listView.setAdapter(adapter);

        // Click listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ActivityItem item = activities.get(position);
            try {
                Intent intent = new Intent(DeveloperToolsActivity.this, item.activityClass);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
