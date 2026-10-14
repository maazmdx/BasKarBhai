# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path/to/sdk/tools/proguard/proguard-android.txt

# Keep OkHttp & Coil
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Accessibility Service & ViewBinding
-keep class com.baskarbhai.roast.service.** { *; }
-keep class com.baskarbhai.roast.data.** { *; }
-keep class com.baskarbhai.roast.ui.** { *; }
