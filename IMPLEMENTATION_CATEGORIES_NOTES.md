# Guide d'implémentation : Catégories personnalisées & Loading pour Notes

## Date: 2025-10-14
## Version: PTMS v2.0 Android

---

## 📋 Vue d'ensemble

Ce document décrit l'implémentation de **3 fonctionnalités** manquantes pour le système de notes :

1. **✅ Synchronisation audio post-upload** (Déjà implémenté dans `OfflineSyncManager`)
2. **🔜 Catégories personnalisées** (`note_type_id`)
3. **🔜 Loading indicator** lors de l'upload audio

---

## ✅ 1. Synchronisation audio (DÉJÀ FAIT)

### Localisation
`appAndroid/app/src/main/java/com/ptms/mobile/sync/OfflineSyncManager.java` (lignes 375-486)

### Fonctionnement actuel
```java
private void syncPendingProjectNotes(int userId, SyncCallback callback) {
    // Récupère notes en attente
    List<ProjectNote> pendingNotes = dbHelper.getPendingProjectNotesByUserId(userId);

    for (ProjectNote note : pendingNotes) {
        // Si note audio ET fichier local existe
        if (("audio".equals(note.getNoteType()) || "dictation".equals(note.getNoteType()))
            && note.getLocalAudioPath() != null) {
            File audioFile = new File(note.getLocalAudioPath());

            if (audioFile.exists()) {
                // Préparer fichier pour upload multipart
                RequestBody audioBody = RequestBody.create(
                    MediaType.parse("audio/mpeg"),
                    audioFile
                );

                MultipartBody.Part audioPart = MultipartBody.Part.createFormData(
                    "audio_file",
                    audioFile.getName(),
                    audioBody
                );

                // Upload via API
                Call<ApiService.CreateNoteResponse> call = apiService.createProjectNote(
                    token, projectId, noteType, title, content, isImportant, audioPart
                );
            }
        }
    }
}
```

**Status** : ✅ **Fonctionnel** - L'upload audio est automatique au retour réseau.

---

## 🔜 2. Catégories personnalisées (note_type_id)

### Backend PHP (✅ Déjà prêt)

**API** : `api/note-types.php`
- `GET /api/note-types.php` - Liste des catégories (système + personnelles)
- `POST /api/note-types.php` - Créer catégorie personnelle
- `PUT /api/note-types.php` - Modifier catégorie
- `DELETE /api/note-types.php?id=X` - Supprimer catégorie

**Table MySQL** : `note_types`
```sql
CREATE TABLE note_types (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,          -- NULL = système, INT = personnel
    name VARCHAR(50),
    slug VARCHAR(50),
    icon VARCHAR(50),          -- FontAwesome class
    color VARCHAR(20),         -- Hex color
    description TEXT,
    is_system TINYINT(1),
    sort_order INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**10 types système prédéfinis** :
1. Projet (`project`) - 📊 Bleu
2. Personnel (`personal`) - 👤 Gris
3. Réunion (`meeting`) - 👥 Cyan
4. Tâche (`todo`) - ✅ Vert
5. Idée (`idea`) - 💡 Jaune
6. Problème (`issue`) - ⚠️ Rouge
7. Urgent (`urgent`) - 🔥 Rouge vif
8. Client (`client`) - 🤝 Turquoise
9. Documentation (`documentation`) - 📚 Violet
10. Autre (`other`) - 📁 Gris

---

### Android - À implémenter

#### Étape 1 : Ajouter table note_types à la BDD locale

**Fichier** : `OfflineDatabaseHelper.java`

**Ajouter** :
```java
private static final String TABLE_NOTE_TYPES = "note_types";
private static final int DATABASE_VERSION = 5; // Incrémenter version

