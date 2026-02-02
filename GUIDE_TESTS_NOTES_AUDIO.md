# 🎤 Guide de Test - Système de Notes Audio Offline

## 📋 Prérequis

- ✅ APK compilé : `app/build/outputs/apk/debug/app-debug.apk`
- ✅ Serveur PHP accessible (avec endpoint `/api/project-notes.php`)
- ✅ Base de données avec table `project_notes`
- ✅ Utilisateur Android authentifié

## 🧪 Scénarios de Test

### 1️⃣ **Test Création Note Texte (Online)**

**Étapes** :
1. Ouvrir l'app Android
2. Se connecter
3. Naviguer vers un projet
4. Cliquer sur FAB "+" pour ajouter une note
5. Sélectionner type "Texte"
6. Saisir titre : "Test Note Texte"
7. Saisir contenu : "Ceci est un test de note texte"
8. Cocher "Important" si désiré
9. Ajouter tags : "test, texte"
10. Cliquer "Enregistrer"

**Résultat Attendu** :
- ✅ Message "Note sauvegardée avec succès"
- ✅ Retour automatique à la liste
- ✅ Note visible dans la liste
- ✅ Note visible sur le Web

---

### 2️⃣ **Test Création Note Audio (Online)**

**Étapes** :
1. Ajouter une nouvelle note
2. Sélectionner type "Audio"
3. Saisir titre : "Test Enregistrement Audio"
4. Cliquer "🎤 Démarrer l'enregistrement"
5. **Autoriser l'accès au microphone** (popup système)
6. Parler pendant 10 secondes
7. Cliquer "⏹ Arrêter l'enregistrement"
8. Vérifier l'indicateur "Audio enregistré (00:XX)"
9. Cliquer "Enregistrer"

**Résultat Attendu** :
- ✅ Enregistrement démarre (timer visible)
- ✅ Fichier audio sauvegardé localement
- ✅ Upload vers le serveur (si online)
- ✅ Note visible avec durée audio
- ✅ Fichier présent dans `uploads/audio_notes/{project_id}/`

---

### 3️⃣ **Test Mode Offline (Sans Connexion)**

**Étapes** :
1. **Désactiver le WiFi/données mobiles**
2. Ajouter une nouvelle note audio
3. Enregistrer pendant 5 secondes
4. Sauvegarder la note
5. Vérifier que la note apparaît avec badge "[Local]"
6. **Réactiver la connexion**
7. Attendre quelques secondes
8. Rafraîchir la liste

**Résultat Attendu** :
- ✅ Note sauvegardée localement malgré absence réseau
- ✅ Message "sera synchronisée plus tard"
- ✅ Badge "[Local]" visible
- ✅ Après reconnexion : synchronisation automatique
- ✅ Badge "[Local]" disparaît après sync
- ✅ Note visible sur le Web après sync

---

### 4️⃣ **Test Lecture Audio Locale (Offline)**

**Étapes** :
1. Créer une note audio en mode offline (voir test 3)
2. Cliquer sur la note dans la liste
3. Lire le dialog de détails
4. Cliquer "🎵 Écouter"

**Résultat Attendu** :
- ✅ Lecteur audio système s'ouvre
- ✅ Fichier local lu depuis `/audio_notes/note_XXX.mp3`
- ✅ Lecture fonctionnelle

---

### 5️⃣ **Test Lecture Audio Serveur (Online)**

**Étapes** :
1. Créer une note audio en mode online (voir test 2)
2. Attendre la synchronisation
3. Cliquer sur la note
4. Cliquer "🎵 Écouter"

**Résultat Attendu** :
- ✅ Lecteur audio s'ouvre
- ✅ Audio streamé depuis `/api/project-notes-audio.php?note_id=XXX`
- ✅ Lecture fonctionnelle

---

### 6️⃣ **Test Synchronisation Multiple Notes**

**Étapes** :
1. Passer en mode offline
2. Créer 5 notes différentes :
   - 2 notes texte
   - 2 notes audio
   - 1 note dictée
3. Vérifier que toutes ont le badge "[Local]"
4. Repasser en mode online
5. Observer la synchronisation

**Résultat Attendu** :
- ✅ Toutes les notes sauvegardées localement
- ✅ Synchronisation automatique au retour online
- ✅ Ordre de sync respecté (FIFO)
- ✅ Toutes les notes visibles sur le Web
- ✅ Fichiers audio uploadés correctement

---

### 7️⃣ **Test Filtrage par Utilisateur (Sécurité)**

