# Keep stacktraces readeable.
-keepattributes SourceFile,LineNumberTable

# ROADMAP 11.9: Moshi's AdapterMethodsFactory validates @FromJson/@ToJson methods via
# Method.getGenericParameterTypes() and requires delegate parameters to be ParameterizedType
# with raw type JsonAdapter (e.g. ProxerLibJava's FixRatingDetailsAdapter.fromJson(
# JsonReader, JsonAdapter<RatingDetails>)). R8 obfuscation mangles the generic signature,
# so release builds crashed on login with "Unexpected signature for ... fromJson(...)".
# Keep Moshi and the lib's adapter classes un-obfuscated so their signatures survive intact.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.squareup.moshi.** { *; }
-keep class me.proxer.library.internal.adapter.** { *; }

# ROADMAP 11.9 (part 2): R8 obfuscation of ProxerLibJava broke ProxerApi construction on
# release builds (messageless ClassCastException inside UsersApi.<init> chain at login,
# reproduced on emulator with -dontoptimize still crashing -> renaming, not merging, is the
# culprit). Keep the whole lib un-obfuscated.
-keep class me.proxer.library.** { *; }

# Keep essential support library class.
-keep,allowoptimization class androidx.core.app.CoreComponentFactory { *; }

# Keep fields in R which are accessed through reflection.
-keepclasseswithmembers,allowoptimization class **.R$* {
    public static final int define_*;
}

# Remove all kinds of logging.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Material Preference
-keepclassmembers,allowoptimization class androidx.preference.PreferenceManager {
    void setNoCommit(boolean);
}

# Cast
-keepclasseswithmembers,allowoptimization class androidx.mediarouter.app.MediaRouteActionProvider {
    public <init>(...);
}

# Fix for Community-Material-Typeface
-keepclassmembers,allowoptimization enum com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial$Icon* { *; }

# TODO: Remove once it works without.
# Workaround crash in Google Cast.
-keep,allowoptimization class com.google.android.gms.internal.cast.** { *; }

# Ignore warnings about specific classes not being available on Android JDK.
-dontwarn java.util.concurrent.Flow$*

# TODO: Remove once next stable version is released.
# Generic Moshi rules.
-keepnames @com.squareup.moshi.JsonClass class *
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers @com.squareup.moshi.JsonClass @kotlin.Metadata class * {
    synthetic <init>(...);
}

-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*
-keep class <1>_<2>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*$*
-keep class <1>_<2>_<3>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*$*$*
-keep class <1>_<2>_<3>_<4>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*$*$*$*
-keep class <1>_<2>_<3>_<4>_<5>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*$*$*$*$*
-keep class <1>_<2>_<3>_<4>_<5>_<6>JsonAdapter {
    <init>(...);
    <fields>;
}

# Keep raw resources that are looked up via Resources.getIdentifier at runtime
# (aboutlibraries.json in the license list).
-keep class **.R$raw { *; }
