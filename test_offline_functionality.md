# 🧪 Guide de Test - Fonctionnalité Hors Ligne PTMS

## 📋 **Checklist de Tests**

### **Phase 1 : Tests de Base**

#### ✅ **Test 1 : Installation et Démarrage**
- [ ] Installer l'APK sur un appareil/émulateur
- [ ] Lancer l'application
- [ ] Vérifier que l'écran de connexion s'affiche
- [ ] Vérifier qu'aucune erreur de compilation n'apparaît

#### ✅ **Test 2 : Connexion Normale**
- [ ] Se connecter avec des identifiants valides
- [ ] Vérifier l'accès au dashboard
- [ ] Vérifier que l'indicateur affiche "🟢 Connecté"
- [ ] Vérifier que les projets et types de travail se chargent

### **Phase 2 : Tests de Saisie d'Heures**

#### ✅ **Test 3 : Saisie en Mode Connecté**
- [ ] Aller dans "Saisie d'heures"
- [ ] Vérifier que l'indicateur affiche "Connecté"
- [ ] Sélectionner un projet et un type de travail
- [ ] Saisir des heures (ex: 9h00-17h00)
- [ ] Ajouter une description
- [ ] Cliquer "Enregistrer"
- [ ] Vérifier le message "✅ Heures sauvegardées avec succès"

#### ✅ **Test 4 : Mode Hors Ligne**
- [ ] **Couper le WiFi/données mobiles**
- [ ] Retourner à l'application
- [ ] Vérifier que l'indicateur affiche "🟠 Hors ligne"
- [ ] Aller dans "Saisie d'heures"
- [ ] Vérifier que les projets et types de travail sont toujours disponibles (cache)
- [ ] Saisir de nouvelles heures
- [ ] Vérifier le message "✅ Heures sauvegardées hors ligne"

#### ✅ **Test 5 : Synchronisation Automatique**
- [ ] **Remettre le WiFi/données mobiles**
- [ ] Retourner à l'application
- [ ] Vérifier que l'indicateur passe à "🟢 Connecté"
- [ ] Vérifier la notification "Connexion rétablie - Synchronisation automatique"
- [ ] Vérifier que le compteur de rapports en attente diminue
- [ ] Attendre quelques secondes et vérifier que le statut passe à "Synchronisé"

### **Phase 3 : Tests Avancés**

#### ✅ **Test 6 : Service en Arrière-Plan**
- [ ] S'assurer qu'il y a des heures en attente de synchronisation
- [ ] **Fermer complètement l'application**
- [ ] Attendre 5 minutes
- [ ] Vérifier les notifications de synchronisation
- [ ] Rouvrir l'application et vérifier que les heures sont synchronisées

#### ✅ **Test 7 : Synchronisation Manuelle**
- [ ] Aller dans "Saisie d'heures"
- [ ] Cliquer sur le bouton "Sync" dans la barre de statut
- [ ] Vérifier que la synchronisation se lance
- [ ] Vérifier les messages de progression
- [ ] Vérifier le message de fin de synchronisation

#### ✅ **Test 8 : Gestion des Erreurs**
- [ ] Simuler une erreur serveur (couper le serveur PTMS)
- [ ] Essayer de synchroniser
- [ ] Vérifier que les heures restent en attente
- [ ] Remettre le serveur en ligne
- [ ] Vérifier que la synchronisation reprend automatiquement

### **Phase 4 : Tests de Performance**

#### ✅ **Test 9 : Saisie Multiple Hors Ligne**
- [ ] Couper la connexion
- [ ] Saisir plusieurs heures (5-10 entrées)
- [ ] Vérifier que toutes sont sauvegardées localement
- [ ] Remettre la connexion
- [ ] Vérifier que toutes sont synchronisées

#### ✅ **Test 10 : Connexion Instable**
- [ ] Simuler une connexion instable (WiFi qui se coupe/remet)
- [ ] Continuer à saisir des heures
- [ ] Vérifier que l'application s'adapte automatiquement
- [ ] Vérifier qu'aucune donnée n'est perdue

## 🔍 **Points de Vérification Critiques**

### **Interface Utilisateur**
- [ ] Indicateurs de connexion visibles et corrects
- [ ] Compteur de rapports en attente fonctionnel
- [ ] Messages de confirmation clairs
- [ ] Bouton de synchronisation manuelle accessible

### **Fonctionnalité Hors Ligne**
- [ ] Cache des projets et types de travail fonctionnel
- [ ] Validation des données en local
- [ ] Sauvegarde locale garantie
- [ ] Interface adaptée au mode hors ligne

### **Synchronisation**
- [ ] Détection automatique de la connexion
- [ ] Synchronisation immédiate à la reconnexion
- [ ] Gestion des erreurs et retry
- [ ] Service en arrière-plan fonctionnel

### **Données**
- [ ] Aucune perte de données
- [ ] Intégrité des données préservée
- [ ] Statuts de synchronisation corrects
- [ ] Cohérence entre local et serveur

## 🐛 **Problèmes Potentiels et Solutions**

### **Problème : "Pas de projets disponibles"**
- **Cause :** Cache local vide
- **Solution :** Se connecter à internet pour charger les données
- **Test :** Vérifier que les projets apparaissent après reconnexion

### **Problème : "Synchronisation échouée"**
- **Cause :** Erreur serveur ou réseau
- **Solution :** Vérifier la connexion et réessayer
- **Test :** Simuler une erreur et vérifier le retry automatique

### **Problème : "Service non démarré"**
- **Cause :** Permissions manquantes ou erreur de démarrage
- **Solution :** Vérifier les permissions et redémarrer l'app
- **Test :** Vérifier les logs et les notifications

### **Problème : "Données corrompues"**
- **Cause :** Erreur de base de données locale
- **Solution :** Effacer les données de l'app et reconnecter
- **Test :** Vérifier l'intégrité des données après sync

## 📊 **Métriques de Succès**

### **Performance**
- ✅ Temps de démarrage < 3 secondes
- ✅ Saisie d'heures < 1 seconde
- ✅ Synchronisation < 30 secondes pour 10 éléments
- ✅ Consommation mémoire < 100MB

### **Fiabilité**
- ✅ 100% des heures sauvegardées localement
- ✅ 100% de synchronisation réussie en ligne
- ✅ 0% de perte de données
- ✅ Retry automatique fonctionnel

### **Expérience Utilisateur**
- ✅ Interface intuitive et claire
- ✅ Messages d'état compréhensibles
- ✅ Fonctionnement transparent
- ✅ Pas de blocage en mode hors ligne

## 📝 **Rapport de Test**

### **Template de Rapport**
```
Date de test: ___________
Appareil: ___________
Version Android: ___________
Version APK: ___________

Tests réussis: ___/10
Tests échoués: ___/10

Problèmes identifiés:
1. _________________________
2. _________________________
3. _________________________

Recommandations:
1. _________________________
2. _________________________
3. _________________________

Statut global: ✅ Réussi / ❌ Échec
```

---

**Note :** Ce guide de test doit être utilisé après chaque compilation pour s'assurer que la fonctionnalité hors ligne fonctionne correctement dans tous les scénarios.
