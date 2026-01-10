# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# AIDL interfaces - must be kept for Shizuku UserService
-keep class com.np3.reclight.IShellService { *; }
-keep class com.np3.reclight.IShellService$* { *; }

# ShellService - Shizuku UserService implementation
-keep class com.np3.reclight.shizuku.ShellService { *; }

# Keep all Binder/AIDL related classes
-keep class * extends android.os.Binder { *; }
-keep class * implements android.os.IInterface { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Glance widget classes
-keep class com.np3.reclight.widget.** { *; }
-keep class androidx.glance.** { *; }
