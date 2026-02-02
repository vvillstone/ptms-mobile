# Build Instructions - PTMS Android App

## Security Fixes Applied - January 2025

All security fixes and chat enhancements have been successfully implemented. See `SECURITY_FIXES_2025_01.md` for full details.

---

## Building the APK

### Option 1: Command Line Build

**Debug Build**:
```bash
cd C:\Devs\web\appAndroid
gradlew.bat assembleDebug
```

**Release Build**:
```bash
cd C:\Devs\web\appAndroid
gradlew.bat assembleRelease
```

### Option 2: Android Studio

1. Open Android Studio
2. Open project: `C:\Devs\web\appAndroid`
3. Build → Build Bundle(s) / APK(s) → Build APK(s)

---

## APK Output Location

**Automatic Copy** (configured in build.gradle):
```
C:\Devs\web\uploads\apk\PTMS-Mobile-v2.0-debug-{timestamp}.apk
```

**Default Gradle Output**:
```
C:\Devs\web\appAndroid\app\build\outputs\apk\debug\
```

---

## Testing the Security Fixes

### 1. Test Debug Mode Toggle

**Test Debug OFF** (default):
```
1. Install APK
2. Open app → Settings
3. Verify "Mode débogage" is unchecked
4. Use app, make API calls
5. Check logcat: adb logcat | grep "API_CLIENT"
6. Verify: "Mode production - Logging HTTP désactivé"
7. Verify: No HTTP request/response bodies in logs
```

**Test Debug ON**:
```
1. Open app → Settings
2. Check ✅ "Mode débogage"
3. Click "Sauvegarder"
4. Use app, make API calls
5. Check logcat: adb logcat | grep "API_CLIENT"
6. Verify: "Mode debug activé - Logging HTTP BODY"
7. Verify: HTTP request/response bodies appear in logs
```

### 2. Test Server URL Configuration

**Test URL Change**:
```
1. Open app → Settings
2. Enter new server IP: e.g., "192.168.1.100"
3. Verify "URL complète" preview updates
4. Click "Tester la connexion"
5. Verify connection test uses new URL
6. Click "Sauvegarder"
7. Try login with new server
8. Restart app
9. Verify server URL persisted
```

### 3. Test SSL Toggle

**Test with Self-Signed Certificate**:
```
1. Connect to server with self-signed SSL cert
2. If SSL enabled → connection fails (expected)
3. Settings → Check ✅ "Ignorer les certificats SSL"
4. Click "Sauvegarder"
5. Try connection again
6. Verify: Connection successful
```

### 4. Test Chat Enable/Disable

**Test Chat Disabled**:
```
1. Open app → Settings
2. Scroll to "Paramètres du Chat"
3. Uncheck ☐ "💬 Activer le Chat"
4. Click "Sauvegarder"
5. Navigate to Chat
6. Open a conversation
7. Verify:
   - Message input disabled (grayed out)
   - Hint: "💬 Chat désactivé - Mode lecture seule"
   - Send button disabled (dimmed, alpha 0.5)
   - Attach button disabled (dimmed, alpha 0.5)
   - Toast: "Chat en mode lecture seule..."
8. Try typing → verify input disabled
9. Verify old messages still visible
10. Check logcat: Verify "Chat désactivé - Polling non démarré"
```

**Test Chat Re-enabled**:
```
1. Settings → Check ✅ "💬 Activer le Chat"
2. Click "Sauvegarder"
3. Open chat conversation
4. Verify:
   - Message input enabled
   - Send button enabled
   - Attach button enabled
   - Can type and send messages
   - Polling active (check logcat)
```

---

## Modified Files Checklist

Verify all files have been modified correctly:

- [x] `app/src/main/java/com/ptms/mobile/api/ApiClient.java`
  - HTTP logging conditional on debug mode

- [x] `app/src/main/java/com/ptms/mobile/utils/SettingsManager.java`
  - Added `isChatEnabled()` / `setChatEnabled()`
  - Added `KEY_CHAT_ENABLED` and `DEFAULT_CHAT_ENABLED`

- [x] `app/src/main/res/layout/activity_settings_simple.xml`
  - Added chat enable/disable checkbox (`cb_enable_chat`)

- [x] `app/src/main/java/com/ptms/mobile/activities/AppSettingsActivity.java`
  - Added checkbox handling
  - Added ApiClient reconfiguration trigger

- [x] `app/src/main/java/com/ptms/mobile/activities/ChatActivity.java`
  - Added `setupChatMode()` method
  - Added chat enabled check in `sendMessage()`
  - Added chat enabled check in `startPolling()`

---

## Compilation Verification

**Before building**, verify no compilation errors:

1. Open project in Android Studio
2. Build → Clean Project
3. Build → Rebuild Project
4. Check "Build" output for errors
5. If errors found → review modified files

**Common Issues**:
- Missing imports → Android Studio auto-fix (Alt+Enter)
- Syntax errors → Review code carefully
- Missing resources → Check XML files

---

## Deployment Checklist

### Before Deploying to Production

- [ ] Test all security fixes (see above)
- [ ] Test on multiple devices (Android 7.0+)
- [ ] Test with production server URL
- [ ] Verify SSL enabled for production
- [ ] Verify debug mode OFF by default
- [ ] Test chat enable/disable functionality
- [ ] Check crash reports (if any)
- [ ] Review ProGuard configuration
- [ ] Sign APK with release keystore
- [ ] Test release build on device

### Production Settings Recommendations

**For Users**:
```
Server URL: [Your production server]
SSL Verification: ✅ ON (uncheck "Ignorer les certificats SSL")
Timeout: 30 seconds
Debug Mode: ☐ OFF
Chat: ✅ ON (unless saving battery)
Chat Polling: ✅ ON
Chat Version: V1 (Polling) - more stable
```

---

## Troubleshooting

### Build Fails

**Issue**: Gradle build fails
**Solution**:
```bash
cd C:\Devs\web\appAndroid
gradlew.bat clean
gradlew.bat assembleDebug
```

### APK Not Generated

**Issue**: APK file missing after build
**Solution**:
1. Check build output for errors
2. Look in: `app/build/outputs/apk/debug/`
3. Check auto-copy target: `C:\Devs\web\uploads\apk\`

### Settings Not Persisting

**Issue**: Settings reset after app restart
**Solution**:
- Verify SharedPreferences write permission
- Check for app data clear
- Review SettingsManager code

### Chat Still Active When Disabled

**Issue**: Chat polling continues after disabling
**Solution**:
- Restart app completely
- Check SettingsManager.isChatEnabled()
- Verify checkbox state saved

---

## Support

**Issues**: Check `SECURITY_FIXES_2025_01.md` for known limitations
**Documentation**: See `CLAUDE.md` for project overview
**Questions**: Contact development team

---

**Last Updated**: January 2025
**Version**: 2.0 with Security Fixes
**Status**: ✅ Ready to build and test
