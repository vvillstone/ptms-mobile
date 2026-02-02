# Mise à jour Android - Système de Notes avec Catégories
**Date**: 16 Janvier 2025

## 📱 Résumé des modifications

### Objectif
Mettre à jour l'application Android PTMS pour supporter:
- Notes personnelles (sans projet)
- Nouveau système de catégories personnalisables
- Affichage des catégories avec couleurs et icônes
- Gestion des catégories depuis l'app mobile

---

## ✅ Fichiers modifiés

### 1. **Modèle ProjectNote.java**
**Chemin**: `app/src/main/java/com/ptms/mobile/models/ProjectNote.java`

**Changements**:
- `projectId` devient nullable (`Integer` au lieu de `int`)
- Ajout de champs pour les catégories:
  - `noteTypeId` (Integer)
  - `noteTypeName` (String)
  - `noteTypeSlug` (String)
  - `noteTypeIcon` (String)
  - `noteTypeColor` (String)

**Nouvelles méthodes**:
- `getCategoryEmoji()` - Retourne l'emoji de la catégorie
- `getCategoryColor()` - Retourne la couleur Android Color
- `isPersonalNote()` - Vérifie si c'est une note sans projet
- `getGroupIcon()` - Marqué @Deprecated (legacy)

---

## 🆕 Fichiers créés

### 1. **Modèle NoteType.java**
**Chemin**: `app/src/main/java/com/ptms/mobile/models/NoteType.java`

**Description**: Modèle pour représenter une catégorie de note

**Propriétés**:
```java
private int id;
private Integer userId;
private String name;
private String slug;
private String icon;
private String color;
private String description;
private boolean isSystem;
private int sortOrder;
```

**Méthodes utiles**:
- `getEmoji()` - Retourne un emoji selon le slug
- `getColorInt()` - Parse la couleur pour Android
- `isCustom()` - Vérifie si c'est un type personnalisé
- `toString()` - Retourne le nom pour les spinners

---

### 2. **Activité NoteCategoriesActivity.java**
**Chemin**: `app/src/main/java/com/ptms/mobile/activities/NoteCategoriesActivity.java`

**Description**: Interface de gestion des catégories de notes

**Fonctionnalités**:
- Affiche les catégories système (10 prédéfinies)
- Affiche les catégories personnalisées de l'utilisateur
- Permet de créer une nouvelle catégorie
- Permet de supprimer les catégories personnalisées
- Auto-génération du slug depuis le nom
- Sélection de couleur hexadécimale

**API utilisées**:
- `GET /api/note-types.php` - Charger les catégories
- `POST /api/note-types.php` - Créer une catégorie
- `DELETE /api/note-types.php?id={id}` - Supprimer une catégorie

---

## 📋 Fichiers de layout à créer

### 1. **activity_note_categories.xml**
**Chemin**: `app/src/main/res/layout/activity_note_categories.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Bouton ajouter -->
    <Button
        android:id="@+id/btnAddCategory"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="➕ Nouvelle catégorie"
        android:backgroundTint="@color/colorPrimary"
        android:textColor="@android:color/white"
        android:layout_marginBottom="16dp"/>

    <!-- ScrollView pour les catégories -->
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <!-- Section catégories système -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Catégories système"
                android:textSize="18sp"
                android:textStyle="bold"
                android:layout_marginBottom="8dp"/>

            <LinearLayout
                android:id="@+id/systemTypesContainer"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"/>

            <!-- Séparateur -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="#DDDDDD"
                android:layout_marginVertical="16dp"/>

            <!-- Section catégories personnalisées -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Mes catégories personnalisées"
                android:textSize="18sp"
                android:textStyle="bold"
                android:layout_marginBottom="8dp"/>

            <LinearLayout
                android:id="@+id/customTypesContainer"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"/>

        </LinearLayout>
    </ScrollView>

</LinearLayout>
```

---