private static final String CREATE_TABLE_NOTE_TYPES =
    "CREATE TABLE " + TABLE_NOTE_TYPES + "(" +
    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
    COLUMN_SERVER_ID + " INTEGER," +
    "user_id INTEGER," +
    "name TEXT NOT NULL," +
    "slug TEXT NOT NULL," +
    "icon TEXT," +
    "color TEXT DEFAULT '#6c757d'," +
    "description TEXT," +
    "is_system INTEGER DEFAULT 0," +
    "sort_order INTEGER DEFAULT 0," +
    COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
    COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
    COLUMN_SYNCED + " INTEGER DEFAULT 1" +
    ")";

@Override
public void onCreate(SQLiteDatabase db) {
    // ... tables existantes
    db.execSQL(CREATE_TABLE_NOTE_TYPES);
}

@Override
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // ... migrations existantes

    if (oldVersion < 5) {
        // Version 5: Ajout table note_types
        db.execSQL(CREATE_TABLE_NOTE_TYPES);
        Log.d(TAG, "Table note_types créée");
    }
}
```

#### Étape 2 : Méthodes CRUD pour note_types

**Ajouter dans `OfflineDatabaseHelper.java`** :
```java
// ==================== GESTION DES TYPES DE NOTES ====================

/**
 * Insère un type de note en cache local
 */
public long insertNoteType(NoteType noteType) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();

    values.put(COLUMN_SERVER_ID, noteType.getId());

    if (noteType.getUserId() != null) {
        values.put("user_id", noteType.getUserId());
    } else {
        values.putNull("user_id");
    }

    values.put("name", noteType.getName());
    values.put("slug", noteType.getSlug());
    values.put("icon", noteType.getIcon());
    values.put("color", noteType.getColor());
    values.put("description", noteType.getDescription());
    values.put("is_system", noteType.isSystem() ? 1 : 0);
    values.put("sort_order", noteType.getSortOrder());
    values.put(COLUMN_SYNCED, 1); // Toujours synchronisé (vient du serveur)

    long id = db.insert(TABLE_NOTE_TYPES, null, values);
    db.close();

    Log.d(TAG, "Type de note inséré: " + noteType.getName());
    return id;
}

/**
 * Récupère tous les types de notes (système + personnels)
 */
public List<NoteType> getAllNoteTypes() {
    List<NoteType> types = new ArrayList<>();
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        "SELECT * FROM " + TABLE_NOTE_TYPES +
        " ORDER BY is_system DESC, sort_order ASC, name ASC",
        null
    );

    if (cursor.moveToFirst()) {
        do {
            NoteType type = extractNoteTypeFromCursor(cursor);
            types.add(type);
        } while (cursor.moveToNext());
    }

    cursor.close();
    db.close();

    Log.d(TAG, "Types de notes récupérés: " + types.size());
    return types;
}

/**
 * Récupère les types système seulement
 */
public List<NoteType> getSystemNoteTypes() {
    List<NoteType> types = new ArrayList<>();
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        "SELECT * FROM " + TABLE_NOTE_TYPES +
        " WHERE is_system = 1" +
        " ORDER BY sort_order ASC",
        null
    );

    if (cursor.moveToFirst()) {
        do {
            NoteType type = extractNoteTypeFromCursor(cursor);
            types.add(type);
        } while (cursor.moveToNext());
    }

    cursor.close();
    db.close();

    return types;
}

/**
 * Récupère les types personnalisés d'un utilisateur
 */
public List<NoteType> getCustomNoteTypesByUserId(int userId) {
    List<NoteType> types = new ArrayList<>();
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        "SELECT * FROM " + TABLE_NOTE_TYPES +
        " WHERE is_system = 0 AND user_id = ?" +
        " ORDER BY sort_order ASC, name ASC",
        new String[]{String.valueOf(userId)}
    );

    if (cursor.moveToFirst()) {
        do {
            NoteType type = extractNoteTypeFromCursor(cursor);
            types.add(type);
        } while (cursor.moveToNext());
    }

    cursor.close();
    db.close();

    return types;
}

/**
 * Récupère un type par ID serveur
 */
