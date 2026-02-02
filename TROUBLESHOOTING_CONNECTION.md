# Guide de Dépannage - Problèmes de Connexion PTMS Mobile

## Problème : Échec de Connexion/Login

### Symptômes Courants
- L'application ne parvient pas à se connecter au serveur
- Message d'erreur "Erreur réseau" ou "Serveur non accessible"
- Message "Serveur retourne HTML au lieu de JSON"
- Timeout lors de la connexion

### Causes Possibles

#### 1. URL du Serveur Incorrecte

L'URL par défaut dans l'application est `https://192.168.188.28/api/`

**URLs Candidates à Tester:**
- Production: `https://serveralpha.protti.group/api/`
- Réseau local: `https://192.168.188.28/api/`
- Alternative HTTP: `http://192.168.188.28/api/`
- Serveur alternatif: `https://192.168.188.21/api/`

#### 2. Certificat SSL Non Valide

Si vous utilisez HTTPS avec un certificat autosigné, vous devez activer "Ignorer SSL" dans les paramètres.

#### 3. Endpoint API Incorrect

L'application cherche les endpoints suivants:
- `/api/login.php`
- `/api/projects.php`
- `/api/work-types.php`
- `/api/time-entry.php`
- `/api/reports.php`
- `/api/profile.php`

### Solution : Configuration de l'URL

#### Méthode 1: Via l'Interface de l'Application

1. **Ouvrir les Paramètres**
   - Sur l'écran de connexion, appuyez sur le bouton "Paramètres" (icône d'engrenage)

2. **Configurer l'URL du Serveur**
   - Saisissez l'URL du serveur dans le champ "URL du Serveur"
   - Format requis: `https://domaine.com/api/` ou `http://ip.address/api/`
   - **Important**: L'URL doit se terminer par `/`

3. **Configuration SSL**
   - Si le serveur utilise HTTPS avec un certificat autosigné, cochez "Ignorer SSL"
   - En production avec un certificat valide, décochez cette option

4. **Ajuster le Timeout**
   - Par défaut: 30 secondes
   - Si le serveur est lent, augmentez à 60 secondes

5. **Tester la Connexion**
   - Appuyez sur "Test Connexion" pour vérifier `login.php`
   - Appuyez sur "Test URL de Base" pour vérifier l'accessibilité du serveur

6. **Sauvegarder**
   - Appuyez sur "Enregistrer" pour sauvegarder les paramètres
   - Retournez à l'écran de connexion et réessayez

#### Méthode 2: Modification du Code Source

Si vous devez changer l'URL par défaut de manière permanente:

**Fichier**: `appAndroid/app/src/main/java/com/ptms/mobile/utils/SettingsManager.java`

```java
// Ligne 18 - Changer l'URL par défaut
private static final String DEFAULT_SERVER_URL = "https://serveralpha.protti.group/api/";
```

**Fichier**: `appAndroid/app/src/main/java/com/ptms/mobile/utils/ApiConfig.java`

```java
// Ligne 8 - Changer l'URL de fallback
public static final String BASE_URL = "https://serveralpha.protti.group/api/";
```

Après modification, recompilez l'application:
```bash
cd appAndroid
.\build_apk.bat
```

### Vérification Côté Serveur

#### Structure Attendue par l'Application

**Endpoint**: `/api/login.php`

**Requête POST:**
```json
{
  "email": "utilisateur@exemple.com",
  "password": "motdepasse"
}
```

**Réponse Attendue (Succès):**
```json
{
  "success": true,
  "message": "Connexion réussie",
  "token": "base64_encoded_token",
  "user": {
    "id": 1,
    "email": "utilisateur@exemple.com",
    "firstname": "Prénom",
    "lastname": "Nom",
    "department": "Département",
    "position": "Poste"
  }
}
```

**Réponse Attendue (Échec):**
```json
{
  "success": false,
  "message": "Email ou mot de passe incorrect"
}
```

### Diagnostics Avancés

