# Add project specific ProGuard rules here.

# WebView related
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.arena.ai.app.** {
    *;
}

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
