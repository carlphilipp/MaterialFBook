# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/jake/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class me.zeeroooo.materialfb.webview.JavaScriptInterfaces {
   public *;
}

# Local fun
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keeppackagenames org.jsoup.nodes

-keep class android.support.v7.widget.SearchView { *; }

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

-keep,includedescriptorclasses class com.github.clans.fab.FloatingActionMenu { *; }
-keep,includedescriptorclasses class me.zeeroooo.materialfb.webview.MFBWebView { *; }
-keep,includedescriptorclasses class com.github.chrisbanes.photoview.PhotoView { *; }

-dontnote kotlin.internal.PlatformImplementationsKt
-dontnote kotlin.reflect.jvm.internal.ReflectionFactoryImpl
-dontnote kotlin.internal.PlatformImplementationsKt
-dontnote com.bumptech.glide.Glide
