#!/bin/bash

# Script de mise à jour des références aux layouts dans les fichiers Java
echo "🔄 Mise à jour des références aux layouts..."

BASE_DIR="C:/Devs/web/appAndroid/app/src/main/java/com/ptms/mobile"

# Fonction pour remplacer les références de layout
replace_layout() {
    local old=$1
    local new=$2
    echo "  → R.layout.$old → R.layout.$new"
    find "$BASE_DIR" -name "*.java" -type f -exec sed -i "s/R\.layout\.${old}/R.layout.${new}/g" {} \;
}

# CORE
replace_layout "activity_main" "activity_splash"
replace_layout "activity_login" "activity_authentication"
replace_layout "activity_loading" "activity_app_loading"
replace_layout "activity_initial_auth" "activity_first_launch_auth"

# DASHBOARD
replace_layout "activity_dashboard" "activity_home"
replace_layout "activity_statistics_dashboard" "activity_statistics"

# TIME
replace_layout "activity_offline_time_entry" "activity_time_entry"
replace_layout "activity_reports_enhanced" "activity_time_reports"
replace_layout "activity_agenda" "activity_timeline"

# NOTES
replace_layout "activity_notes_menu" "activity_notes"
replace_layout "activity_project_notes_list" "activity_project_selector_for_notes"
replace_layout "activity_project_notes" "activity_project_notes_list"
replace_layout "activity_create_note_unified" "activity_note_editor"
replace_layout "activity_note_viewer" "activity_note_detail"
replace_layout "activity_notes_agenda" "activity_notes_timeline"

# CHAT
replace_layout "activity_chat_rooms" "activity_conversations"
replace_layout "activity_chat_users_list" "activity_chat_users"
replace_layout "activity_create_conversation" "activity_new_conversation"

# PROFILE
replace_layout "activity_profile" "activity_user_profile"
replace_layout "activity_settings" "activity_app_settings"
replace_layout "activity_search" "activity_global_search"

# DIAGNOSTIC
replace_layout "activity_dev_mode" "activity_developer_tools"
replace_layout "activity_diagnostic" "activity_system_diagnostics"
replace_layout "activity_offline_diagnostic" "activity_offline_mode_diagnostics"
replace_layout "activity_notes_diagnostic" "activity_notes_diagnostics"
replace_layout "activity_role_test" "activity_role_compatibility_test"
replace_layout "activity_sync_files" "activity_sync_management"

echo ""
echo "✅ Références aux layouts mises à jour"
