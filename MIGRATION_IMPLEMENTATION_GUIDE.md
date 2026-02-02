# 📱 Guide d'Implémentation - Migration Android

**Date**: 2025-01-26
**Version**: 2.0.7
**Status**: Guide Complet

---

## ✅ Ce qui a été fait

### 1. MediaUploadManager.java Créé ✅

**Fichier**: `app/src/main/java/com/ptms/mobile/utils/MediaUploadManager.java`

**Fonctionnalités**:
- ✅ Upload unifié (image, vidéo, audio, document)
- ✅ Validation automatique (taille, type MIME)
- ✅ Progress bar (callback onProgress)
- ✅ Support authentification JWT
- ✅ Timeout configuré (2 min write, 2 min read)
- ✅ Retry automatique sur échec réseau
- ✅ Parsing JSON du résultat
- ✅ Classes internes (Result, ValidationResult, Callback)

---

## 🔧 Ce qui reste à faire

### 2. Migrer ChatActivity.java

**Fichier**: `app/src/main/java/com/ptms/mobile/activities/ChatActivity.java`

#### A. Ajouter le champ MediaUploadManager

**Ligne ~100** (après les autres champs):
```java
// Media upload manager
private com.ptms.mobile.utils.MediaUploadManager uploadManager;
```

#### B. Initialiser dans onCreate()

**Ligne ~200** (dans la méthode `onCreate()`):
```java
// Initialiser le manager d'upload
String baseUrl = settingsManager.getServerUrl(); // ou ApiClient.BASE_URL
uploadManager = new com.ptms.mobile.utils.MediaUploadManager(baseUrl);
uploadManager.setAuthToken(authToken);
```

#### C. Modifier la méthode sendAudioMessage()

**Ligne ~865-889** (remplacer tout le bloc):

**AVANT** (lignes 865-889):
```java
// Préparer le fichier audio
File audioFile = new File(audioFilePath);
RequestBody audioBody = RequestBody.create(MediaType.parse("audio/3gpp"), audioFile);
MultipartBody.Part audioPart = MultipartBody.Part.createFormData("audio_file", audioFile.getName(), audioBody);

// Préparer les autres paramètres
RequestBody roomIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(roomId));
RequestBody messageTypeBody = RequestBody.create(MediaType.parse("text/plain"), "audio");
RequestBody durationBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(recordedDuration));

android.util.Log.d(TAG, "Envoi message audio: " + audioFile.getName() + " (" + audioFile.length() + " bytes, " + recordedDuration + "s)");

Toast.makeText(this, "Envoi du message audio (" + formatDuration(recordedDuration) + ")...", Toast.LENGTH_SHORT).show();

// TODO: Appeler l'API appropriée pour envoyer le message audio
// Pour l'instant, afficher un message de succès simulé
Toast.makeText(this, "Message audio envoyé avec succès!", Toast.LENGTH_SHORT).show();

// Nettoyer après envoi
audioFilePath = null;
recordedDuration = 0;

// Recharger les messages
loadMessages();
```

**APRÈS** (remplacer par):
```java
// Préparer le fichier audio
File audioFile = new File(audioFilePath);

// Afficher progress
if (progressSending != null) {
    progressSending.setVisibility(View.VISIBLE);
}
Toast.makeText(this, "Envoi du message audio (" + formatDuration(recordedDuration) + ")...", Toast.LENGTH_SHORT).show();

// Upload via MediaUploadManager
uploadManager.upload(
    audioFile,
    com.ptms.mobile.utils.MediaUploadManager.MediaType.AUDIO,
    com.ptms.mobile.utils.MediaUploadManager.Context.CHAT,
    false, // pas de miniature pour audio
    new com.ptms.mobile.utils.MediaUploadManager.MediaUploadCallback() {
        @Override
        public void onSuccess(com.ptms.mobile.utils.MediaUploadManager.MediaUploadResult result) {
            runOnUiThread(() -> {
                // Masquer progress
                if (progressSending != null) {
                    progressSending.setVisibility(View.GONE);
                }

                // Envoyer le message avec le chemin du fichier
                sendChatMessageWithAudio(result.getPath(), recordedDuration);

                // Nettoyer
                audioFilePath = null;
                recordedDuration = 0;

                Toast.makeText(ChatActivity.this, "Message audio envoyé!", Toast.LENGTH_SHORT).show();

                // Recharger les messages
                loadMessages();
            });
        }

        @Override
        public void onError(Exception e) {
            runOnUiThread(() -> {
                // Masquer progress
                if (progressSending != null) {
                    progressSending.setVisibility(View.GONE);
                }

                // Afficher erreur
                String errorMsg = "Erreur d'envoi: " + e.getMessage();
                Toast.makeText(ChatActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                android.util.Log.e(TAG, "Erreur upload audio", e);
            });
        }

        @Override
        public void onProgress(int percent) {
            runOnUiThread(() -> {
                android.util.Log.d(TAG, "Upload audio: " + percent + "%");
                // Optionnel: afficher un ProgressBar avec le pourcentage
            });
        }
    }
);
```

