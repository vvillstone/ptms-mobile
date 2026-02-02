package com.ptms.mobile.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilitaire pour écrire des logs dans un fichier texte lisible
 * Fichier: /storage/emulated/0/Download/ptms_debug.txt
 */
public class FileLogger {

    private static final String LOG_FILE_NAME = "ptms_debug.txt";
    private static File logFile;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.FRANCE);

    /**
     * Initialise le fichier de log
     */
    public static void init(Context context) {
        try {
            // Utiliser le dossier Download qui est accessible sans permission spéciale
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            logFile = new File(downloadDir, LOG_FILE_NAME);

            // Si le fichier existe et fait plus de 5MB, le supprimer
            if (logFile.exists() && logFile.length() > 5 * 1024 * 1024) {
                logFile.delete();
            }

            // Créer le fichier s'il n'existe pas
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            // Écrire l'en-tête de session
            writeToFile("\n\n========================================");
            writeToFile("NOUVELLE SESSION - " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE).format(new Date()));
            writeToFile("========================================\n");

            android.util.Log.d("FileLogger", "Fichier de log créé: " + logFile.getAbsolutePath());

        } catch (Exception e) {
            android.util.Log.e("FileLogger", "Erreur initialisation fichier log", e);
        }
    }

    /**
     * Écrit un log de type DEBUG
     */
    public static void d(String tag, String message) {
        String logMessage = String.format("%s [%s] %s",
            dateFormat.format(new Date()),
            tag,
            message);

        // Écrire dans le fichier
        writeToFile(logMessage);

        // Aussi écrire dans logcat
        android.util.Log.d(tag, message);
    }

    /**
     * Écrit un log de type ERROR
     */
    public static void e(String tag, String message) {
        String logMessage = String.format("%s [%s] ❌ ERROR: %s",
            dateFormat.format(new Date()),
            tag,
            message);

        writeToFile(logMessage);
        android.util.Log.e(tag, message);
    }

    /**
     * Écrit un log de type ERROR avec exception
     */
    public static void e(String tag, String message, Throwable throwable) {
        String logMessage = String.format("%s [%s] ❌ ERROR: %s",
            dateFormat.format(new Date()),
            tag,
            message);

        writeToFile(logMessage);

        if (throwable != null) {
            writeToFile("   Exception: " + throwable.getClass().getName());
            writeToFile("   Message: " + throwable.getMessage());

            // Stack trace
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                writeToFile("   Stack trace:");
                for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                    writeToFile("      " + stackTrace[i].toString());
                }
            }

            // Cause
            if (throwable.getCause() != null) {
                writeToFile("   Caused by: " + throwable.getCause().getMessage());
            }
        }

        android.util.Log.e(tag, message, throwable);
    }

    /**
     * Écrit une ligne de séparation
     */
    public static void separator() {
        writeToFile("----------------------------------------");
    }

    /**
     * Écrit directement dans le fichier
     */
    private static void writeToFile(String message) {
        if (logFile == null) {
            return;
        }

        try {
            FileWriter writer = new FileWriter(logFile, true); // append mode
            writer.write(message + "\n");
            writer.close();
        } catch (IOException e) {
            android.util.Log.e("FileLogger", "Erreur écriture fichier", e);
        }
    }

    /**
     * Obtient le chemin du fichier de log
     */
    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "Non initialisé";
    }
}
