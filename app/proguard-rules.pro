# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
# -keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
# }
# 穿山甲 SDK 核心规则
-keep class com.bytedance.sdk.** { *; }

# 因为你用到了 JavascriptInterface 桥接，建议也加上这一行，防止你的 WebAppInterface 被混淆
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}