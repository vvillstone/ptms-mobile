# Guide du Widget Timer Flottant - PTMS Mobile v2.0.4

## Vue d'ensemble

Le widget timer flottant est une interface moderne et intuitive pour le suivi du temps de travail. Il permet aux utilisateurs de contrôler le minuteur sans quitter l'application ou l'écran actuel.

## Caractéristiques principales

### 1. Interface flottante
- Widget draggable (déplaçable) sur l'écran
- Reste au-dessus des autres applications
- Position persistante entre les sessions
- Taille optimisée pour ne pas gêner

### 2. Affichage du temps
- Format HH:MM:SS en grande police monospace
- Code couleur selon la durée:
  - **Vert** (0-6h): Temps normal
  - **Orange** (6-8h): Alerte proximité fin journée
  - **Rouge** (8h+): Dépassement temps journalier

### 3. Indicateur d'activité
- Animation pulsante verte quand le timer est actif
- Disparaît quand le timer est en pause
- Feedback visuel clair de l'état

### 4. Contrôles d'action
- **Bouton Play/Pause**: Démarrer ou mettre en pause le timer
  - Icône play (▶) quand timer en pause
  - Icône pause (⏸) quand timer actif
  - Fond blanc circulaire
- **Bouton Stop**: Arrêter complètement le timer
  - Icône stop (⏹)
  - Fond rouge pour danger/warning
  - Confirmation avant arrêt

### 5. Mode expand/collapse
- **Mode collapsed**: Affichage compact (timer + contrôles uniquement)
- **Mode expanded**: Affiche aussi le nom du projet
- Icône chevron pour basculer entre les modes
- Sauvegarde de la préférence utilisateur

## Architecture technique

### Classes principales

#### FloatingTimerWidget.java
```java
public class FloatingTimerWidget {
    // Création et affichage du widget
    public void show(String projectName)

    // Masquage et cleanup
    public void hide()

    // Gestion de la position
    private void setupLayoutParams()

    // Gestion du drag & drop
    private void setupDragListener()

    // Mise à jour du timer
    private void updateDisplay()
}
```

### Composants clés

1. **WindowManager**: Gère l'overlay flottant
   ```java
   WindowManager.LayoutParams params = new WindowManager.LayoutParams(
       WindowManager.LayoutParams.WRAP_CONTENT,
       WindowManager.LayoutParams.WRAP_CONTENT,
       WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
       WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
       PixelFormat.TRANSLUCENT
   );
   ```

2. **BroadcastReceiver**: Synchronisation avec TimerService
   ```java
   private void registerTimerReceiver() {
       IntentFilter filter = new IntentFilter(TimerService.ACTION_TIMER_UPDATE);
       context.registerReceiver(timerUpdateReceiver, filter);
   }
   ```

3. **ObjectAnimator**: Animation du pulse indicator
   ```java
   pulseAnimator = ObjectAnimator.ofFloat(pulseIndicator, "alpha", 1f, 0.3f);
   pulseAnimator.setDuration(1000);
   pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
   pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
   ```

## Permissions requises

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

### Runtime permission
Pour Android 6.0+ (API 23), demander la permission overlay:
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    if (!Settings.canDrawOverlays(context)) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + context.getPackageName()));
        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
    }
}
```

## Intégration avec TimerService

### 1. Démarrage du timer
```java
// Dans TimerActivity ou MainActivity
Intent serviceIntent = new Intent(this, TimerService.class);
serviceIntent.setAction(TimerService.ACTION_START);
serviceIntent.putExtra("project_id", projectId);
serviceIntent.putExtra("project_name", projectName);
startService(serviceIntent);

// Affichage du widget
FloatingTimerWidget widget = new FloatingTimerWidget(this);
widget.show(projectName);
```

### 2. Mise à jour en temps réel
Le TimerService envoie des broadcasts chaque seconde:
```java
// Dans TimerService
Intent updateIntent = new Intent(ACTION_TIMER_UPDATE);
updateIntent.putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds);
updateIntent.putExtra(EXTRA_IS_RUNNING, isRunning);
sendBroadcast(updateIntent);
```

Le widget reçoit et affiche:
```java
// Dans FloatingTimerWidget
timerUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        elapsedSeconds = intent.getLongExtra(TimerService.EXTRA_ELAPSED_SECONDS, 0);
        isRunning = intent.getBooleanExtra(TimerService.EXTRA_IS_RUNNING, false);
        updateDisplay();
    }
};
```

### 3. Actions utilisateur
```java
// Play/Pause
btnPlayPause.setOnClickListener(v -> {
    Intent intent = new Intent(context, TimerService.class);
    intent.setAction(isRunning ? TimerService.ACTION_PAUSE : TimerService.ACTION_RESUME);
    context.startService(intent);
});

