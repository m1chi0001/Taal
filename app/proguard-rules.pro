# ProGuard rules for WSEI Plan
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault
-dontwarn org.jsoup.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlinx.atomicfu.**
-dontwarn java.lang.management.**
-dontwarn java.nio.file.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn android.app.Notification$ProgressStyle**

# Kotlinx Serialization (DTOs, domain models, encrypted credentials, kotlinx-datetime, enums)
-keep,includedescriptorclasses class pl.lublin.wsei.plan.**$$serializer { *; }
-keepclassmembers class pl.lublin.wsei.plan.** { *** Companion; }
-keepclasseswithmembers class pl.lublin.wsei.plan.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class kotlinx.datetime.serializers.** { *; }
-keepclassmembers enum pl.lublin.wsei.plan.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Domain & Remote layers (Ktor, ABP DTOs, HTML parser models)
-keep class pl.lublin.wsei.plan.data.remote.** { *; }
-keep class pl.lublin.wsei.plan.domain.model.** { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# Data layer: DTOs, lenient serializers, Room entities/DAOs, snapshots, credentials
-keep class pl.lublin.wsei.plan.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Jsoup HTML fallback parser
-keep class org.jsoup.** { *; }

# Live Notifications, Foreground Services, Alarm Receivers, and Hilt WorkManager Workers
-keep class pl.lublin.wsei.plan.service.** { *; }
