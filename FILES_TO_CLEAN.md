# Fichiers obsolètes à nettoyer - appAndroid

## Backup créé
- **Fichier**: `C:\Devs\web\_backups\appAndroid_backup_20251014_234912.tar.gz`
- **Taille**: 2.3 MB
- **Date**: 2025-10-14 23:49

---

## 1. Dossier _backup_old_activities (à supprimer complètement)
**Emplacement**: `app/src/main/java/com/ptms/mobile/activities/_backup_old_activities/`

Fichiers:
- `activity_add_project_note.xml` (11 KB)
- `activity_create_note_unified_OLD.xml` (14 KB)
- `AddProjectNoteActivity.java.old` (17 KB)

**Raison**: Anciens fichiers déjà sauvegardés, ne sont plus utilisés

---

## 2. NotesActivity (Interface simplifiée - NON utilisée)
**Statut**: Interface alternative avec BottomSheet, remplacée par NotesMenuActivity (D002)

### Fichier Java:
- `app/src/main/java/com/ptms/mobile/activities/NotesActivity.java`

### Layouts associés:
- `app/src/main/res/layout/activity_notes_simple.xml`
- `app/src/main/res/layout/dialog_add_note_simple.xml`
- `app/src/main/res/layout/item_note_simple.xml`

### Référence à corriger AVANT suppression:
- **AgendaActivity.java** ligne 480: Change `NotesActivity.class` → `NotesMenuActivity.class`

---

## 3. Dossier backups dans appAndroid (à supprimer)
**Emplacement**: `appAndroid/backups/`

Contient:
- `chat_v2_backup_20251010_020355/`

**Raison**: Anciens backups internes, déjà inclus dans backup général

---

## 4. Dossier bin (généré temporairement)
**Emplacement**: `appAndroid/bin/`

**Raison**: Fichiers temporaires de build

---

## Résumé des actions

### Étape 1: Corrections de code
- [ ] Modifier `AgendaActivity.java` ligne 480: `NotesActivity.class` → `NotesMenuActivity.class`

### Étape 2: Suppression des fichiers obsolètes
- [ ] Supprimer `app/src/main/java/com/ptms/mobile/activities/_backup_old_activities/` (complet)
- [ ] Supprimer `app/src/main/java/com/ptms/mobile/activities/NotesActivity.java`
- [ ] Supprimer `app/src/main/res/layout/activity_notes_simple.xml`
- [ ] Supprimer `app/src/main/res/layout/dialog_add_note_simple.xml`
- [ ] Supprimer `app/src/main/res/layout/item_note_simple.xml`
- [ ] Supprimer `appAndroid/backups/` (complet)
- [ ] Supprimer `appAndroid/bin/` (complet)

### Étape 3: Vérification
- [ ] Compiler le projet
- [ ] Tester l'application
- [ ] Vérifier qu'aucune référence à NotesActivity ne subsiste (sauf DevModeActivity qui est documenté)

---

## Notes importantes

- **DevModeActivity** garde la référence à NotesActivity pour documentation (marqué "NON utilisée")
- **NotesMenuActivity (D002)** est maintenant l'interface principale pour les notes
- Tous les anciens fichiers sont sauvegardés dans `_backups/appAndroid_backup_20251014_234912.tar.gz`
