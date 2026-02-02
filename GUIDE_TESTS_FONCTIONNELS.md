# 📱 Guide de Tests Fonctionnels - PTMS Mobile v2.0.1

**Date**: 2025-10-23
**Version**: 2.0.1
**Type**: Tests fonctionnels sur devices physiques/émulateurs

---

## 🎯 Objectif des Tests Fonctionnels

Valider le bon fonctionnement de l'application PTMS Mobile sur différentes versions Android, en conditions réelles d'utilisation.

**Focus**: Nouvelles fonctionnalités v2.0.1
- ✅ Mode UPDATE pour notes (sans doublons)
- ✅ Affichage noms utilisateurs dans le chat
- ✅ Synchronisation offline
- ✅ Fonctionnalités existantes (régression)

---

## 📋 Configurations de Test Requises

### Devices/Émulateurs Recommandés

| Version Android | API Level | Device Recommandé | Priorité |
|-----------------|-----------|-------------------|----------|
| **Android 7.0 (Nougat)** | API 24 | Émulateur Pixel 2 | 🔴 Haute |
| **Android 10** | API 29 | Émulateur Pixel 3 | 🟡 Moyenne |
| **Android 12** | API 31 | Émulateur Pixel 5 | 🟡 Moyenne |
| **Android 14** | API 34 | Émulateur Pixel 8 | 🔴 Haute |

**Rationale**:
- **Android 7.0** - Version minimum supportée (API 24)
- **Android 14** - Version target (API 34)
- **Android 10/12** - Versions intermédiaires courantes

---

## 🔧 Préparation de l'Environnement

### 1. Installation de l'APK

**APK Debug** (pour tests):
```bash
# Localisation
C:\Devs\web\uploads\apk\PTMS-Mobile-v2.0-debug-[DATE].apk

# Installation via ADB
adb install -r C:\Devs\web\uploads\apk\PTMS-Mobile-v2.0-debug-[DATE].apk

# Ou glisser-déposer sur l'émulateur
```

**Vérification**:
```bash
adb shell pm list packages | grep ptms
# Devrait afficher: package:com.ptms.mobile.debug
```

---

### 2. Configuration du Serveur Backend

**URL du serveur**: `https://serveralpha.protti.group`

**Comptes de test**:
- **Admin**: admin@protti.group / [mot de passe]
- **Manager**: manager@protti.group / [mot de passe]
- **Employé**: employee@protti.group / [mot de passe]

**Vérification connectivité**:
```bash
# Depuis le device/émulateur
adb shell ping -c 3 serveralpha.protti.group
```

---

### 3. Logs en Temps Réel

**Activer les logs**:
```bash
# Logs PTMS seulement
adb logcat -s PTMS:* API_CLIENT:*

# Tous les logs (verbose)
adb logcat *:V

# Filtrer par tag spécifique
adb logcat -s CreateNoteUnifiedActivity:*
adb logcat -s ChatActivity:*
```

---

## ✅ Scénarios de Test Prioritaires

### 🎯 Scénario 1: Authentification et Session
**Priorité**: 🔴 Critique
**Durée estimée**: 5 minutes

#### Actions
1. Lancer l'application
2. Entrer identifiants valides
3. Appuyer sur "Login"
4. Vérifier redirection vers dashboard

#### Résultats Attendus
- ✅ Login réussi
- ✅ Token JWT sauvegardé
- ✅ Nom utilisateur affiché
- ✅ Dashboard chargé

#### Vérification Logs
```bash
adb logcat -s AuthenticationManager:*
# Chercher: "💾 Sauvegarde unifiée des données de connexion"
# Chercher: "✓ Sauvegarde SessionManager"
```

#### Cas d'Erreur
- ❌ Login échoue → Vérifier connexion serveur
- ❌ Token non sauvegardé → Vérifier SharedPreferences
- ❌ Dashboard vide → Vérifier récupération profil

---

### 🎯 Scénario 2: Création de Note (INSERT)
**Priorité**: 🔴 Critique
**Durée estimée**: 3 minutes

#### Actions
1. Aller dans "Projets"
2. Sélectionner un projet
3. Appuyer sur "Nouvelle Note"
4. Remplir formulaire:
   - Type: Texte
   - Titre: "Test Note Création [DATE]"
   - Contenu: "Ceci est un test de création"
5. Appuyer sur "Enregistrer"

#### Résultats Attendus
- ✅ Message: "Note créée avec succès"
- ✅ Retour à la liste des notes
- ✅ Note visible dans la liste

#### Vérification Logs
```bash
adb logcat -s CreateNoteUnifiedActivity:*
# Chercher: "Mode création: nouvelle note"
# Chercher: "Note créée avec succès"
```

#### Vérification Backend
```sql
SELECT * FROM project_notes
WHERE title = 'Test Note Création [DATE]'
ORDER BY created_at DESC LIMIT 1;
```

---

