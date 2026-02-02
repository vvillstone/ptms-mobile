package com.ptms.mobile.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper pour gérer les permissions runtime Android 6.0+
 *
 * Usage:
 * <pre>
 * if (PermissionsHelper.checkAudioPermission(this)) {
 *     // Permission accordée
 * } else {
 *     PermissionsHelper.requestAudioPermission(this, REQUEST_CODE);
 * }
 * </pre>
 */
public class PermissionsHelper {

    // Request codes pour les permissions
    public static final int REQUEST_AUDIO_PERMISSION = 101;
    public static final int REQUEST_STORAGE_PERMISSION = 102;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 103;
    public static final int REQUEST_ALL_PERMISSIONS = 199;

    /**
     * Vérifie si une permission est accordée
     */
    public static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Pré-Android 6.0, permissions accordées à l'installation
    }

    /**
     * Demande une permission
     */
    public static void requestPermission(Activity activity, String permission, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
        }
    }

    /**
     * Demande plusieurs permissions
     */
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    // ==================== AUDIO RECORDING ====================

    /**
     * Vérifie permission RECORD_AUDIO
     */
    public static boolean checkAudioPermission(Context context) {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO);
    }

    /**
     * Demande permission RECORD_AUDIO
     */
    public static void requestAudioPermission(Activity activity, int requestCode) {
        requestPermission(activity, Manifest.permission.RECORD_AUDIO, requestCode);
    }

    /**
     * Vérifie si on doit afficher une explication pour RECORD_AUDIO
     */
    public static boolean shouldShowAudioRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.RECORD_AUDIO);
        }
        return false;
    }

    // ==================== STORAGE ====================

    /**
     * Vérifie permissions STORAGE (Android < 13)
     */
    public static boolean checkStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ n'a plus besoin de READ/WRITE_EXTERNAL_STORAGE
            return true;
        }
        return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * Demande permissions STORAGE
     */
    public static void requestStoragePermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ utilise les permissions granulaires (images, videos, audio)
            return;
        }
        String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        requestPermissions(activity, permissions, requestCode);
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Vérifie permission POST_NOTIFICATIONS (Android 13+)
     */
    public static boolean checkNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPermission(context, Manifest.permission.POST_NOTIFICATIONS);
        }
        return true; // Pré-Android 13, pas de permission requise
    }

    /**
     * Demande permission POST_NOTIFICATIONS
     */
    public static void requestNotificationPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(activity, Manifest.permission.POST_NOTIFICATIONS, requestCode);
        }
    }

    // ==================== ALL PERMISSIONS ====================

    /**
     * Vérifie toutes les permissions nécessaires pour l'app
     */
    public static boolean checkAllPermissions(Context context) {
        boolean audio = checkAudioPermission(context);
        boolean storage = checkStoragePermission(context);
        boolean notifications = checkNotificationPermission(context);

        return audio && storage && notifications;
    }

    /**
     * Demande toutes les permissions nécessaires
     */
    public static void requestAllPermissions(Activity activity, int requestCode) {
        // Liste des permissions selon la version Android
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            // Android < 13
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        requestPermissions(activity, permissions, requestCode);
    }

    /**
     * Récupère les permissions manquantes
     */
    public static String[] getMissingPermissions(Context context) {
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();

        if (!checkAudioPermission(context)) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!checkStoragePermission(context)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                missing.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!checkNotificationPermission(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                missing.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        return missing.toArray(new String[0]);
    }

    /**
     * Vérifie les résultats d'une demande de permission
     */
    public static boolean verifyPermissionResults(String[] permissions, int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Message d'explication pour chaque permission
     */
    public static String getPermissionRationale(String permission) {
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                return "Permission microphone requise pour enregistrer des notes audio.";

            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Permission stockage requise pour sauvegarder les fichiers localement.";

            case Manifest.permission.POST_NOTIFICATIONS:
                return "Permission notifications requise pour vous alerter des synchronisations et rappels.";

            default:
                return "Cette permission est nécessaire pour le bon fonctionnement de l'application.";
        }
    }
}
