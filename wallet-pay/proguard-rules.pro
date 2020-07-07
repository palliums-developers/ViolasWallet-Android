# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepclassmembers,includecode class **DTO{<fields>;}
-keepclassmembers,includecode class **DTO$**{<fields>;}

-keepclassmembers,includecode class com.smallraw.support.switchcompat.SwitchButton{
    void setProgress(float);
    float getProgress();
}

-keep class admission_control.*** {*;}
-keep class admission_control.***$*** {*;}
-keep class mempool_status.*** {*;}
-keep class mempool_status.***$*** {*;}
-keep class types.*** {*;}
-keep class types.***$*** {*;}

# ViewModel
-keepnames class androidx.lifecycle.ViewModel
-keepclassmembers public class * extends androidx.lifecycle.ViewModel { public <init>(...); }

# Scan
-keep class cn.bertsir.zbar.Qr.**{*;}

# EventBus
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# wallet connect
#-keep class com.violas.walletconnect.models.*{*;}
#-keep class com.violas.walletconnect.models.*$Companion{*;}
#-keepclassmembers class **.WC**{
#    public ** component1();
#    <fields>;}
#-keepclassmembers class **.WC**$Companion{
#    public ** component1();
#    <fields>;}
-keep class com.violas.walletconnect.***{*;}
-keep class com.violas.walletconnect.***$Companion{*;}
#-keep class com.violas.walletconnect.session.**{*;}
#-keep class com.violas.walletconnect.session.**$Companion{*;}