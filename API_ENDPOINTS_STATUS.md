# État des endpoints API - Android ↔ Serveur

## ✅ Endpoints fonctionnels

| Endpoint Android | Fichier Serveur | Status |
|------------------|-----------------|--------|
| `login.php` | `/api/login.php` | ✅ OK |
| `projects.php` | `/api/projects.php` | ✅ OK |
| `work-types.php` | `/api/work-types.php` | ✅ OK |
| `time-entry.php` | `/api/time-entry.php` | ✅ OK |
| `reports.php` | `/api/reports.php` | ✅ OK |
| `profile.php` | `/api/profile.php` | ✅ OK |

## ✅ Endpoints Chat (Créés!)

| Endpoint Android | Fichier Serveur | Status |
|------------------|-----------------|--------|
| `chat-rooms.php` | `/api/chat-rooms.php` | ✅ CRÉÉ |
| `chat-messages.php` | `/api/chat-messages.php` | ✅ CRÉÉ |
| `chat-send.php` | `/api/chat-send.php` | ✅ CRÉÉ |
| `chat-users.php` | `/api/chat-users.php` | ✅ CRÉÉ |
| `chat-typing.php` | `/api/chat-typing.php` | ✅ CRÉÉ |
| `chat-mark-read.php` | `/api/chat-mark-read.php` | ✅ CRÉÉ |
| `chat/conversations` | **Note:** Endpoint différent, non utilisé | ℹ️ Optionnel |

## ℹ️ Endpoints obsolètes

| Endpoint | Note |
|----------|------|
| `system/status` | Endpoint non implémenté côté serveur |
| `system/search` | Endpoint non implémenté côté serveur |

## 🔧 Configuration URL

**Format simplifié** :
- Tu entres : `192.168.188.28`
- L'app utilise : `https://192.168.188.28/api/`

**Normalisation automatique** :
- `192.168.188.28` → `https://192.168.188.28/api/`
- `http://192.168.188.28` → `http://192.168.188.28/api/`
- `serveralpha.protti.group` → `https://serveralpha.protti.group/api/`

## 📝 Notes importantes

1. **Login fonctionne** : L'endpoint `login.php` est opérationnel
2. **Chat non fonctionnel** : Les endpoints de chat n'existent pas côté serveur
3. **Gestion de projet OK** : Tous les endpoints essentiels (projets, rapports, temps) fonctionnent
4. **Les tests utilisent maintenant l'URL normalisée** ✅

## 🎯 Priorités

1. ✅ **Login** - Fonctionnel
2. ✅ **Time Entry** - Fonctionnel
3. ✅ **Reports** - Fonctionnel
4. ❌ **Chat** - Nécessite création des endpoints serveur
