package com.ptms.mobile.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.ptms.mobile.R;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.auth.TokenManager;
import com.ptms.mobile.models.NoteType;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.storage.MediaStorageManager;
import com.ptms.mobile.sync.BidirectionalSyncManager;
import com.ptms.mobile.utils.PhotoManager;
import com.ptms.mobile.utils.MediaUploadManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité unifiée pour créer des notes (texte, dictée, audio)
 */
public class NoteEditorActivity extends AppCompatActivity {

    private static final String TAG = "CreateNoteUnified";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_SPEECH_RECOGNITION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = PhotoManager.REQUEST_IMAGE_CAPTURE;
    private static final int REQUEST_IMAGE_PICK = PhotoManager.REQUEST_IMAGE_PICK;
    private static final int REQUEST_VIDEO_CAPTURE = 300;
    private static final int REQUEST_VIDEO_PICK = 301;

    // Mode édition
    public static final String EXTRA_EDIT_MODE = "edit_mode";
    public static final String EXTRA_NOTE_ID = "note_id";
    public static final String EXTRA_PROJECT_ID = "project_id";

    private boolean isEditMode = false;
    private int editNoteId = -1;

    // UI Components - Type selection
    private RadioGroup radioGroupNoteType;

    // UI Components - Cards for each type
    private MaterialCardView cardTextInput;
    private MaterialCardView cardDictationInput;
    private MaterialCardView cardAudioInput;
    private MaterialCardView cardVideoInput;

    // UI Components - Text mode
    private TextInputEditText editContent;

    // UI Components - Dictation mode
    private Button btnStartDictation;
    private TextView tvDictationStatus;
    private TextInputEditText editDictationContent;

    // UI Components - Audio mode
    private Button btnStartRecording;
    private Button btnStopRecording;
    private Button btnPlayAudio;
    private TextView tvRecordingStatus;
    private TextView tvRecordingDuration;

    // UI Components - Common fields
    private TextInputEditText editTitle;
    private AutoCompleteTextView spinnerProject;
    private AutoCompleteTextView spinnerNoteGroup;
    private MaterialCheckBox checkImportant;
    private AutoCompleteTextView editTags;
    private Button btnSave;

    // Data
    private BidirectionalSyncManager syncManager;
    private ApiService apiService;
    private String authToken;
    private MediaUploadManager uploadManager;

    // Image management
    private PhotoManager photoManager;
    private MediaStorageManager mediaStorageManager;
    private ImageView imgPreview;
    private Button btnCamera;
    private Button btnGallery;
    private File currentImageFile;
    private String currentImagePath;
    private List<Project> projects = new ArrayList<>();
    private List<NoteType> noteTypes = new ArrayList<>();
    private List<String> commonTags = new ArrayList<>();

    // Audio recording
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String audioFilePath;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private Handler recordingHandler = new Handler();
    private long recordingStartTime = 0;
    private int recordedDuration = 0; // Durée en secondes de l'enregistrement terminé

    // UI Components - Video mode
    private Button btnRecordVideo;
    private Button btnSelectVideo;
    private TextView tvVideoStatus;
    private ImageView imgVideoThumbnail;
    private TextView tvVideoDuration;

    // Video recording
    private String videoFilePath;
    private File currentVideoFile;

    // Current note type
    private String currentNoteType = "text"; // text, dictation, audio, video