public NoteType getNoteTypeByServerId(int serverId) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        "SELECT * FROM " + TABLE_NOTE_TYPES + " WHERE server_id = ?",
        new String[]{String.valueOf(serverId)}
    );

    NoteType type = null;
    if (cursor.moveToFirst()) {
        type = extractNoteTypeFromCursor(cursor);
    }

    cursor.close();
    db.close();

    return type;
}

/**
 * Extrait un NoteType depuis un Cursor
 */
private NoteType extractNoteTypeFromCursor(Cursor cursor) {
    NoteType type = new NoteType();

    type.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SERVER_ID)));

    int userIdIndex = cursor.getColumnIndexOrThrow("user_id");
    if (!cursor.isNull(userIdIndex)) {
        type.setUserId(cursor.getInt(userIdIndex));
    }

    type.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
    type.setSlug(cursor.getString(cursor.getColumnIndexOrThrow("slug")));
    type.setIcon(cursor.getString(cursor.getColumnIndexOrThrow("icon")));
    type.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
    type.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
    type.setSystem(cursor.getInt(cursor.getColumnIndexOrThrow("is_system")) == 1);
    type.setSortOrder(cursor.getInt(cursor.getColumnIndexOrThrow("sort_order")));
    type.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
    type.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));

    return type;
}

/**
 * Vide le cache des types de notes
 */
public void clearNoteTypes() {
    SQLiteDatabase db = this.getWritableDatabase();
    db.delete(TABLE_NOTE_TYPES, null, null);
    db.close();
    Log.d(TAG, "Cache des types de notes vidé");
}
```

#### Étape 3 : Synchronisation des note_types

**Ajouter dans `OfflineSyncManager.java`** (après ligne 215) :
```java
/**
 * Synchronise les types de notes depuis le serveur
 */
private void syncNoteTypes() {
    try {
        String token = getAuthToken();
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "Token manquant pour sync note_types");
            return;
        }

        Call<ApiService.NoteTypesResponse> call = apiService.getNoteTypes(token);
        call.enqueue(new Callback<ApiService.NoteTypesResponse>() {
            @Override
            public void onResponse(Call<ApiService.NoteTypesResponse> call,
                                 Response<ApiService.NoteTypesResponse> response) {
                if (response.isSuccessful() && response.body() != null
                    && response.body().success) {
                    // Vider cache et recharger
                    dbHelper.clearNoteTypes();

                    for (NoteType noteType : response.body().types) {
                        dbHelper.insertNoteType(noteType);
                    }

                    Log.d(TAG, "Types de notes synchronisés: " + response.body().types.size());
                }
            }

            @Override
            public void onFailure(Call<ApiService.NoteTypesResponse> call, Throwable t) {
                Log.e(TAG, "Échec sync note_types", t);
            }
        });

    } catch (Exception e) {
        Log.e(TAG, "Erreur sync note_types", e);
    }
}
```

**Appeler dans `syncReferenceData()`** (ligne 163) :
```java
private void syncReferenceData() {
    try {
        // ... sync projets et work types existant

        // ✅ AJOUTER : Sync note types
        syncNoteTypes();

    } catch (Exception e) {
        Log.e(TAG, "Erreur synchronisation données de référence", e);
    }
}
```

#### Étape 4 : Ajouter endpoints dans ApiService

**Fichier** : `ApiService.java`

**Ajouter** :
```java
// ==================== NOTE TYPES (CATÉGORIES) ====================

/**
 * Récupère les types de notes (système + personnels)
 */
@GET("note-types.php")
Call<NoteTypesResponse> getNoteTypes(@Header("Authorization") String token);

/**
 * Crée un type de note personnalisé
 */
@POST("note-types.php")
Call<CreateNoteTypeResponse> createNoteType(
    @Header("Authorization") String token,
    @Body NoteType noteType
);

/**
 * Modifie un type de note personnalisé
 */
@PUT("note-types.php")
Call<ApiResponse> updateNoteType(
    @Header("Authorization") String token,
    @Body NoteType noteType
);

