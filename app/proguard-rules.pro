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
-optimizationpasses 5                                                           # 代码混淆的压缩比例，值介于0-7，默认5
-verbose                                                                        # 混淆时记录日志
-dontshrink                                                                     # 关闭压缩
-dontpreverify                                                                  # 关闭预校验(作用于Java平台，Android不需要，去掉可加快混淆)
-dontoptimize                                                                   # 关闭代码优化
#-dontobfuscate                                                                  # 关闭混淆
-ignorewarnings                                                                 # 忽略警告
-dontusemixedcaseclassnames                                                     # 混淆后类型都为小写
-dontskipnonpubliclibraryclasses                                                # 不跳过非公共的库的类
-printmapping mapping.txt                                                       # 生成原类名与混淆后类名的映射文件mapping.txt
-useuniqueclassmembernames                                                      # 把混淆类中的方法名也混淆
-allowaccessmodification                                                        # 优化时允许访问并修改有修饰符的类及类的成员
-renamesourcefileattribute SourceFile                                           # 将源码中有意义的类名转换成SourceFile，用于混淆具体崩溃代码
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*        # 指定混淆时采用的算法
-keepattributes *Annotation*d                                                   # 保留注解
-keepattributes Signature                                                       # 保留泛型

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
# 保留support下的所有类及其内部类
-dontwarn android.support.**
-keep class android.support.* {*;}
# 保留support下的类的继承类及其内部类
-keep public class * extends android.support.v4.*
-keep public class * extends android.support.v7.*
-keep public class * extends android.support.annotation.*
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
# 不混淆自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
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
#解决webview和js的交互问题
#其中的xx.xx.xx.xxx换成自己的完整包名
#如果是内部类使用了webview与js的交互功能，则需要添加“$”后面跟着的是内部类名
#如果不使用内部类，直接xx.xx.xx.xxx就可以了
#-keepclassmembers class xx.xx.xx.xxx$InJavaScriptLocalObj {
#   public *;
#}
#-keepattributes JavascriptInterface
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
##-----------------------------------------第三方依赖库--------------------------------------------
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
# 保留通过Gson序列化/反序列化的应用程序类不被混淆
# 将下面替换成自己的实体类
-keep class **bean.** {*;}
-keep interface **bean.** {*;}
-keep public class * extends com.example.fragment.library.base.http.HttpResponse
#----------------------------- okhttp ---------------------------------
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
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
# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
#---------------------------- glide --------------------------------
-keep class **.glide.** {*;}
-keep interface **.glide.** {*;}
##-----------------------------------------腾讯TBS--------------------------------------------
-dontwarn dalvik.**
-dontwarn com.tencent.smtt.**
-keep class com.tencent.smtt.** {
    *;
}
-keep class com.tencent.tbs.** {
    *;
}