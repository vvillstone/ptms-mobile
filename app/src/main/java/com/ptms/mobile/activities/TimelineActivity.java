package com.ptms.mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ptms.mobile.R;
import com.ptms.mobile.adapters.AgendaAdapter;
import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.AgendaItem;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.utils.ApiManager;
import com.ptms.mobile.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Agenda unifié - rapports + notes par date
 */
public class TimelineActivity extends AppCompatActivity {

    private static final String TAG = "AGENDA";

    private CalendarView calendarView;
    private ListView listView;
    private TextView tvEmpty;
    private TextView tvSelectedTitle;
    private ProgressBar progressBar;
    private AgendaAdapter adapter;
    private List<TimeReport> allReports = new ArrayList<>();
    private List<ProjectNote> allNotes = new ArrayList<>();
    private List<AgendaItem> dayItems = new ArrayList<>();
    private ApiService apiService;
    private SessionManager sessionManager;
    private String authToken;
    // ✅ FIX: Use Locale.US for ISO dates (prevents crashes)
    private final SimpleDateFormat apiDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_timeline);

            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("📅 Agenda");
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }

            calendarView = findViewById(R.id.calendar_view);
            listView = findViewById(R.id.list_view);
            tvEmpty = findViewById(R.id.tv_empty);
            tvSelectedTitle = findViewById(R.id.tv_selected_title);
            progressBar = findViewById(R.id.progress_bar);

            if (calendarView == null || listView == null || tvEmpty == null || progressBar == null || tvSelectedTitle == null) {
                Toast.makeText(this, "Erreur interface agenda", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            sessionManager = new SessionManager(this);

            adapter = new AgendaAdapter(this, dayItems);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AgendaItem item = dayItems.get(position);
                    showItemDetailsDialog(item);
                }
            });

            try {
                apiService = ApiClient.getInstance(this).getApiService();
            } catch (Exception e) {
                android.util.Log.e("AGENDA", "Erreur API client", e);
                Toast.makeText(this, "Erreur API", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            authToken = getSharedPreferences("ptms_prefs", MODE_PRIVATE).getString("auth_token", null);
            if (authToken == null || authToken.isEmpty()) {
                Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Charger la plage du mois courant par défaut
            Calendar cal = Calendar.getInstance();
            Date end = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date start = cal.getTime();

            loadAllData(start, end, new Runnable() {
                @Override
                public void run() {
                    Date selected = new Date(calendarView.getDate());
                    filterForSelectedDay(selected);
                    updateSelectedTitle(selected);
                }
            });

            calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                @Override
                public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth, 0, 0, 0);
                    Date d = c.getTime();
                    filterForSelectedDay(d);
                    updateSelectedTitle(d);
                }
            });

        } catch (Exception e) {
            android.util.Log.e("AGENDA", "Erreur onCreate", e);
            Toast.makeText(this, "Erreur initialisation: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Charger rapports ET notes
     */
    private void loadAllData(Date from, Date to, Runnable onDone) {
        setLoading(true);
        // Charger en parallèle: rapports + notes
        loadReportsRange(from, to, () -> {
            loadNotes(() -> {
                setLoading(false);
                if (onDone != null) onDone.run();
            });
        });
    }

    private void loadReportsRange(Date from, Date to, Runnable onDone) {
        if (authToken == null) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String df = apiDate.format(from);
        String dt = apiDate.format(to);
        Call<List<TimeReport>> call = apiService.getReports(authToken, df, dt, null);
        call.enqueue(new Callback<List<TimeReport>>() {
            @Override
            public void onResponse(Call<List<TimeReport>> call, Response<List<TimeReport>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        allReports = response.body();
                        Log.d(TAG, "Rapports chargés: " + allReports.size());
                        if (onDone != null) onDone.run();
                    } else {
                        Toast.makeText(TimelineActivity.this, "Erreur chargement rapports: " + response.code(), Toast.LENGTH_SHORT).show();
                        if (onDone != null) onDone.run();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur réponse rapports", e);
                    if (onDone != null) onDone.run();
                }
            }

            @Override
            public void onFailure(Call<List<TimeReport>> call, Throwable t) {
                Toast.makeText(TimelineActivity.this, "Erreur réseau rapports: " + t.getMessage(), Toast.LENGTH_LONG).show();
                if (onDone != null) onDone.run();
            }
        });
    }

    /**
     * Charger toutes les notes de l'utilisateur
     */
    private void loadNotes(Runnable onDone) {
        String url = ApiManager.getBaseUrl() + "/api/project-notes.php?all=1";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            parseNotes(response);
                            Log.d(TAG, "Notes chargées: " + allNotes.size());
                        } else {
                            Log.w(TAG, "Erreur chargement notes: " + response.optString("message"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing notes", e);
                    }
                    if (onDone != null) onDone.run();
                },
                error -> {
                    Log.e(TAG, "Error loading notes: " + (error != null ? error.getMessage() : "unknown"));
                    if (onDone != null) onDone.run();
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
     * Parser les notes depuis JSON
     */
    private void parseNotes(JSONObject jsonResponse) {
        try {
            allNotes.clear();
            JSONArray notesArray = jsonResponse.getJSONArray("notes");

            for (int i = 0; i < notesArray.length(); i++) {
                JSONObject noteObj = notesArray.getJSONObject(i);
                ProjectNote note = new ProjectNote();

                note.setId(noteObj.getInt("id"));
                note.setProjectId(noteObj.optInt("projectId", 0));
                note.setProjectName(noteObj.optString("projectName", null));
                note.setUserId(noteObj.getInt("userId"));
                note.setNoteType(noteObj.getString("noteType"));
                note.setNoteGroup(noteObj.optString("noteGroup", "project"));
                note.setTitle(noteObj.optString("title", null));
                note.setContent(noteObj.optString("content", null));
                note.setAudioPath(noteObj.optString("audioPath", null));
                note.setAudioDuration(noteObj.optInt("audioDuration", 0));
                note.setTranscription(noteObj.optString("transcription", null));
                note.setImportant(noteObj.getBoolean("isImportant"));
                note.setAuthorName(noteObj.getString("authorName"));
                note.setCreatedAt(noteObj.getString("createdAt"));
                note.setUpdatedAt(noteObj.optString("updatedAt", null));

                allNotes.add(note);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notes", e);
        }
    }

    private void filterForSelectedDay(Date day) {
        try {
            String dayStr = apiDate.format(day);
            dayItems.clear();

            // Ajouter rapports du jour
            for (TimeReport r : allReports) {
                if (dayStr.equals(r.getReportDate())) {
                    dayItems.add(AgendaItem.fromReport(r));
                }
            }

            // Ajouter notes du jour
            for (ProjectNote note : allNotes) {
                String noteDate = extractDate(note.getCreatedAt());
                if (dayStr.equals(noteDate)) {
                    dayItems.add(AgendaItem.fromNote(note));
                }
            }

            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(dayItems.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText(dayItems.isEmpty() ? "Aucune activité ce jour" : "");

            Log.d(TAG, "Jour " + dayStr + ": " + dayItems.size() + " items (" +
                    countReports(dayItems) + " rapports, " + countNotes(dayItems) + " notes)");
        } catch (Exception e) {
            Log.e(TAG, "Erreur filtrage jour", e);
        }
    }

    private String extractDate(String timestamp) {
        if (timestamp == null || timestamp.length() < 10) {
            return "";
        }
        return timestamp.substring(0, 10);
    }

    private int countReports(List<AgendaItem> items) {
        int count = 0;
        for (AgendaItem item : items) {
            if (item.getType() == AgendaItem.Type.REPORT) count++;
        }
        return count;
    }

    private int countNotes(List<AgendaItem> items) {
        int count = 0;
        for (AgendaItem item : items) {
            if (item.getType() == AgendaItem.Type.NOTE) count++;
        }
        return count;
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSelectedTitle(Date day) {
        if (tvSelectedTitle == null) return;
        try {
            String ds = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(day);
            double total = 0.0;
            String iso = apiDate.format(day);

            int notesCount = 0;
            for (AgendaItem item : dayItems) {
                if (item.getType() == AgendaItem.Type.REPORT) {
                    total += item.getReport().getHours();
                } else {
                    notesCount++;
                }
            }

            String text = ds;
            if (total > 0) {
                text += " • " + String.format(Locale.FRANCE, "%.2fh", total);
            }
            if (notesCount > 0) {
                text += " • " + notesCount + " note" + (notesCount > 1 ? "s" : "");
            }

            tvSelectedTitle.setText(text);
        } catch (Exception ignored) {}
    }

    /**
     * Afficher détails d'un item (rapport ou note)
     */
    private void showItemDetailsDialog(AgendaItem item) {
        if (item.getType() == AgendaItem.Type.REPORT) {
            showReportDetails(item.getReport());
        } else {
            showNoteDetails(item.getNote());
        }
    }

    private void showReportDetails(TimeReport r) {
        try {
            StringBuilder sb = new StringBuilder();
            String project = r.getProjectName() != null ? r.getProjectName() : (r.getProjectId() == 0 ? "(Sans projet)" : ("#" + r.getProjectId()));
            String workType = r.getWorkTypeName() != null ? r.getWorkTypeName() : ("#" + r.getWorkTypeId());
            sb.append("Date: ").append(r.getReportDate()).append("\n");
            sb.append("Heures: ").append(r.getDatetimeFrom()).append(" - ").append(r.getDatetimeTo()).append(" (" ).append(String.format(Locale.FRANCE, "%.2f", r.getHours())).append("h)\n");
            sb.append("Projet: ").append(project).append("\n");
            sb.append("Type: ").append(workType).append("\n\n");
            if (r.getDescription() != null && !r.getDescription().isEmpty()) {
                sb.append(r.getDescription());
            } else {
                sb.append("(Aucune description)");
            }

            new AlertDialog.Builder(this)
                    .setTitle("⏱️ Rapport de temps")
                    .setMessage(sb.toString())
                    .setPositiveButton("Modifier", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(TimelineActivity.this, TimeEntryActivity.class);
                            intent.putExtra("prefill_project_id", r.getProjectId());
                            intent.putExtra("prefill_work_type_id", r.getWorkTypeId());
                            intent.putExtra("prefill_description", r.getDescription());
                            intent.putExtra("prefill_date", r.getReportDate());
                            intent.putExtra("prefill_time_from", r.getDatetimeFrom());
                            intent.putExtra("prefill_time_to", r.getDatetimeTo());
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur ouverture OfflineTimeEntryActivity", e);
                            Toast.makeText(this, "Erreur ouverture modification", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("Fermer", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Erreur affichage détails rapport", e);
            Toast.makeText(this, "Erreur affichage détails", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNoteDetails(ProjectNote note) {
        try {
            StringBuilder sb = new StringBuilder();

            if (note.getProjectName() != null && !note.getProjectName().isEmpty()) {
                sb.append("📊 Projet: ").append(note.getProjectName()).append("\n\n");
            } else {
                sb.append("👤 Note personnelle\n\n");
            }

            if ("text".equals(note.getNoteType()) && note.getContent() != null) {
                sb.append(note.getContent());
            } else if ("dictation".equals(note.getNoteType()) && note.getTranscription() != null) {
                sb.append("🗣️ Transcription:\n\n").append(note.getTranscription());
            } else if ("audio".equals(note.getNoteType())) {
                sb.append("🎵 Note audio");
                if (note.getAudioDuration() != null && note.getAudioDuration() > 0) {
                    sb.append("\nDurée: ").append(note.getFormattedDuration());
                }
            }

            sb.append("\n\n---\n");
            sb.append("👤 ").append(note.getAuthorName()).append("\n");
            sb.append("📅 ").append(note.getCreatedAt());

            if (note.isImportant()) {
                sb.append("\n⭐ Important");
            }

            String title = note.getTitle();
            if (title == null || title.isEmpty()) {
                title = "Note " + note.getNoteType();
            }

            new AlertDialog.Builder(this)
                    .setTitle(note.getGroupIcon() + " " + title)
                    .setMessage(sb.toString())
                    .setPositiveButton("Fermer", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Erreur affichage détails note", e);
            Toast.makeText(this, "Erreur affichage détails", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_agenda, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_note) {
            // Ouvrir NotesMenuActivity pour ajouter une note
            Intent intent = new Intent(this, NotesActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_refresh) {
            // Recharger les données
            Calendar cal = Calendar.getInstance();
            Date end = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date start = cal.getTime();

            loadAllData(start, end, () -> {
                Date selected = new Date(calendarView.getDate());
                filterForSelectedDay(selected);
                updateSelectedTitle(selected);
                Toast.makeText(this, "✅ Données rafraîchies", Toast.LENGTH_SHORT).show();
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}


