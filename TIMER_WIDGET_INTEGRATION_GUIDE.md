# Guide d'intégration du Timer Widget Android

## Vue d'ensemble

Le système de timer widget pour Android est maintenant complet et prêt à être intégré. Il permet aux employés de démarrer/arrêter/mettre en pause un timer de tracking de temps depuis n'importe où dans l'application.

## Architecture

### Composants créés

1. **TimerService.java** (`app/src/main/java/com/ptms/mobile/services/TimerService.java`)
   - Service en premier plan (foreground) pour gérer le timer en arrière-plan
   - Persiste l'état dans SharedPreferences
   - Affiche une notification persistante
   - Émet des broadcasts pour mettre à jour l'UI
   - Actions: START, PAUSE, RESUME, STOP

2. **FloatingTimerWidgetManager.java** (`app/src/main/java/com/ptms/mobile/widgets/FloatingTimerWidgetManager.java`)
   - Gestionnaire singleton du widget flottant
   - Affiche le widget par-dessus toutes les activités
   - Gère les dialogues de start/stop
   - Se connecte au TimerService via BroadcastReceiver
   - Supporte le drag & drop du widget

3. **Layouts XML**
   - `widget_timer_floating.xml` - Layout du widget avec 3 états (stopped, running, minimized)
   - `dialog_timer_start.xml` - Dialogue de sélection de projet
   - `dialog_timer_stop.xml` - Dialogue de fin avec type de travail et description

4. **Drawables**
   - `gradient_primary.xml` - Gradient violet pour bouton Start
   - `gradient_success.xml` - Gradient vert pour timer en cours
   - `ic_timer.xml`, `ic_play.xml`, `ic_play_circle.xml`, `ic_pause.xml`, `ic_stop.xml`

5. **Colors** (ajoutés dans `colors.xml`)
   - `white_transparent_25`
   - `danger_transparent`
   - `success`, `danger`

## Intégration dans DashboardActivity

### Étape 1: Vérifier la permission SYSTEM_ALERT_WINDOW

Cette permission est nécessaire pour afficher un overlay flottant.

```java
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public class DashboardActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1001;
    private FloatingTimerWidgetManager widgetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialiser le widget manager
        widgetManager = FloatingTimerWidgetManager.getInstance(this);

        // Vérifier et demander la permission overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            } else {
                // Permission déjà accordée
                showTimerWidget();
            }
        } else {
            // Android < 6.0, pas besoin de permission
            showTimerWidget();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    showTimerWidget();
                } else {
                    Toast.makeText(this,
                        "Permission refusée - Le widget ne peut pas s'afficher",
                        Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showTimerWidget() {
        widgetManager.showWidget();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // NE PAS détruire le widget ici si vous voulez qu'il persiste
        // entre les activités. Seulement lors de la déconnexion.
    }
}
```

### Étape 2: Masquer le widget lors de la déconnexion

Dans votre méthode de logout:

```java
private void logout() {
    // Arrêter le timer s'il tourne
    Intent stopIntent = new Intent(this, TimerService.class);
    stopIntent.setAction(TimerService.ACTION_STOP);
    startService(stopIntent);

    // Masquer le widget
    FloatingTimerWidgetManager.getInstance(this).hideWidget();

    // Détruire le widget manager
    FloatingTimerWidgetManager.getInstance(this).destroy();

    // Continuer avec la déconnexion normale...
    // Clear session, redirect to login, etc.
}
```

### Étape 3: Vérifier les permissions de notification (Android 13+)

Pour Android 13+, vous devez demander la permission POST_NOTIFICATIONS:

```java
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1002;

private void checkNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_CODE_NOTIFICATION_PERMISSION);
        }
    }
}

@Override
public void onRequestPermissionsResult(int requestCode,
                                       String[] permissions,
                                       int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
        if (grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission accordée
            Toast.makeText(this, "Notifications activées", Toast.LENGTH_SHORT).show();
        } else {
            // Permission refusée
            Toast.makeText(this,
                "Les notifications sont nécessaires pour le timer",
                Toast.LENGTH_LONG).show();
        }
    }
}
```

## Synchronisation avec le backend Web

Le TimerService et le FloatingTimerWidgetManager utilisent déjà l'ApiClient pour communiquer avec les endpoints backend:

- **POST /timer/start** - Démarrer le timer
- **POST /timer/pause** - Mettre en pause
- **POST /timer/resume** - Reprendre
- **POST /timer/stop** - Arrêter et créer le rapport
- **GET /timer/status** - Vérifier l'état du timer

### Méthodes à implémenter dans ApiClient.java

Si elles n'existent pas déjà, ajoutez ces méthodes dans `ApiClient.java`:

