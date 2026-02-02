package com.ptms.mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.ptms.mobile.R;

/**
 * Menu principal pour gérer les notes
 */
public class NotesActivity extends AppCompatActivity {

    private MaterialCardView cardAllNotes, cardProjectNotes, cardPersonalNotes, cardGroupNotes, cardImportantNotes, cardDiagnostic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📝 Notes");
        }

        // Initialiser les vues
        cardAllNotes = findViewById(R.id.card_all_notes);
        cardProjectNotes = findViewById(R.id.card_project_notes);
        cardPersonalNotes = findViewById(R.id.card_personal_notes);
        cardGroupNotes = findViewById(R.id.card_group_notes);
        cardImportantNotes = findViewById(R.id.card_important_notes);
        cardDiagnostic = findViewById(R.id.card_diagnostic);

        // Configurer les clics
        setupListeners();
    }

    private void setupListeners() {
        // Toutes les notes
        cardAllNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, NotesListActivity.class);
                startActivity(intent);
            }
        });

        // Notes de projet
        cardProjectNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, ProjectSelectorForNotesActivity.class);
                startActivity(intent);
            }
        });

        // Notes personnelles
        cardPersonalNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ouvrir AllNotesActivity avec filtre "personal"
                Intent intent = new Intent(NotesActivity.this, NotesListActivity.class);
                intent.putExtra("filter", "personal");
                startActivity(intent);
            }
        });

        // Notes de groupe
        cardGroupNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ouvrir AllNotesActivity avec filtre "meeting" (groupe)
                Intent intent = new Intent(NotesActivity.this, NotesListActivity.class);
                intent.putExtra("filter", "meeting");
                startActivity(intent);
            }
        });

        // Notes importantes
        cardImportantNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ouvrir AllNotesActivity avec filtre "important"
                Intent intent = new Intent(NotesActivity.this, NotesListActivity.class);
                intent.putExtra("filter", "important");
                startActivity(intent);
            }
        });

        // Diagnostic
        cardDiagnostic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, NotesDiagnosticsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_calendar) {
            // Ouvrir l'agenda/calendrier des notes
            Intent intent = new Intent(NotesActivity.this, NotesTimelineActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_refresh) {
            // Rafraîchir la liste (redémarrer l'activité)
            recreate();
            return true;
        } else if (id == R.id.action_filter) {
            // Ouvrir la liste de toutes les notes avec recherche
            Intent intent = new Intent(NotesActivity.this, NotesListActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_open_agenda) {
            // Ouvrir l'agenda des notes (doublon de action_calendar)
            Intent intent = new Intent(NotesActivity.this, NotesTimelineActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_diagnostic) {
            // Ouvrir le diagnostic
            Intent intent = new Intent(NotesActivity.this, NotesDiagnosticsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