### 2. **item_note_category.xml**
**Chemin**: `app/src/main/res/layout/item_note_category.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/categoryCard"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="8dp"
    app:cardElevation="2dp"
    app:cardCornerRadius="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp"
        android:gravity="center_vertical">

        <!-- Indicateur de couleur -->
        <View
            android:id="@+id/categoryColorIndicator"
            android:layout_width="8dp"
            android:layout_height="match_parent"
            android:layout_marginEnd="12dp"/>

        <!-- Emoji -->
        <TextView
            android:id="@+id/categoryEmoji"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="📁"
            android:textSize="24sp"
            android:layout_marginEnd="12dp"/>

        <!-- Informations -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/categoryName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Nom"
                android:textSize="16sp"
                android:textStyle="bold"/>

            <TextView
                android:id="@+id/categorySlug"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="slug"
                android:textSize="12sp"
                android:textColor="#888888"/>

        </LinearLayout>

        <!-- Bouton supprimer -->
        <Button
            android:id="@+id/btnDeleteCategory"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="🗑️"
            android:backgroundTint="#dc3545"
            android:textColor="@android:color/white"
            android:visibility="gone"/>

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

---

### 3. **dialog_add_note_category.xml**
**Chemin**: `app/src/main/res/layout/dialog_add_note_category.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Nom -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Nom de la catégorie"
        android:layout_marginBottom="8dp">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/categoryNameInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"/>

    </com.google.android.material.textfield.TextInputLayout>

    <!-- Slug -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Slug (auto-généré)"
        android:layout_marginBottom="8dp">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/categorySlugInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"
            android:enabled="false"/>

    </com.google.android.material.textfield.TextInputLayout>

    <!-- Icône -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Icône FontAwesome (ex: fa-fire)"
        android:layout_marginBottom="8dp">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/categoryIconInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"
            android:text="fa-folder"/>

    </com.google.android.material.textfield.TextInputLayout>

    <!-- Couleur -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Couleur hexadécimale (ex: #ff0000)"
        android:layout_marginBottom="8dp">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/categoryColorInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"
            android:text="#6c757d"/>

    </com.google.android.material.textfield.TextInputLayout>

    <!-- Exemple de couleur -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Aperçu: "
            android:textSize="14sp"/>

        <View
            android:id="@+id/colorPreview"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="#6c757d"/>

    </LinearLayout>

</LinearLayout>
```

---

## 🔄 Modifications à apporter aux activités existantes

### 1. **AddProjectNoteActivity.java**

**À modifier**:
```java
// Ajouter un spinner pour sélectionner la catégorie
private Spinner categorySpinner;
private List<NoteType> noteTypes = new ArrayList<>();

// Dans onCreate()
categorySpinner = findViewById(R.id.categorySpinner);
loadNoteTypes();

// Nouvelle méthode pour charger les types
private void loadNoteTypes() {
    new Thread(() -> {
        try {
            String baseUrl = sessionManager.getServerUrl();
            URL url = new URL(baseUrl + "/api/note-types.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // ... connexion et parsing JSON ...

            runOnUiThread(() -> {
                ArrayAdapter<NoteType> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, noteTypes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}

// Lors de l'envoi de la note, inclure note_type_id
private void saveNote() {
    // ...
    JSONObject noteData = new JSONObject();
    noteData.put("project_id", projectId); // Peut être null
    noteData.put("note_type_id", selectedCategory.getId());
    noteData.put("note_type", noteType);
    // ...
}
```

---

### 2. **ProjectNotesAdapter.java**

**À modifier**:
```java
// Dans onBindViewHolder()
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    ProjectNote note = notes.get(position);

    // Afficher l'emoji de catégorie
    holder.categoryEmoji.setText(note.getCategoryEmoji());

    // Afficher la couleur de catégorie
    if (note.getNoteTypeColor() != null) {
        holder.categoryIndicator.setBackgroundColor(note.getCategoryColor());
    }

    // Afficher le nom de la catégorie
    if (note.getNoteTypeName() != null) {
        holder.categoryName.setText(note.getNoteTypeName());
        holder.categoryName.setVisibility(View.VISIBLE);
    } else {
        holder.categoryName.setVisibility(View.GONE);
    }

    // Badge "Note personnelle" si sans projet
    if (note.isPersonalNote()) {
        holder.personalBadge.setVisibility(View.VISIBLE);
    } else {
        holder.personalBadge.setVisibility(View.GONE);
    }

    // ...
}
```

---

### 3. **NotesMenuActivity.java**

**À ajouter**:
```java
// Ajouter un bouton pour gérer les catégories
Button btnManageCategories = findViewById(R.id.btnManageCategories);
btnManageCategories.setOnClickListener(v -> {
    Intent intent = new Intent(this, NoteCategoriesActivity.class);
    startActivity(intent);
});

// Ajouter un bouton pour les notes personnelles
Button btnPersonalNotes = findViewById(R.id.btnPersonalNotes);
btnPersonalNotes.setOnClickListener(v -> {
    Intent intent = new Intent(this, AllNotesActivity.class);
    intent.putExtra("personal_only", true);
    startActivity(intent);
});
```

---

## 📱 Modifications UI à apporter

### Layout item_project_note.xml
**À ajouter**:
```xml
<!-- Indicateur de couleur de catégorie -->
<View
    android:id="@+id/categoryColorIndicator"
    android:layout_width="4dp"
    android:layout_height="match_parent"/>

<!-- Emoji de catégorie -->
<TextView
    android:id="@+id/categoryEmoji"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textSize="20sp"/>

<!-- Nom de catégorie -->
<TextView
    android:id="@+id/categoryName"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textSize="12sp"
    android:textColor="#888888"/>

<!-- Badge note personnelle -->
<TextView
    android:id="@+id/personalBadge"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="👤 Personnel"
    android:background="#e0e0e0"
    android:padding="4dp"
    android:textSize="10sp"
    android:visibility="gone"/>
```

---

## 🔗 Ajout dans AndroidManifest.xml

```xml
<activity
    android:name=".activities.NoteCategoriesActivity"
    android:label="Catégories de notes"
    android:parentActivityName=".activities.NotesMenuActivity"
    android:theme="@style/AppTheme"/>
```

---

## 🧪 Tests à effectuer

### Tests fonctionnels
- [ ] Charger les catégories depuis l'API
- [ ] Afficher les 10 catégories système
- [ ] Créer une catégorie personnalisée
- [ ] Supprimer une catégorie personnalisée
- [ ] Créer une note avec catégorie
- [ ] Créer une note personnelle (sans projet)
- [ ] Afficher les notes avec leurs catégories
- [ ] Voir la couleur de la catégorie sur chaque note
- [ ] Filtrer par catégorie (si implémenté)

### Tests d'intégration
- [ ] Synchronisation notes web ↔ Android
- [ ] Mode hors ligne avec catégories
- [ ] Upload audio avec catégorie
- [ ] Affichage cohérent web/mobile

---

## 📦 Dépendances Gradle

**Vérifier** que `app/build.gradle` contient:
```gradle
dependencies {
    implementation 'com.google.android.material:material:1.9.0'
    // ... autres dépendances
}
```

---

## 🚀 Déploiement

### Étapes

1. **Synchroniser Synology Drive**
   - Attendre que tous les fichiers soient synchronisés

2. **Compiler l'app Android**
   ```bash
   cd appAndroid
   gradlew.bat build
   ```

3. **Installer sur appareil**
   ```bash
   gradlew.bat installDebug
   ```

4. **Tester**
   - Ouvrir l'app
   - Aller dans "Notes" → "Gérer les catégories"
   - Créer une catégorie personnalisée
   - Créer une note avec cette catégorie
   - Vérifier l'affichage

---

## 📝 Résumé des fonctionnalités

### ✅ Ajouté
- Modèle `NoteType` pour les catégories
- Activité de gestion des catégories
- Support des notes personnelles (sans projet)
- Affichage des couleurs de catégories
- Émojis de catégories
- Auto-génération des slugs

### 🔄 Modifié
- `ProjectNote` avec support des catégories
- Champs pour les métadonnées de catégorie
- Méthodes d'affichage mises à jour

### 📋 À faire (optionnel)
- Filtrage par catégorie dans la liste
- Tri par catégorie
- Statistiques par catégorie
- Sélecteur de couleur visuel dans le dialogue
- Aperçu en temps réel de la couleur
- Upload d'icône personnalisée

---

**Version**: 2.0.0
**Date**: 16 Janvier 2025
**Compatibilité**: Android 7.0+ (API 24+)
**Statut**: Prêt pour déploiement après compilation
