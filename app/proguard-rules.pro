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
# ========================== 全局选项 ==========================
# 注：R8 会忽略 -optimizationpasses / -dontpreverify / -dontskipnonpubliclibraryclasses /
#     -useuniqueclassmembernames / -printconfiguration（无参形式）。仅保留对 R8 有效的项。
-verbose                                                                        # 输出详细日志，便于定位 R8 报错
-ignorewarnings                                                                 # 忽略 dontwarn 之外的零散警告
-dontusemixedcaseclassnames                                                     # 混淆后类名只用小写，避免在大小写不敏感文件系统上冲突
-printmapping mapping.txt                                                       # 输出符号映射文件，便于线上崩溃还原
-allowaccessmodification                                                        # 允许 R8 调整访问修饰符以支持更激进的内联/合并
-renamesourcefileattribute SourceFile                                           # 将源文件名替换为 SourceFile，配合 LineNumberTable 还原崩溃栈
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*        # 关闭过激算法：保留字段名/算术化简、避免类合并影响反射
# 必要的属性：注解（含运行时注解）、内部类签名、泛型签名、行号、异常表
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions

# 指定外部模糊字典
-obfuscationdictionary ./dictionary
# 指定class模糊字典
-classobfuscationdictionary ./dictionary
# 指定package模糊字典
-packageobfuscationdictionary ./dictionary

# 保留指定类不被混淆
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

#保留四大组件相关
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

# 保留support下的所有类及其内部类
-dontwarn android.support.**
-keep class android.support.* {*;}

# 保留support下的类的继承类及其内部类
-keep public class * extends android.support.v4.*
-keep public class * extends android.support.v7.*
-keep public class * extends android.support.annotation.*

# 保留androidx相关
-dontwarn androidx.**
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-keep,allowobfuscation @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class *
-keepclassmembers class * { @androidx.annotation.Keep *; }

# 不混淆资源类
-keep class **.R$* {*;}
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留方法参数是view的方法，使@OnClick等不会被影响
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keepclassmembers class * extends androidx.fragment.app.Fragment {
   public void *(android.view.View);
}

# 对于带有回调函数的onXXEvent、**On*Listener的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
    void *(**On*Listener);
}

#保留自定义控件（继承自 View）不被混淆
-keep public class * extends android.view.View{
  *** get*();
  void set*(***);
  public <init>(android.content.Context);
  public <init>(android.content.Context, android.util.AttributeSet);
  public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembers class * {
  public <init>(android.content.Context, android.util.AttributeSet);
  public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 不混淆枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留Parcelable序列化类不被混淆
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留Serializable序列化的类不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 避免Log打印输出
-assumenosideeffects class android.util.Log {
   public static *** v(...);
   public static *** d(...);
   public static *** i(...);
   public static *** w(...);
}

#不混淆js接口
-keepattributes *JavascriptInterface*
-keepclassmembers class * extends android.webkit.WebViewClient {
  public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
  public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebViewClient {
  public void *(android.webkit.WebView, java.lang.String);
}

#----------------------------- kotlinx.coroutines ---------------------------------
# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
# Same story for the standard library's SafeContinuation that also uses AtomicReferenceFieldUpdater
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
# These classes are only required by kotlinx.coroutines.debug.AgentPremain, which is only loaded when
# kotlinx-coroutines-core is used as a Java agent, so these are not needed in contexts where ProGuard is used.
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal
##----------------------------------------- 第三方依赖库 --------------------------------------------
#----------------------------- pinyin4j ---------------------------------
-keep class com.hp.hpl.sparta.* {*;}
-keep class net.sourceforge.pinyin4j.* {*;}
#----------------------------- gson ---------------------------------
# Gson specific classes
-dontwarn sun.misc.**
# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
#----------------------------- okhttp ---------------------------------
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
#---------------------------- retrofit --------------------------------
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# ============================== 业务数据模型保护 ==============================
# 历史遗留：旧规则写的是 com.example.miaow.**.bean/data，但本仓库实际业务包名是
# com.example.fragment.project.**，导致 release 包混淆后 Gson 反射拿不到字段、
# 反序列化结果整体为 null（首页/项目/导航全部空数据 → "重试"页）。

# 1) Gson 反序列化目标：data 包下所有 Bean（Article / Banner / User / Tree / HotKey ...）
-keep class com.example.fragment.project.data.** { *; }
-keep interface com.example.fragment.project.data.** { *; }
# 1.1) 嵌套类（如 Article$Tag、Coin$CoinInfoBean）在 R8 fullMode 下也需要保留
-keepclassmembers class com.example.fragment.project.data.** { *; }

# 2) Room 实体 / DAO / Database：注解处理器生成代码会反射访问字段名
-keep class com.example.fragment.project.database.** { *; }
-keep interface com.example.fragment.project.database.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# 3) HttpResponse 框架：基类与所有子类（带泛型 data 字段）
-keep public class com.example.miaow.base.http.HttpResponse { *; }
-keep public class * extends com.example.miaow.base.http.HttpResponse { *; }

# ============================== kotlinx.serialization ==============================
# WanNavGraph.kt 使用 @Serializable 配合 Compose Navigation typed routes。
# R8 fullMode 下若不 keep Companion 与 $$serializer，release 包路由解析会抛 SerializationException。
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
# 保留所有 @Serializable 类的 Companion 与 serializer()
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# 保留生成的 $$serializer 内部类
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$$serializer {
    *;
}
# kotlinx.serialization 运行时入口
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class kotlinx.serialization.internal.** { *; }
-dontwarn kotlinx.serialization.**
