package com.ptms.mobile.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ✅ Gestionnaire de photos pour les notes
 *
 * Fonctionnalités :
 * - Prise de photo avec caméra
 * - Sélection photo depuis galerie
 * - Compression et redimensionnement
 * - Gestion des permissions
 * - Support Android 13+ (TIRAMISU)
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class PhotoManager {

    private static final String TAG = "PhotoManager";

    // Permissions
    private static final int PERMISSION_CAMERA = 100;
    private static final int PERMISSION_STORAGE = 101;
    private static final int PERMISSION_READ_MEDIA_IMAGES = 102;

    // Codes de requête
    public static final int REQUEST_IMAGE_CAPTURE = 1001;
    public static final int REQUEST_IMAGE_PICK = 1002;

    // Configuration compression
    private static final int MAX_IMAGE_WIDTH = 1920;
    private static final int MAX_IMAGE_HEIGHT = 1080;
    private static final int JPEG_QUALITY = 85;

    private final Context context;
    private File currentPhotoFile;
    private Uri currentPhotoUri;

    public PhotoManager(Context context) {
        this.context = context.getApplicationContext();
    }

    // ==================== PERMISSIONS ====================

    /**
     * Vérifie si les permissions caméra sont accordées
     */
    public boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Vérifie si les permissions de stockage sont accordées
     */
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ : READ_MEDIA_IMAGES
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 et inférieur : READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Demande les permissions caméra
     */
    public void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_CAMERA
        );
    }

    /**
     * Demande les permissions de stockage
     */
    public void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    PERMISSION_READ_MEDIA_IMAGES
            );
        } else {
            // Android 12 et inférieur
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_STORAGE
            );
        }
    }

    // ==================== CAMÉRA ====================

    /**
     * Crée un Intent pour prendre une photo avec la caméra
     */
    public Intent createCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Créer un fichier pour stocker la photo
        try {
            currentPhotoFile = createImageFile();
            if (currentPhotoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        currentPhotoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                Log.d(TAG, "📸 Photo sera sauvegardée dans: " + currentPhotoFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Erreur création fichier photo", e);
            return null;
        }

        return takePictureIntent;
    }

    /**
     * Crée un fichier unique pour stocker l'image
     */
    private File createImageFile() throws IOException {
        // Créer un nom de fichier unique
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "PTMS_NOTE_" + timeStamp + "_";

        // Dossier de stockage
        File storageDir = new File(context.getFilesDir(), "photos");
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (!created) {
                throw new IOException("Impossible de créer le dossier photos");
            }
        }

        // Créer le fichier
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        Log.d(TAG, "✅ Fichier image créé: " + imageFile.getAbsolutePath());
        return imageFile;
    }

    /**
     * Récupère le fichier de la photo prise
     */
    public File getCurrentPhotoFile() {
        return currentPhotoFile;
    }

    /**
     * Récupère l'URI de la photo prise
     */
    public Uri getCurrentPhotoUri() {
        return currentPhotoUri;
    }

    // ==================== GALERIE ====================

    /**
     * Crée un Intent pour sélectionner une photo depuis la galerie
     */
    public Intent createGalleryIntent() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK);
        pickPhotoIntent.setType("image/*");
        return pickPhotoIntent;
    }

    // ==================== TRAITEMENT D'IMAGE ====================

    /**
     * Compresse et redimensionne une image
     *
     * @param sourcePath Chemin du fichier source
     * @param outputPath Chemin du fichier de sortie
     * @return true si succès, false sinon
     */
    public boolean compressImage(String sourcePath, String outputPath) {
        try {
            Log.d(TAG, "🔄 Compression image:");
            Log.d(TAG, "  • Source: " + sourcePath);
            Log.d(TAG, "  • Sortie: " + outputPath);

            // Décoder les dimensions de l'image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(sourcePath, options);

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            Log.d(TAG, "  • Dimensions originales: " + originalWidth + "x" + originalHeight);

            // Calculer le ratio de redimensionnement
            int inSampleSize = calculateInSampleSize(options, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;

            // Décoder l'image avec redimensionnement
            Bitmap bitmap = BitmapFactory.decodeFile(sourcePath, options);
            if (bitmap == null) {
                Log.e(TAG, "❌ Impossible de décoder l'image");
                return false;
            }

            // Corriger l'orientation (EXIF)
            bitmap = rotateImageIfRequired(bitmap, sourcePath);

            // Sauvegarder l'image compressée
            FileOutputStream out = new FileOutputStream(outputPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            out.flush();
            out.close();

            Log.d(TAG, "✅ Image compressée:");
            Log.d(TAG, "  • Dimensions finales: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            Log.d(TAG, "  • Taille fichier: " + formatFileSize(new File(outputPath).length()));

            bitmap.recycle();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur compression image", e);
            return false;
        }
    }

    /**
     * Compresse une image depuis un URI (galerie)
     */
    public boolean compressImageFromUri(Uri imageUri, String outputPath) {
        try {
            Log.d(TAG, "🔄 Compression image depuis URI:");
            Log.d(TAG, "  • URI: " + imageUri.toString());
            Log.d(TAG, "  • Sortie: " + outputPath);

            // Ouvrir InputStream depuis l'URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "❌ Impossible d'ouvrir l'URI");
                return false;
            }

            // Décoder les dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            Log.d(TAG, "  • Dimensions originales: " + originalWidth + "x" + originalHeight);

            // Calculer le ratio de redimensionnement
            int inSampleSize = calculateInSampleSize(options, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;

            // Décoder l'image avec redimensionnement
            inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap == null) {
                Log.e(TAG, "❌ Impossible de décoder l'image");
                return false;
            }

            // Sauvegarder l'image compressée
            FileOutputStream out = new FileOutputStream(outputPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            out.flush();
            out.close();

            Log.d(TAG, "✅ Image compressée:");
            Log.d(TAG, "  • Dimensions finales: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            Log.d(TAG, "  • Taille fichier: " + formatFileSize(new File(outputPath).length()));

            bitmap.recycle();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur compression image depuis URI", e);
            return false;
        }
    }

    /**
     * Calcule le ratio de redimensionnement
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Corrige l'orientation de l'image selon les données EXIF
     */
    private Bitmap rotateImageIfRequired(Bitmap img, String path) {
        try {
            ExifInterface ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            Log.w(TAG, "Impossible de lire les données EXIF", e);
            return img;
        }
    }

    /**
     * Applique une rotation à l'image
     */
    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    // ==================== UTILITAIRES ====================

    /**
     * Formate une taille de fichier en format lisible
     */
    public String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    /**
     * Supprime le fichier photo actuel
     */
    public void deleteCurrentPhoto() {
        if (currentPhotoFile != null && currentPhotoFile.exists()) {
            boolean deleted = currentPhotoFile.delete();
            if (deleted) {
                Log.d(TAG, "🗑️ Photo supprimée: " + currentPhotoFile.getName());
            }
            currentPhotoFile = null;
            currentPhotoUri = null;
        }
    }

    /**
     * Nettoie les photos temporaires anciennes (> 7 jours)
     */
    public void cleanupOldTempPhotos() {
        File photosDir = new File(context.getFilesDir(), "photos");
        if (!photosDir.exists()) return;

        File[] files = photosDir.listFiles();
        if (files == null) return;

        long currentTime = System.currentTimeMillis();
        long sevenDaysAgo = currentTime - (7 * 24 * 60 * 60 * 1000L);

        int deletedCount = 0;
        for (File file : files) {
            if (file.lastModified() < sevenDaysAgo) {
                if (file.delete()) {
                    deletedCount++;
                }
            }
        }

        if (deletedCount > 0) {
            Log.d(TAG, "🧹 Nettoyage: " + deletedCount + " photos temporaires supprimées");
        }
    }
}
