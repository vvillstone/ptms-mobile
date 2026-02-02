package com.ptms.mobile.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ptms.mobile.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

/**
 * Activité pour afficher des médias en plein écran
 * - Images avec zoom/pan (PhotoView)
 * - Vidéos avec lecteur
 * - Documents (TODO: utiliser un viewer PDF)
 */
public class MediaViewerActivity extends AppCompatActivity {

    private static final String TAG = "MediaViewerActivity";

    public static final String EXTRA_MEDIA_URL = "media_url";
    public static final String EXTRA_MEDIA_TYPE = "media_type";  // image, video, document
    public static final String EXTRA_MEDIA_TITLE = "media_title";

    private Toolbar toolbar;
    private PhotoView photoView;
    private VideoView videoView;
    private TextView tvDocumentInfo;
    private ProgressBar progressBar;

    private String mediaUrl;
    private String mediaType;
    private String mediaTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);

        // Récupérer les paramètres
        mediaUrl = getIntent().getStringExtra(EXTRA_MEDIA_URL);
        mediaType = getIntent().getStringExtra(EXTRA_MEDIA_TYPE);
        mediaTitle = getIntent().getStringExtra(EXTRA_MEDIA_TITLE);

        if (mediaUrl == null || mediaUrl.isEmpty()) {
            Toast.makeText(this, "URL du média manquante", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurer la toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mediaTitle != null ? mediaTitle : "Média");
        }

        // Initialiser les vues
        photoView = findViewById(R.id.photo_view);
        videoView = findViewById(R.id.video_view);
        tvDocumentInfo = findViewById(R.id.tv_document_info);
        progressBar = findViewById(R.id.progress_bar);

        // Afficher le média selon le type
        if ("video".equalsIgnoreCase(mediaType)) {
            displayVideo();
        } else if ("document".equalsIgnoreCase(mediaType)) {
            displayDocument();
        } else {
            // Par défaut, traiter comme une image
            displayImage();
        }
    }

    /**
     * Affiche une image avec support zoom/pan
     */
    private void displayImage() {
        photoView.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
        tvDocumentInfo.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Charger l'image en arrière-plan
        new Thread(() -> {
            try {
                // Vérifier si c'est un fichier local ou une URL
                Bitmap bitmap;
                if (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://")) {
                    // URL distante
                    java.net.URL url = new java.net.URL(mediaUrl);
                    bitmap = BitmapFactory.decodeStream(url.openStream());
                } else {
                    // Fichier local
                    File file = new File(mediaUrl);
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                }

                final Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (finalBitmap != null) {
                        photoView.setImageBitmap(finalBitmap);
                        Log.d(TAG, "Image chargée avec succès");
                    } else {
                        Toast.makeText(this, "Impossible de charger l'image", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Impossible de décoder l'image");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Erreur chargement image: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Affiche une vidéo avec contrôles
     */
    private void displayVideo() {
        photoView.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        tvDocumentInfo.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        try {
            Uri videoUri = Uri.parse(mediaUrl);
            videoView.setVideoURI(videoUri);

            // Ajouter les contrôles de lecture
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            // Démarrer la lecture automatiquement
            videoView.setOnPreparedListener(mp -> {
                videoView.start();
                Log.d(TAG, "Vidéo prête à être lue");
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Erreur lecture vidéo: what=" + what + ", extra=" + extra);
                Toast.makeText(this, "Erreur de lecture vidéo", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur affichage vidéo: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Affiche les informations d'un document
     */
    private void displayDocument() {
        photoView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        tvDocumentInfo.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        // TODO: Intégrer un viewer PDF (ex: AndroidPdfViewer)
        tvDocumentInfo.setText("📄 Document\n\n" +
                "Nom: " + (mediaTitle != null ? mediaTitle : "Document") + "\n" +
                "URL: " + mediaUrl + "\n\n" +
                "Note: La visualisation de documents PDF n'est pas encore implémentée.\n" +
                "Vous pouvez télécharger le fichier ou l'ouvrir dans une application externe.");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Arrêter la vidéo si en cours de lecture
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libérer les ressources
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
