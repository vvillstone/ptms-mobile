package com.ptms.mobile.widgets;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.ptms.mobile.R;
import com.ptms.mobile.services.TimerService;

/**
 * ✅ Widget flottant moderne pour le timer
 *
 * Fonctionnalités :
 * - Interface compacte et élégante
 * - Draggable (déplaçable)
 * - Actions rapides (pause/stop)
 * - Animations fluides
 * - Mise à jour en temps réel
 * - Persistant au-dessus des autres apps
 * - Design Material 3
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class FloatingTimerWidget {

    private static final String TAG = "FloatingTimerWidget";

    private final Context context;
    private final WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    // UI Components
    private CardView cardTimer;
    private TextView textTime;
    private TextView textProject;
    private ImageButton buttonPlayPause;
    private ImageButton buttonStop;
    private ImageButton buttonExpand;
    private View pulseIndicator;

    // État
    private boolean isShowing = false;
    private boolean isRunning = false;
    private boolean isExpanded = false;
    private long elapsedSeconds = 0;
    private String projectName = "";

    // Dragging
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    // Broadcast receiver
    private BroadcastReceiver timerUpdateReceiver;

    public FloatingTimerWidget(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Affiche le widget flottant
     */
    public void show(String projectName) {
        if (isShowing) {
            Log.w(TAG, "Widget déjà affiché");
            return;
        }

        Log.d(TAG, "========================================");
        Log.d(TAG, "⏱️ AFFICHAGE TIMER WIDGET FLOTTANT");
        Log.d(TAG, "========================================");
        Log.d(TAG, "Projet: " + projectName);

        this.projectName = projectName;

        // Créer la vue
        createFloatingView();

        // Configurer les paramètres de fenêtre
        setupLayoutParams();

        // Ajouter à la fenêtre
        try {
            windowManager.addView(floatingView, params);
            isShowing = true;
            Log.d(TAG, "✅ Widget affiché avec succès");
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur affichage widget", e);
        }

        // Enregistrer le receiver
        registerTimerReceiver();
    }

    /**
     * Masque le widget
     */
    public void hide() {
        if (!isShowing) {
            return;
        }

        Log.d(TAG, "🔴 Masquage du widget");

        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
            isShowing = false;
        } catch (Exception e) {
            Log.e(TAG, "Erreur masquage widget", e);
        }

        // Désenregistrer le receiver
        unregisterTimerReceiver();
    }

    /**
     * Crée la vue flottante
     */
    private void createFloatingView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        floatingView = inflater.inflate(R.layout.widget_floating_timer, null);

        // Récupérer les vues
        cardTimer = floatingView.findViewById(R.id.cardTimer);
        textTime = floatingView.findViewById(R.id.textTime);
        textProject = floatingView.findViewById(R.id.textProject);
        buttonPlayPause = floatingView.findViewById(R.id.buttonPlayPause);
        buttonStop = floatingView.findViewById(R.id.buttonStop);
        buttonExpand = floatingView.findViewById(R.id.buttonExpand);
        pulseIndicator = floatingView.findViewById(R.id.pulseIndicator);

        // Initialiser
        textProject.setText(projectName);
        textTime.setText("00:00:00");
        isRunning = true;

        // Événements
        setupTouchListener();
        setupButtonListeners();

        // Démarrer l'animation de pulsation
        startPulseAnimation();
    }

    /**
     * Configure les paramètres de la fenêtre flottante
     */
    private void setupLayoutParams() {
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
        params.x = 0;
        params.y = 100;
    }

    /**
     * Configure le touch listener pour le drag
     */
    private void setupTouchListener() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private static final int CLICK_THRESHOLD = 10;
            private long touchStartTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStartTime = System.currentTimeMillis();
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
                        long touchDuration = System.currentTimeMillis() - touchStartTime;
                        float deltaX = Math.abs(event.getRawX() - initialTouchX);
                        float deltaY = Math.abs(event.getRawY() - initialTouchY);

                        if (touchDuration < 200 && deltaX < CLICK_THRESHOLD && deltaY < CLICK_THRESHOLD) {
                            // C'est un click, pas un drag
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Configure les listeners des boutons
     */
    private void setupButtonListeners() {
        // Play/Pause
        buttonPlayPause.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                resumeTimer();
            }
        });

        // Stop
        buttonStop.setOnClickListener(v -> {
            stopTimer();
        });

        // Expand/Collapse
        buttonExpand.setOnClickListener(v -> {
            toggleExpand();
        });
    }

    /**
     * Démarre l'animation de pulsation
     */
    private void startPulseAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(pulseIndicator, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(pulseIndicator, "scaleY", 1f, 1.3f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(pulseIndicator, "alpha", 1f, 0.3f, 1f);

        scaleX.setDuration(1500);
        scaleY.setDuration(1500);
        alpha.setDuration(1500);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());
        alpha.setInterpolator(new DecelerateInterpolator());

        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    /**
     * Met en pause le timer
     */
    private void pauseTimer() {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(TimerService.ACTION_PAUSE);
        context.startService(intent);

        isRunning = false;
        buttonPlayPause.setImageResource(R.drawable.ic_play);
        pulseIndicator.setVisibility(View.GONE);

        Log.d(TAG, "⏸️ Timer mis en pause");
    }

    /**
     * Reprend le timer
     */
    private void resumeTimer() {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(TimerService.ACTION_RESUME);
        context.startService(intent);

        isRunning = true;
        buttonPlayPause.setImageResource(R.drawable.ic_pause);
        pulseIndicator.setVisibility(View.VISIBLE);
        startPulseAnimation();

        Log.d(TAG, "▶️ Timer repris");
    }

    /**
     * Arrête le timer
     */
    private void stopTimer() {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(TimerService.ACTION_STOP);
        context.startService(intent);

        hide();

        Log.d(TAG, "⏹️ Timer arrêté");
    }

    /**
     * Bascule entre mode compact/étendu
     */
    private void toggleExpand() {
        isExpanded = !isExpanded;

        if (isExpanded) {
            textProject.setMaxLines(3);
            buttonExpand.setImageResource(R.drawable.ic_collapse);
        } else {
            textProject.setMaxLines(1);
            buttonExpand.setImageResource(R.drawable.ic_expand);
        }

        Log.d(TAG, isExpanded ? "📖 Mode étendu" : "📕 Mode compact");
    }

    /**
     * Enregistre le receiver pour les mises à jour
     */
    private void registerTimerReceiver() {
        timerUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                elapsedSeconds = intent.getLongExtra(TimerService.EXTRA_ELAPSED_SECONDS, 0);
                boolean running = intent.getBooleanExtra(TimerService.EXTRA_IS_RUNNING, false);

                updateDisplay();

                if (running != isRunning) {
                    isRunning = running;
                    if (isRunning) {
                        buttonPlayPause.setImageResource(R.drawable.ic_pause);
                        pulseIndicator.setVisibility(View.VISIBLE);
                        startPulseAnimation();
                    } else {
                        buttonPlayPause.setImageResource(R.drawable.ic_play);
                        pulseIndicator.setVisibility(View.GONE);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(TimerService.BROADCAST_TIMER_UPDATE);
        context.registerReceiver(timerUpdateReceiver, filter);
    }

    /**
     * Désenregistre le receiver
     */
    private void unregisterTimerReceiver() {
        if (timerUpdateReceiver != null) {
            try {
                context.unregisterReceiver(timerUpdateReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Erreur désenregistrement receiver", e);
            }
        }
    }

    /**
     * Met à jour l'affichage du temps
     */
    private void updateDisplay() {
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        textTime.setText(timeString);

        // Changer la couleur selon la durée
        if (hours >= 8) {
            textTime.setTextColor(context.getResources().getColor(R.color.danger));
        } else if (hours >= 6) {
            textTime.setTextColor(context.getResources().getColor(R.color.warning));
        } else {
            textTime.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        }
    }

    /**
     * Vérifie si le widget est affiché
     */
    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Met à jour le nom du projet
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
        if (textProject != null) {
            textProject.setText(projectName);
        }
    }
}
