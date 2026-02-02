# Guide des Permissions Runtime Android

## 📋 Vue d'ensemble

Depuis Android 6.0 (API 23), les permissions dangereuses doivent être demandées au runtime, pas seulement dans le manifest.

**Permissions utilisées par PTMS Mobile:**

| Permission | Usage | Android Version | Critique |
|-----------|-------|-----------------|----------|
| `RECORD_AUDIO` | Notes audio/dictée | Toutes | ✅ Oui |
| `WRITE_EXTERNAL_STORAGE` | Sauvegarde fichiers | < 13 | ⚠️ Optionnel |
| `READ_EXTERNAL_STORAGE` | Lecture fichiers | < 13 | ⚠️ Optionnel |
| `POST_NOTIFICATIONS` | Notifications sync | >= 13 | ⚠️ Optionnel |
| `INTERNET` | API calls | Toutes | ✅ Oui (auto) |
| `ACCESS_NETWORK_STATE` | Détection offline | Toutes | ✅ Oui (auto) |

---

## 🔧 Utilisation de PermissionsHelper

### 1. Vérifier une permission

```java
import com.ptms.mobile.utils.PermissionsHelper;

// Vérifier audio
if (PermissionsHelper.checkAudioPermission(this)) {
    // Permission accordée - lancer l'enregistrement
    startRecording();
} else {
    // Demander la permission
    PermissionsHelper.requestAudioPermission(this, PermissionsHelper.REQUEST_AUDIO_PERMISSION);
}
```

### 2. Demander plusieurs permissions

```java
// Demander toutes les permissions
PermissionsHelper.requestAllPermissions(this, PermissionsHelper.REQUEST_ALL_PERMISSIONS);
```

### 3. Gérer le résultat

```java
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    switch (requestCode) {
        case PermissionsHelper.REQUEST_AUDIO_PERMISSION:
            if (PermissionsHelper.verifyPermissionResults(permissions, grantResults)) {
                // Permission accordée
                startRecording();
            } else {
                // Permission refusée
                Toast.makeText(this, "Permission microphone requise pour les notes audio",
                    Toast.LENGTH_LONG).show();
            }
            break;

        case PermissionsHelper.REQUEST_ALL_PERMISSIONS:
            if (PermissionsHelper.verifyPermissionResults(permissions, grantResults)) {
                // Toutes permissions accordées
                Toast.makeText(this, "Permissions accordées", Toast.LENGTH_SHORT).show();
            } else {
                // Certaines permissions refusées
                String[] missing = PermissionsHelper.getMissingPermissions(this);
                Log.w("Permissions", "Permissions manquantes: " + Arrays.toString(missing));
            }
            break;
    }
}
```

### 4. Afficher une explication (recommandé)

```java
// Avant de demander la permission
if (PermissionsHelper.shouldShowAudioRationale(this)) {
    // L'utilisateur a déjà refusé - expliquer pourquoi c'est nécessaire
    new AlertDialog.Builder(this)
        .setTitle("Permission microphone")
        .setMessage(PermissionsHelper.getPermissionRationale(Manifest.permission.RECORD_AUDIO))
        .setPositiveButton("OK", (dialog, which) -> {
            PermissionsHelper.requestAudioPermission(this, PermissionsHelper.REQUEST_AUDIO_PERMISSION);
        })
        .setNegativeButton("Annuler", null)
        .show();
} else {
    // Première demande - demander directement
    PermissionsHelper.requestAudioPermission(this, PermissionsHelper.REQUEST_AUDIO_PERMISSION);
}
```

---

## 📝 Exemple Complet: Activity avec Audio

```java
public class CreateNoteUnifiedActivity extends AppCompatActivity {

    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        Button btnRecord = findViewById(R.id.btn_record_audio);
        btnRecord.setOnClickListener(v -> handleRecordClick());
    }

    private void handleRecordClick() {
        // Vérifier la permission avant d'enregistrer
        if (PermissionsHelper.checkAudioPermission(this)) {
            toggleRecording();
        } else {
            requestAudioPermissionWithRationale();
        }
    }

    private void requestAudioPermissionWithRationale() {
        if (PermissionsHelper.shouldShowAudioRationale(this)) {
            // Afficher explication
            new AlertDialog.Builder(this)
                .setTitle("Permission requise")
                .setMessage("L'accès au microphone est nécessaire pour enregistrer des notes audio.")
                .setPositiveButton("Autoriser", (dialog, which) -> {
                    PermissionsHelper.requestAudioPermission(this,
                        PermissionsHelper.REQUEST_AUDIO_PERMISSION);
                })
                .setNegativeButton("Annuler", null)
                .show();
        } else {
            // Première demande
            PermissionsHelper.requestAudioPermission(this,
                PermissionsHelper.REQUEST_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsHelper.REQUEST_AUDIO_PERMISSION) {
            if (PermissionsHelper.verifyPermissionResults(permissions, grantResults)) {
                // Permission accordée - lancer l'enregistrement
                toggleRecording();
            } else {
                // Permission refusée
                Toast.makeText(this,
                    "Impossible d'enregistrer sans permission microphone",
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
        isRecording = !isRecording;
    }

    private void startRecording() {
        // Votre code d'enregistrement
        Log.d("Audio", "Enregistrement démarré");
    }

    private void stopRecording() {
        // Votre code d'arrêt
        Log.d("Audio", "Enregistrement arrêté");
    }
}
```

---

## ✅ Checklist d'intégration

Pour chaque activité utilisant des permissions:

- [ ] Vérifier la permission avant utilisation
- [ ] Demander la permission si nécessaire
- [ ] Gérer `onRequestPermissionsResult()`
- [ ] Afficher explication si `shouldShowRequestPermissionRationale()`
- [ ] Prévoir un fallback si permission refusée
- [ ] Tester sur Android 6.0+ ET Android 13+
- [ ] Documenter quelle permission est requise

---

## 🔍 Debugging

### Logs utiles:

```java
// Voir toutes les permissions manquantes
String[] missing = PermissionsHelper.getMissingPermissions(this);
Log.d("Permissions", "Manquantes: " + Arrays.toString(missing));

// Vérifier l'état actuel
Log.d("Permissions", "Audio: " + PermissionsHelper.checkAudioPermission(this));
Log.d("Permissions", "Storage: " + PermissionsHelper.checkStoragePermission(this));
Log.d("Permissions", "Notifications: " + PermissionsHelper.checkNotificationPermission(this));
```

### Via ADB:

```bash
# Vérifier permissions accordées
adb shell dumpsys package com.ptms.mobile | grep permission

# Révoquer une permission (test)
adb shell pm revoke com.ptms.mobile android.permission.RECORD_AUDIO

# Accorder une permission
adb shell pm grant com.ptms.mobile android.permission.RECORD_AUDIO
```

---

## 📚 Références

- [Android Permissions Guide](https://developer.android.com/guide/topics/permissions/overview)
- [Request Runtime Permissions](https://developer.android.com/training/permissions/requesting)
- [Android 13 Notification Permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)

---

**Mis à jour:** 21 Octobre 2025
**Version:** PTMS Mobile v2.0
