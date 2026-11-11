# 运行期安全防护库的 R8 规则（随 AAR 合并，宿主无需手写）。
#
# 三条约束：
# 1. JNI 类名与方法名必须保留——native 侧用 RegisterNatives 按名注册；
# 2. 公开入口与配置类保留可读名字——崩溃栈与接入方排障需要；
# 3. native 方法禁止裁剪——只在 native 侧「按名注册」的方法在 Kotlin 里没有调用点，
#    否则会被 R8 删掉，导致 RegisterNatives 失败（实测踩过）。

# 1) JNI 桥：类名 + 构造器 + load() + 全部 native 方法
-keepclasseswithmembernames class com.example.security.SecurityNative {
    native <methods>;
}
-keepclassmembers class com.example.security.SecurityNative {
    <init>(java.lang.String, java.lang.String);
    public boolean load();
    native <methods>;
}

# 2) 公开 API 与配置对象
-keep class com.example.security.SecurityGuard { public *; }
-keep class com.example.security.SecurityConfig { *; }
-keep class com.example.security.SecurityHit { *; }
-keep class com.example.security.Severity { *; }
-keep class com.example.security.Action { *; }

# 3) 检测实现类不需要保留名字（R8 可正常混淆）