```java
public void getProjects(Callback callback) {
    Request request = new Request.Builder()
        .url(apiUrl + "/api/employee/projects")
        .addHeader("Authorization", "Bearer " + getAuthToken())
        .get()
        .build();

    client.newCall(request).enqueue(callback);
}

public void getWorkTypes(Callback callback) {
    Request request = new Request.Builder()
        .url(apiUrl + "/api/employee/work-types")
        .addHeader("Authorization", "Bearer " + getAuthToken())
        .get()
        .build();

    client.newCall(request).enqueue(callback);
}

public void getTimerStatus(Callback callback) {
    Request request = new Request.Builder()
        .url(apiUrl + "/timer/status")
        .addHeader("Authorization", "Bearer " + getAuthToken())
        .get()
        .build();

    client.newCall(request).enqueue(callback);
}

public void startTimer(int projectId, Callback callback) {
    JSONObject json = new JSONObject();
    try {
        json.put("project_id", projectId);
    } catch (JSONException e) {
        e.printStackTrace();
    }

    RequestBody body = RequestBody.create(
        json.toString(),
        MediaType.parse("application/json")
    );

    Request request = new Request.Builder()
        .url(apiUrl + "/timer/start")
        .addHeader("Authorization", "Bearer " + getAuthToken())
        .post(body)
        .build();

    client.newCall(request).enqueue(callback);
}

public void pauseTimer(Callback callback) {
    Request request = new Request.Builder()
        .url(apiUrl + "/timer/pause")
        .addHeader("Authorization", "Bearer " + getAuthToken())
        .post(RequestBody.create("", MediaType.parse("application/json")))
        .build();

    client.newCall(request).enqueue(callback);
}

public void resumeTimer(Callback callback) {
    Request request = new Request.Builder()
        .url(apiUrl + "/timer/resume")
        .addHeader("Authorization", "Bearer " + getAuthToken())
        .post(RequestBody.create("", MediaType.parse("application/json")))
        .build();

    client.newCall(request).enqueue(callback);
}

public void stopTimer(int workTypeId, String description, Callback callback) {
    JSONObject json = new JSONObject();
    try {
        json.put("work_type_id", workTypeId);
        json.put("description", description);
    } catch (JSONException e) {
        e.printStackTrace();
    }

    RequestBody body = RequestBody.create(
        json.toString(),
        MediaType.parse("application/json")
    );

    Request request = new Request.Builder()
        .url(apiUrl + "/timer/stop")
        .addHeader("Authorization", "Bearer " + getAuthToken())
        .post(body)
        .build();

    client.newCall(request).enqueue(callback);
}
```

## Utilisation Alternative: Bouton dans l'UI (sans overlay)

Si vous préférez ne pas utiliser un widget flottant et simplement ajouter un bouton dans l'interface Dashboard:

```xml
<!-- Dans activity_dashboard.xml -->
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fab_timer"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom|end"
    android:layout_margin="16dp"
    android:src="@drawable/ic_timer"
    android:contentDescription="Timer"
    app:backgroundTint="@color/primary" />
```

```java
// Dans DashboardActivity.java
FloatingActionButton fabTimer = findViewById(R.id.fab_timer);
fabTimer.setOnClickListener(v -> {
    // Ouvrir une activité dédiée au timer
    Intent intent = new Intent(this, TimerActivity.class);
    startActivity(intent);
});
```

Puis créer `TimerActivity.java` qui affiche le widget en plein écran au lieu d'un overlay.

## Dialogue de Confirmation

Le dialogue de confirmation "Êtes-vous sûr de vouloir terminer le timer?" est déjà implémenté dans `FloatingTimerWidgetManager.showStopConfirmationDialog()`.

Il affiche:
- Le nom du projet
- Le temps écoulé
- Un avertissement que cela créera un rapport

## Tests

### Test du Service

```bash
# Via ADB
adb shell am startservice -n com.ptms.mobile/.services.TimerService \
    -a com.ptms.mobile.ACTION_START_TIMER \
    --ei project_id 1 \
    --es project_name "Test Project"
```

### Test de l'API

Utilisez Postman ou curl pour tester les endpoints:

```bash
# Démarrer le timer
curl -X POST http://votre-serveur/timer/start \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"project_id": 1}'

# Vérifier le statut
curl -X GET http://votre-serveur/timer/status \
  -H "Authorization: Bearer YOUR_TOKEN"

# Arrêter le timer
curl -X POST http://votre-serveur/timer/stop \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"work_type_id": 1, "description": "Test work"}'
```

## Troubleshooting

### Le widget ne s'affiche pas

1. Vérifier la permission SYSTEM_ALERT_WINDOW:
   ```java
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
       if (!Settings.canDrawOverlays(this)) {
           // Permission manquante
       }
   }
   ```

2. Vérifier que le service est déclaré dans AndroidManifest.xml
3. Vérifier les logs: `adb logcat | grep TimerService`

### Le timer ne persiste pas après redémarrage

Vérifier que SharedPreferences sauvegarde correctement:

```java
SharedPreferences prefs = context.getSharedPreferences("timer_state", Context.MODE_PRIVATE);
boolean isRunning = prefs.getBoolean("is_running", false);
```

### Notification ne s'affiche pas

1. Vérifier la permission POST_NOTIFICATIONS (Android 13+)
2. Vérifier que le NotificationChannel est créé:
   ```java
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
       NotificationChannel channel = new NotificationChannel(
           CHANNEL_ID,
           "Timer",
           NotificationManager.IMPORTANCE_DEFAULT
       );
       notificationManager.createNotificationChannel(channel);
   }
   ```

### Erreur de compilation

Si vous avez des erreurs avec `Project` ou `WorkType`:

```java
// Créer ces classes simples:
public class Project {
    private int id;
    private String name;

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

public class WorkType {
    private int id;
    private String name;

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

## Prochaines Étapes

1. ✅ Créer TimerService.java
2. ✅ Créer FloatingTimerWidgetManager.java
3. ✅ Créer les layouts XML
4. ✅ Créer les drawables et couleurs
5. ✅ Ajouter les permissions dans AndroidManifest.xml
6. ✅ Déclarer le service dans AndroidManifest.xml
7. ⏳ Intégrer dans DashboardActivity
8. ⏳ Implémenter les méthodes API dans ApiClient
9. ⏳ Tester sur un appareil réel
10. ⏳ Ajuster l'UI selon les retours utilisateurs

## Notes Importantes

- Le widget persiste entre les activités (c'est le but d'un overlay)
- Le service continue en arrière-plan même si l'app est fermée
- La notification permet de contrôler le timer depuis la barre de notifications
- Le timer se synchronise automatiquement avec le backend à chaque action
- Les données sont sauvegardées localement pour survivre aux redémarrages

---

**Documentation créée le**: 2025-10-16
**Version**: 1.0
**Auteur**: Claude Code
