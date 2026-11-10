# ProGuard rules for WSEI Plan
-keepattributes *Annotation*,InnerClasses,Signature
-dontwarn org.jsoup.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn java.lang.management.**
-dontwarn javax.annotation.**
-keep,includedescriptorclasses class pl.lublin.wsei.plan.**$$serializer { *; }
-keepclassmembers class pl.lublin.wsei.plan.** { *** Companion; }
-keepclasseswithmembers class pl.lublin.wsei.plan.** { kotlinx.serialization.KSerializer serializer(...); }
-keep class pl.lublin.wsei.plan.data.remote.** { *; }
-keep class pl.lublin.wsei.plan.domain.model.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# Data layer: DTOs, lenient serializers, Room entities serialized into snapshots
-keep class pl.lublin.wsei.plan.data.** { *; }
-keep class org.jsoup.** { *; }
