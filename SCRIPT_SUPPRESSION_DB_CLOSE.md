# Script de suppression des db.close()

Ce fichier documente toutes les lignes à supprimer dans `OfflineDatabaseHelper.java`.

## IMPORTANT
Les lignes suivantes contiennent `db.close()` et doivent être supprimées:

### Instructions
1. Ouvrir `OfflineDatabaseHelper.java` dans votre éditeur
2. Utiliser la fonction "Rechercher et remplacer"
3. Chercher: `\s*db\.close\(\);?\s*` (regex)
4. Remplacer par: rien (vide)
5. Sauvegarder

### OU manuellement supprimer les lignes suivantes:

Selon `grep -n "db.close()" OfflineDatabaseHelper.java`:

- Ligne ~364 dans `insertProject()`
- Ligne ~395 dans `getAllProjects()`
- Ligne ~404 dans `clearProjects()`
- Ligne ~443 dans `insertWorkType()`
- Ligne ~468 dans `getAllWorkTypes()`
- Ligne ~477 dans `clearWorkTypes()`
- Ligne ~505 dans `insertTimeReport()`
- Ligne ~541 dans `getAllPendingTimeReports()`
- Ligne ~558 dans `updateTimeReportSyncStatus()`
- Ligne ~573 dans `markTimeReportAsSynced()`
- Ligne ~589 dans `getPendingSyncCount()`
- Ligne ~669 dans `getAllPendingProjectNotes()`
- Ligne ~700 dans `getPendingProjectNotesByUserId()`
- Ligne ~720 dans `getProjectNotesByProjectId()`
- Ligne ~749 dans `getProjectNotesByProjectIdAndUserId()`
- Ligne ~779 dans `getPersonalNotesByUserId()`
- Ligne ~805 dans `getAllNotesByUserId()`
- Ligne ~835 dans `getNotesByGroupAndUserId()`
- Ligne ~913 dans `updateProjectNoteSyncStatus()`
- Ligne ~929 dans `markProjectNoteAsSynced()`
- Ligne ~947 dans `getPendingNotesSyncCount()`
- Ligne ~970 dans `getPendingNotesSyncCountByUserId()`
- Ligne ~978 dans `clearProjectNotes()`
- Ligne ~1004 dans `insertNoteType()`
- Ligne ~1032 dans `getAllNoteTypes()`
- Ligne ~1039 dans `clearNoteTypes()`
- Ligne ~1051 dans `clearAllData()`
- Ligne ~1068 dans `getDatabaseSize()`

**TOTAL: ~27 occurrences**

## Ajoutez aussi `synchronized`

À toutes les méthodes publiques, ajouter `synchronized`:

```java
// AVANT
public long insertProject(Project project) {

// APRÈS
public synchronized long insertProject(Project project) {
```

Méthodes concernées:
- insertProject, getAllProjects, clearProjects
- insertWorkType, getAllWorkTypes, clearWorkTypes
- insertTimeReport, getAllPendingTimeReports
- updateTimeReportSyncStatus, markTimeReportAsSynced, getPendingSyncCount
- insertProjectNote, getAllPendingProjectNotes, etc.
- Toutes les méthodes publiques!
