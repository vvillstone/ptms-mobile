package com.ptms.mobile.utils;

import android.content.Context;
import android.util.Log;

import com.ptms.mobile.database.OfflineDatabaseHelper;
import com.ptms.mobile.models.Project;
import com.ptms.mobile.models.ProjectNote;
import com.ptms.mobile.models.TimeReport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✅ Gestionnaire de recherche avancée multi-critères
 *
 * Fonctionnalités :
 * - Recherche unifiée (projets, notes, rapports)
 * - Filtres multiples (date, catégorie, projet, tags)
 * - Recherche full-text avec normalisation
 * - Tri personnalisable
 * - Résultats groupés par type
 * - Historique de recherche
 * - Suggestions intelligentes
 *
 * @version 1.0
 * @date 2025-10-23
 */
public class SearchManager {

    private static final String TAG = "SearchManager";

    // Types de recherche
    public enum SearchType {
        ALL,        // Tout
        PROJECTS,   // Projets uniquement
        NOTES,      // Notes uniquement
        REPORTS     // Rapports uniquement
    }

    // Critères de tri
    public enum SortBy {
        RELEVANCE,      // Pertinence
        DATE_DESC,      // Date décroissante (plus récent)
        DATE_ASC,       // Date croissante (plus ancien)
        TITLE_ASC,      // Titre A-Z
        TITLE_DESC      // Titre Z-A
    }

    // Filtres de date
    public enum DateFilter {
        ALL,            // Toutes les dates
        TODAY,          // Aujourd'hui
        YESTERDAY,      // Hier
        THIS_WEEK,      // Cette semaine
        THIS_MONTH,     // Ce mois
        LAST_MONTH,     // Mois dernier
        THIS_YEAR,      // Cette année
        CUSTOM          // Période personnalisée
    }

    private final Context context;
    private final OfflineDatabaseHelper dbHelper;
    private final SessionManager sessionManager;

    // Historique de recherche (max 20 entrées)
    private static final int MAX_HISTORY_SIZE = 20;
    private List<String> searchHistory;

    // Cache des résultats
    private Map<String, SearchResults> resultsCache;
    private static final int MAX_CACHE_SIZE = 10;

