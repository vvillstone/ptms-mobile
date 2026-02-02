# Guide de Configuration - Application Android PTMS

## 📱 Configuration de l'URL du serveur

### ✅ Format simplifié

Tu peux maintenant entrer **juste l'adresse IP ou le domaine** dans les paramètres.

L'application ajoutera automatiquement :
- Le protocole (`https://` par défaut)
- Le chemin de l'API (`/api/`)

### Exemples d'entrées acceptées :

| Tu entres | L'app utilise |
|-----------|---------------|
| `192.168.188.28` | `https://192.168.188.28/api/` |
| `http://192.168.188.28` | `http://192.168.188.28/api/` |
| `https://192.168.188.28` | `https://192.168.188.28/api/` |
| `serveralpha.protti.group` | `https://serveralpha.protti.group/api/` |

### 📝 Étapes de configuration

1. **Ouvre l'application** et clique sur l'icône ⚙️ (paramètres)

2. **Entre l'adresse de ton serveur** :
   - Pour serveur local : `192.168.188.28`
   - Pour serveur distant : `serveralpha.protti.group`

3. **Vérifie le preview** :
   - L'URL complète s'affiche en dessous en bleu
   - Exemple : "URL complète: https://192.168.188.28/api/"

4. **Active "Ignorer SSL"** si tu utilises HTTPS avec un certificat auto-signé

5. **Clique sur "Tester la connexion"** (pas "Tester l'URL de base")
   - ✅ Devrait retourner code 400 ou 200 (normal, on teste juste la connexion)
   - ❌ Si erreur SSL → Active "Ignorer SSL"
   - ❌ Si timeout → Augmente le timeout
   - ❌ Si serveur non accessible → Vérifie l'IP/domaine

6. **Sauvegarde** les paramètres

## 🔐 Création d'un utilisateur de test

### Via SQL (méthode recommandée)

Exécute ce SQL dans phpMyAdmin ou MySQL Workbench :

```sql
INSERT INTO users (username, email, password, firstname, lastname, type, status)
VALUES (
    'testemploye',
    'test@ptms.local',
    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Test',
    'Employé',
    4,
    1
);
```

**Identifiants de connexion :**
- **Username** : `testemploye`
- **Email** : `test@ptms.local`
- **Password** : `test123`

Tu peux utiliser **soit le username soit l'email** pour te connecter !

## 🚀 Compilation et installation

```bash
cd C:\devs\web\appAndroid
gradlew.bat assembleDebug
```

L'APK sera dans :
`app\build\outputs\apk\debug\app-debug.apk`

**Installation :**
- **Via câble USB** : `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- **Via partage** : Copie l'APK sur le téléphone et installe manuellement

## 🐛 Diagnostic des erreurs

### Erreur 401 (Non autorisé)
- ❌ Email ou username incorrect
- ❌ Mot de passe incorrect
- ❌ L'utilisateur n'existe pas dans la base
- ❌ Type d'utilisateur incorrect (doit être 2 ou 4)

### Erreur 403 (Accès interdit)
- ✅ **Si "Tester l'URL de base"** → C'est normal !
- ❌ **Si "Tester la connexion"** → Vérifier fichier `.htaccess` dans `/api/`

### Erreur 404 (Non trouvé)
- ❌ Endpoint `/api/login.php` n'existe pas
- ❌ URL mal configurée

### Erreur SSL
- ✅ Active "Ignorer SSL" dans les paramètres

### Timeout
- ✅ Augmente le timeout (30 → 60 secondes)
- ❌ Serveur trop lent ou inaccessible

## 📊 Logs du serveur

Les logs se trouvent dans le fichier d'erreur PHP de ton serveur web.

Regarde les logs après une tentative de connexion pour voir :
- `=== LOGIN API DEBUG ===`
- `Raw input: {...}`
- `Recherche utilisateur: ...`
- `Utilisateur trouvé: OUI/NON`

## ✅ Checklist avant de tester

- [ ] Serveur web démarré (Apache/Nginx)
- [ ] MySQL démarré
- [ ] Fichier `.htaccess` dans `/api/` créé
- [ ] Utilisateur créé dans la base de données
- [ ] URL configurée dans l'app (juste l'IP)
- [ ] "Ignorer SSL" activé (si HTTPS auto-signé)
- [ ] Test de connexion réussi (code 400 ou 200)
- [ ] APK recompilé et réinstallé

## 🎯 Valeurs par défaut

- **URL** : `192.168.188.28`
- **Protocole** : `https://` (ajouté automatiquement)
- **Chemin API** : `/api/` (ajouté automatiquement)
- **Ignorer SSL** : `Activé` par défaut
- **Timeout** : `30 secondes`
