package com.ptms.mobile.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.ptms.mobile.R;
import com.ptms.mobile.models.NoteType;
import com.ptms.mobile.utils.SessionManager;
import com.ptms.mobile.utils.SettingsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Activité de gestion des catégories de notes
 */
public class NoteCategoriesActivity extends AppCompatActivity {

    private LinearLayout systemTypesContainer;
    private LinearLayout customTypesContainer;
    private Button btnAddCategory;
    private SessionManager sessionManager;
    private SettingsManager settingsManager;
    private List<NoteType> systemTypes = new ArrayList<>();
    private List<NoteType> customTypes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_categories);

        sessionManager = new SessionManager(this);
        settingsManager = new SettingsManager(this);

        // Initialiser les vues
        systemTypesContainer = findViewById(R.id.systemTypesContainer);
        customTypesContainer = findViewById(R.id.customTypesContainer);
        btnAddCategory = findViewById(R.id.btnAddCategory);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Catégories de notes");
        }

        // Charger les catégories
        loadCategories();

        // Bouton ajouter une catégorie
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    /**
     * Charger les catégories depuis l'API
     */
    private void loadCategories() {
        new Thread(() -> {
            try {
                String baseUrl = settingsManager.getServerUrlRaw();
                URL url = new URL(baseUrl + "/api/note-types.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                // Authentification
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parser la réponse
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.getBoolean("success")) {
                        parseCategories(jsonResponse);
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                    Toast.makeText(this, "Erreur de chargement: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    /**
     * Parser les catégories depuis la réponse JSON
     */
    private void parseCategories(JSONObject jsonResponse) {
        try {
            systemTypes.clear();
            customTypes.clear();

            // Types système
            if (jsonResponse.has("systemTypes")) {
                JSONArray systemArray = jsonResponse.getJSONArray("systemTypes");
                for (int i = 0; i < systemArray.length(); i++) {
                    JSONObject typeObj = systemArray.getJSONObject(i);
                    NoteType type = parseNoteType(typeObj);
                    systemTypes.add(type);
                }
            }

            // Types personnalisés
            if (jsonResponse.has("customTypes")) {
                JSONArray customArray = jsonResponse.getJSONArray("customTypes");
                for (int i = 0; i < customArray.length(); i++) {
                    JSONObject typeObj = customArray.getJSONObject(i);
                    NoteType type = parseNoteType(typeObj);
                    customTypes.add(type);
                }
            }

            // Mettre à jour l'UI
            runOnUiThread(this::updateUI);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parser un objet NoteType depuis JSON
     */
    private NoteType parseNoteType(JSONObject obj) throws Exception {
        NoteType type = new NoteType();
        type.setId(obj.getInt("id"));
        type.setName(obj.getString("name"));
        type.setSlug(obj.getString("slug"));
        type.setIcon(obj.optString("icon", "fa-folder"));
        type.setColor(obj.optString("color", "#6c757d"));
        type.setDescription(obj.optString("description"));
        type.setSystem(obj.getBoolean("isSystem"));
        type.setSortOrder(obj.getInt("sortOrder"));

        if (!obj.isNull("userId")) {
            type.setUserId(obj.getInt("userId"));
        }

        return type;
    }

    /**
     * Mettre à jour l'interface utilisateur
     */
    private void updateUI() {
        // Afficher les types système
        systemTypesContainer.removeAllViews();
        for (NoteType type : systemTypes) {
            View card = createCategoryCard(type, false);
            systemTypesContainer.addView(card);
        }

        // Afficher les types personnalisés
        customTypesContainer.removeAllViews();
        if (customTypes.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("Aucune catégorie personnalisée.\nCréez-en une avec le bouton + ci-dessus!");
            emptyView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyView.setPadding(16, 32, 16, 32);
            customTypesContainer.addView(emptyView);
        } else {
            for (NoteType type : customTypes) {
                View card = createCategoryCard(type, true);
                customTypesContainer.addView(card);
            }
        }
    }

    /**
     * Créer une carte pour afficher une catégorie
     */
    private View createCategoryCard(NoteType type, boolean isDeletable) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_note_category,
            customTypesContainer, false);

        MaterialCardView card = view.findViewById(R.id.categoryCard);
        TextView nameText = view.findViewById(R.id.categoryName);
        TextView slugText = view.findViewById(R.id.categorySlug);
        TextView emojiText = view.findViewById(R.id.categoryEmoji);
        View colorIndicator = view.findViewById(R.id.categoryColorIndicator);
        Button btnDelete = view.findViewById(R.id.btnDeleteCategory);

        // Afficher les données
        nameText.setText(type.getName());
        slugText.setText(type.getSlug());
        emojiText.setText(type.getEmoji());

        // Couleur
        try {
            colorIndicator.setBackgroundColor(type.getColorInt());
        } catch (Exception e) {
            colorIndicator.setBackgroundColor(Color.parseColor("#6c757d"));
        }

        // Bouton supprimer (uniquement pour les types personnalisés)
        if (isDeletable) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> confirmDeleteCategory(type));
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        return view;
    }

    /**
     * Afficher le dialogue pour ajouter une catégorie
     */
    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note_category, null);

        EditText nameInput = dialogView.findViewById(R.id.categoryNameInput);
        EditText slugInput = dialogView.findViewById(R.id.categorySlugInput);
        EditText iconInput = dialogView.findViewById(R.id.categoryIconInput);
        EditText colorInput = dialogView.findViewById(R.id.categoryColorInput);

        // Auto-générer le slug depuis le nom
        nameInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String slug = s.toString().toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
                slugInput.setText(slug);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        new AlertDialog.Builder(this)
            .setTitle("Nouvelle catégorie")
            .setView(dialogView)
            .setPositiveButton("Créer", (dialog, which) -> {
                String name = nameInput.getText().toString().trim();
                String slug = slugInput.getText().toString().trim();
                String icon = iconInput.getText().toString().trim();
                String color = colorInput.getText().toString().trim();

                if (name.isEmpty() || slug.isEmpty()) {
                    Toast.makeText(this, "Nom et slug requis", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (icon.isEmpty()) icon = "fa-folder";
                if (color.isEmpty()) color = "#6c757d";

                createCategory(name, slug, icon, color);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    /**
     * Créer une nouvelle catégorie via l'API
     */
    private void createCategory(String name, String slug, String icon, String color) {
        new Thread(() -> {
            try {
                String baseUrl = settingsManager.getServerUrlRaw();
                URL url = new URL(baseUrl + "/api/note-types.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Authentification
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                // Créer le JSON
                JSONObject data = new JSONObject();
                data.put("name", name);
                data.put("slug", slug);
                data.put("icon", icon);
                data.put("color", color);

                // Envoyer
                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Catégorie créée!", Toast.LENGTH_SHORT).show();
                        loadCategories();
                    });
                } else {
                    runOnUiThread(() ->
                        Toast.makeText(this, "Erreur: " + responseCode, Toast.LENGTH_SHORT).show()
                    );
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    /**
     * Confirmer la suppression d'une catégorie
     */
    private void confirmDeleteCategory(NoteType type) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer la catégorie?")
            .setMessage("Êtes-vous sûr de vouloir supprimer \"" + type.getName() + "\"?")
            .setPositiveButton("Supprimer", (dialog, which) -> deleteCategory(type.getId()))
            .setNegativeButton("Annuler", null)
            .show();
    }

    /**
     * Supprimer une catégorie via l'API
     */
    private void deleteCategory(int typeId) {
        new Thread(() -> {
            try {
                String baseUrl = settingsManager.getServerUrlRaw();
                URL url = new URL(baseUrl + "/api/note-types.php?id=" + typeId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");

                // Authentification
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Catégorie supprimée", Toast.LENGTH_SHORT).show();
                        loadCategories();
                    });
                } else {
                    // Lire le message d'erreur
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    try {
                        JSONObject errorJson = new JSONObject(response.toString());
                        String errorMsg = errorJson.optString("message", "Erreur inconnue");
                        runOnUiThread(() ->
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                        );
                    } catch (Exception e) {
                        runOnUiThread(() ->
                            Toast.makeText(this, "Erreur: " + responseCode, Toast.LENGTH_SHORT).show()
                        );
                    }
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
