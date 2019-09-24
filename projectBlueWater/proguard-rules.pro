# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/david/apps/android-sdks/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn org.apache.commons.**
-dontwarn com.google.**
-dontwarn org.apache.http**
-dontwarn org.joda.time**
-dontwarn ch.qos.logback**

-keep class android.arch.lifecycle.** {*;}

# okhttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepnames class * extends androidx.customview.view.AbsSavedState {
    public static final ** CREATOR;
}

-keepnames class androidx.drawerlayout.widget.DrawerLayout$SavedState { *; }

# -printseeds /home/david/sandbox/projectBlue/projectBlueWater/release/seeds.txt # print out classes that are kept
# -printusage /home/david/sandbox/projectBlue/projectBlueWater/release/usage.txt # print out classes that are obfuscated
