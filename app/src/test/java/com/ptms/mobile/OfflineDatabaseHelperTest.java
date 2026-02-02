package com.ptms.mobile;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.TimeReport;
import com.ptms.mobile.models.ProjectNote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests unitaires pour OfflineDatabaseHelper
 *
 * Vérifie:
 * - Création et structure de la base de données
 * - Opérations CRUD sur les projets
 * - Opérations CRUD sur les rapports de temps
 * - Opérations CRUD sur les notes de projet
 * - Synchronisation des données
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class OfflineDatabaseHelperTest {

    private OfflineDatabaseHelper dbHelper;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbHelper = new OfflineDatabaseHelper(context);

        // Nettoyer la base avant chaque test
        clearDatabase();
    }

    @After
    public void tearDown() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private void clearDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM projects");
        db.execSQL("DELETE FROM time_reports");
        db.execSQL("DELETE FROM work_types");
        db.execSQL("DELETE FROM project_notes");
    }

    /**
     * Test 1: Vérifier que la base de données est créée correctement
     */
    @Test
    public void testDatabaseCreation() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        assertNotNull("La base de données devrait être créée", db);
        assertTrue("La base de données devrait être ouverte", db.isOpen());
    }

    /**
     * Test 2: Vérifier que toutes les tables existent
     */
    @Test
    public void testTablesExist() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        assertTrue("La table 'projects' devrait exister", tableExists(db, "projects"));
        assertTrue("La table 'time_reports' devrait exister", tableExists(db, "time_reports"));
        assertTrue("La table 'work_types' devrait exister", tableExists(db, "work_types"));
        assertTrue("La table 'project_notes' devrait exister", tableExists(db, "project_notes"));
    }

    private boolean tableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            new String[]{tableName}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Test 3: Insérer et récupérer un projet
     */
    @Test
    public void testInsertProject() {
        // Given: Un projet
        Project project = new Project();
        project.setId(1);
        project.setName("Projet Test");
        project.setDescription("Description du projet test");
        project.setStatus(1); // 1 = active

        // When: Insérer le projet
        long result = dbHelper.insertProject(project);

        // Then: Le projet devrait être inséré
        assertTrue("L'insertion devrait réussir", result > 0);
    }

    /**
     * Test 4: Récupérer tous les projets
     */
    @Test
    public void testGetAllProjects() {
        // Given: Plusieurs projets
        Project p1 = createProject(1, "Projet 1", 1); // 1 = active
        Project p2 = createProject(2, "Projet 2", 2); // 2 = completed

        dbHelper.insertProject(p1);
        dbHelper.insertProject(p2);

        // When: Récupérer tous les projets
        List<Project> projects = dbHelper.getAllProjects();

        // Then: Tous les projets devraient être récupérés
        assertNotNull("La liste ne devrait pas être nulle", projects);
        assertEquals("Il devrait y avoir 2 projets", 2, projects.size());
    }

    /**
     * Test 5: Compter les projets
     */
    @Test
    public void testGetProjectCount() {
        // Given: 3 projets
        dbHelper.insertProject(createProject(1, "Projet 1", 1)); // 1 = active
        dbHelper.insertProject(createProject(2, "Projet 2", 1)); // 1 = active
        dbHelper.insertProject(createProject(3, "Projet 3", 2)); // 2 = completed

        // When: Compter les projets
        int count = dbHelper.getProjectCount();

        // Then: Le compte devrait être correct
        assertEquals("Il devrait y avoir 3 projets", 3, count);
    }

    /**
     * Test 6: Insérer un rapport de temps
     */
    @Test
    public void testInsertTimeReport() {
        // Given: Un rapport de temps
        TimeReport report = new TimeReport();
        report.setProjectId(100);
        report.setWorkTypeId(200);
        report.setDatetimeFrom("2025-10-22 08:00:00");
        report.setDatetimeTo("2025-10-22 16:00:00");
        report.setHours(7.5);
        report.setDescription("Travail effectué");
        report.setSynced(false);

        // When: Insérer le rapport
        long result = dbHelper.insertTimeReport(report);

        // Then: L'insertion devrait réussir
        assertTrue("L'insertion devrait réussir", result > 0);
    }

    /**
     * Test 7: Récupérer les rapports non synchronisés
     */
    @Test
    public void testGetAllPendingTimeReports() {
        // Given: Rapports non synchronisés
        dbHelper.insertTimeReport(createTimeReport(100, 7.5, false));
        dbHelper.insertTimeReport(createTimeReport(101, 8.0, false));

        // When: Récupérer les rapports non synchronisés
        List<TimeReport> pendingReports = dbHelper.getAllPendingTimeReports();

        // Then: Les rapports non synchronisés devraient être retournés
        assertNotNull("La liste ne devrait pas être nulle", pendingReports);
        assertEquals("Il devrait y avoir 2 rapports non synchronisés", 2, pendingReports.size());
    }

    /**
     * Test 8: Compter les rapports en attente de sync
     */
    @Test
    public void testGetPendingSyncCount() {
        // Given: 3 rapports non synchronisés
        dbHelper.insertTimeReport(createTimeReport(100, 7.5, false));
        dbHelper.insertTimeReport(createTimeReport(101, 8.0, false));
        dbHelper.insertTimeReport(createTimeReport(102, 6.5, false));

        // When: Compter les rapports en attente
        int count = dbHelper.getPendingSyncCount();

        // Then: Le compte devrait être correct
        assertEquals("Il devrait y avoir 3 rapports en attente", 3, count);
    }

    /**
     * Test 9: Insérer une note de projet
     */
    @Test
    public void testInsertProjectNote() {
        // Given: Une note de projet
        ProjectNote note = new ProjectNote();
        note.setProjectId(100);
        note.setTitle("Note de test");
        note.setContent("Contenu de la note");
        note.setNoteType("text");
        note.setImportant(true);

        // When: Insérer la note
        long result = dbHelper.insertProjectNote(note);

        // Then: L'insertion devrait réussir
        assertTrue("L'insertion devrait réussir", result > 0);
    }

    /**
     * Test 10: Récupérer une note par ID
     */
    @Test
    public void testGetProjectNoteById() {
        // Given: Une note insérée
        ProjectNote note = new ProjectNote();
        note.setProjectId(100);
        note.setTitle("Note de test");
        note.setContent("Contenu");
        note.setNoteType("text");

        long id = dbHelper.insertProjectNote(note);

        // When: Récupérer la note
        ProjectNote retrieved = dbHelper.getProjectNoteById((int)id);

        // Then: La note devrait être récupérée
        assertNotNull("La note devrait être trouvée", retrieved);
        assertEquals("Le titre devrait correspondre", "Note de test", retrieved.getTitle());
    }

    // ==================== Helper Methods ====================

    private Project createProject(int id, String name, int status) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setStatus(status);
        project.setDescription("Description de " + name);
        return project;
    }

    private TimeReport createTimeReport(int projectId, double hours, boolean synced) {
        TimeReport report = new TimeReport();
        report.setProjectId(projectId);
        report.setWorkTypeId(1);
        report.setDatetimeFrom("2025-10-22 08:00:00");
        report.setDatetimeTo("2025-10-22 16:00:00");
        report.setHours(hours);
        report.setDescription("Description du rapport");
        report.setSynced(synced);
        return report;
    }
}
