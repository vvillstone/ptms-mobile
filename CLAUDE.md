# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

PTMS Mobile - Android companion app for the PTMS time tracking system. Built with Java, using Retrofit/OkHttp for API communication and SQLite for offline-first data storage.

**Min SDK:** 24 (Android 7.0) | **Target SDK:** 34 | **Java:** 17

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config in gradle.properties)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.ptms.mobile.OfflineDatabaseHelperTest"

# Clean build
./gradlew clean build

# Install debug APK via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat -s PTMS:* API_CLIENT:* UnifiedSync:* AuthManager:* OfflineModeManager:*
```

Build scripts: `build_apk.bat` (Windows) / `build_apk.sh` (Linux/Mac)

## Architecture

### Offline-First with Bidirectional Sync

The app follows a **local-first architecture** where all data operations write to SQLite first, then sync to server in background:

1. **BidirectionalSyncManager** (`sync/BidirectionalSyncManager.java`) - Single source of truth for all sync operations
   - Downloads reference data (projects, work types) from server
   - Uploads pending time reports and notes
   - Conflict resolution: **Server always wins** (Master-Slave pattern)
   - Sync types: `FULL`, `UPLOAD_ONLY`, `DOWNLOAD_ONLY`

2. **OfflineDatabaseHelper** (`database/OfflineDatabaseHelper.java`) - SQLite wrapper
   - Tables: `projects`, `work_types`, `time_reports`, `project_notes`, `note_types`
   - Tracks sync status: `pending`, `synced`, `failed`
   - In-memory cache with 5-minute validity for performance

3. **OfflineModeManager** (`managers/OfflineModeManager.java`) - Connection state machine
   - States: `ONLINE`, `OFFLINE`, `SYNCING`, `UNKNOWN`
   - Auto-detects connectivity via `ServerHealthCheck`
   - Triggers sync on reconnection

### Authentication

**AuthenticationManager** (`auth/AuthenticationManager.java`) - Unified auth singleton that merges:
- `SessionManager` (active session)
- `ptms_prefs` SharedPreferences (persistent token/user data)
- `InitialAuthManager` (first-launch auth)

Key methods:
- `isLoggedIn()` - Checks session OR stored token
- `saveLoginData(token, employee)` - Saves to both SessionManager and SharedPreferences
- `canUseOffline()` - Checks if offline login is available
- `validateOfflineCredentials()` - SHA-256 hash comparison for offline auth

### API Layer

**ApiClient** (`api/ApiClient.java`) - Retrofit singleton with:
- OkHttp caching via `CacheManager`/`CacheInterceptor`
- Auto Bearer token formatting
- Configurable SSL bypass for development
- Debug logging toggle via `SettingsManager`

**ApiService** (`api/ApiService.java`) - Retrofit interface defining all endpoints:
- Auth: `POST login.php`
- Data: `GET projects.php`, `GET work-types.php`, `GET reports.php`
- Time entry: `POST time-entry.php`
- Chat: `chat-rooms.php`, `chat-messages.php`, `chat-send.php`
- Notes: `project-notes.php` (multipart for media upload)

### Key Singletons

| Class | Access Pattern | Purpose |
|-------|----------------|---------|
| `ApiClient` | `getInstance(context)` | HTTP client |
| `AuthenticationManager` | `getInstance(context)` | Auth state |
| `OfflineModeManager` | `getInstance(context)` | Connection state |
| `BidirectionalSyncManager` | `new BidirectionalSyncManager(context)` | Sync operations |

## Key Conventions

1. **Sync Status** - Use `"pending"`, `"synced"`, `"failed"` strings (not booleans)
2. **Date Formats** - Use `Locale.US` for ISO dates (`yyyy-MM-dd HH:mm:ss`) to prevent locale crashes
3. **User Types** - Integer constants: ADMIN=1, MANAGER=2, ACCOUNTANT=3, EMPLOYEE=4, VIEWER=5
4. **SharedPreferences** - Main prefs file: `"ptms_prefs"`, sync prefs: `"unified_sync_prefs"`
5. **Logging Tags** - Consistent tags: `API_CLIENT`, `AuthManager`, `UnifiedSync`, `OfflineModeManager`

## Testing

Tests use Robolectric (SDK 28) with JUnit 4:

```java
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MyTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }
}
```

Test files: `app/src/test/java/com/ptms/mobile/`
- `OfflineDatabaseHelperTest.java` - SQLite CRUD operations
- `OfflineSyncManagerTest.java` - Sync logic
- `AuthenticationManagerTest.java` - Auth flows (requires SessionManager mocking)

## Package Structure

```
com.ptms.mobile/
├── activities/      # UI screens (30+ activities)
├── adapters/        # RecyclerView adapters
├── api/             # ApiClient, ApiService, CacheManager
├── auth/            # AuthenticationManager, TokenManager, InitialAuthManager
├── database/        # OfflineDatabaseHelper (SQLite)
├── managers/        # OfflineModeManager
├── models/          # Data classes (Employee, Project, TimeReport, etc.)
├── services/        # Background services (AutoSyncService, TimerService, FCM)
├── storage/         # MediaStorageManager
├── sync/            # BidirectionalSyncManager, SyncStateManager
├── utils/           # NetworkUtils, SessionManager, SettingsManager
├── websocket/       # WebSocketChatClient
├── widgets/         # FloatingTimerWidget
└── workers/         # WorkManager tasks (CacheCleanupWorker, MediaUploadWorker)
```

## Backend API

Base URL configured via `SettingsManager.getServerUrl()` (default: `https://serveralpha.protti.group/api/`)

All authenticated endpoints require `Authorization: Bearer <token>` header.

## CI/CD - GitHub Actions

Le projet inclut une configuration GitHub Actions pour compiler automatiquement l'APK.

### Fichiers
- `.github/workflows/build-apk.yml` - Workflow de build
- `.github/README.md` - Documentation CI/CD

### Déclencheurs
- Push sur `main`/`master`
- Pull Request
- Manuel (workflow_dispatch)
- Tags `v*` → crée une Release

### Utilisation rapide

```bash
# Initialiser git et pusher vers GitHub
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/USER/ptms-mobile.git
git push -u origin main

# L'APK sera disponible dans Actions > Artifacts
```

### Déploiement auto (optionnel)

Configurer dans GitHub:
- **Secrets:** `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`
- **Variables:** `DEPLOY_ENABLED=true`
