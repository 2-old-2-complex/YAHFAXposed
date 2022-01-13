# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/fuzion24/bin/android_sdk_home/tools/proguard/proguard-android.txt
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
# -keep public class com.android.module.xiaohongshu6_56_0.httpserver.HttpResponseData
# -dontoptimize
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }
-keep   public   class  * extends android.app.Activity
-keep public   class   *   extends   android.app.Application
-keep  public   class  *  extends  android.app.Service
-keep  public  class  *  extends  android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public  class   *  extends  android.app.backup.BackupAgentHelper
-keep   public  class * extends android.preference.Preference