### 🎯 Scénario 3: Édition de Note (UPDATE) - 🆕 v2.0.1
**Priorité**: 🔴 Critique (nouvelle fonctionnalité)
**Durée estimée**: 5 minutes

#### Actions
1. Aller dans "Projets"
2. Sélectionner un projet
3. Ouvrir une note existante
4. Appuyer sur "Modifier" (icône crayon)
5. Modifier le contenu:
   - Titre: "Test Note MODIFIÉE [DATE]"
   - Contenu: "Contenu mis à jour"
6. Appuyer sur "Enregistrer"

#### Résultats Attendus
- ✅ Message: "Note mise à jour avec succès"
- ✅ Retour à la liste des notes
- ✅ Note modifiée visible
- ✅ **PAS DE DOUBLON** (vérifier qu'il n'y a qu'une note)

#### Vérification Logs
```bash
adb logcat -s CreateNoteUnifiedActivity:*
# Chercher: "Mode édition: mise à jour de la note ID [X]"
# Chercher: "Note mise à jour avec succès"
```

#### Vérification Backend (CRITIQUE)
```sql
-- Compter les notes avec ce titre
SELECT COUNT(*) as count FROM project_notes
WHERE title LIKE 'Test Note MODIFIÉE%';
-- Doit retourner: count = 1 (PAS 2!)

-- Voir historique
SELECT id, title, created_at, updated_at
FROM project_notes
WHERE title LIKE 'Test Note%'
ORDER BY created_at DESC;
```

---

### 🎯 Scénario 4: Chat avec Noms Utilisateurs - 🆕 v2.0.1
**Priorité**: 🔴 Critique (nouvelle fonctionnalité)
**Durée estimée**: 5 minutes

#### Actions
1. Aller dans "Chat"
2. Sélectionner une conversation existante
3. Observer les noms des utilisateurs
4. Envoyer un message
5. Vérifier nom de l'expéditeur

#### Résultats Attendus
- ✅ Noms complets affichés (pas "Utilisateur [ID]")
- ✅ Message envoyé avec nom correct
- ✅ Cache utilisateurs fonctionne

#### Vérification Logs
```bash
adb logcat -s ChatActivity:*
# Chercher: "👤 Utilisateur récupéré depuis l'API"
# Chercher: "💾 Nom mis en cache"
```

#### Test Cache
1. Éteindre WiFi/données
2. Rouvrir chat
3. Vérifier noms toujours affichés (depuis cache)

---

### 🎯 Scénario 5: Saisie Heures Offline
**Priorité**: 🟡 Haute
**Durée estimée**: 7 minutes

#### Actions
1. Activer mode avion
2. Aller dans "Saisie d'heures"
3. Créer entrée:
   - Projet: [Sélectionner]
   - Date: Aujourd'hui
   - Heures: 7.5
   - Description: "Test offline"
4. Enregistrer
5. Désactiver mode avion
6. Attendre synchronisation automatique

#### Résultats Attendus
- ✅ Sauvegarde locale réussie (mode offline)
- ✅ Message: "Sauvegardé localement"
- ✅ Sync automatique après reconnexion
- ✅ Données visible sur le serveur

#### Vérification Logs
```bash
adb logcat -s OfflineSyncManager:*
# Chercher: "Sauvegarde locale réussie"
# Chercher: "Connexion détectée - Lancement de la synchronisation"
# Chercher: "Synchronisation complétée"
```

---

### 🎯 Scénario 6: Rapports et Statistiques
**Priorité**: 🟡 Moyenne
**Durée estimée**: 3 minutes

#### Actions
1. Aller dans "Rapports"
2. Sélectionner période (semaine en cours)
3. Vérifier totaux
4. Filtrer par projet

#### Résultats Attendus
- ✅ Liste des heures saisies
- ✅ Totaux corrects
- ✅ Filtrage fonctionne
- ✅ Export possible (si implémenté)

---

### 🎯 Scénario 7: Profil Utilisateur
**Priorité**: 🟢 Basse
**Durée estimée**: 2 minutes

#### Actions
1. Aller dans "Profil"
2. Vérifier informations affichées
3. Modifier paramètres (si disponible)

#### Résultats Attendus
- ✅ Nom correct
- ✅ Email correct
- ✅ Type utilisateur correct
- ✅ Modifications sauvegardées

---

## 🔍 Tests de Régression

### Fonctionnalités Existantes à Re-tester
1. ✅ Login/Logout
2. ✅ Liste projets
3. ✅ Saisie heures
4. ✅ Chat basique
5. ✅ Notifications (si implémentées)

**Objectif**: S'assurer que les nouvelles fonctionnalités n'ont pas cassé l'existant.

---

## 🐛 Tests Négatifs (Error Handling)

### Test 1: Connexion Perdue Pendant Opération
**Actions**:
1. Commencer à créer une note
2. Couper connexion (mode avion)
3. Tenter d'enregistrer

**Attendu**:
- ⚠️ Message d'erreur clair
- ⚠️ Option de sauvegarde locale
- ⚠️ Pas de crash

---

### Test 2: Token Expiré
**Actions**:
1. Se connecter
2. Attendre expiration token (ou manipuler manuellement)
3. Tenter une action nécessitant auth

**Attendu**:
- ⚠️ Redirection vers login
- ⚠️ Message "Session expirée"
- ⚠️ Pas de crash

---

### Test 3: Données Corrompues
**Actions**:
1. Modifier manuellement SharedPreferences via ADB
```bash
adb shell
run-as com.ptms.mobile.debug
cd shared_prefs
cat ptms_prefs.xml
```
2. Redémarrer app

**Attendu**:
- ⚠️ App détecte corruption
- ⚠️ Reset automatique ou message d'erreur
- ⚠️ Pas de crash

---

## 📊 Rapport de Test

### Template de Rapport

```markdown
# Rapport de Test Fonctionnel - [DATE]

## Configuration
- **Device**: [Nom du device/émulateur]
- **Android**: [Version]
- **APK**: PTMS-Mobile-v2.0-debug-[DATE].apk
- **Serveur**: serveralpha.protti.group
- **Testeur**: [Nom]

## Résultats

### Scénario 1: Authentification
- [ ] ✅ PASS
- [ ] ❌ FAIL - [Description]
- [ ] ⚠️ PARTIAL - [Description]

### Scénario 2: Création Note
- [ ] ✅ PASS
- [ ] ❌ FAIL - [Description]

### Scénario 3: Édition Note (UPDATE)
- [ ] ✅ PASS - Pas de doublon
- [ ] ❌ FAIL - Doublon créé
- [ ] ❌ FAIL - Erreur: [Description]

### Scénario 4: Chat Noms Utilisateurs
- [ ] ✅ PASS
- [ ] ❌ FAIL - [Description]

### Scénario 5: Offline Sync
- [ ] ✅ PASS
- [ ] ❌ FAIL - [Description]

## Bugs Identifiés
1. [Description du bug]
   - **Sévérité**: Critique/Haute/Moyenne/Basse
   - **Reproduction**: [Étapes]
   - **Logs**: [Extrait]

## Recommandations
- [Recommandation 1]
- [Recommandation 2]

## Signature
- **Testeur**: [Nom]
- **Date**: [Date]
- **Durée**: [Durée totale]
```

---

## 🔧 Commandes Utiles

### Debug
```bash
# Clear app data
adb shell pm clear com.ptms.mobile.debug

# Vérifier SharedPreferences
adb shell run-as com.ptms.mobile.debug cat shared_prefs/ptms_prefs.xml

# Vérifier base SQLite
adb shell run-as com.ptms.mobile.debug ls databases/
adb pull /data/data/com.ptms.mobile.debug/databases/ptms_offline.db

# Capture écran
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Enregistrer vidéo
adb shell screenrecord /sdcard/demo.mp4
# Ctrl+C pour arrêter
adb pull /sdcard/demo.mp4
```

### Performance
```bash
# Mémoire utilisée
adb shell dumpsys meminfo com.ptms.mobile.debug

# CPU usage
adb shell top -n 1 | grep ptms

# Taille APK
adb shell pm path com.ptms.mobile.debug
adb shell ls -lh [path_from_above]
```

---

## ✅ Checklist Pré-Test

Avant de commencer les tests, vérifier:

- [ ] APK installé correctement
- [ ] Serveur backend accessible
- [ ] Comptes de test créés
- [ ] ADB configuré et fonctionnel
- [ ] Logs activés
- [ ] Template de rapport prêt
- [ ] Screenshots/vidéos outils prêts

---

## 🎯 Critères de Validation

### Pour passer en Production
L'application doit:
- ✅ **100%** des scénarios critiques (🔴) passent
- ✅ **80%+** des scénarios haute priorité (🟡) passent
- ✅ **Aucun bug critique** non résolu
- ✅ **Pas de crash** sur scénarios principaux
- ✅ **Édition notes sans doublons** (critique v2.0.1)
- ✅ **Noms utilisateurs affichés** dans chat (critique v2.0.1)

### Pour Beta Testing
L'application doit:
- ✅ **80%+** des scénarios critiques passent
- ✅ **Bugs critiques** identifiés et documentés
- ✅ **Plan de correction** établi

---

## 📞 Support et Questions

**Documentation**:
- README.md
- ANDROID_BUILD_GUIDE.md
- TESTS_FINAL_REPORT_2025_10_23.md

**Logs**:
- Toujours inclure logs complets dans rapports de bugs
- Format: `adb logcat > test_[SCENARIO]_[DATE].log`

**Contact**:
- Équipe Dev: [Email]
- Support PTMS: [Email]

---

**Créé le**: 2025-10-23
**Version**: 1.0
**Pour**: PTMS Mobile v2.0.1
**Status**: Prêt pour tests