    // Network status
    private boolean isOnline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        // Vérifier si mode édition
        isEditMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
        editNoteId = getIntent().getIntExtra(EXTRA_NOTE_ID, -1);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Modifier la Note" : "Nouvelle Note");
        }

        // Initialiser les services
        syncManager = new BidirectionalSyncManager(this);
        apiService = com.ptms.mobile.api.ApiClient.getInstance(this).getApiService();

        // Initialiser les gestionnaires d'images
        photoManager = new PhotoManager(this);
        mediaStorageManager = new MediaStorageManager(this);

        // ✅ Utiliser TokenManager centralisé au lieu de SharedPreferences direct
        TokenManager tokenManager = TokenManager.getInstance(this);
        authToken = tokenManager.getToken();

        // Initialiser MediaUploadManager
        SharedPreferences prefs = getSharedPreferences("ptms_prefs", MODE_PRIVATE);
        String baseUrl = prefs.getString("server_url", "https://serveralpha.protti.group");
        uploadManager = new MediaUploadManager(baseUrl);
        uploadManager.setAuthToken(authToken);

        // Initialiser les vues
        initViews();

        // Charger les données
        loadProjects();
        loadNoteTypes();

        // Configurer les listeners
        setupListeners();
        setupImageListeners();

        // Si mode édition, charger la note existante
        if (isEditMode && editNoteId > 0) {
            loadExistingNote(editNoteId);
        }
    }

    private void initViews() {
        // Type selection
        radioGroupNoteType = findViewById(R.id.radioGroupNoteType);

        // Cards
        cardTextInput = findViewById(R.id.cardTextInput);
        cardDictationInput = findViewById(R.id.cardDictationInput);
        cardAudioInput = findViewById(R.id.cardAudioInput);
        cardVideoInput = findViewById(R.id.cardVideoInput);

        // Text mode
        editContent = findViewById(R.id.editContent);

        // Dictation mode
        btnStartDictation = findViewById(R.id.btnStartDictation);
        tvDictationStatus = findViewById(R.id.tvDictationStatus);
        editDictationContent = findViewById(R.id.editDictationContent);

        // Audio mode
        btnStartRecording = findViewById(R.id.btnStartRecording);
        btnStopRecording = findViewById(R.id.btnStopRecording);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus);
        tvRecordingDuration = findViewById(R.id.tvRecordingDuration);

        // Video mode
        btnRecordVideo = findViewById(R.id.btnRecordVideo);
        btnSelectVideo = findViewById(R.id.btnSelectVideo);
        tvVideoStatus = findViewById(R.id.tvVideoStatus);
        imgVideoThumbnail = findViewById(R.id.imgVideoThumbnail);
        tvVideoDuration = findViewById(R.id.tvVideoDuration);

        // Common fields
        editTitle = findViewById(R.id.editTitle);
        spinnerProject = findViewById(R.id.spinnerProject);
        spinnerNoteGroup = findViewById(R.id.spinnerNoteGroup);
        checkImportant = findViewById(R.id.checkImportant);
        editTags = findViewById(R.id.editTags);
        btnSave = findViewById(R.id.btnSave);

        // Image controls
        imgPreview = findViewById(R.id.img_preview);
        btnCamera = findViewById(R.id.btn_camera);
        btnGallery = findViewById(R.id.btn_gallery);

        // Masquer les contrôles images initialement
        if (imgPreview != null) imgPreview.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // RadioGroup listener - switch between note types
        radioGroupNoteType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioText) {
                showTextMode();
            } else if (checkedId == R.id.radioDictation) {
                showDictationMode();
            } else if (checkedId == R.id.radioAudio) {
                showAudioMode();
            } else if (checkedId == R.id.radioVideo) {
                showVideoMode();
            }
        });

        // Dictation button
        btnStartDictation.setOnClickListener(v -> startDictation());

        // Audio recording buttons
        btnStartRecording.setOnClickListener(v -> startRecording());
        btnStopRecording.setOnClickListener(v -> stopRecording());
        btnPlayAudio.setOnClickListener(v -> playAudio());

        // Video recording buttons
        btnRecordVideo.setOnClickListener(v -> recordVideo());
        btnSelectVideo.setOnClickListener(v -> selectVideo());

        // Save button
        btnSave.setOnClickListener(v -> saveNote());
    }

    private void showTextMode() {
        currentNoteType = "text";
        cardTextInput.setVisibility(View.VISIBLE);
        cardDictationInput.setVisibility(View.GONE);
        cardAudioInput.setVisibility(View.GONE);
        cardVideoInput.setVisibility(View.GONE);
    }

    private void showDictationMode() {
        currentNoteType = "dictation";
        cardTextInput.setVisibility(View.GONE);
        cardDictationInput.setVisibility(View.VISIBLE);
        cardAudioInput.setVisibility(View.GONE);
        cardVideoInput.setVisibility(View.GONE);
    }

    private void showAudioMode() {
        currentNoteType = "audio";
        cardTextInput.setVisibility(View.GONE);
        cardDictationInput.setVisibility(View.GONE);
        cardAudioInput.setVisibility(View.VISIBLE);
        cardVideoInput.setVisibility(View.GONE);
    }

    private void showVideoMode() {
        currentNoteType = "video";
        cardTextInput.setVisibility(View.GONE);
        cardDictationInput.setVisibility(View.GONE);
        cardAudioInput.setVisibility(View.GONE);
        cardVideoInput.setVisibility(View.VISIBLE);
    }

    private void loadProjects() {
        projects = syncManager.getProjects();

        List<String> projectNames = new ArrayList<>();
        projectNames.add("Aucun projet"); // Option pour notes personnelles
        for (Project project : projects) {
            projectNames.add(project.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, projectNames);
        spinnerProject.setAdapter(adapter);
    }

    private void loadNoteTypes() {
        // TODO: Implémenter getNoteTypes() dans BidirectionalSyncManager ou OfflineDatabaseHelper
        noteTypes = new ArrayList<>();

        List<String> typeNames = new ArrayList<>();
        for (NoteType type : noteTypes) {
            typeNames.add(type.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, typeNames);
        spinnerNoteGroup.setAdapter(adapter);

        // Set default to "Projet"
        if (!typeNames.isEmpty()) {
            spinnerNoteGroup.setText("Projet", false);
        }
    }

    private void startDictation() {
        // Vérifier la permission microphone
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        // Lancer la reconnaissance vocale
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...");

        try {
            startActivityForResult(intent, REQUEST_SPEECH_RECOGNITION);
            tvDictationStatus.setText("En écoute...");
        } catch (Exception e) {
            Toast.makeText(this, "Reconnaissance vocale non disponible", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Speech recognition error", e);
        }
    }

    private void startRecording() {
        // Vérifier la permission microphone
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        try {
            // Préparer le fichier audio
            File audioDir = new File(getExternalFilesDir(null), "audio_notes");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            audioFilePath = new File(audioDir, "note_" + System.currentTimeMillis() + ".3gp").getAbsolutePath();

            // Configurer le MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            // UI updates
            btnStartRecording.setEnabled(false);
            btnStopRecording.setEnabled(true);
            tvRecordingStatus.setText("Enregistrement en cours...");

            // Start timer
            updateRecordingDuration();

            Toast.makeText(this, "Enregistrement démarré", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, "Audio recording failed", e);
            Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                isRecording = false;

                // Calculer et sauvegarder la durée RÉELLE de l'enregistrement
                long elapsed = System.currentTimeMillis() - recordingStartTime;
                recordedDuration = (int) (elapsed / 1000);

                // UI updates
                btnStartRecording.setEnabled(true);
                btnStopRecording.setEnabled(false);
                tvRecordingStatus.setText("Enregistrement terminé (" + formatDuration(recordedDuration) + ")");

                recordingHandler.removeCallbacksAndMessages(null);

                Toast.makeText(this, "Enregistrement sauvegardé: " + formatDuration(recordedDuration), Toast.LENGTH_SHORT).show();

                Log.d(TAG, "Audio recorded successfully: " + audioFilePath + " (duration: " + recordedDuration + "s)");

            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording", e);
                Toast.makeText(this, "Erreur arrêt enregistrement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Lire l'enregistrement audio
     */
    private void playAudio() {
        if (audioFilePath == null || !new File(audioFilePath).exists()) {
            Toast.makeText(this, "Aucun audio à lire", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            // Arrêter la lecture
            stopPlaying();
        } else {
            // Démarrer la lecture
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(audioFilePath);
                mediaPlayer.prepare();
                mediaPlayer.start();

                isPlaying = true;
                btnPlayAudio.setText("⏸️ Arrêter");

                // Listener pour la fin de lecture
                mediaPlayer.setOnCompletionListener(mp -> {
                    isPlaying = false;
                    btnPlayAudio.setText("▶️ Écouter");
                    Toast.makeText(NoteEditorActivity.this, "Lecture terminée", Toast.LENGTH_SHORT).show();
                });

                Toast.makeText(this, "Lecture en cours...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Playing audio: " + audioFilePath);

            } catch (IOException e) {
                Log.e(TAG, "Error playing audio", e);
                Toast.makeText(this, "Erreur lecture audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                isPlaying = false;
                btnPlayAudio.setText("▶️ Écouter");
            }
        }
    }

    /**
     * Arrêter la lecture audio
     */
    private void stopPlaying() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
                btnPlayAudio.setText("▶️ Écouter");
                Toast.makeText(this, "Lecture arrêtée", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping playback", e);
            }
        }
    }

    /**
     * Formater la durée en MM:SS
     */
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    private void updateRecordingDuration() {
        if (isRecording) {
            long elapsed = System.currentTimeMillis() - recordingStartTime;
            int seconds = (int) (elapsed / 1000) % 60;
            int minutes = (int) (elapsed / 1000) / 60;

            tvRecordingDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

            recordingHandler.postDelayed(this::updateRecordingDuration, 1000);
        }
    }

    private void saveNote() {
        // Désactiver le bouton pour éviter les clics multiples
        btnSave.setEnabled(false);
        btnSave.setText("Enregistrement...");

        // Validation
        String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un titre", Toast.LENGTH_SHORT).show();
            // Réactiver le bouton en cas d'erreur de validation
            btnSave.setEnabled(true);
            btnSave.setText("Enregistrer");
            return;
        }

        String content = "";
        String transcription = null;
        String audioPath = null;
        Integer audioDuration = null;

        // Récupérer le contenu selon le type
        if (currentNoteType.equals("text")) {
            content = editContent.getText() != null ? editContent.getText().toString().trim() : "";
            if (content.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer du contenu", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("Enregistrer");
                return;
            }
        } else if (currentNoteType.equals("dictation")) {
            transcription = editDictationContent.getText() != null ?
                    editDictationContent.getText().toString().trim() : "";
            content = transcription; // Le contenu est la transcription
            if (content.isEmpty()) {
                Toast.makeText(this, "Veuillez dicter du texte", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("Enregistrer");
                return;
            }
        } else if (currentNoteType.equals("audio")) {
            if (audioFilePath == null || !new File(audioFilePath).exists()) {
                Toast.makeText(this, "Veuillez enregistrer un audio", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("Enregistrer");
                return;
            }
            if (recordedDuration == 0) {
                Toast.makeText(this, "Durée d'enregistrement invalide", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Audio file exists but duration is 0: " + audioFilePath);
                btnSave.setEnabled(true);
                btnSave.setText("Enregistrer");
                return;
            }
            audioPath = audioFilePath;
            audioDuration = recordedDuration; // Utiliser la durée sauvegardée lors de stopRecording()

            Log.d(TAG, "Preparing to save audio note: path=" + audioPath + ", duration=" + audioDuration + "s");
        }

        // Récupérer les autres champs
        String projectName = spinnerProject.getText() != null ?
                spinnerProject.getText().toString().trim() : "";
        Integer projectId = null;
        if (!projectName.isEmpty() && !projectName.equals("Aucun projet")) {
            for (Project project : projects) {
                if (project.getName().equals(projectName)) {
                    projectId = project.getId();
                    break;
                }
            }
        }

        String categoryName = spinnerNoteGroup.getText() != null ?
                spinnerNoteGroup.getText().toString().trim() : "Projet";
        Integer noteTypeId = null;
        String noteGroup = "project"; // default
        for (NoteType type : noteTypes) {
            if (type.getName().equals(categoryName)) {
                noteTypeId = type.getId();
                noteGroup = type.getSlug();
                break;
            }
        }

        boolean isImportant = checkImportant.isChecked();
        String tags = editTags.getText() != null ? editTags.getText().toString().trim() : null;

        // Envoyer la note à l'API
        sendNoteToApi(projectId, currentNoteType, noteTypeId, noteGroup, title, content,
                audioPath, audioDuration, transcription, isImportant, tags, currentImagePath);
    }

    private void sendNoteToApi(Integer projectId, String noteType, Integer noteTypeId,
                                String noteGroup, String title, String content,
                                String audioPath, Integer audioDuration, String transcription,
                                boolean isImportant, String tags, String imagePath) {

        // Upload média d'abord si nécessaire, puis créer la note
        if (audioPath != null && !audioPath.isEmpty()) {
            uploadAudioAndCreateNote(projectId, noteType, noteTypeId, noteGroup, title, content,
                    audioPath, audioDuration, transcription, isImportant, tags, imagePath);
        } else if (imagePath != null && !imagePath.isEmpty()) {
            uploadImageAndCreateNote(projectId, noteType, noteTypeId, noteGroup, title, content,
                    imagePath, transcription, isImportant, tags);
        } else if (videoFilePath != null && !videoFilePath.isEmpty()) {
            uploadVideoAndCreateNote(projectId, noteType, noteTypeId, noteGroup, title, content,
                    videoFilePath, transcription, isImportant, tags);
        } else {
            // Pas de média, créer la note directement
            createNoteWithoutMedia(projectId, noteType, noteTypeId, noteGroup, title, content,
                    transcription, isImportant, tags);
        }
    }

    /**
     * Upload audio puis créer la note
     */
    private void uploadAudioAndCreateNote(Integer projectId, String noteType, Integer noteTypeId,
                                           String noteGroup, String title, String content,
                                           String audioPath, Integer audioDuration, String transcription,
                                           boolean isImportant, String tags, String imagePath) {
        File audioFile = new File(audioPath);

        if (!audioFile.exists() || audioFile.length() == 0) {
            Toast.makeText(this, "Fichier audio invalide", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            btnSave.setText("Enregistrer");
            return;
        }

        Log.d(TAG, "Upload audio: " + audioFile.getName() + " (" + audioFile.length() + " bytes)");

        uploadManager.upload(
            audioFile,
            MediaUploadManager.MediaType.AUDIO,
            MediaUploadManager.Context.NOTES,
            false,
            new MediaUploadManager.MediaUploadCallback() {
                @Override
                public void onSuccess(MediaUploadManager.MediaUploadResult result) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Audio uploadé: " + result.getPath());

                        // Si image aussi, uploader l'image puis créer la note
                        if (imagePath != null && !imagePath.isEmpty()) {
                            uploadImageAndCreateNoteWithAudio(projectId, noteType, noteTypeId, noteGroup,
                                    title, content, result.getPath(), audioDuration, transcription,
                                    isImportant, tags, imagePath);
                        } else {
                            // Créer la note avec l'audio uploadé
                            createNoteWithMedia(projectId, noteType, noteTypeId, noteGroup, title, content,
                                    result.getPath(), null, null, audioDuration, transcription, isImportant, tags);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(NoteEditorActivity.this, "Erreur upload audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Upload audio failed", e);
                        btnSave.setEnabled(true);
                        btnSave.setText("Enregistrer");
                    });
                }

                @Override
                public void onProgress(int percent) {
                    runOnUiThread(() -> {
                        btnSave.setText("Upload audio: " + percent + "%");
                    });
                }
            }
        );
    }

    /**
     * Upload image puis créer la note (avec audio déjà uploadé si applicable)
     */
    private void uploadImageAndCreateNoteWithAudio(Integer projectId, String noteType, Integer noteTypeId,
                                                    String noteGroup, String title, String content,
                                                    String uploadedAudioPath, Integer audioDuration,
                                                    String transcription, boolean isImportant, String tags,
                                                    String imagePath) {
        File imageFile = new File(imagePath);

        uploadManager.upload(
            imageFile,
            MediaUploadManager.MediaType.IMAGE,
            MediaUploadManager.Context.NOTES,
            true,
            new MediaUploadManager.MediaUploadCallback() {
                @Override
                public void onSuccess(MediaUploadManager.MediaUploadResult result) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Image uploadée: " + result.getPath());
                        createNoteWithMedia(projectId, noteType, noteTypeId, noteGroup, title, content,
                                uploadedAudioPath, result.getPath(), null, audioDuration, transcription,
                                isImportant, tags);
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(NoteEditorActivity.this, "Erreur upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Upload image failed", e);
                        btnSave.setEnabled(true);
                        btnSave.setText("Enregistrer");
                    });
                }

                @Override
                public void onProgress(int percent) {
                    runOnUiThread(() -> {
                        btnSave.setText("Upload image: " + percent + "%");
                    });
                }
            }
        );
    }

    /**
     * Upload image et créer la note
     */
    private void uploadImageAndCreateNote(Integer projectId, String noteType, Integer noteTypeId,
                                           String noteGroup, String title, String content,
                                           String imagePath, String transcription, boolean isImportant,
                                           String tags) {
        File imageFile = new File(imagePath);

        uploadManager.upload(
            imageFile,
            MediaUploadManager.MediaType.IMAGE,
            MediaUploadManager.Context.NOTES,
            true,
            new MediaUploadManager.MediaUploadCallback() {
                @Override
                public void onSuccess(MediaUploadManager.MediaUploadResult result) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Image uploadée: " + result.getPath());
                        createNoteWithMedia(projectId, noteType, noteTypeId, noteGroup, title, content,
                                null, result.getPath(), null, null, transcription, isImportant, tags);
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(NoteEditorActivity.this, "Erreur upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Upload image failed", e);
                        btnSave.setEnabled(true);
                        btnSave.setText("Enregistrer");
                    });
                }

                @Override
                public void onProgress(int percent) {
                    runOnUiThread(() -> {
                        btnSave.setText("Upload image: " + percent + "%");
                    });
                }
            }
        );
    }

    /**
     * Upload vidéo et créer la note
     */
    private void uploadVideoAndCreateNote(Integer projectId, String noteType, Integer noteTypeId,
                                           String noteGroup, String title, String content,
                                           String videoPath, String transcription, boolean isImportant,
                                           String tags) {
        File videoFile = new File(videoPath);

        uploadManager.upload(
            videoFile,
            MediaUploadManager.MediaType.VIDEO,
            MediaUploadManager.Context.NOTES,
            true,
            new MediaUploadManager.MediaUploadCallback() {
                @Override
                public void onSuccess(MediaUploadManager.MediaUploadResult result) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Vidéo uploadée: " + result.getPath());
                        createNoteWithMedia(projectId, noteType, noteTypeId, noteGroup, title, content,
                                null, null, result.getPath(), null, transcription, isImportant, tags);
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(NoteEditorActivity.this, "Erreur upload vidéo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Upload vidéo failed", e);
                        btnSave.setEnabled(true);
                        btnSave.setText("Enregistrer");
                    });
                }

                @Override
                public void onProgress(int percent) {
                    runOnUiThread(() -> {
                        btnSave.setText("Upload vidéo: " + percent + "%");
                    });
                }
            }
        );
    }

    /**
     * Créer la note avec les chemins des médias uploadés
     */
    private void createNoteWithMedia(Integer projectId, String noteType, Integer noteTypeId,
                                      String noteGroup, String title, String content,
                                      String audioPath, String imagePath, String videoPath,
                                      Integer audioDuration, String transcription,
                                      boolean isImportant, String tags) {
        // Créer les RequestBody
        RequestBody projectIdBody = projectId != null ?
                RequestBody.create(MediaType.parse("text/plain"), String.valueOf(projectId)) : null;
        RequestBody noteTypeBody = RequestBody.create(MediaType.parse("text/plain"), noteType);
        RequestBody noteTypeIdBody = noteTypeId != null ?
                RequestBody.create(MediaType.parse("text/plain"), String.valueOf(noteTypeId)) : null;
        RequestBody noteGroupBody = RequestBody.create(MediaType.parse("text/plain"), noteGroup != null ? noteGroup : "project");
        RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), title != null ? title : "");
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), content != null ? content : "");
        RequestBody transcriptionBody = transcription != null ?
                RequestBody.create(MediaType.parse("text/plain"), transcription) : null;
        RequestBody isImportantBody = RequestBody.create(MediaType.parse("text/plain"), isImportant ? "1" : "0");
        RequestBody tagsBody = tags != null && !tags.isEmpty() ?
                RequestBody.create(MediaType.parse("text/plain"), tags) : null;

        // Ajouter les chemins des médias uploadés
        RequestBody audioPathBody = audioPath != null ?
                RequestBody.create(MediaType.parse("text/plain"), audioPath) : null;
        RequestBody imagePathBody = imagePath != null ?
                RequestBody.create(MediaType.parse("text/plain"), imagePath) : null;
        RequestBody videoPathBody = videoPath != null ?
                RequestBody.create(MediaType.parse("text/plain"), videoPath) : null;
        RequestBody audioDurationBody = audioDuration != null ?
                RequestBody.create(MediaType.parse("text/plain"), String.valueOf(audioDuration)) : null;

        Call<ApiService.CreateNoteResponse> call;

        if (isEditMode && editNoteId > 0) {
            Log.d(TAG, "Mode édition: mise à jour de la note ID " + editNoteId);
            RequestBody noteIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(editNoteId));

            call = apiService.updateProjectNote(
                    "Bearer " + authToken,
                    noteIdBody,
                    projectIdBody,
                    noteTypeBody,
                    noteTypeIdBody,
                    noteGroupBody,
                    titleBody,
                    contentBody,
                    transcriptionBody,
                    isImportantBody,
                    tagsBody,
                    null, // Les médias sont déjà uploadés, on passe les chemins
                    null
            );
        } else {
            Log.d(TAG, "Mode création: nouvelle note avec médias uploadés");
            call = apiService.createProjectNote(
                    "Bearer " + authToken,
                    projectIdBody,
                    noteTypeBody,
                    noteTypeIdBody,
                    noteGroupBody,
                    titleBody,
                    contentBody,
                    transcriptionBody,
                    isImportantBody,
                    tagsBody,
                    null, // Les médias sont déjà uploadés
                    null
            );
        }

        call.enqueue(new Callback<ApiService.CreateNoteResponse>() {
            @Override
            public void onResponse(Call<ApiService.CreateNoteResponse> call,
                                    Response<ApiService.CreateNoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().success) {
                        String successMessage = isEditMode ?
                                "Note mise à jour avec succès" :
                                "Note créée avec succès";
                        Toast.makeText(NoteEditorActivity.this,
                                successMessage, Toast.LENGTH_SHORT).show();

                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(NoteEditorActivity.this,
                                "Erreur: " + response.body().message, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Save note failed: " + response.body().message);
                        btnSave.setEnabled(true);
                        btnSave.setText("Enregistrer");
                    }
                } else {
                    Toast.makeText(NoteEditorActivity.this,
                            "Erreur: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Save note failed: " + response.code());
                    btnSave.setEnabled(true);
                    btnSave.setText("Enregistrer");
                }
            }

            @Override
            public void onFailure(Call<ApiService.CreateNoteResponse> call, Throwable t) {
                Toast.makeText(NoteEditorActivity.this,
                        "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Save note failed", t);
                btnSave.setEnabled(true);
                btnSave.setText("Enregistrer");
            }
        });
    }

    /**
     * Créer la note sans média
     */
    private void createNoteWithoutMedia(Integer projectId, String noteType, Integer noteTypeId,
                                         String noteGroup, String title, String content,
                                         String transcription, boolean isImportant, String tags) {
        createNoteWithMedia(projectId, noteType, noteTypeId, noteGroup, title, content,
                null, null, null, null, transcription, isImportant, tags);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Photo prise avec caméra
                handleCameraResult();
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // Photo sélectionnée depuis galerie
                Uri imageUri = data.getData();
                handleGalleryResult(imageUri);
            } else if (requestCode == REQUEST_VIDEO_CAPTURE && data != null) {
                // Vidéo enregistrée
                handleVideoCapture(data);
            } else if (requestCode == REQUEST_VIDEO_PICK && data != null) {
                // Vidéo sélectionnée depuis galerie
                handleVideoSelection(data);
            } else if (requestCode == REQUEST_SPEECH_RECOGNITION && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String recognizedText = results.get(0);

                    // Ajouter le texte reconnu au contenu existant
                    String currentText = editDictationContent.getText() != null ?
                            editDictationContent.getText().toString() : "";
                    if (!currentText.isEmpty()) {
                        currentText += " ";
                    }
                    editDictationContent.setText(currentText + recognizedText);

                    tvDictationStatus.setText("Texte reconnu !");
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission accordée", Toast.LENGTH_SHORT).show();
                // L'utilisateur peut réessayer
            } else {
                Toast.makeText(this, "Permission microphone requise", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 100) { // PERMISSION_CAMERA from PhotoManager
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 101 || requestCode == 102) { // PERMISSION_STORAGE or READ_MEDIA_IMAGES
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission stockage refusée", Toast.LENGTH_SHORT).show();
            }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Nettoyer le MediaRecorder
        if (mediaRecorder != null) {
            try {
                if (isRecording) {
                    mediaRecorder.stop();
                }
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing mediaRecorder", e);
            }
            mediaRecorder = null;
        }

        // Nettoyer le MediaPlayer
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing mediaPlayer", e);
            }
            mediaPlayer = null;
        }

        recordingHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Charge une note existante pour l'éditer
     */
    private void loadExistingNote(int noteId) {
        Log.d(TAG, "Chargement de la note ID: " + noteId);

        try {
            // Charger depuis la base de données locale
            com.ptms.mobile.database.OfflineDatabaseHelper dbHelper =
                    new com.ptms.mobile.database.OfflineDatabaseHelper(this);
            com.ptms.mobile.models.ProjectNote note = dbHelper.getProjectNoteById(noteId);

            if (note != null) {
                // Pré-remplir les champs
                if (editTitle != null && note.getTitle() != null) {
                    editTitle.setText(note.getTitle());
                }

                if (editContent != null && note.getContent() != null) {
                    editContent.setText(note.getContent());
                }

                // Pré-sélectionner le projet
                if (spinnerProject != null && note.getProjectId() > 0) {
                    for (Project project : projects) {
                        if (project.getId() == note.getProjectId()) {
                            spinnerProject.setText(project.getName(), false);
                            break;
                        }
                    }
                }

                // Pré-sélectionner le type de note si disponible
                if (spinnerNoteGroup != null && note.getNoteType() != null) {
                    spinnerNoteGroup.setText(note.getNoteType(), false);
                }

                // Important checkbox
                if (checkImportant != null) {
                    checkImportant.setChecked(note.isImportant());
                }

                // Tags
                if (editTags != null && note.getTags() != null && !note.getTags().isEmpty()) {
                    editTags.setText(String.join(", ", note.getTags()));
                }

                Log.d(TAG, "Note chargée: " + note.getTitle());
            } else {
                Log.e(TAG, "Note introuvable: " + noteId);
                Toast.makeText(this, "Note introuvable", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement de la note", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== IMAGE MANAGEMENT ====================

    private void setupImageListeners() {
        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                if (photoManager.hasCameraPermission()) {
                    openCamera();
                } else {
                    photoManager.requestCameraPermission(this);
                }
            });
        }

        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                if (photoManager.hasStoragePermission()) {
                    openGallery();
                } else {
                    photoManager.requestStoragePermission(this);
                }
            });
        }
    }

    private void openCamera() {
        Intent intent = photoManager.createCameraIntent();
        if (intent != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Erreur: impossible d'ouvrir la caméra", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = photoManager.createGalleryIntent();
        if (intent != null) {
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        } else {
            Toast.makeText(this, "Erreur: impossible d'ouvrir la galerie", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCameraResult() {
        File photoFile = photoManager.getCurrentPhotoFile();
        if (photoFile != null && photoFile.exists()) {
            Log.d(TAG, "📸 Photo capturée: " + photoFile.getAbsolutePath());

            // Compresser l'image
            try {
                String compressedPath = getFilesDir().getAbsolutePath() + "/compressed_" + System.currentTimeMillis() + ".jpg";
                boolean success = photoManager.compressImage(photoFile.getAbsolutePath(), compressedPath);

                if (success) {
                    currentImageFile = new File(compressedPath);
                    currentImagePath = compressedPath;
                    displayImagePreview(compressedPath);
                    Toast.makeText(this, "✅ Photo ajoutée", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "❌ Erreur compression image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur compression", e);
                Toast.makeText(this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleGalleryResult(Uri imageUri) {
        Log.d(TAG, "🖼️ Image sélectionnée: " + imageUri.toString());

        try {
            String compressedPath = getFilesDir().getAbsolutePath() + "/gallery_" + System.currentTimeMillis() + ".jpg";
            boolean success = photoManager.compressImageFromUri(imageUri, compressedPath);

            if (success) {
                currentImageFile = new File(compressedPath);
                currentImagePath = compressedPath;
                displayImagePreview(compressedPath);
                Toast.makeText(this, "✅ Image ajoutée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Erreur compression image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur compression", e);
            Toast.makeText(this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayImagePreview(String imagePath) {
        if (imgPreview != null && imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                imgPreview.setImageBitmap(bitmap);
                imgPreview.setVisibility(View.VISIBLE);
                Log.d(TAG, "✅ Aperçu image affiché");
            }
        }
    }

    /**
     * Enregistrer une vidéo
     */
    private void recordVideo() {
        Log.d(TAG, "🎬 Enregistrement vidéo démarré");

        Intent takeVideoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            // Limiter la durée et la qualité pour réduire la taille
            takeVideoIntent.putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 60); // 60 secondes max
            takeVideoIntent.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0); // 0 = basse qualité, 1 = haute

            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        } else {
            Toast.makeText(this, "❌ Aucune application caméra trouvée", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sélectionner une vidéo depuis la galerie
     */
    private void selectVideo() {
        Log.d(TAG, "🎞️ Sélection vidéo démarrée");

        Intent pickVideoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        pickVideoIntent.setType("video/*");

        if (pickVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(pickVideoIntent, REQUEST_VIDEO_PICK);
        } else {
            Toast.makeText(this, "❌ Aucune application galerie trouvée", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Traiter le résultat de la capture vidéo
     */
    private void handleVideoCapture(Intent data) {
        Uri videoUri = data.getData();
        if (videoUri != null) {
            Log.d(TAG, "🎥 Vidéo capturée: " + videoUri.toString());
            processVideoFile(videoUri);
        } else {
            Toast.makeText(this, "❌ Erreur lors de la capture vidéo", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Traiter le résultat de la sélection vidéo
     */
    private void handleVideoSelection(Intent data) {
        Uri videoUri = data.getData();
        if (videoUri != null) {
            Log.d(TAG, "🎬 Vidéo sélectionnée: " + videoUri.toString());
            processVideoFile(videoUri);
        } else {
            Toast.makeText(this, "❌ Erreur lors de la sélection vidéo", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Traiter le fichier vidéo (copie locale + miniature)
     */
    private void processVideoFile(Uri videoUri) {
        try {
            // Copier la vidéo dans le stockage local de l'app
            String videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
            File videoFile = new File(getFilesDir(), videoFileName);

            // Copier le fichier
            java.io.InputStream inputStream = getContentResolver().openInputStream(videoUri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(videoFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            currentVideoFile = videoFile;
            videoFilePath = videoFile.getAbsolutePath();

            // Générer une miniature
            generateVideoThumbnail(videoFilePath);

            // Obtenir la durée de la vidéo
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(videoFilePath);
            String duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();

            int durationSec = Integer.parseInt(duration) / 1000;

            // Afficher le statut
            tvVideoStatus.setText("✅ Vidéo ajoutée (" + formatDuration(durationSec) + ")");
            tvVideoDuration.setText(formatDuration(durationSec));
            tvVideoDuration.setVisibility(View.VISIBLE);

            Toast.makeText(this, "✅ Vidéo ajoutée avec succès", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Erreur traitement vidéo", e);
            Toast.makeText(this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Générer une miniature pour la vidéo
     */
    private void generateVideoThumbnail(String videoPath) {
        try {
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(videoPath);
            Bitmap bitmap = retriever.getFrameAtTime(1000000); // Frame à 1 seconde
            retriever.release();

            if (bitmap != null && imgVideoThumbnail != null) {
                imgVideoThumbnail.setImageBitmap(bitmap);
                imgVideoThumbnail.setVisibility(View.VISIBLE);
                Log.d(TAG, "✅ Miniature vidéo générée");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur génération miniature", e);
        }
    }

}
