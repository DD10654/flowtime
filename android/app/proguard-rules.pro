# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keep,includedescriptorclasses class com.flowtime.app.**$$serializer { *; }
-keepclassmembers class com.flowtime.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.flowtime.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