/**
 * Supprime un type de note personnalisé
 */
@DELETE("note-types.php")
Call<ApiResponse> deleteNoteType(
    @Header("Authorization") String token,
    @Query("id") int typeId
);

// ==================== RESPONSE CLASSES ====================

class NoteTypesResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("types")
    public List<NoteType> types;

    @SerializedName("systemTypes")
    public List<NoteType> systemTypes;

    @SerializedName("customTypes")
    public List<NoteType> customTypes;

    @SerializedName("count")
    public int count;

    @SerializedName("message")
    public String message;
}

class CreateNoteTypeResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("type")
    public NoteType type;
}
```

#### Étape 5 : Activity pour gérer les catégories

**Créer** : `NoteCategoriesActivity.java`

```java
package com.ptms.mobile.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptms.mobile.R;
import com.ptms.mobile.adapters.NoteTypesAdapter;
import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.models.NoteType;

import java.util.ArrayList;
import java.util.List;

public class NoteCategoriesActivity extends AppCompatActivity {

    private RecyclerView rvSystemTypes, rvCustomTypes;
    private NoteTypesAdapter systemAdapter, customAdapter;
    private FloatingActionButton fabAddCategory;
    private OfflineDatabaseHelper dbHelper;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_categories);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Catégories de notes");
        }

        // Init
        dbHelper = new OfflineDatabaseHelper(this);
        userId = getSharedPreferences("ptms_prefs", MODE_PRIVATE)
                    .getInt("user_id", 0);

        initViews();
        loadCategories();
    }

    private void initViews() {
        rvSystemTypes = findViewById(R.id.rv_system_types);
        rvCustomTypes = findViewById(R.id.rv_custom_types);
        fabAddCategory = findViewById(R.id.fab_add_category);

        // RecyclerViews
        rvSystemTypes.setLayoutManager(new LinearLayoutManager(this));
        rvCustomTypes.setLayoutManager(new LinearLayoutManager(this));

        systemAdapter = new NoteTypesAdapter(new ArrayList<>(), false);
        customAdapter = new NoteTypesAdapter(new ArrayList<>(), true);

        rvSystemTypes.setAdapter(systemAdapter);
        rvCustomTypes.setAdapter(customAdapter);

        // FAB pour créer catégorie
        fabAddCategory.setOnClickListener(v -> showCreateCategoryDialog());
    }

    private void loadCategories() {
        // Charger types système
        List<NoteType> systemTypes = dbHelper.getSystemNoteTypes();
        systemAdapter.updateData(systemTypes);

        // Charger types personnalisés
        List<NoteType> customTypes = dbHelper.getCustomNoteTypesByUserId(userId);
        customAdapter.updateData(customTypes);

        // Afficher/masquer sections
        findViewById(R.id.section_custom).setVisibility(
            customTypes.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    private void showCreateCategoryDialog() {
        // TODO: Ouvrir DialogFragment pour créer catégorie
        Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
```

**Layout** : `activity_note_categories.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

        <com.google.android.material.appbar.AppBarLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content">

            <androidx.appcompat.widget.Toolbar
                android:id="@+id/toolbar"
                android:layout_width="match_parent"
                android:layout_height="?attr/actionBarSize"
                android:background="?attr/colorPrimary"
                android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar" />
        </com.google.android.material.appbar.AppBarLayout>

        <ScrollView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:padding="16dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical">

                <!-- Types système -->
                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="CATÉGORIES SYSTÈME"
                    android:textSize="12sp"
                    android:textStyle="bold"
                    android:textColor="#757575"
                    android:layout_marginBottom="8dp" />

                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/rv_system_types"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:nestedScrollingEnabled="false"
                    android:layout_marginBottom="24dp" />

                <!-- Types personnalisés -->
                <LinearLayout
                    android:id="@+id/section_custom"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:visibility="gone">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="MES CATÉGORIES PERSONNALISÉES"
                        android:textSize="12sp"
                        android:textStyle="bold"
                        android:textColor="#757575"
                        android:layout_marginBottom="8dp" />

                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/rv_custom_types"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:nestedScrollingEnabled="false" />
                </LinearLayout>
            </LinearLayout>
        </ScrollView>
    </LinearLayout>

    <!-- FAB pour ajouter catégorie -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab_add_category"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:src="@android:drawable/ic_input_add"
        app:fabSize="normal" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

---

## 🔜 3. Loading Indicator pour upload audio

### Problème
Lors de l'enregistrement d'une note audio offline, l'utilisateur ne voit pas la progression de l'upload quand le réseau revient.

### Solution proposée

#### Étape 1 : Ajouter ProgressDialog ou BottomSheet

**Dans `OfflineSyncManager.java`**, modifier `syncPendingProjectNotes` pour notifier via callback :

```java
public interface SyncCallback {
    void onSyncStarted();
    void onSyncProgress(String message);
    void onSyncCompleted(int syncedCount, int failedCount);
    void onSyncError(String error);

    // ✅ NOUVEAU: Upload audio en cours
    void onAudioUploadStarted(String fileName);
    void onAudioUploadProgress(String fileName, int progress);
    void onAudioUploadCompleted(String fileName);
}
```

**Ensuite dans `DashboardActivity.java`**, écouter ces callbacks :

```java
offlineModeManager.addListener(new OfflineModeManager.ModeChangeListener() {
    @Override
    public void onAudioUploadStarted(String fileName) {
        runOnUiThread(() -> {
            // Afficher ProgressDialog ou BottomSheet
            showUploadProgress(fileName);
        });
    }

    @Override
    public void onAudioUploadProgress(String fileName, int progress) {
        runOnUiThread(() -> {
            // Mettre à jour la barre de progression
            updateUploadProgress(fileName, progress);
        });
    }

    @Override
    public void onAudioUploadCompleted(String fileName) {
        runOnUiThread(() -> {
            // Masquer le loading
            hideUploadProgress();
            Toast.makeText(this, "✅ Audio uploadé : " + fileName, Toast.LENGTH_SHORT).show();
        });
    }
});
```

#### Étape 2 : ProgressDialog simple

```java
private ProgressDialog uploadDialog;

private void showUploadProgress(String fileName) {
    uploadDialog = new ProgressDialog(this);
    uploadDialog.setTitle("Upload audio");
    uploadDialog.setMessage("Envoi de " + fileName + "...");
    uploadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    uploadDialog.setMax(100);
    uploadDialog.setProgress(0);
    uploadDialog.setCancelable(false);
    uploadDialog.show();
}

private void updateUploadProgress(String fileName, int progress) {
    if (uploadDialog != null && uploadDialog.isShowing()) {
        uploadDialog.setProgress(progress);
    }
}

private void hideUploadProgress() {
    if (uploadDialog != null && uploadDialog.isShowing()) {
        uploadDialog.dismiss();
        uploadDialog = null;
    }
}
```

#### Étape 3 : Alternative BottomSheet moderne

**Layout** : `bottom_sheet_upload.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp"
    android:background="@android:color/white">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="🎤 Upload audio en cours..."
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp" />

    <TextView
        android:id="@+id/tv_file_name"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="note_audio_20251014.mp3"
        android:textSize="14sp"
        android:textColor="#757575"
        android:layout_marginBottom="8dp" />

    <ProgressBar
        android:id="@+id/progress_bar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:max="100"
        android:progress="0" />

    <TextView
        android:id="@+id/tv_progress"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="0%"
        android:textSize="12sp"
        android:textColor="#757575"
        android:layout_gravity="end"
        android:layout_marginTop="4dp" />
</LinearLayout>
```

**Java** :
```java
private BottomSheetDialog uploadBottomSheet;

private void showUploadProgress(String fileName) {
    View view = getLayoutInflater().inflate(R.layout.bottom_sheet_upload, null);
    uploadBottomSheet = new BottomSheetDialog(this);
    uploadBottomSheet.setContentView(view);
    uploadBottomSheet.setCancelable(false);

    TextView tvFileName = view.findViewById(R.id.tv_file_name);
    tvFileName.setText(fileName);

    uploadBottomSheet.show();
}

private void updateUploadProgress(String fileName, int progress) {
    if (uploadBottomSheet != null && uploadBottomSheet.isShowing()) {
        View view = uploadBottomSheet.findViewById(R.id.progress_bar);
        if (view != null) {
            ProgressBar progressBar = (ProgressBar) view;
            progressBar.setProgress(progress);

            TextView tvProgress = uploadBottomSheet.findViewById(R.id.tv_progress);
            if (tvProgress != null) {
                tvProgress.setText(progress + "%");
            }
        }
    }
}

private void hideUploadProgress() {
    if (uploadBottomSheet != null && uploadBottomSheet.isShowing()) {
        uploadBottomSheet.dismiss();
        uploadBottomSheet = null;
    }
}
```

---

## 📊 Récapitulatif des modifications

### Fichiers à modifier

| Fichier | Action | Priorité |
|---------|--------|----------|
| `OfflineDatabaseHelper.java` | Ajouter table + méthodes note_types | ⭐⭐⭐ Haute |
| `OfflineSyncManager.java` | Ajouter sync note_types | ⭐⭐⭐ Haute |
| `ApiService.java` | Ajouter endpoints note-types | ⭐⭐⭐ Haute |
| `NoteCategoriesActivity.java` | Créer (nouveau) | ⭐⭐ Moyenne |
| `NoteTypesAdapter.java` | Créer (nouveau) | ⭐⭐ Moyenne |
| `DashboardActivity.java` | Ajouter loading indicator | ⭐ Basse |

### Fichiers à créer

| Fichier | Description |
|---------|-------------|
| `NoteCategoriesActivity.java` | Gestion catégories |
| `activity_note_categories.xml` | Layout catégories |
| `NoteTypesAdapter.java` | Adapter RecyclerView |
| `item_note_type.xml` | Item catégorie |
| `bottom_sheet_upload.xml` | Loading upload audio |
| `CreateCategoryDialogFragment.java` | Dialog création catégorie |

---

## 🧪 Tests à effectuer

### Test 1: Synchronisation note_types
1. Lancer app avec réseau
2. Vérifier que les 10 types système sont chargés
3. Créer un type personnalisé via web
4. Relancer app → Vérifier qu'il apparaît

### Test 2: Création catégorie personnalisée
1. Ouvrir `NoteCategoriesActivity`
2. Cliquer sur FAB (+)
3. Remplir formulaire (nom, couleur, icône)
4. Sauvegarder
5. Vérifier apparition dans la liste

### Test 3: Utilisation catégorie dans note
1. Créer nouvelle note
2. Sélectionner catégorie personnalisée
3. Sauvegarder
4. Vérifier `note_type_id` != null en BDD

### Test 4: Loading upload audio
1. Mode offline, enregistrer note audio
2. Réactiver réseau
3. Vérifier que ProgressDialog/BottomSheet apparaît
4. Vérifier progression jusqu'à 100%
5. Vérifier toast "✅ Audio uploadé"

---

## 🎯 Prochaines étapes

1. ✅ **Synchronisation audio** : Déjà implémenté
2. 🔜 **Implémenter table note_types en BDD Android** (15 min)
3. 🔜 **Ajouter sync note_types** (10 min)
4. 🔜 **Créer NoteCategoriesActivity** (30 min)
5. 🔜 **Ajouter loading indicator** (20 min)
6. 🔜 **Tests complets** (30 min)

**Estimation totale** : ~2 heures de développement

---

**Auteur** : Claude Code
**Date** : 2025-10-14
**Version** : PTMS v2.0 Android
