package com.ptms.mobile.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ✅ Gestionnaire de stockage local pour fichiers multimédias
 *
 * Responsabilités :
 * - Sauvegarder fichiers en local (audio, images, vidéos)
 * - Compression images
 * - Génération thumbnails
 * - Nettoyage cache
 * - Calcul espace utilisé
 *
 * @version 1.0
 * @date 2025-10-20
 */
public class MediaStorageManager {

    private static final String TAG = "MediaStorageManager";
    private Context context;

    // Dossiers de stockage
    private static final String MEDIA_DIR = "media";
    private static final String AUDIO_DIR = "audio";
    private static final String IMAGE_DIR = "images";
    private static final String VIDEO_DIR = "videos";
    private static final String THUMBNAIL_DIR = "thumbnails";

    // Paramètres de compression
    private static final int MAX_IMAGE_WIDTH = 1920;
    private static final int MAX_IMAGE_HEIGHT = 1920;
    private static final int IMAGE_QUALITY = 85; // 0-100
    private static final int THUMBNAIL_SIZE = 200;

    /**
     * Callback pour opérations asynchrones
     */
    public interface OperationCallback {
        void onSuccess(File file);
        void onError(String error);
    }

    /**
     * Callback pour progress
     */
    public interface ProgressCallback {
        void onProgress(int progress);
    }

    public MediaStorageManager(Context context) {
        this.context = context.getApplicationContext();
        initializeDirectories();
    }

    // ==================== INITIALISATION ====================

    /**
     * Crée les dossiers de stockage s'ils n'existent pas
     */
    private void initializeDirectories() {
        File mediaRoot = new File(context.getFilesDir(), MEDIA_DIR);
        File audioDir = new File(mediaRoot, AUDIO_DIR);
        File imageDir = new File(mediaRoot, IMAGE_DIR);
        File videoDir = new File(mediaRoot, VIDEO_DIR);
        File thumbDir = new File(mediaRoot, THUMBNAIL_DIR);

        if (!mediaRoot.exists()) mediaRoot.mkdirs();
        if (!audioDir.exists()) audioDir.mkdirs();
        if (!imageDir.exists()) imageDir.mkdirs();
        if (!videoDir.exists()) videoDir.mkdirs();
        if (!thumbDir.exists()) thumbDir.mkdirs();

        Log.d(TAG, "Dossiers initialisés: " + mediaRoot.getAbsolutePath());
    }

    // ==================== SAUVEGARDE FICHIERS ====================