    public SearchManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new OfflineDatabaseHelper(context);
        this.sessionManager = new SessionManager(context);
        this.searchHistory = loadSearchHistory();
        this.resultsCache = new HashMap<>();
    }

    // ==================== RECHERCHE PRINCIPALE ====================

    /**
     * Recherche unifiée avec tous les critères
     */
    public SearchResults search(SearchCriteria criteria) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🔍 RECHERCHE AVANCÉE");
        Log.d(TAG, "========================================");
        Log.d(TAG, "Query: " + criteria.query);
        Log.d(TAG, "Type: " + criteria.searchType);
        Log.d(TAG, "Sort: " + criteria.sortBy);
        Log.d(TAG, "Date filter: " + criteria.dateFilter);

        // Vérifier le cache
        String cacheKey = criteria.getCacheKey();
        if (resultsCache.containsKey(cacheKey)) {
            Log.d(TAG, "✅ Résultats en cache");
            return resultsCache.get(cacheKey);
        }

        SearchResults results = new SearchResults();
        results.query = criteria.query;
        results.searchType = criteria.searchType;

        // Normaliser la requête
        String normalizedQuery = normalizeText(criteria.query);

        // Rechercher selon le type
        switch (criteria.searchType) {
            case ALL:
                results.projects = searchProjects(normalizedQuery, criteria);
                results.notes = searchNotes(normalizedQuery, criteria);
                results.reports = searchReports(normalizedQuery, criteria);
                break;

            case PROJECTS:
                results.projects = searchProjects(normalizedQuery, criteria);
                break;

            case NOTES:
                results.notes = searchNotes(normalizedQuery, criteria);
                break;

            case REPORTS:
                results.reports = searchReports(normalizedQuery, criteria);
                break;
        }

        // Calculer le total
        results.totalCount = results.projects.size() +
                            results.notes.size() +
                            results.reports.size();

        // Trier les résultats
        sortResults(results, criteria.sortBy);

        // Mettre en cache
        cacheResults(cacheKey, results);

        // Sauvegarder dans l'historique
        addToSearchHistory(criteria.query);

        Log.d(TAG, "✅ Recherche terminée:");
        Log.d(TAG, "  • Projets: " + results.projects.size());
        Log.d(TAG, "  • Notes: " + results.notes.size());
        Log.d(TAG, "  • Rapports: " + results.reports.size());
        Log.d(TAG, "  • Total: " + results.totalCount);
        Log.d(TAG, "========================================");

        return results;
    }

    // ==================== RECHERCHE PAR TYPE ====================

    /**
     * Recherche dans les projets
     */
    private List<Project> searchProjects(String query, SearchCriteria criteria) {
        List<Project> allProjects = dbHelper.getAllProjects();
        List<Project> filtered = new ArrayList<>();

        for (Project project : allProjects) {
            if (matchesProjectCriteria(project, query, criteria)) {
                filtered.add(project);
            }
        }

        return filtered;
    }

    /**
     * Recherche dans les notes
     */
    private List<ProjectNote> searchNotes(String query, SearchCriteria criteria) {
        int userId = sessionManager.getUserId();
        List<ProjectNote> allNotes = dbHelper.getAllNotesByUserId(userId);
        List<ProjectNote> filtered = new ArrayList<>();

        for (ProjectNote note : allNotes) {
            if (matchesNoteCriteria(note, query, criteria)) {
                filtered.add(note);
            }
        }

        return filtered;
    }

    /**
     * Recherche dans les rapports
     */
    private List<TimeReport> searchReports(String query, SearchCriteria criteria) {
        // Pour l'instant, utiliser la méthode getAllPendingTimeReports()
        // TODO: Créer une méthode getAllTimeReports() pour obtenir tous les rapports
        List<TimeReport> allReports = dbHelper.getAllPendingTimeReports();
        List<TimeReport> filtered = new ArrayList<>();

        for (TimeReport report : allReports) {
            if (matchesReportCriteria(report, query, criteria)) {
                filtered.add(report);
            }
        }

        return filtered;
    }

    // ==================== CRITÈRES DE CORRESPONDANCE ====================

    /**
     * Vérifie si un projet correspond aux critères
     */
    private boolean matchesProjectCriteria(Project project, String query, SearchCriteria criteria) {
        // Filtre par projet spécifique
        if (criteria.projectId != null && project.getId() != criteria.projectId) {
            return false;
        }

        // Recherche textuelle
        if (query != null && !query.isEmpty()) {
            String normalizedName = normalizeText(project.getName());
            String normalizedDescription = normalizeText(project.getDescription());

            if (!normalizedName.contains(query) &&
                !normalizedDescription.contains(query)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie si une note correspond aux critères
     */
    private boolean matchesNoteCriteria(ProjectNote note, String query, SearchCriteria criteria) {
        // Filtre par projet
        if (criteria.projectId != null &&
            (note.getProjectId() == null || note.getProjectId() != criteria.projectId)) {
            return false;
        }

        // Filtre par catégorie
        if (criteria.category != null && !criteria.category.isEmpty()) {
            if (note.getNoteTypeSlug() == null ||
                !note.getNoteTypeSlug().equalsIgnoreCase(criteria.category)) {
                return false;
            }
        }

        // Filtre par tags
        if (criteria.tags != null && !criteria.tags.isEmpty()) {
            List<String> noteTags = note.getTags();
            if (noteTags == null || noteTags.isEmpty()) {
                return false;
            }

            boolean hasMatchingTag = false;
            for (String criteriaTag : criteria.tags) {
                for (String noteTag : noteTags) {
                    if (normalizeText(noteTag).contains(normalizeText(criteriaTag))) {
                        hasMatchingTag = true;
                        break;
                    }
                }
                if (hasMatchingTag) break;
            }

            if (!hasMatchingTag) {
                return false;
            }
        }

        // Filtre par importance
        if (criteria.importantOnly && !note.isImportant()) {
            return false;
        }

        // Filtre par date
        if (!matchesDateFilter(note.getCreatedAt(), criteria.dateFilter,
                               criteria.startDate, criteria.endDate)) {
            return false;
        }

        // Recherche textuelle
        if (query != null && !query.isEmpty()) {
            String normalizedTitle = normalizeText(note.getTitle());
            String normalizedContent = normalizeText(note.getContent());
            String normalizedTranscription = normalizeText(note.getTranscription());

            if (!normalizedTitle.contains(query) &&
                !normalizedContent.contains(query) &&
                !normalizedTranscription.contains(query)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie si un rapport correspond aux critères
     */
    private boolean matchesReportCriteria(TimeReport report, String query, SearchCriteria criteria) {
        // Filtre par projet
        if (criteria.projectId != null && report.getProjectId() != criteria.projectId) {
            return false;
        }

        // Filtre par date
        if (!matchesDateFilter(report.getReportDate(), criteria.dateFilter,
                               criteria.startDate, criteria.endDate)) {
            return false;
        }

        // Recherche textuelle
        if (query != null && !query.isEmpty()) {
            String normalizedProjectName = normalizeText(report.getProjectName());
            String normalizedWorkType = normalizeText(report.getWorkTypeName());
            String normalizedComment = normalizeText(report.getDescription());

            if (!normalizedProjectName.contains(query) &&
                !normalizedWorkType.contains(query) &&
                !normalizedComment.contains(query)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie si une date correspond au filtre
     */
    private boolean matchesDateFilter(String dateStr, DateFilter filter,
                                       String startDate, String endDate) {
        if (filter == DateFilter.ALL) {
            return true;
        }

        if (dateStr == null || dateStr.isEmpty()) {
            return false;
        }

        // TODO: Implémenter la logique de filtre de date
        // Pour l'instant, accepter toutes les dates
        return true;
    }

    // ==================== TRI DES RÉSULTATS ====================

    /**
     * Trie les résultats selon le critère choisi
     */
    private void sortResults(SearchResults results, SortBy sortBy) {
        switch (sortBy) {
            case DATE_DESC:
                sortByDateDesc(results);
                break;

            case DATE_ASC:
                sortByDateAsc(results);
                break;

            case TITLE_ASC:
                sortByTitleAsc(results);
                break;

            case TITLE_DESC:
                sortByTitleDesc(results);
                break;

            case RELEVANCE:
            default:
                // Le tri par pertinence est déjà implicite dans l'ordre de recherche
                break;
        }
    }

    private void sortByDateDesc(SearchResults results) {
        Collections.sort(results.notes, (a, b) -> {
            String dateA = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String dateB = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return dateB.compareTo(dateA);
        });

        Collections.sort(results.reports, (a, b) -> {
            String dateA = a.getReportDate() != null ? a.getReportDate() : "";
            String dateB = b.getReportDate() != null ? b.getReportDate() : "";
            return dateB.compareTo(dateA);
        });
    }

    private void sortByDateAsc(SearchResults results) {
        Collections.sort(results.notes, (a, b) -> {
            String dateA = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String dateB = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return dateA.compareTo(dateB);
        });

        Collections.sort(results.reports, (a, b) -> {
            String dateA = a.getReportDate() != null ? a.getReportDate() : "";
            String dateB = b.getReportDate() != null ? b.getReportDate() : "";
            return dateA.compareTo(dateB);
        });
    }

    private void sortByTitleAsc(SearchResults results) {
        Collections.sort(results.projects, (a, b) ->
            a.getName().compareToIgnoreCase(b.getName()));

        Collections.sort(results.notes, (a, b) -> {
            String titleA = a.getTitle() != null ? a.getTitle() : "";
            String titleB = b.getTitle() != null ? b.getTitle() : "";
            return titleA.compareToIgnoreCase(titleB);
        });
    }

    private void sortByTitleDesc(SearchResults results) {
        Collections.sort(results.projects, (a, b) ->
            b.getName().compareToIgnoreCase(a.getName()));

        Collections.sort(results.notes, (a, b) -> {
            String titleA = a.getTitle() != null ? a.getTitle() : "";
            String titleB = b.getTitle() != null ? b.getTitle() : "";
            return titleB.compareToIgnoreCase(titleA);
        });
    }

    // ==================== NORMALISATION TEXTE ====================

    /**
     * Normalise le texte pour la recherche (supprime accents, minuscules)
     */
    private String normalizeText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Convertir en minuscules
        text = text.toLowerCase();

        // Supprimer les accents
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("\\p{M}", "");

        return text;
    }

    // ==================== HISTORIQUE DE RECHERCHE ====================

    /**
     * Charge l'historique de recherche depuis les préférences
     */
    private List<String> loadSearchHistory() {
        // TODO: Charger depuis SharedPreferences
        return new ArrayList<>();
    }

    /**
     * Ajoute une requête à l'historique
     */
    private void addToSearchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        query = query.trim();

        // Retirer si existe déjà
        searchHistory.remove(query);

        // Ajouter au début
        searchHistory.add(0, query);

        // Limiter la taille
        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            searchHistory = searchHistory.subList(0, MAX_HISTORY_SIZE);
        }

        // TODO: Sauvegarder dans SharedPreferences
    }

    /**
     * Récupère l'historique de recherche
     */
    public List<String> getSearchHistory() {
        return new ArrayList<>(searchHistory);
    }

    /**
     * Efface l'historique de recherche
     */
    public void clearSearchHistory() {
        searchHistory.clear();
        // TODO: Effacer depuis SharedPreferences
    }

    // ==================== SUGGESTIONS ====================

    /**
     * Génère des suggestions de recherche basées sur l'historique et le contenu
     */
    public List<String> getSuggestions(String partialQuery) {
        List<String> suggestions = new ArrayList<>();

        if (partialQuery == null || partialQuery.isEmpty()) {
            // Retourner l'historique récent
            return getSearchHistory();
        }

        String normalized = normalizeText(partialQuery);

        // Suggestions depuis l'historique
        for (String historyItem : searchHistory) {
            if (normalizeText(historyItem).startsWith(normalized)) {
                suggestions.add(historyItem);
            }
        }

        // Limiter à 5 suggestions
        if (suggestions.size() > 5) {
            suggestions = suggestions.subList(0, 5);
        }

        return suggestions;
    }

    // ==================== CACHE ====================

    /**
     * Met en cache les résultats de recherche
     */
    private void cacheResults(String key, SearchResults results) {
        if (resultsCache.size() >= MAX_CACHE_SIZE) {
            // Supprimer le plus ancien (simple FIFO)
            String firstKey = resultsCache.keySet().iterator().next();
            resultsCache.remove(firstKey);
        }

        resultsCache.put(key, results);
    }

    /**
     * Efface le cache de résultats
     */
    public void clearCache() {
        resultsCache.clear();
        Log.d(TAG, "🗑️ Cache de recherche effacé");
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * Critères de recherche
     */
    public static class SearchCriteria {
        public String query = "";
        public SearchType searchType = SearchType.ALL;
        public SortBy sortBy = SortBy.RELEVANCE;
        public DateFilter dateFilter = DateFilter.ALL;
        public Integer projectId = null;
        public String category = null;
        public List<String> tags = null;
        public boolean importantOnly = false;
        public String startDate = null;
        public String endDate = null;

        public String getCacheKey() {
            StringBuilder sb = new StringBuilder();
            sb.append(query).append("|");
            sb.append(searchType).append("|");
            sb.append(sortBy).append("|");
            sb.append(dateFilter).append("|");
            sb.append(projectId).append("|");
            sb.append(category).append("|");
            sb.append(tags).append("|");
            sb.append(importantOnly);
            return sb.toString();
        }
    }

    /**
     * Résultats de recherche
     */
    public static class SearchResults {
        public String query;
        public SearchType searchType;
        public List<Project> projects = new ArrayList<>();
        public List<ProjectNote> notes = new ArrayList<>();
        public List<TimeReport> reports = new ArrayList<>();
        public int totalCount = 0;
    }
}
