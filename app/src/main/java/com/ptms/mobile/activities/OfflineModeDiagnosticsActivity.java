package com.ptms.mobile.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ptms.mobile.R;

/**
 * Activité de diagnostic du mode offline
 * Affiche toutes les données sauvegardées pour le mode offline
 */
public class OfflineModeDiagnosticsActivity extends AppCompatActivity {

    private TextView tvDiagnostic;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_mode_diagnostics);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Diagnostic Mode Offline");
        }

        tvDiagnostic = findViewById(R.id.tv_diagnostic);
        prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);

        displayDiagnostic();
    }

    private void displayDiagnostic() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DIAGNOSTIC MODE OFFLINE ===\n\n");

        // 1. Vérifier offline_login_enabled
        boolean offlineEnabled = prefs.getBoolean("offline_login_enabled", false);
        sb.append("📌 Login offline activé: ");
        sb.append(offlineEnabled ? "✅ OUI" : "❌ NON");
        sb.append("\n\n");

        // 2. Vérifier credentials offline
        String offlineEmail = prefs.getString("offline_email", null);
        String offlinePasswordHash = prefs.getString("offline_password_hash", null);

        sb.append("📧 Email offline: ");
        if (offlineEmail != null) {
            sb.append("✅ ").append(offlineEmail);
        } else {
            sb.append("❌ ABSENT");
        }
        sb.append("\n\n");

        sb.append("🔐 Hash mot de passe: ");
        if (offlinePasswordHash != null) {
            sb.append("✅ ").append(offlinePasswordHash.substring(0, Math.min(16, offlinePasswordHash.length()))).append("...");
        } else {
            sb.append("❌ ABSENT");
        }
        sb.append("\n\n");

        // 3. Vérifier données utilisateur (nouvelles clés)
        int userId = prefs.getInt("user_id", -1);
        String userName = prefs.getString("user_name", null);
        String userEmail = prefs.getString("user_email", null);
        int userType = prefs.getInt("user_type", -1);

        // Compatibilité avec anciennes clés
        int oldEmployeeId = prefs.getInt("employee_id", -1);
        String oldEmployeeName = prefs.getString("employee_name", null);

        sb.append("👤 User ID (nouvelle clé): ");
        if (userId != -1) {
            sb.append("✅ ").append(userId);
        } else {
            sb.append("❌ ABSENT");
        }
        sb.append("\n");

        sb.append("👤 Employee ID (ancienne clé): ");
        if (oldEmployeeId != -1) {
            sb.append("⚠️ ").append(oldEmployeeId).append(" (obsolète)");
        } else {
            sb.append("✓ Absent (normal)");
        }
        sb.append("\n\n");

        sb.append("👤 User Name (nouvelle clé): ");
        if (userName != null) {
            sb.append("✅ ").append(userName);
        } else {
            sb.append("❌ ABSENT");
        }
        sb.append("\n");

        sb.append("👤 Employee Name (ancienne clé): ");
        if (oldEmployeeName != null) {
            sb.append("⚠️ ").append(oldEmployeeName).append(" (obsolète)");
        } else {
            sb.append("✓ Absent (normal)");
        }
        sb.append("\n\n");

        sb.append("📧 User Email: ");
        if (userEmail != null) {
            sb.append("✅ ").append(userEmail);
        } else {
            sb.append("❌ ABSENT");
        }
        sb.append("\n\n");

        sb.append("🔰 User Type: ");
        if (userType != -1) {
            String typeText = "";
            switch (userType) {
                case 1: typeText = "Admin"; break;
                case 2: typeText = "Manager"; break;
                case 3: typeText = "Accountant"; break;
                case 4: typeText = "Employee"; break;
                case 5: typeText = "Viewer"; break;
                default: typeText = "Inconnu"; break;
            }
            sb.append("✅ ").append(userType).append(" (").append(typeText).append(")");
        } else {
            sb.append("❌ ABSENT");
        }
        sb.append("\n\n");

        // 4. Vérifier token
        String authToken = prefs.getString("auth_token", null);
        sb.append("🔑 Auth Token: ");
        if (authToken != null) {
            sb.append("✅ ").append(authToken.substring(0, Math.min(20, authToken.length()))).append("...");
        } else {
            sb.append("❌ ABSENT");
        }
        sb.append("\n\n");

        // 5. Conclusion
        sb.append("=== CONCLUSION ===\n\n");

        // Utiliser les nouvelles clés avec fallback sur anciennes
        int finalUserId = (userId != -1) ? userId : oldEmployeeId;
        String finalUserName = (userName != null) ? userName : oldEmployeeName;

        boolean canLoginOffline = offlineEnabled &&
                                  offlineEmail != null &&
                                  offlinePasswordHash != null &&
                                  finalUserId != -1 &&
                                  finalUserName != null;

        if (canLoginOffline) {
            sb.append("✅ Login offline POSSIBLE\n");
            sb.append("Toutes les données nécessaires sont présentes.");
        } else {
            sb.append("❌ Login offline IMPOSSIBLE\n\n");
            sb.append("Données manquantes:\n");
            if (!offlineEnabled) sb.append("  - offline_login_enabled = false\n");
            if (offlineEmail == null) sb.append("  - offline_email absent\n");
            if (offlinePasswordHash == null) sb.append("  - offline_password_hash absent\n");
            if (finalUserId == -1) sb.append("  - user_id / employee_id absent ⚠️\n");
            if (finalUserName == null) sb.append("  - user_name / employee_name absent ⚠️\n");
            sb.append("\n⚠️ Vous devez vous connecter EN LIGNE une première fois pour sauvegarder ces données.");
        }

        tvDiagnostic.setText(sb.toString());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