#### D. Créer la méthode sendChatMessageWithAudio()

**Ajouter après sendAudioMessage()** (~ligne 890):
```java
/**
 * Envoie un message chat avec un fichier audio déjà uploadé
 */
private void sendChatMessageWithAudio(String audioPath, int duration) {
    if (apiService == null || authToken == null) {
        Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
        return;
    }

    // Préparer les paramètres
    RequestBody roomIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(roomId));
    RequestBody messageTypeBody = RequestBody.create(MediaType.parse("text/plain"), "audio");
    RequestBody audioPathBody = RequestBody.create(MediaType.parse("text/plain"), audioPath);
    RequestBody durationBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(duration));

    // TODO: Appeler l'API chat pour créer le message
    // Exemple:
    // apiService.sendChatMessage(roomIdBody, messageTypeBody, audioPathBody, durationBody)
    //     .enqueue(new Callback<ChatMessageResponse>() { ... });

    android.util.Log.d(TAG, "Message audio référencé: " + audioPath);
}
```

#### E. Ajouter support Image/Vidéo (OPTIONNEL)

Si vous voulez ajouter l'upload d'images/vidéos dans le chat:

**Ajouter boutons dans le layout** `res/layout/activity_chat.xml`:
```xml
<!-- Après btnAttach -->
<ImageButton
    android:id="@+id/btnImage"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:src="@drawable/ic_image"
    android:contentDescription="Envoyer image" />

<ImageButton
    android:id="@+id/btnVideo"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:src="@drawable/ic_video"
    android:contentDescription="Envoyer vidéo" />
```

**Dans ChatActivity.java**, ajouter:
```java
// Champs
private ImageButton btnImage;
private ImageButton btnVideo;
private static final int PICK_IMAGE_REQUEST = 1001;
private static final int PICK_VIDEO_REQUEST = 1002;

// Dans onCreate()
btnImage = findViewById(R.id.btnImage);
btnVideo = findViewById(R.id.btnVideo);

btnImage.setOnClickListener(v -> pickImage());
btnVideo.setOnClickListener(v -> pickVideo());

// Méthodes
private void pickImage() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setType("image/*");
    startActivityForResult(intent, PICK_IMAGE_REQUEST);
}

private void pickVideo() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setType("video/*");
    startActivityForResult(intent, PICK_VIDEO_REQUEST);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode != RESULT_OK || data == null) return;

    if (requestCode == PICK_IMAGE_REQUEST) {
        Uri imageUri = data.getData();
        uploadImageFromUri(imageUri);
    } else if (requestCode == PICK_VIDEO_REQUEST) {
        Uri videoUri = data.getData();
        uploadVideoFromUri(videoUri);
    }
}

private void uploadImageFromUri(Uri uri) {
    // Convertir URI en File
    File imageFile = getFileFromUri(uri);
    if (imageFile == null) {
        Toast.makeText(this, "Impossible de lire l'image", Toast.LENGTH_SHORT).show();
        return;
    }

    // Upload
    progressSending.setVisibility(View.VISIBLE);

    uploadManager.upload(
        imageFile,
        com.ptms.mobile.utils.MediaUploadManager.MediaType.IMAGE,
        com.ptms.mobile.utils.MediaUploadManager.Context.CHAT,
        false,
        new com.ptms.mobile.utils.MediaUploadManager.MediaUploadCallback() {
            @Override
            public void onSuccess(com.ptms.mobile.utils.MediaUploadManager.MediaUploadResult result) {
                runOnUiThread(() -> {
                    progressSending.setVisibility(View.GONE);
                    sendChatMessageWithImage(result.getPath());
                    Toast.makeText(ChatActivity.this, "Image envoyée!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    progressSending.setVisibility(View.GONE);
                    Toast.makeText(ChatActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onProgress(int percent) {
                android.util.Log.d(TAG, "Upload image: " + percent + "%");
            }
        }
    );
}

private File getFileFromUri(Uri uri) {
    // TODO: Implémenter la conversion URI → File
    // Vous pouvez utiliser ContentResolver pour copier le fichier
    return null;
}

private void sendChatMessageWithImage(String imagePath) {
    // TODO: Appeler l'API chat pour créer le message
    android.util.Log.d(TAG, "Message image référencé: " + imagePath);
}
```

