# Guide des Widgets Dashboard - PTMS Mobile v2.0.5

## Vue d'ensemble

Le système de widgets dashboard fournit des statistiques complètes et des visualisations sur le temps de travail enregistré dans PTMS Mobile. Il permet aux utilisateurs de suivre leur productivité avec des graphiques et des métriques en temps réel.

## Composants

### 1. DashboardWidgetManager
**Fichier**: `dashboard/DashboardWidgetManager.java`

Gestionnaire centralisé pour toutes les statistiques du dashboard.

#### Fonctionnalités

- **Calcul des statistiques**: Jour, semaine, mois
- **Cache intelligent**: 5 minutes de cache pour optimiser les performances
- **Top projets**: Les 5 projets avec le plus d'heures
- **Distribution**: Répartition par type de travail
- **Tendance**: Graphique des 7 derniers jours

#### Structure des données

```java
public static class DashboardStats {
    public TimeStats todayStats;         // Statistiques du jour
    public TimeStats weekStats;          // Statistiques de la semaine
    public TimeStats monthStats;         // Statistiques du mois
    public List<ProjectStats> topProjects; // Top 5 projets
    public Map<String, Double> workTypeDistribution; // Distribution par type
    public List<DayStats> weeklyTrend;   // Tendance 7 jours
}
```

#### Utilisation

```java
// Récupérer le gestionnaire
DashboardWidgetManager manager = DashboardWidgetManager.getInstance(context);

// Charger les statistiques
DashboardStats stats = manager.getDashboardStats(false); // false = utilise le cache

// Forcer le rafraîchissement
DashboardStats freshStats = manager.getDashboardStats(true);

// Vider le cache
manager.clearCache();
```

### 2. StatisticsCardWidget
**Fichier**: `dashboard/widgets/StatisticsCardWidget.java`

Widget carte Material Design pour afficher une statistique simple.

#### Caractéristiques

- Design Material 3
- Titre personnalisable
- Valeur principale avec formatage
- Sous-titre optionnel
- Icône circulaire
- Couleur de valeur modifiable

#### Layout

- Card radius: 16dp
- Elevation: 4dp
- Padding: 16dp
- Compatible padding activé

#### Utilisation

```java
StatisticsCardWidget widget = findViewById(R.id.widget_today);
widget.setTitle("AUJOURD'HUI");
widget.setValue("8h 30min");
widget.setSubtitle("3 entrées");
widget.setValueColor(R.color.success);
```

### 3. ProjectChartWidget
**Fichier**: `dashboard/widgets/ProjectChartWidget.java`

Widget graphique en camembert (donut chart) pour la distribution des projets.

#### Caractéristiques

- Graphique en anneau (donut)
- 8 couleurs différentes
- Total au centre
- Segments proportionnels
- Animation smooth

#### Données affichées

- Nom du projet
- Heures travaillées
- Pourcentage du total
- Couleur unique par projet

#### Rendu personnalisé

```java
@Override
protected void onDraw(Canvas canvas) {
    // Dessine les segments en arc
    for (ChartSegment segment : segments) {
        paint.setColor(segment.color);
        canvas.drawArc(bounds, segment.startAngle, segment.sweepAngle, true, paint);
    }

    // Cercle central blanc (effet donut)
    paint.setColor(0xFFFFFFFF);
    canvas.drawCircle(centerX, centerY, innerRadius, paint);

    // Affiche le total au centre
    canvas.drawText("Total", centerX, centerY - 10, textPaint);
    canvas.drawText(totalText, centerX, centerY + 25, textPaint);
}
```

#### Utilisation

```java
ProjectChartWidget chart = findViewById(R.id.chart_projects);
chart.setData(stats.topProjects, stats.monthStats.totalHours);

// Récupérer les segments pour la légende
List<ChartSegment> segments = chart.getSegments();
```

### 4. WeeklyTrendWidget
**Fichier**: `dashboard/widgets/WeeklyTrendWidget.java`

Widget graphique linéaire pour afficher la tendance hebdomadaire.

#### Caractéristiques

- Graphique linéaire avec remplissage
- Grille horizontale (0h, 4h, 8h)
- Points interactifs
- Labels des jours de la semaine
- Auto-scaling basé sur les données

#### Rendu