// Stop
btnStop.setOnClickListener(v -> {
    new AlertDialog.Builder(context)
        .setTitle("Arrêter le timer")
        .setMessage("Voulez-vous arrêter le timer ?")
        .setPositiveButton("Arrêter", (dialog, which) -> {
            Intent intent = new Intent(context, TimerService.class);
            intent.setAction(TimerService.ACTION_STOP);
            context.startService(intent);
            hide();
        })
        .setNegativeButton("Annuler", null)
        .show();
});
```

## Gestion de la position

### Sauvegarde de la position
```java
private void savePosition(int x, int y) {
    SharedPreferences prefs = context.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE);
    prefs.edit()
        .putInt("position_x", x)
        .putInt("position_y", y)
        .apply();
}
```

### Restauration de la position
```java
private void restorePosition() {
    SharedPreferences prefs = context.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE);
    params.x = prefs.getInt("position_x", 0);
    params.y = prefs.getInt("position_y", 0);
}
```

### Drag & Drop
```java
floatingView.setOnTouchListener(new View.OnTouchListener() {
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                windowManager.updateViewLayout(floatingView, params);
                return true;

            case MotionEvent.ACTION_UP:
                savePosition(params.x, params.y);
                return true;
        }
        return false;
    }
});
```

## Design Material

### Layout (widget_floating_timer.xml)
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:cardCornerRadius="16dp"
    app:cardElevation="8dp"
    app:cardBackgroundColor="@color/surface">

    <!-- Contenu du widget -->

</com.google.android.material.card.MaterialCardView>
```

### Drawables

1. **bg_pulse_indicator.xml**: Indicateur pulsant vert
```xml
<shape android:shape="oval">
    <solid android:color="@color/success" />
</shape>
```

2. **bg_timer_button.xml**: Bouton action blanc
```xml
<shape android:shape="oval">
    <solid android:color="@color/white" />
</shape>
```

3. **bg_timer_button_danger.xml**: Bouton stop rouge
```xml
<shape android:shape="oval">
    <solid android:color="@color/danger" />
</shape>
```

4. **Icônes vectorielles**:
   - `ic_play.xml`: Play triangle
   - `ic_pause.xml`: Pause bars
   - `ic_stop.xml`: Stop square
   - `ic_expand.xml`: Chevron down
   - `ic_collapse.xml`: Chevron up

## Gestion du cycle de vie

### Démarrage de l'application
```java
// Dans onCreate() de l'Activity principale
if (TimerService.isRunning()) {
    String projectName = TimerService.getCurrentProjectName();
    FloatingTimerWidget widget = new FloatingTimerWidget(this);
    widget.show(projectName);
}
```

### Arrêt de l'application
```java
// Dans onDestroy()
@Override
protected void onDestroy() {
    super.onDestroy();
    if (floatingWidget != null) {
        floatingWidget.hide(); // Cleanup mais garde le service actif
    }
}
```

### Background/Foreground
Le widget persiste en background grâce au WindowManager. Le service continue de tourner même si l'application est fermée.

## Bonnes pratiques

### 1. Gestion de la mémoire
```java
@Override
public void hide() {
    if (floatingView != null && floatingView.getParent() != null) {
        windowManager.removeView(floatingView);
    }

    if (timerUpdateReceiver != null) {
        context.unregisterReceiver(timerUpdateReceiver);
        timerUpdateReceiver = null;
    }

    if (pulseAnimator != null) {
        pulseAnimator.cancel();
        pulseAnimator = null;
    }

    floatingView = null;
}
```

### 2. Vérification des permissions
Toujours vérifier avant d'afficher:
```java
public void show(String projectName) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!Settings.canDrawOverlays(context)) {
            requestOverlayPermission();
            return;
        }
    }
    // Affichage du widget
}
```

### 3. Gestion des erreurs
```java
try {
    windowManager.addView(floatingView, params);
} catch (WindowManager.BadTokenException e) {
    Log.e(TAG, "Cannot add window overlay", e);
    // Fallback vers notification
}
```

## Tests

### Test manuel
1. Démarrer un timer depuis l'application
2. Vérifier que le widget s'affiche
3. Tester le drag & drop
4. Vérifier la mise à jour en temps réel
5. Tester play/pause
6. Tester stop avec confirmation
7. Vérifier les codes couleur (6h, 8h)
8. Tester expand/collapse
9. Fermer l'application et vérifier que le widget persiste
10. Réouvrir l'application et vérifier la synchronisation

### Tests de régression
- Timer continue après rotation écran
- Widget ne bloque pas les interactions avec autres apps
- Position sauvegardée correctement
- Pas de fuite mémoire après hide/show multiple
- BroadcastReceiver correctement désenregistré

## Améliorations futures possibles

1. **Thèmes**: Support du mode sombre/clair
2. **Tailles**: Widget redimensionnable
3. **Statistiques**: Affichage temps total du jour
4. **Gestes**: Double-tap pour actions rapides
5. **Notifications**: Integration avec notification permanente
6. **Multi-projets**: Switch rapide entre projets actifs
7. **Rapports**: Envoi direct depuis le widget
8. **Offline**: Queue des actions si pas de connexion

## Ressources

- [WindowManager Documentation](https://developer.android.com/reference/android/view/WindowManager)
- [Material Design - Cards](https://m3.material.io/components/cards)
- [Android Services Guide](https://developer.android.com/guide/components/services)
- [BroadcastReceiver Best Practices](https://developer.android.com/guide/components/broadcasts)

---

**Version**: 2.0.4
**Date**: Octobre 2025
**Auteur**: PTMS Development Team
