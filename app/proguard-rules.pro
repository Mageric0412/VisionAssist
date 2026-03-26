# VisionAssist ProGuard Rules

# TensorFlow Lite
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# HMS
-keep class com.huawei.** { *; }
-dontwarn com.huawei.**

# Kotlin
-keepattributes *Annotation*
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