```java
// Ligne et remplissage
canvas.drawPath(fillPath, fillPaint);  // Zone sous la ligne
canvas.drawPath(linePath, linePaint);  // Ligne principale

// Points avec cercle blanc au centre
for (Point point : points) {
    canvas.drawCircle(point.x, point.y, 8f, pointPaint); // Point extérieur
    canvas.drawCircle(point.x, point.y, 4f, whitePaint); // Point intérieur
}
```

#### Utilisation

```java
WeeklyTrendWidget trend = findViewById(R.id.chart_weekly_trend);
trend.setData(stats.weeklyTrend);
```

### 5. DashboardActivity
**Fichier**: `activities/DashboardActivity.java`

Activité principale qui intègre tous les widgets.

#### Structure UI

```
AppBar (Toolbar)
  └─ SwipeRefreshLayout
      └─ NestedScrollView
          └─ LinearLayout
              ├─ ProgressBar (loading)
              ├─ No Data View
              └─ Content Container
                  ├─ Statistiques Résumées (4 cartes en grille 2x2)
                  │   ├─ Aujourd'hui
                  │   ├─ Cette semaine
                  │   ├─ Ce mois
                  │   └─ Moyenne/jour
                  ├─ Tendance Hebdomadaire (Card + Chart)
                  └─ Top 5 Projets (Card + Chart + Legend)
```

#### Chargement des données

```java
private void loadDashboardData(boolean forceRefresh) {
    showLoading(true);

    executorService.execute(() -> {
        // Récupérer les stats en arrière-plan
        DashboardStats stats = widgetManager.getDashboardStats(forceRefresh);

        runOnUiThread(() -> {
            updateUI(stats);
            showLoading(false);
            swipeRefresh.setRefreshing(false);
        });
    });
}
```

#### Pull-to-refresh

- SwipeRefreshLayout pour actualiser
- Bouton manuel de rafraîchissement si aucune donnée
- Animation de chargement

## Base de données

### Nouvelles méthodes OfflineDatabaseHelper

#### getTimeReportsByDateRange()

Récupère les rapports de temps dans une plage de dates.

```java
public synchronized List<TimeReport> getTimeReportsByDateRange(String startDate, String endDate) {
    Cursor cursor = db.query(
        TABLE_TIME_REPORTS,
        null,
        COLUMN_REPORT_DATE + " >= ? AND " + COLUMN_REPORT_DATE + " <= ?",
        new String[]{startDate, endDate},
        null, null,
        COLUMN_REPORT_DATE + " DESC"
    );
    // Extraction des données...
}
```

**Paramètres**:
- `startDate`: Date de début au format "yyyy-MM-dd"
- `endDate`: Date de fin au format "yyyy-MM-dd"

**Retour**: Liste de TimeReport triés par date décroissante

#### getProjectById()

Récupère un projet par son ID.

```java
public synchronized Project getProjectById(int projectId) {
    Cursor cursor = db.query(
        TABLE_PROJECTS,
        null,
        COLUMN_ID + " = ?",
        new String[]{String.valueOf(projectId)},
        null, null, null
    );
    // Extraction du projet...
}
```

**Paramètres**:
- `projectId`: ID du projet

**Retour**: Objet Project ou null si non trouvé

## Statistiques calculées

### TimeStats

```java
public static class TimeStats {
    public String startDate;              // Date de début
    public String endDate;                // Date de fin
    public double totalHours;             // Total d'heures
    public int totalEntries;              // Nombre d'entrées
    public double averageHoursPerDay;     // Moyenne par jour
    public Map<String, Double> projectBreakdown; // Répartition par projet
}
```

**Méthodes de formatage**:
```java
stats.getFormattedTotalHours()    // "8h 30min"
stats.getFormattedAverageHours()  // "4h 15min"
```

### ProjectStats

```java
public static class ProjectStats {
    public int projectId;        // ID du projet
    public String projectName;   // Nom du projet
    public double totalHours;    // Total d'heures
    public int entryCount;       // Nombre d'entrées
}
```

**Méthodes**:
```java
stats.getFormattedHours()             // "25h 45min"
stats.getPercentage(totalHours)       // 42.3 (pourcentage du total)
```

### DayStats

```java
public static class DayStats {
    public String date;          // Date "yyyy-MM-dd"
    public String dayOfWeek;     // "Lun", "Mar", etc.
    public double totalHours;    // Total d'heures
    public int entryCount;       // Nombre d'entrées
}
```

## Calculs automatiques

