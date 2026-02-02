# GitHub Actions - PTMS Mobile

## Configuration automatique de build APK

Ce dossier contient la configuration GitHub Actions pour compiler automatiquement l'APK Android.

## Déclencheurs

Le workflow se déclenche automatiquement:
- **Push** sur `main` ou `master` (si fichiers modifiés dans `app/`)
- **Pull Request** vers `main` ou `master`
- **Manuellement** via l'interface GitHub (Actions > Build Android APK > Run workflow)
- **Tag** commençant par `v` (ex: `v2.0.1`) → crée une Release automatique

## Utilisation

### 1. Initialiser le dépôt Git

```bash
cd appAndroid
git init
git add .
git commit -m "Initial commit"
```

### 2. Créer le dépôt sur GitHub

1. Allez sur https://github.com/new
2. Créez un nouveau dépôt (ex: `ptms-mobile`)
3. Suivez les instructions pour pusher le code existant:

```bash
git remote add origin https://github.com/VOTRE_USER/ptms-mobile.git
git branch -M main
git push -u origin main
```

### 3. Récupérer l'APK

1. Allez dans l'onglet **Actions** du dépôt
2. Cliquez sur le workflow "Build Android APK"
3. Téléchargez l'artifact **PTMS-Mobile-APK**

## Configuration du déploiement automatique (optionnel)

Pour déployer automatiquement l'APK sur votre serveur:

### Secrets à configurer

Dans **Settings > Secrets and variables > Actions**, ajoutez:

| Secret | Description |
|--------|-------------|
| `SERVER_HOST` | Adresse IP ou hostname du serveur |
| `SERVER_USER` | Utilisateur SSH |
| `SERVER_SSH_KEY` | Clé privée SSH (format PEM) |

### Variables à configurer

Dans **Settings > Secrets and variables > Actions > Variables**, ajoutez:

| Variable | Valeur |
|----------|--------|
| `DEPLOY_ENABLED` | `true` |

## Créer une Release

Pour créer une release avec APK téléchargeable:

```bash
git tag v2.0.1
git push origin v2.0.1
```

Cela:
1. Déclenche le build
2. Crée une Release GitHub automatique
3. Attache l'APK à la release

## Structure du workflow

```
.github/
└── workflows/
    └── build-apk.yml    # Workflow principal
```

## Personnalisation

Modifiez `build-apk.yml` pour:
- Changer la version Java (`JAVA_VERSION`)
- Modifier le SDK Android (`ANDROID_COMPILE_SDK`)
- Ajouter des étapes de test
- Configurer la signature release
