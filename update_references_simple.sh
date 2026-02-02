#!/bin/bash

# Script simplifié de mise à jour des références
echo "🔄 Mise à jour des références aux activités..."

BASE_DIR="C:/Devs/web/appAndroid/app/src/main/java/com/ptms/mobile"

# Fonction pour remplacer simplement une classe par une autre
replace_simple() {
    local old=$1
    local new=$2
    echo "  → $old → $new"
    find "$BASE_DIR" -name "*.java" -type f -exec sed -i "s/${old}\.class/${new}.class/g" {} \;
    find "$BASE_DIR" -name "*.java" -type f -exec sed -i "s/import com\.ptms\.mobile\.activities\.${old};/import com.ptms.mobile.activities.${new};/g" {} \;
}

# CORE
replace_simple "MainActivity" "SplashActivity"
replace_simple "LoginActivity" "AuthenticationActivity"
replace_simple "LoadingActivity" "AppLoadingActivity"
replace_simple "InitialAuthActivity" "FirstLaunchAuthActivity"

# DASHBOARD
replace_simple "DashboardActivity" "HomeActivity"
replace_simple "StatisticsDashboardActivity" "StatisticsActivity"

# TIME
replace_simple "OfflineTimeEntryActivity" "TimeEntryActivity"
replace_simple "ReportsEnhancedActivity" "TimeReportsActivity"
replace_simple "AgendaActivity" "TimelineActivity"

# NOTES
replace_simple "NotesMenuActivity" "NotesActivity"
replace_simple "AllNotesActivity" "NotesListActivity"
replace_simple "CreateNoteUnifiedActivity" "NoteEditorActivity"
replace_simple "NoteViewerActivity" "NoteDetailActivity"
replace_simple "ProjectNotesActivity" "ProjectNotesListActivity"
replace_simple "NotesAgendaActivity" "NotesTimelineActivity"

# CHAT
replace_simple "ChatRoomsActivity" "ConversationsActivity"
replace_simple "ChatActivityV2" "ChatWebSocketActivity"
replace_simple "ChatUsersListActivity" "ChatUsersActivity"
replace_simple "CreateConversationActivity" "NewConversationActivity"

# PROFILE
replace_simple "ProfileActivity" "UserProfileActivity"
replace_simple "SettingsActivity" "AppSettingsActivity"
replace_simple "SearchActivity" "GlobalSearchActivity"

# DIAGNOSTIC
replace_simple "DevModeActivity" "DeveloperToolsActivity"
replace_simple "DiagnosticActivity" "SystemDiagnosticsActivity"
replace_simple "OfflineDiagnosticActivity" "OfflineModeDiagnosticsActivity"
replace_simple "NotesDiagnosticActivity" "NotesDiagnosticsActivity"
replace_simple "RoleTestActivity" "RoleCompatibilityTestActivity"
replace_simple "SyncFilesActivity" "SyncManagementActivity"

echo ""
echo "✅ Références mises à jour"