### Moyenne par jour

```java
private double calculateAveragePerDay(double totalHours, String startDate, String endDate) {
    Date start = dateFormat.parse(startDate);
    Date end = dateFormat.parse(endDate);

    long diffMillis = end.getTime() - start.getTime();
    int days = (int) (diffMillis / (1000 * 60 * 60 * 24)) + 1;

    return totalHours / days;
}
```

### Tendance hebdomadaire

Calcule les statistiques pour les 7 derniers jours:

```java
for (int i = 6; i >= 0; i--) {
    calendar.add(Calendar.DAY_OF_YEAR, -i);
    String date = dateFormat.format(calendar.getTime());

    TimeStats dayStats = getStatsForPeriod(date, date);
    // Ajouter à la tendance...
}
```

## Cache et performances

### Système de cache

- **Durée**: 5 minutes
- **Type**: In-memory (DashboardStats)
- **Invalidation**: Manuelle ou automatique après expiration

```java
private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

public DashboardStats getDashboardStats(boolean forceRefresh) {
    long currentTime = System.currentTimeMillis();

    if (!forceRefresh && cachedStats != null &&
        (currentTime - lastCacheUpdate) < CACHE_DURATION) {
        return cachedStats; // Retourne le cache
    }

    // Recalcule les statistiques
    DashboardStats stats = new DashboardStats();
    // ...

    cachedStats = stats;
    lastCacheUpdate = currentTime;

    return stats;
}
```

### Optimisations

1. **Requêtes SQL optimisées**: Utilise des index sur `report_date`
2. **Chargement asynchrone**: ExecutorService pour éviter le blocage UI
3. **Cache intelligent**: 5 minutes pour éviter les recalculs fréquents
4. **Formatage paresseux**: Formatage des valeurs uniquement lors de l'affichage

## Layouts XML

### activity_dashboard.xml

- **Type**: CoordinatorLayout
- **AppBar**: MaterialToolbar
- **Refresh**: SwipeRefreshLayout
- **Scroll**: NestedScrollView
- **Content**: LinearLayout vertical

### widget_statistics_card.xml

- **Container**: LinearLayout vertical
- **Icône**: 48x48dp avec background circulaire
- **Titre**: 12sp bold, couleur secondaire
- **Valeur**: 24sp bold, couleur primaire
- **Sous-titre**: 11sp, couleur secondaire

### item_project_legend.xml

- **Container**: LinearLayout horizontal
- **Indicateur**: View 16x16dp (couleur du projet)
- **Nom**: TextView flexible avec ellipsis
- **Heures**: TextView bold
- **Pourcentage**: TextView couleur secondaire

## Couleurs et design

### Palette

```xml
<!-- Graphique projets (camembert) -->
0xFF2196F3  <!-- Blue -->
0xFF4CAF50  <!-- Green -->
0xFFF44336  <!-- Red -->
0xFFFF9800  <!-- Orange -->
0xFF9C27B0  <!-- Purple -->
0xFFFFEB3B  <!-- Yellow -->
0xFF00BCD4  <!-- Cyan -->
0xFFE91E63  <!-- Pink -->
```

### Material Design

- **Cards**: 12dp corner radius, 2dp elevation
- **Statistics Cards**: 16dp corner radius, 4dp elevation
- **Icons**: 24x24dp standard size
- **Padding**: 16dp standard content padding
- **Margins**: 4dp entre les cartes de la grille

## Utilisation typique

### Afficher le dashboard

```java
Intent intent = new Intent(this, DashboardActivity.class);
startActivity(intent);
```

### Intégration dans MainActivity

```java
// Ajouter un bouton Dashboard dans le menu
MenuItem dashboardItem = menu.findItem(R.id.action_dashboard);
dashboardItem.setOnMenuItemClickListener(item -> {
    Intent intent = new Intent(this, DashboardActivity.class);
    startActivity(intent);
    return true;
});
```

### Rafraîchir les données

```java
// Pull-to-refresh (automatique via SwipeRefreshLayout)
swipeRefresh.setOnRefreshListener(() -> loadDashboardData(true));

// Bouton manuel
btnRefresh.setOnClickListener(v -> loadDashboardData(true));
```

## Gestion des états

### États possibles

1. **Loading**: Affichage du ProgressBar
2. **Content**: Affichage des widgets avec données
3. **No Data**: Message et bouton de rafraîchissement
4. **Error**: Toast avec message d'erreur