#### Test de Connectivité Réseau

1. **Vérifier l'Accessibilité du Serveur**
   ```
   ping serveralpha.protti.group
   ou
   ping 192.168.188.28
   ```

2. **Tester l'Endpoint depuis un Navigateur**
   - Ouvrez: `https://serveralpha.protti.group/api/login.php`
   - Vous devriez voir une réponse JSON (même si c'est une erreur)
   - Si vous voyez du HTML, l'endpoint n'existe pas ou est mal configuré

3. **Tester avec cURL**
   ```bash
   curl -X POST https://serveralpha.protti.group/api/login.php \
     -H "Content-Type: application/json" \
     -d '{"email":"test@exemple.com","password":"test"}'
   ```

#### Analyser les Logs

**Android Studio Logcat:**
```
adb logcat | grep -E "LOGIN|API_CLIENT|SETTINGS"
```

Les logs montreront:
- L'URL utilisée pour la connexion
- Les codes de réponse HTTP
- Les messages d'erreur détaillés
- Le format de la réponse (HTML vs JSON)

### URLs de Serveur par Environnement

| Environnement | URL | Ignorer SSL | Notes |
|--------------|-----|-------------|-------|
| **Production** | `https://serveralpha.protti.group/api/` | Non | Certificat valide requis |
| **Développement Local** | `https://192.168.188.28/api/` | Oui | Réseau local uniquement |
| **Développement HTTP** | `http://192.168.188.28/api/` | N/A | Non sécurisé, développement seulement |

### Problèmes Spécifiques

#### Erreur: "Serveur retourne HTML au lieu de JSON"

**Cause:** L'endpoint n'existe pas ou retourne une page d'erreur

**Solutions:**
1. Vérifiez que le fichier `/api/login.php` existe sur le serveur
2. Vérifiez les permissions du fichier (lecture pour le serveur web)
3. Vérifiez les règles de réécriture Apache/Nginx
4. Testez l'endpoint directement dans un navigateur

#### Erreur: "Erreur SSL"

**Cause:** Certificat SSL invalide ou autosigné

**Solutions:**
1. Activez "Ignorer SSL" dans les paramètres de l'application
2. Ou installez un certificat SSL valide sur le serveur

#### Erreur: "Timeout"

**Cause:** Le serveur est trop lent ou inaccessible

**Solutions:**
1. Augmentez le timeout dans les paramètres (60 secondes)
2. Vérifiez la connexion réseau
3. Vérifiez que le serveur est en ligne

#### Erreur: "UnknownHostException"

**Cause:** Le nom de domaine ne peut pas être résolu

**Solutions:**
1. Vérifiez que le domaine existe et est accessible
2. Vérifiez votre connexion internet
3. Essayez avec une adresse IP directement

### Checklist de Dépannage

- [ ] L'URL du serveur est correcte et se termine par `/`
- [ ] Le serveur est accessible (ping, navigateur)
- [ ] L'endpoint `/api/login.php` existe et est accessible
- [ ] "Ignorer SSL" est activé si nécessaire
- [ ] Le timeout est suffisant (30-60 secondes)
- [ ] Le téléphone est connecté au même réseau (si IP locale)
- [ ] Les credentials de test sont valides
- [ ] Les logs Android montrent des détails de l'erreur

### Support

Si le problème persiste après avoir suivi ce guide:

1. Activez le "Mode Debug" dans les paramètres
2. Collectez les logs via `adb logcat`
3. Testez l'endpoint avec cURL ou Postman
4. Vérifiez la configuration côté serveur

## URLs de Test Recommandées

Pour un test rapide, essayez ces URLs dans l'ordre:

1. `https://serveralpha.protti.group/api/` (Production)
2. `https://192.168.188.28/api/` (Local HTTPS)
3. `http://192.168.188.28/api/` (Local HTTP)

Utilisez les boutons de test dans les paramètres pour valider chaque URL avant de sauvegarder.