**Étapes** :
1. Utilisateur A : Créer une note pour Projet X
2. Se déconnecter
3. Utilisateur B : Se connecter
4. Naviguer vers Projet X
5. Vérifier la liste des notes

**Résultat Attendu** :
- ✅ Utilisateur B ne voit PAS les notes locales de A
- ✅ Utilisateur B voit uniquement ses propres notes locales
- ✅ Utilisateur B voit toutes les notes synchronisées du projet

---

### 8️⃣ **Test Suppression Note**

**Étapes** :
1. Créer une note audio
2. Attendre la synchronisation
3. Cliquer sur l'icône "🗑️" sur la note
4. Confirmer la suppression
5. Vérifier le serveur

**Résultat Attendu** :
- ✅ Note supprimée de la liste
- ✅ Note supprimée de la DB serveur
- ✅ Fichier audio supprimé du serveur
- ✅ Note disparue du Web

---

### 9️⃣ **Test Retry Logic (Échec Sync)**

**Étapes** :
1. Créer une note en mode online
2. **Arrêter temporairement le serveur PHP**
3. La note passe en `syncStatus=failed`
4. **Redémarrer le serveur**
5. Attendre ou déclencher manuellement la sync

**Résultat Attendu** :
- ✅ Note marquée comme "failed" après tentative
- ✅ Compteur `syncAttempts` incrémenté
- ✅ Retry automatique lors de la prochaine sync
- ✅ Note finalement synchronisée avec succès

---

### 🔟 **Test Tags et Importance**

**Étapes** :
1. Créer une note avec tags : "urgent, client, deadline"
2. Cocher "Marquer comme important"
3. Sauvegarder
4. Vérifier dans la liste et le détail

**Résultat Attendu** :
- ✅ Étoile ⭐ visible dans la liste
- ✅ Tags affichés dans le détail
- ✅ Tags sauvegardés en DB (format JSON ou CSV)
- ✅ Filtre/recherche par tags fonctionnel

---

## 🐛 Points de Contrôle

### Logs Android
```bash
adb logcat | grep -E "OfflineSyncManager|AddProjectNoteActivity|ProjectNotesActivity"
```

### Vérification Base de Données Locale (SQLite)
```bash
adb shell
cd /data/data/com.ptms.mobile/databases
sqlite3 ptms_offline.db
SELECT * FROM project_notes;
```

### Vérification Base de Données Serveur
```sql
SELECT * FROM project_notes ORDER BY created_at DESC LIMIT 10;
```

### Vérification Fichiers Audio Serveur
```bash
ls -lh uploads/audio_notes/{project_id}/
```

---

## 📊 Checklist de Validation

- [ ] ✅ Notes texte créées et synchronisées
- [ ] ✅ Notes audio enregistrées et uploadées
- [ ] ✅ Mode offline fonctionnel (sauvegarde locale)
- [ ] ✅ Synchronisation automatique au retour online
- [ ] ✅ Lecture audio depuis fichier local
- [ ] ✅ Lecture audio depuis serveur
- [ ] ✅ Badge "[Local]" pour notes non sync
- [ ] ✅ Filtrage par utilisateur (sécurité)
- [ ] ✅ Suppression note + fichier audio
- [ ] ✅ Retry logic en cas d'échec
- [ ] ✅ Tags et importance fonctionnels
- [ ] ✅ Interface Web compatible
- [ ] ✅ Permissions microphone gérées
- [ ] ✅ Timer d'enregistrement précis
- [ ] ✅ Aucune fuite mémoire (MediaRecorder released)

---

## 🔧 Dépannage

### Erreur "Permission microphone refusée"
- Aller dans Paramètres Android > Apps > PTMS > Permissions
- Activer "Microphone"

### Note reste en "[Local]" indéfiniment
- Vérifier la connexion réseau
- Vérifier les logs : `syncStatus` et `syncError`
- Forcer une sync manuelle si nécessaire

### Audio ne se lit pas
- Vérifier que le fichier existe (local ou serveur)
- Vérifier qu'une app de lecture audio est installée
- Tester l'URL directement dans le navigateur

### Fichier audio très volumineux
- Limite serveur : 50MB
- Format recommandé : AAC (compression optimale)
- Durée recommandée : < 10 minutes

---

## 🎉 Résultat Final Attendu

Un système **100% fonctionnel** permettant :
- 📝 Créer des notes texte, audio, dictée
- 🔄 Synchronisation automatique et intelligente
- 📱 Mode offline complet
- 🔒 Sécurité par utilisateur
- 🎵 Lecture audio locale et distante
- 🌐 Compatible Web et Android

**Bon test ! 🚀**