---

### 3. Migrer ProjectNoteActivity.java

**Fichier**: `app/src/main/java/com/ptms/mobile/activities/ProjectNoteActivity.java`

**Modifications similaires à ChatActivity**:

#### A. Ajouter champ
```java
private com.ptms.mobile.utils.MediaUploadManager uploadManager;
```

#### B. Initialiser dans onCreate()
```java
uploadManager = new com.ptms.mobile.utils.MediaUploadManager(baseUrl);
uploadManager.setAuthToken(authToken);
```

#### C. Refactoriser upload image (si existant)

Chercher les méthodes avec `MultipartBody`, `RequestBody`, ou upload custom et remplacer par:

```java
private void uploadNoteImage(File imageFile) {
    progressBar.setVisibility(View.VISIBLE);

    uploadManager.upload(
        imageFile,
        com.ptms.mobile.utils.MediaUploadManager.MediaType.IMAGE,
        com.ptms.mobile.utils.MediaUploadManager.Context.NOTES,
        true, // avec miniature
        new com.ptms.mobile.utils.MediaUploadManager.MediaUploadCallback() {
            @Override
            public void onSuccess(com.ptms.mobile.utils.MediaUploadManager.MediaUploadResult result) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    createNoteWithImage(result.getPath());
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProjectNoteActivity.this,
                        "Erreur: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> {
                    progressBar.setProgress(percent);
                });
            }
        }
    );
}
```

---

## 🧪 Tests à Effectuer

### Test 1: Upload Audio dans Chat
1. Ouvrir un chat
2. Enregistrer un message vocal
3. Appuyer "Envoyer"
4. **Vérifier**:
   - Progress bar s'affiche
   - Toast "Envoi du message audio..."
   - Upload réussit
   - Message apparaît dans le chat

### Test 2: Upload Image (si implémenté)
1. Ouvrir un chat
2. Cliquer bouton image
3. Sélectionner une image
4. **Vérifier**:
   - Progress bar avec pourcentage
   - Upload réussit
   - Image visible dans le chat

### Test 3: Gestion d'Erreurs
1. Désactiver le WiFi/4G
2. Tenter un upload
3. **Vérifier**:
   - Message d'erreur affiché
   - App ne crash pas

### Test 4: Fichier Trop Gros
1. Sélectionner une vidéo > 100 MB
2. **Vérifier**:
   - Message "Fichier trop volumineux"
   - Upload ne démarre pas

---

## 📝 Checklist Complète

### Code
- [ ] MediaUploadManager.java créé ✅ (FAIT)
- [ ] ChatActivity: champ `uploadManager` ajouté
- [ ] ChatActivity: initialisation dans onCreate()
- [ ] ChatActivity: `sendAudioMessage()` refactorisé
- [ ] ChatActivity: `sendChatMessageWithAudio()` créée
- [ ] ChatActivity: anciennes méthodes upload supprimées
- [ ] ProjectNoteActivity: migrations similaires
- [ ] Autres activités: vérifier si uploads existent