    /**
     * Sauvegarde un fichier multimédia en local
     *
     * @param sourceFile Fichier source
     * @param mediaType Type: "audio", "image", "video"
     * @return Fichier sauvegardé
     */
    public File saveMediaFile(File sourceFile, String mediaType) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Fichier source invalide");
        }

        // Déterminer le dossier de destination
        String subDir;
        switch (mediaType.toLowerCase()) {
            case "audio":
            case "dictation":
                subDir = AUDIO_DIR;
                break;
            case "image":
            case "photo":
                subDir = IMAGE_DIR;
                break;
            case "video":
                subDir = VIDEO_DIR;
                break;
            default:
                subDir = AUDIO_DIR; // Par défaut
        }

        // Générer nom de fichier unique
        // ✅ FIX: Use Locale.US for consistent timestamp formatting
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(new Date());
        String extension = getFileExtension(sourceFile);
        String fileName = mediaType + "_" + timestamp + "." + extension;

        File destDir = new File(new File(context.getFilesDir(), MEDIA_DIR), subDir);
        File destFile = new File(destDir, fileName);

        // Copier le fichier
        copyFile(sourceFile, destFile);

        Log.d(TAG, "Fichier sauvegardé: " + destFile.getAbsolutePath() + " (" + destFile.length() + " bytes)");
        return destFile;
    }

    /**
     * Copie un fichier
     */
    private void copyFile(File source, File dest) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(dest);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.flush();

        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
        }
    }

    // ==================== COMPRESSION IMAGES ====================

    /**
     * Compresse une image avant sauvegarde
     *
     * @param imageFile Fichier image source
     * @param maxWidth Largeur max (0 = pas de limite)
     * @param quality Qualité 0-100
     * @return Fichier compressé
     */
    public File compressImage(File imageFile, int maxWidth, int quality) throws IOException {
        if (imageFile == null || !imageFile.exists()) {
            throw new IOException("Fichier image invalide");
        }

        // Charger l'image
        Bitmap original = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (original == null) {
            throw new IOException("Impossible de décoder l'image");
        }

        // Calculer nouvelles dimensions
        int width = original.getWidth();
        int height = original.getHeight();

        if (maxWidth > 0 && width > maxWidth) {
            float ratio = (float) maxWidth / width;
            width = maxWidth;
            height = (int) (height * ratio);
        }

        // Redimensionner si nécessaire
        Bitmap resized = width != original.getWidth() ?
            Bitmap.createScaledBitmap(original, width, height, true) : original;

        // Sauvegarder compressé
        // ✅ FIX: Use Locale.US for consistent timestamp formatting
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(new Date());
        File compressedFile = new File(
            new File(new File(context.getFilesDir(), MEDIA_DIR), IMAGE_DIR),
            "image_" + timestamp + ".jpg"
        );

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(compressedFile);
            resized.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
            if (resized != original) resized.recycle();
            original.recycle();
        }

        Log.d(TAG, "Image compressée: " + imageFile.length() + " → " + compressedFile.length() + " bytes");
        return compressedFile;
    }

    // ==================== THUMBNAILS ====================

    /**
     * Génère une miniature (thumbnail) pour une image ou vidéo
     *
     * @param mediaFile Fichier source
     * @return Fichier thumbnail
     */
    public File createThumbnail(File mediaFile) throws IOException {
        if (mediaFile == null || !mediaFile.exists()) {
            throw new IOException("Fichier invalide");
        }

        Bitmap thumbnail;
        String mimeType = getMimeType(mediaFile);

        if (mimeType.startsWith("image/")) {
            // Thumbnail pour image
            Bitmap original = BitmapFactory.decodeFile(mediaFile.getAbsolutePath());
            if (original == null) {
                throw new IOException("Impossible de décoder l'image");
            }

            thumbnail = ThumbnailUtils.extractThumbnail(original, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
            original.recycle();

        } else if (mimeType.startsWith("video/")) {
            // Thumbnail pour vidéo (frame à 1 seconde)
            thumbnail = ThumbnailUtils.createVideoThumbnail(
                mediaFile.getAbsolutePath(),
                android.provider.MediaStore.Video.Thumbnails.MINI_KIND
            );

            if (thumbnail == null) {
                throw new IOException("Impossible de créer thumbnail vidéo");
            }

        } else {
            throw new IOException("Type de fichier non supporté pour thumbnail: " + mimeType);
        }

        // Sauvegarder thumbnail
        // ✅ FIX: Use Locale.US for consistent timestamp formatting
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(new Date());
        File thumbFile = new File(
            new File(new File(context.getFilesDir(), MEDIA_DIR), THUMBNAIL_DIR),
            "thumb_" + timestamp + ".jpg"
        );

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(thumbFile);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
            thumbnail.recycle();
        }

        Log.d(TAG, "Thumbnail créé: " + thumbFile.getAbsolutePath());
        return thumbFile;
    }

    // ==================== NETTOYAGE CACHE ====================

    /**
     * Nettoie les fichiers locaux anciens et synchronisés
     *
     * @param olderThanDays Fichiers plus vieux que X jours
     * @return Nombre de fichiers supprimés
     */
    public int cleanupOldCache(int olderThanDays) {
        int deletedCount = 0;
        long cutoffTime = System.currentTimeMillis() - (olderThanDays * 24L * 60 * 60 * 1000);

        File mediaRoot = new File(context.getFilesDir(), MEDIA_DIR);
        deletedCount += deleteOldFiles(mediaRoot, cutoffTime);

        Log.d(TAG, "Nettoyage cache: " + deletedCount + " fichiers supprimés (> " + olderThanDays + " jours)");
        return deletedCount;
    }

    /**
     * Supprime récursivement les fichiers anciens
     */
    private int deleteOldFiles(File dir, long cutoffTime) {
        int count = 0;

        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                count += deleteOldFiles(file, cutoffTime);
            } else if (file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    count++;
                    Log.d(TAG, "Fichier supprimé: " + file.getName());
                }
            }
        }

        return count;
    }

    /**
     * Supprime un fichier spécifique
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        if (file.exists() && file.delete()) {
            Log.d(TAG, "Fichier supprimé: " + filePath);
            return true;
        }

        return false;
    }

    // ==================== UTILITAIRES ====================

    /**
     * Calcule l'espace utilisé par le cache
     *
     * @return Taille en bytes
     */
    public long getCacheSize() {
        File mediaRoot = new File(context.getFilesDir(), MEDIA_DIR);
        return getDirectorySize(mediaRoot);
    }

    /**
     * Calcule la taille d'un dossier récursivement
     */
    private long getDirectorySize(File dir) {
        long size = 0;

        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                size += getDirectorySize(file);
            } else {
                size += file.length();
            }
        }

        return size;
    }

    /**
     * Formatte une taille en bytes de manière lisible
     */
    public String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Récupère l'extension d'un fichier
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Détermine le type MIME d'un fichier
     */
    public String getMimeType(File file) {
        String extension = getFileExtension(file).toLowerCase();

        switch (extension) {
            // Audio
            case "m4a":
            case "aac":
                return "audio/m4a";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "3gp":
            case "3gpp":
                return "audio/3gpp";
            case "webm":
                return "audio/webm";

            // Images
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";

            // Vidéos
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/avi";
            case "mkv":
                return "video/mkv";

            default:
                return "application/octet-stream";
        }
    }

    /**
     * Vérifie si le fichier est une image
     */
    public boolean isImageFile(File file) {
        String mimeType = getMimeType(file);
        return mimeType.startsWith("image/");
    }

    /**
     * Vérifie si le fichier est une vidéo
     */
    public boolean isVideoFile(File file) {
        String mimeType = getMimeType(file);
        return mimeType.startsWith("video/");
    }

    /**
     * Vérifie si le fichier est un audio
     */
    public boolean isAudioFile(File file) {
        String mimeType = getMimeType(file);
        return mimeType.startsWith("audio/");
    }
}
