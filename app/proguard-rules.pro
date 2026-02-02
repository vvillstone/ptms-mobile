# ============================================================================
# ProGuard Rules pour PTMS Mobile v2.0
# ============================================================================
# Documentation: https://developer.android.com/build/shrink-code
# ============================================================================

# ==================== RÈGLES GÉNÉRALES ====================

# Conserver les annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Conserver les méthodes natives
-keepclasseswithmembernames class * {
    native <methods>;
}

# Conserver les enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Conserver Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Conserver Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==================== MODELS (GSON/JSON) ====================

# Conserver tous les models pour Gson/Retrofit
-keep class com.ptms.mobile.models.** { *; }
-keepclassmembers class com.ptms.mobile.models.** { *; }

# Règles Gson générales
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== RETROFIT & OKHTTP ====================

# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# OkHttp Platform
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ==================== API LAYER ====================

# Conserver les interfaces API
-keep interface com.ptms.mobile.api.** { *; }
-keep class com.ptms.mobile.api.ApiService { *; }
-keep class com.ptms.mobile.api.ApiClient { *; }

# Conserver les request/response classes
-keep class com.ptms.mobile.api.*Request { *; }
-keep class com.ptms.mobile.api.*Response { *; }

# ==================== JWT (JSON Web Token) ====================

-keep class io.jsonwebtoken.** { *; }
-keepnames class io.jsonwebtoken.* { *; }
-keepnames interface io.jsonwebtoken.* { *; }

-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ==================== WEBSOCKET ====================

-keep class org.java_websocket.** { *; }
-dontwarn org.java_websocket.**

# ==================== ANDROIDX & MATERIAL ====================

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# ==================== APPLICATION CLASSES ====================

# Conserver les Activities
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Conserver les méthodes onClick dans les layouts XML
-keepclassmembers class * {
    public void onClick*(...);
}

# Conserver les adapters custom
-keep class com.ptms.mobile.adapters.** { *; }

# Conserver les managers
-keep class com.ptms.mobile.managers.** { *; }
-keep class com.ptms.mobile.auth.** { *; }
-keep class com.ptms.mobile.sync.** { *; }
-keep class com.ptms.mobile.storage.** { *; }

# Conserver les utils (peuvent contenir des références réflexives)
-keep class com.ptms.mobile.utils.** { *; }

# Conserver les services
-keep class com.ptms.mobile.services.** { *; }

# Conserver les workers
-keep class com.ptms.mobile.workers.** { *; }

# ==================== DATABASE ====================

# SQLite
-keep class com.ptms.mobile.database.** { *; }
-keep class android.database.sqlite.** { *; }

# ==================== WEBSOCKET CLIENT ====================

-keep class com.ptms.mobile.websocket.** { *; }

# ==================== WIDGETS ====================

-keep class com.ptms.mobile.widgets.** { *; }

# ==================== OPTIMISATION ====================

# Optimiser le code (safe optimizations)
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Ne pas pré-vérifier (gain de vitesse de build)
-dontpreverify

# ==================== WARNINGS À IGNORER ====================

# Ignorer les warnings pour les libs tierces
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.**

# Kotlin
-dontwarn kotlin.**
-dontwarn kotlinx.**

# SLF4J (logging)
-dontwarn org.slf4j.impl.StaticLoggerBinder

# ==================== DEBUGGING ====================

# En cas de problème, décommenter pour voir les classes conservées:
# -printseeds seeds.txt
# -printusage unused.txt
# -printmapping mapping.txt

# Conserver les numéros de ligne pour les stack traces
-keepattributes SourceFile,LineNumberTable

# Renommer les fichiers sources en "SourceFile" pour masquer les vrais noms
-renamesourcefileattribute SourceFile

# ==================== SÉCURITÉ ====================

# Obfusquer tous les noms de classes/méthodes sauf ceux explicitement conservés
-repackageclasses ''

# Ne pas afficher les notes pour les classes dupliquées
-dontnote **

# ============================================================================
# FIN DES RÈGLES PROGUARD
# ============================================================================