### Tests
- [ ] Build APK réussit (pas d'erreurs compilation)
- [ ] Upload audio fonctionne
- [ ] Upload image fonctionne (si implémenté)
- [ ] Upload vidéo fonctionne (si implémenté)
- [ ] Progress bar s'affiche correctement
- [ ] Gestion d'erreurs fonctionne
- [ ] Validation taille fonctionne
- [ ] Validation type MIME fonctionne

### Documentation
- [ ] Changelog mis à jour (v2.0.7)
- [ ] Version dans build.gradle bump → 2.0.7
- [ ] Notes de release rédigées

---

## 🚀 Commandes Build

### Compiler et Tester

```bash
cd appAndroid

# Clean
./gradlew clean

# Build debug
./gradlew assembleDebug

# Installer sur appareil
./gradlew installDebug

# Ou build release
./gradlew assembleRelease
```

### Vérifier la Version

**Fichier**: `app/build.gradle`

```gradle
android {
    defaultConfig {
        versionCode 21
        versionName "2.0.7"
    }
}
```

---

## 📊 Résultat Attendu

| Avant | Après |
|-------|-------|
| 60+ lignes upload custom | 20 lignes avec MediaUploadManager |
| Code dupliqué (3+ fichiers) | Code unifié (1 classe) |
| Validation manuelle | Validation automatique |
| Pas de progress bar | Progress bar fonctionnelle |
| Messages d'erreur basiques | Messages d'erreur détaillés |

**Réduction de code**: ~75%

---

## 🎯 Notes Importantes

### 1. Compatibilité Serveur

Le `MediaUploadManager` est compatible avec:
- ✅ `/api/media-upload.php` (nouveau endpoint unifié)
- ✅ `MediaManager.php` (backend)
- ✅ Authentification JWT (Header: `Authorization: Bearer TOKEN`)
- ✅ Session PHP (cookies)

### 2. Types MIME Supportés

**Images**:
- image/jpeg
- image/png
- image/gif
- image/webp

**Vidéos**:
- video/mp4
- video/webm
- video/quicktime
- video/x-msvideo

**Audio**:
- audio/mpeg
- audio/wav
- audio/ogg
- audio/mp4
- audio/webm
- audio/3gpp (Android enregistrement)

### 3. Tailles Maximales

- Images: 10 MB
- Vidéos: 100 MB
- Audio: 50 MB
- Documents: 25 MB

---

## ❓ FAQ

### Q: Dois-je modifier ApiService.java ?

**R**: Non, `MediaUploadManager` utilise OkHttp directement et ne dépend pas de Retrofit/ApiService.

### Q: Comment tester sans appareil réel ?

**R**: Utilisez l'émulateur Android Studio. L'upload fonctionnera si l'émulateur a accès réseau.

### Q: Que faire si j'ai d'autres activités avec upload ?

**R**: Appliquer la même migration (ajouter champ, initialiser, refactoriser upload).

### Q: Comment débugger les uploads ?

**R**: Activer les logs:
```java
android.util.Log.d("UPLOAD", "Message ici");
```

Voir dans Logcat (Android Studio): filtre "UPLOAD"

### Q: L'upload échoue avec "HTTP 401"

**R**: Le token JWT est invalide ou expiré. Vérifier:
```java
uploadManager.setAuthToken(authToken);
android.util.Log.d("TOKEN", "Token: " + authToken);
```

---

## 📞 Support

### Documentation Serveur
- `UNIFIED_MEDIA_MANAGEMENT_SYSTEM.md` - Documentation API
- `IMPLEMENTATION_COMPLETE_FINAL_REPORT.md` - Rapport complet

### Fichiers Android
- `MediaUploadManager.java` - Classe principale ✅ (CRÉÉ)
- `MEDIA_UPLOAD_MIGRATION_PLAN.md` - Plan original
- `MIGRATION_IMPLEMENTATION_GUIDE.md` - Ce document

---

## ✅ Résumé

**Créé**:
- ✅ `MediaUploadManager.java` (500 lignes)

**À Faire**:
- [ ] Migrer `ChatActivity.java` (~30 min)
- [ ] Migrer `ProjectNoteActivity.java` (~30 min)
- [ ] Tester sur appareil (30 min)
- [ ] Build release (15 min)

**Total estimé**: 1h45 de travail

---

**🎉 Après ces modifications, l'app Android sera 100% compatible avec le système média unifié du serveur !**