### Transitions

```java
private void showLoading(boolean show) {
    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    contentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
}

private void showContent() {
    contentContainer.setVisibility(View.VISIBLE);
    tvNoData.setVisibility(View.GONE);
    btnRefresh.setVisibility(View.GONE);
}

private void showNoData() {
    contentContainer.setVisibility(View.GONE);
    tvNoData.setVisibility(View.VISIBLE);
    btnRefresh.setVisibility(View.VISIBLE);
}
```

## Tests

### Test manuel

1. **Données existantes**:
   - Créer plusieurs rapports de temps
   - Ouvrir le dashboard
   - Vérifier que toutes les statistiques sont affichées

2. **Pull-to-refresh**:
   - Glisser vers le bas
   - Vérifier l'animation de rafraîchissement
   - Confirmer que les données sont actualisées

3. **Sans données**:
   - Base de données vide
   - Vérifier le message "Aucune donnée disponible"
   - Tester le bouton de rafraîchissement

4. **Graphiques**:
   - Vérifier que le camembert affiche tous les projets
   - Vérifier que la tendance affiche 7 jours
   - Confirmer que les couleurs sont cohérentes avec la légende

### Tests unitaires potentiels

```java
@Test
public void testCalculateAveragePerDay() {
    double average = widgetManager.calculateAveragePerDay(
        40.0,  // 40 heures
        "2025-10-15",  // Lundi
        "2025-10-19"   // Vendredi (5 jours)
    );
    assertEquals(8.0, average, 0.01);  // 40h / 5j = 8h/j
}

@Test
public void testCacheDuration() {
    DashboardStats stats1 = widgetManager.getDashboardStats(false);
    DashboardStats stats2 = widgetManager.getDashboardStats(false);

    // Doit retourner la même instance (cache)
    assertSame(stats1, stats2);
}

@Test
public void testForceRefresh() {
    DashboardStats stats1 = widgetManager.getDashboardStats(false);
    DashboardStats stats2 = widgetManager.getDashboardStats(true);

    // Force refresh devrait recalculer
    // (Vérifier les timestamps internes)
}
```

## Améliorations futures possibles

1. **Export**: Exporter les statistiques en PDF/Excel
2. **Période personnalisée**: Sélecteur de dates pour période custom
3. **Comparaison**: Comparer deux périodes (mois vs mois précédent)
4. **Objectifs**: Définir des objectifs d'heures et afficher la progression
5. **Notifications**: Alertes si objectifs non atteints
6. **Filtres**: Filtrer par projet, type de travail, statut
7. **Animation**: Transitions animées lors du chargement
8. **Détails**: Cliquer sur un projet pour voir les détails
9. **Widgets Home Screen**: Widgets Android pour l'écran d'accueil
10. **Offline support**: Cache persistant pour mode hors ligne

## Résolution de problèmes

### Problème: "Aucune donnée disponible"

**Causes possibles**:
- Base de données vide
- Aucun rapport de temps créé
- Filtre de date ne correspond à aucune donnée

**Solutions**:
1. Créer des rapports de temps
2. Vérifier les dates des rapports existants
3. Rafraîchir manuellement

### Problème: Graphiques ne s'affichent pas

**Causes possibles**:
- Données nulles ou vides
- Erreur de calcul

**Solutions**:
1. Vérifier les logs (TAG: "DashboardWidgetManager")
2. Vérifier que `getTimeReportsByDateRange()` retourne des données
3. Tester avec des données de test

### Problème: Performances lentes

**Causes possibles**:
- Trop de rapports en base
- Cache désactivé
- Calculs lourds sur UI thread

**Solutions**:
1. Vérifier que le cache fonctionne (5 minutes)
2. Limiter la période d'analyse
3. Optimiser les requêtes SQL
4. Utiliser ExecutorService pour le chargement

## Ressources

- [Material Design - Cards](https://m3.material.io/components/cards)
- [Android Canvas Drawing](https://developer.android.com/reference/android/graphics/Canvas)
- [SwipeRefreshLayout Guide](https://developer.android.com/training/swipe/add-swipe-interface)
- [Custom Views Tutorial](https://developer.android.com/develop/ui/views/layout/custom-views/create-view)

---

**Version**: 2.0.5
**Date**: Octobre 2025
**Auteur**: PTMS Development Team
