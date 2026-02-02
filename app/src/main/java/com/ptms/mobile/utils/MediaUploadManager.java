package com.ptms.mobile.utils;

import android.webkit.MimeTypeMap;

import okhttp3.*;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

/**
 * Gestionnaire centralisé pour l'upload de médias vers le serveur
 * Compatible avec le nouveau endpoint unifié /api/media-upload.php
 *
 * @version 2.0.7
 * @date 2025-01-26
 */
public class MediaUploadManager {

    // Types de médias supportés
    public enum MediaType {
        IMAGE("image"),
        VIDEO("video"),
        AUDIO("audio"),
        DOCUMENT("document");

        private final String value;
        MediaType(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    // Contextes d'upload
    public enum Context {
        CHAT("chat"),
        NOTES("notes"),
        PROFILE("profile"),
        INVOICE("invoice"),
        DOCUMENT("document"),
        TEMP("temp");

        private final String value;
        Context(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    // Tailles maximales par type (en octets)
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;    // 10 MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;   // 100 MB
    private static final long MAX_AUDIO_SIZE = 50 * 1024 * 1024;    // 50 MB
    private static final long MAX_DOCUMENT_SIZE = 25 * 1024 * 1024; // 25 MB

    private static final String API_ENDPOINT = "/api/media-upload.php";
    private final OkHttpClient client;
    private final String baseUrl;
    private String authToken;

    public MediaUploadManager(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)  // 2 minutes pour gros fichiers
            .readTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    /**
     * Définir le token d'authentification
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * Upload un fichier média
     *
     * @param file Fichier à uploader
     * @param mediaType Type de média
     * @param context Contexte d'upload
     * @param generateThumbnail Générer une miniature
     * @param callback Callback pour le résultat
     */
    public void upload(
        File file,
        MediaType mediaType,
        Context context,
        boolean generateThumbnail,
        MediaUploadCallback callback
    ) {
        // Validation locale
        ValidationResult validation = validate(file, mediaType);
        if (!validation.isValid()) {
            if (callback != null) {
                callback.onError(new Exception(validation.getError()));
            }
            return;
        }

        // Détecter le type MIME
        String mimeType = getMimeType(file);
        if (mimeType == null) {
            if (callback != null) {
                callback.onError(new Exception("Impossible de détecter le type de fichier"));
            }
            return;
        }

        // Construire la requête multipart
        RequestBody fileBody = RequestBody.create(file, okhttp3.MediaType.parse(mimeType));

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.getName(), fileBody)
            .addFormDataPart("media_type", mediaType.getValue())
            .addFormDataPart("context", context.getValue())
            .addFormDataPart("thumbnail", generateThumbnail ? "1" : "0");

        RequestBody requestBody = multipartBuilder.build();

        // Wrapper pour progress tracking
        ProgressRequestBody progressBody = new ProgressRequestBody(requestBody, (bytesWritten, contentLength) -> {
            int percent = (int) ((100 * bytesWritten) / contentLength);
            if (callback != null) {
                callback.onProgress(percent);
            }
        });

        // Construire la requête
        Request.Builder requestBuilder = new Request.Builder()
            .url(baseUrl + API_ENDPOINT)
            .post(progressBody);

        // Ajouter l'authentification si disponible
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Exécuter la requête de manière asynchrone
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);

                        if (jsonObject.getBoolean("success")) {
                            MediaUploadResult result = parseResult(jsonObject);
                            if (callback != null) {
                                callback.onSuccess(result);
                            }
                        } else {
                            String message = jsonObject.optString("message", "Upload échoué");
                            if (callback != null) {
                                callback.onError(new Exception(message));
                            }
                        }
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onError(new Exception("Erreur de parsing: " + e.getMessage()));
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError(new Exception("HTTP " + response.code() + ": " + response.message()));
                    }
                }
            }
        });
    }

    /**
     * Valide un fichier avant upload
     */
    public ValidationResult validate(File file, MediaType mediaType) {
        // Vérifier l'existence
        if (file == null || !file.exists()) {
            return new ValidationResult(false, "Fichier introuvable");
        }

        // Vérifier la lisibilité
        if (!file.canRead()) {
            return new ValidationResult(false, "Impossible de lire le fichier");
        }

        // Vérifier la taille
        long fileSize = file.length();
        long maxSize = getMaxSize(mediaType);

        if (fileSize == 0) {
            return new ValidationResult(false, "Fichier vide");
        }

        if (fileSize > maxSize) {
            String maxMB = String.format("%.0f MB", maxSize / (1024.0 * 1024.0));
            return new ValidationResult(false, "Fichier trop volumineux. Maximum: " + maxMB);
        }

        // Vérifier le type MIME
        String mimeType = getMimeType(file);
        if (mimeType == null) {
            return new ValidationResult(false, "Type de fichier inconnu");
        }

        if (!isAllowedMimeType(mimeType, mediaType)) {
            return new ValidationResult(false, "Type de fichier non autorisé: " + mimeType);
        }

        return new ValidationResult(true, null);
    }

    /**
     * Obtient la taille maximale autorisée pour un type de média
     */
    private long getMaxSize(MediaType mediaType) {
        switch (mediaType) {
            case IMAGE: return MAX_IMAGE_SIZE;
            case VIDEO: return MAX_VIDEO_SIZE;
            case AUDIO: return MAX_AUDIO_SIZE;
            case DOCUMENT: return MAX_DOCUMENT_SIZE;
            default: return MAX_IMAGE_SIZE;
        }
    }

    /**
     * Vérifie si un type MIME est autorisé pour un type de média
     */
    private boolean isAllowedMimeType(String mimeType, MediaType mediaType) {
        switch (mediaType) {
            case IMAGE:
                return mimeType.equals("image/jpeg") ||
                       mimeType.equals("image/png") ||
                       mimeType.equals("image/gif") ||
                       mimeType.equals("image/webp");

            case VIDEO:
                return mimeType.equals("video/mp4") ||
                       mimeType.equals("video/webm") ||
                       mimeType.equals("video/quicktime") ||
                       mimeType.equals("video/x-msvideo");

            case AUDIO:
                return mimeType.equals("audio/mpeg") ||
                       mimeType.equals("audio/wav") ||
                       mimeType.equals("audio/ogg") ||
                       mimeType.equals("audio/mp4") ||
                       mimeType.equals("audio/x-m4a") ||
                       mimeType.equals("audio/webm") ||
                       mimeType.equals("audio/3gpp");

            case DOCUMENT:
                return mimeType.equals("application/pdf") ||
                       mimeType.equals("application/msword") ||
                       mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                       mimeType.equals("application/vnd.ms-excel") ||
                       mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                       mimeType.equals("text/plain") ||
                       mimeType.equals("text/csv");

            default:
                return false;
        }
    }

    /**
     * Détecte le type MIME d'un fichier
     */
    private String getMimeType(File file) {
        String extension = getFileExtension(file.getName());
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return null;
    }

    /**
     * Extrait l'extension d'un fichier
     */
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.lastIndexOf('.') > 0) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        return null;
    }

    /**
     * Parse le résultat JSON de l'upload
     */
    private MediaUploadResult parseResult(JSONObject json) throws Exception {
        MediaUploadResult result = new MediaUploadResult();
        result.setSuccess(json.getBoolean("success"));
        result.setPath(json.getString("path"));
        result.setMimeType(json.optString("mime_type", null));
        result.setFileSize(json.optLong("file_size", 0));
        result.setThumbnailPath(json.optString("thumbnail_path", null));

        // Métadonnées optionnelles
        if (json.has("metadata")) {
            JSONObject metadata = json.getJSONObject("metadata");
            result.setMetadata(metadata);
        }

        return result;
    }

    // Classes internes

    /**
     * Résultat d'un upload
     */
    public static class MediaUploadResult {
        private boolean success;
        private String path;
        private String mimeType;
        private long fileSize;
        private String thumbnailPath;
        private JSONObject metadata;

        // Getters et setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public String getThumbnailPath() { return thumbnailPath; }
        public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

        public JSONObject getMetadata() { return metadata; }
        public void setMetadata(JSONObject metadata) { this.metadata = metadata; }
    }

    /**
     * Résultat de validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String error;

        public ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        public boolean isValid() { return valid; }
        public String getError() { return error; }
    }

    /**
     * Callback pour l'upload
     */
    public interface MediaUploadCallback {
        void onSuccess(MediaUploadResult result);
        void onError(Exception e);
        void onProgress(int percent);
    }

    /**
     * RequestBody avec tracking de progression
     */
    private static class ProgressRequestBody extends RequestBody {
        private final RequestBody delegate;
        private final ProgressListener listener;

        public ProgressRequestBody(RequestBody delegate, ProgressListener listener) {
            this.delegate = delegate;
            this.listener = listener;
        }

        @Override
        public okhttp3.MediaType contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return delegate.contentLength();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            CountingSink countingSink = new CountingSink(sink, contentLength(), listener);
            BufferedSink bufferedSink = Okio.buffer(countingSink);
            delegate.writeTo(bufferedSink);
            bufferedSink.flush();
        }

        private static class CountingSink extends ForwardingSink {
            private final long contentLength;
            private final ProgressListener listener;
            private long bytesWritten = 0;

            public CountingSink(okio.Sink delegate, long contentLength, ProgressListener listener) {
                super(delegate);
                this.contentLength = contentLength;
                this.listener = listener;
            }

            @Override
            public void write(okio.Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                bytesWritten += byteCount;
                if (listener != null) {
                    listener.onProgress(bytesWritten, contentLength);
                }
            }
        }

        private interface ProgressListener {
            void onProgress(long bytesWritten, long contentLength);
        }
    }
}
