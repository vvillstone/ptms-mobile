package com.ptms.mobile.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ptms.mobile.R;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.utils.ApiManager;
import com.ptms.mobile.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activité pour afficher le détail complet d'une note avec édition/suppression
 * D104 - NoteViewerActivity
 */
public class NoteDetailActivity extends AppCompatActivity {

    private static final String TAG = "NoteViewerActivity";
    private static final int REQUEST_EDIT_NOTE = 1001;

    // UI Components
    private Toolbar toolbar;
    private TextView tvNoteTypeIcon, tvNoteTitle, tvNoteTypeLabel, tvImportant;
    private TextView tvProjectName, tvCategory, tvTags;
    private TextView tvNoteContent, tvAuthor, tvCreatedAt, tvUpdatedAt;
    private TextView tvAudioDuration;
    private ImageView imgNoteImage;
    private LinearLayout layoutProjectInfo, layoutCategoryInfo, layoutTags, layoutAudioPlayer;
    private View dividerBeforeContent;
    private Button btnEdit, btnDelete, btnPlayAudio;

    // Data
    private ProjectNote note;
    private SessionManager sessionManager;

    // Audio
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        // Initialiser SessionManager
        sessionManager = new SessionManager(this);

        // Configurer la toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📖 Détail de la note");
        }

        // Initialiser les vues
        initViews();

        // Récupérer la note depuis l'Intent
        loadNoteFromIntent();

        // Afficher la note
        if (note != null) {
            displayNote();
            setupButtons();
        } else {
            Toast.makeText(this, "Erreur: Note introuvable", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Initialise toutes les vues
     */
    private void initViews() {
        // Header
        tvNoteTypeIcon = findViewById(R.id.tv_note_type_icon);
        tvNoteTitle = findViewById(R.id.tv_note_title);
        tvNoteTypeLabel = findViewById(R.id.tv_note_type_label);
        tvImportant = findViewById(R.id.tv_important);

        // Info sections
        layoutProjectInfo = findViewById(R.id.layout_project_info);
        layoutCategoryInfo = findViewById(R.id.layout_category_info);
        layoutTags = findViewById(R.id.layout_tags);
        tvProjectName = findViewById(R.id.tv_project_name);
        tvCategory = findViewById(R.id.tv_category);
        tvTags = findViewById(R.id.tv_tags);

        // Audio player
        layoutAudioPlayer = findViewById(R.id.layout_audio_player);
        tvAudioDuration = findViewById(R.id.tv_audio_duration);
        btnPlayAudio = findViewById(R.id.btn_play_audio);

        // Content
        dividerBeforeContent = findViewById(R.id.divider_before_content);
        tvNoteContent = findViewById(R.id.tv_note_content);
        imgNoteImage = findViewById(R.id.img_note_image);

        // Footer
        tvAuthor = findViewById(R.id.tv_author);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        tvUpdatedAt = findViewById(R.id.tv_updated_at);

        // Buttons
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
    }

    /**
     * Charge la note depuis l'Intent
     */
    private void loadNoteFromIntent() {
        Intent intent = getIntent();

        // Essayer de récupérer l'objet ProjectNote directement
        if (intent.hasExtra("note")) {
            // Impossible de passer l'objet directement, donc on reconstruit depuis les extras
        }

        // Reconstruire la note depuis les extras
        if (intent.hasExtra("note_id")) {
            note = new ProjectNote();
            note.setId(intent.getIntExtra("note_id", 0));
            note.setLocalId(intent.getLongExtra("note_local_id", 0));
            note.setProjectId(intent.getIntExtra("note_project_id", 0));
            note.setProjectName(intent.getStringExtra("note_project_name"));
            note.setUserId(intent.getIntExtra("note_user_id", 0));
            note.setNoteType(intent.getStringExtra("note_type"));
            note.setNoteGroup(intent.getStringExtra("note_group"));
            note.setTitle(intent.getStringExtra("note_title"));
            note.setContent(intent.getStringExtra("note_content"));
            note.setTranscription(intent.getStringExtra("note_transcription"));
            note.setAudioPath(intent.getStringExtra("note_audio_path"));
            note.setAudioDuration(intent.getIntExtra("note_audio_duration", 0));
            note.setLocalFilePath(intent.getStringExtra("note_image_path"));  // Image path
            note.setImportant(intent.getBooleanExtra("note_important", false));
            note.setAuthorName(intent.getStringExtra("note_author"));
            note.setCreatedAt(intent.getStringExtra("note_created_at"));
            note.setUpdatedAt(intent.getStringExtra("note_updated_at"));

            // Tags
            String tagsString = intent.getStringExtra("note_tags");
            if (tagsString != null && !tagsString.isEmpty()) {
                java.util.List<String> tags = java.util.Arrays.asList(tagsString.split(","));
                note.setTags(tags);
            }

            Log.d(TAG, "Note chargée: " + note.getTitle() + " (ID: " + note.getId() + ")");
        }
    }

    /**
     * Affiche toutes les informations de la note
     */
    private void displayNote() {
        // Header
        tvNoteTypeIcon.setText(note.getTypeIcon());
        tvNoteTitle.setText(note.getTitle() != null && !note.getTitle().isEmpty()
            ? note.getTitle()
            : "Note sans titre");

        // Type label
        String typeLabel = getTypeLabel(note.getNoteType());
        tvNoteTypeLabel.setText(typeLabel);

        // Important
        tvImportant.setVisibility(note.isImportant() ? View.VISIBLE : View.GONE);

        // Project info
        if (note.getProjectName() != null && !note.getProjectName().isEmpty()) {
            layoutProjectInfo.setVisibility(View.VISIBLE);
            tvProjectName.setText(note.getProjectName());
        } else {
            layoutProjectInfo.setVisibility(View.GONE);
        }

        // Category
        if (note.getNoteGroup() != null && !note.getNoteGroup().isEmpty()) {
            layoutCategoryInfo.setVisibility(View.VISIBLE);
            tvCategory.setText(getCategoryLabel(note.getNoteGroup()));
        } else {
            layoutCategoryInfo.setVisibility(View.GONE);
        }

        // Tags
        if (note.getTags() != null && !note.getTags().isEmpty()) {
            layoutTags.setVisibility(View.VISIBLE);
            tvTags.setText(String.join(", ", note.getTags()));
        } else {
            layoutTags.setVisibility(View.GONE);
        }

        // Divider
        if (layoutProjectInfo.getVisibility() == View.VISIBLE ||
            layoutCategoryInfo.getVisibility() == View.VISIBLE ||
            layoutTags.getVisibility() == View.VISIBLE) {
            dividerBeforeContent.setVisibility(View.VISIBLE);
        }

        // Audio player
        if ("audio".equals(note.getNoteType()) || "dictation".equals(note.getNoteType())) {
            layoutAudioPlayer.setVisibility(View.VISIBLE);
            if (note.getAudioDuration() != null && note.getAudioDuration() > 0) {
                tvAudioDuration.setText("Durée: " + formatDuration(note.getAudioDuration()));
            } else {
                tvAudioDuration.setText("Durée: N/A");
            }
        } else {
            layoutAudioPlayer.setVisibility(View.GONE);
        }

        // Content
        String content = getDisplayContent();
        if (content != null && !content.isEmpty()) {
            tvNoteContent.setText(content);
            tvNoteContent.setVisibility(View.VISIBLE);
        } else {
            tvNoteContent.setText("(Aucun contenu)");
            tvNoteContent.setVisibility(View.VISIBLE);
        }

        // Image (if exists)
        loadNoteImage();

        // Footer
        tvAuthor.setText("👤 Auteur: " + (note.getAuthorName() != null ? note.getAuthorName() : "Inconnu"));
        tvCreatedAt.setText("📅 Créé le: " + formatDate(note.getCreatedAt()));

        if (note.getUpdatedAt() != null && !note.getUpdatedAt().isEmpty()
            && !note.getUpdatedAt().equals(note.getCreatedAt())) {
            tvUpdatedAt.setVisibility(View.VISIBLE);
            tvUpdatedAt.setText("🔄 Modifié le: " + formatDate(note.getUpdatedAt()));
        } else {
            tvUpdatedAt.setVisibility(View.GONE);
        }
    }

    /**
     * Retourne le contenu à afficher selon le type de note
     */
    private String getDisplayContent() {
        if ("text".equals(note.getNoteType())) {
            return note.getContent();
        } else if ("dictation".equals(note.getNoteType())) {
            if (note.getTranscription() != null && !note.getTranscription().isEmpty()) {
                return "📝 Transcription:\n\n" + note.getTranscription();
            } else {
                return "(Transcription non disponible)";
            }
        } else if ("audio".equals(note.getNoteType())) {
            return "🎵 Note audio\n\nCette note contient un enregistrement audio. " +
                   "Cliquez sur le bouton \"Écouter\" ci-dessus pour lire l'audio.";
        }
        return note.getContent();
    }

    /**
     * Configure les boutons d'action
     */
    private void setupButtons() {
        // Bouton Éditer
        btnEdit.setOnClickListener(v -> editNote());

        // Bouton Supprimer
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        // Bouton Lecture audio
        if (layoutAudioPlayer.getVisibility() == View.VISIBLE) {
            btnPlayAudio.setOnClickListener(v -> playAudio());
        }
    }

    /**
     * Affiche la confirmation de suppression
     */
    private void showDeleteConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Supprimer la note");
        builder.setMessage("Êtes-vous sûr de vouloir supprimer cette note ?\n\n" +
                          "\"" + (note.getTitle() != null ? note.getTitle() : "Sans titre") + "\"\n\n" +
                          "Cette action est irréversible.");
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        builder.setPositiveButton("Supprimer", (dialog, which) -> deleteNote());
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    /**
     * Supprime la note via l'API
     */
    private void deleteNote() {
        if (note.getId() == 0) {
            Toast.makeText(this, "Impossible de supprimer une note non synchronisée", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = ApiManager.getBaseUrl() + "/api/project-notes.php?note_id=" + note.getId();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            Toast.makeText(this, "Note supprimée avec succès", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK); // Indiquer que la note a été supprimée
                            finish();
                        } else {
                            String error = response.optString("message", "Erreur");
                            Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur de parsing", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Erreur suppression note", error);
                    Toast.makeText(this, "Erreur réseau: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };

        ApiManager.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Lit l'audio
     */
    private void playAudio() {
        if (note.getAudioPath() == null || note.getAudioPath().isEmpty()) {
            Toast.makeText(this, "Chemin audio non disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            stopPlaying();
        } else {
            try {
                // Construire l'URL complète
                String audioUrl = ApiManager.getBaseUrl() + note.getAudioPath();
                Log.d(TAG, "Lecture audio: " + audioUrl);

                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepareAsync(); // Préparation asynchrone pour éviter blocage

                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    isPlaying = true;
                    btnPlayAudio.setText("⏸️ Arrêter");
                    Toast.makeText(this, "Lecture en cours...", Toast.LENGTH_SHORT).show();
                });

                mediaPlayer.setOnCompletionListener(mp -> {
                    isPlaying = false;
                    btnPlayAudio.setText("▶️ Écouter");
                    Toast.makeText(this, "Lecture terminée", Toast.LENGTH_SHORT).show();
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "Erreur MediaPlayer: what=" + what + ", extra=" + extra);
                    isPlaying = false;
                    btnPlayAudio.setText("▶️ Écouter");
                    Toast.makeText(this, "Erreur lecture audio", Toast.LENGTH_SHORT).show();
                    return true;
                });

            } catch (IOException e) {
                Log.e(TAG, "Erreur lecture audio", e);
                Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                isPlaying = false;
                btnPlayAudio.setText("▶️ Écouter");
            }
        }
    }

    /**
     * Arrête la lecture audio
     */
    private void stopPlaying() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Erreur arrêt lecture", e);
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        btnPlayAudio.setText("▶️ Écouter");
    }

    /**
     * Formate une durée en secondes vers MM:SS
     */
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    /**
     * Formate une date au format "DD/MM/YYYY HH:mm"
     */
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "N/A";
        }

        try {
            // Format: 2025-10-15 10:30:00
            String[] parts = dateStr.split(" ");
            if (parts.length >= 2) {
                String[] dateParts = parts[0].split("-");
                String[] timeParts = parts[1].split(":");
                return dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0] + " " +
                       timeParts[0] + ":" + timeParts[1];
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur formatage date", e);
        }
        return dateStr;
    }

    /**
     * Charge et affiche le média de la note si elle existe (image ou vidéo)
     */
    private void loadNoteImage() {
        String mediaPath = note.getLocalFilePath();

        if (mediaPath == null || mediaPath.isEmpty()) {
            imgNoteImage.setVisibility(View.GONE);
            return;
        }

        // Déterminer le type de média
        String noteType = note.getNoteType();
        final String mediaType = isVideoType(noteType, mediaPath) ? "video" : "image";

        // Construction de l'URL complète
        // BASE_URL = "https://serveralpha.protti.group/api/"
        // On remonte d'un niveau pour avoir: "https://serveralpha.protti.group/"
        String baseUrl = com.ptms.mobile.utils.ApiConfig.BASE_URL;

        // Retirer "/api/" de la fin pour avoir l'URL racine
        if (baseUrl.endsWith("/api/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 5);  // Retirer "/api/"
        } else if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // Assurer que mediaPath commence sans slash
        if (mediaPath.startsWith("/")) {
            mediaPath = mediaPath.substring(1);
        }

        final String mediaUrl = baseUrl + "/" + mediaPath;
        Log.d(TAG, "Chargement média depuis: " + mediaUrl + " (type: " + mediaType + ")");

        if (mediaType.equals("video")) {
            // Pour les vidéos, afficher une miniature avec un bouton de lecture
            displayVideoThumbnail(mediaUrl);
        } else {
            // Pour les images, charger et afficher normalement
            displayImage(mediaUrl);
        }
    }

    /**
     * Détermine si le média est une vidéo
     */
    private boolean isVideoType(String noteType, String filePath) {
        // Vérifier le type de note
        if ("video".equalsIgnoreCase(noteType)) {
            return true;
        }

        // Vérifier l'extension du fichier
        if (filePath != null) {
            String lowerPath = filePath.toLowerCase();
            return lowerPath.endsWith(".mp4") || lowerPath.endsWith(".avi") ||
                   lowerPath.endsWith(".mov") || lowerPath.endsWith(".mkv") ||
                   lowerPath.endsWith(".webm") || lowerPath.endsWith(".3gp");
        }

        return false;
    }

    /**
     * Affiche une image
     */
    private void displayImage(String imageUrl) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(imageUrl);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(url.openStream());

                runOnUiThread(() -> {
                    if (bitmap != null) {
                        imgNoteImage.setImageBitmap(bitmap);
                        imgNoteImage.setVisibility(View.VISIBLE);

                        // Rendre l'image cliquable pour l'ouvrir en plein écran
                        imgNoteImage.setOnClickListener(v -> openMediaViewer(imageUrl, "image"));

                        Log.d(TAG, "Image chargée avec succès");
                    } else {
                        imgNoteImage.setVisibility(View.GONE);
                        Log.w(TAG, "Impossible de décoder l'image");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Erreur chargement image: " + e.getMessage(), e);
                runOnUiThread(() -> imgNoteImage.setVisibility(View.GONE));
            }
        }).start();
    }

    /**
     * Affiche une miniature de vidéo avec un bouton de lecture
     */
    private void displayVideoThumbnail(final String videoUrl) {
        // Essayer de générer une miniature depuis le fichier local ou afficher une icône
        runOnUiThread(() -> {
            // Créer un bitmap avec une icône de lecture
            android.graphics.Bitmap playIcon = createPlayIconBitmap();
            imgNoteImage.setImageBitmap(playIcon);
            imgNoteImage.setVisibility(View.VISIBLE);

            // Rendre cliquable pour ouvrir la vidéo en plein écran
            imgNoteImage.setOnClickListener(v -> openMediaViewer(videoUrl, "video"));

            Log.d(TAG, "Miniature vidéo affichée");
        });
    }

    /**
     * Crée un bitmap avec une icône de lecture pour les vidéos
     */
    private android.graphics.Bitmap createPlayIconBitmap() {
        int width = 400;
        int height = 300;
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

        // Fond gris
        android.graphics.Paint bgPaint = new android.graphics.Paint();
        bgPaint.setColor(0xFF424242);
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Triangle de lecture (centré)
        android.graphics.Paint playPaint = new android.graphics.Paint();
        playPaint.setColor(0xFFFFFFFF);
        playPaint.setStyle(android.graphics.Paint.Style.FILL);

        android.graphics.Path playPath = new android.graphics.Path();
        int centerX = width / 2;
        int centerY = height / 2;
        int triangleSize = 80;

        playPath.moveTo(centerX - triangleSize / 2, centerY - triangleSize);
        playPath.lineTo(centerX - triangleSize / 2, centerY + triangleSize);
        playPath.lineTo(centerX + triangleSize, centerY);
        playPath.close();

        canvas.drawPath(playPath, playPaint);

        // Texte "Vidéo"
        android.graphics.Paint textPaint = new android.graphics.Paint();
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        canvas.drawText("🎥 Vidéo", centerX, centerY + triangleSize + 60, textPaint);

        return bitmap;
    }

    /**
     * Retourne le label du type de note
     */
    private String getTypeLabel(String noteType) {
        if (noteType == null) return "Note";

        switch (noteType) {
            case "text":
                return "Note de texte";
            case "audio":
                return "Note audio";
            case "dictation":
                return "Note dictée";
            default:
                return "Note";
        }
    }

    /**
     * Retourne le label de la catégorie
     */
    private String getCategoryLabel(String noteGroup) {
        if (noteGroup == null) return "";

        switch (noteGroup) {
            case "project":
                return "📁 Projet";
            case "personal":
                return "👤 Personnel";
            case "meeting":
                return "🤝 Réunion";
            case "todo":
                return "✅ TODO";
            case "idea":
                return "💡 Idée";
            case "issue":
                return "⚠️ Problème";
            case "other":
                return "📌 Autre";
            default:
                return noteGroup;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Ouvre l'activité d'édition de note
     */
    private void editNote() {
        if (note == null) {
            Toast.makeText(this, "Erreur: Note introuvable", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Édition de la note ID: " + note.getId());

        // Ouvrir CreateNoteUnifiedActivity en mode édition
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra(NoteEditorActivity.EXTRA_EDIT_MODE, true);
        intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.getId());
        intent.putExtra(NoteEditorActivity.EXTRA_PROJECT_ID, note.getProjectId());
        startActivityForResult(intent, REQUEST_EDIT_NOTE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT_NOTE && resultCode == RESULT_OK) {
            // Recharger la note modifiée
            if (note != null) {
                // Recharger depuis l'Intent ou la DB
                loadNoteFromIntent();
                if (note != null) {
                    displayNote();
                    Toast.makeText(this, "✅ Note mise à jour", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Ouvre le visualiseur de média en plein écran
     */
    private void openMediaViewer(String mediaUrl, String mediaType) {
        Intent intent = new Intent(this, MediaViewerActivity.class);
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_URL, mediaUrl);
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, mediaType);
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TITLE, note.getTitle());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Nettoyer le MediaPlayer
        stopPlaying();
    }
}
