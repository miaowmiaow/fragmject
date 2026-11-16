# 运行期安全防护库的 R8 规则（随 AAR 合并，宿主无需手写）。
#
# 三条约束：
# 1. JNI 类名与方法名必须保留——native 侧用 RegisterNatives 按名注册；
# 2. 公开 API 不做 keep（见第 2 段），只有 JNI 桥需要保留名字；
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

# 2) 公开 API 不做 keep（P1-5）
#    宿主 R8 与本库在同一次混淆中处理，调用点会一致地重命名。保留成员名只会给攻击者一份
#    「可直接定位的开关清单」（如 expectedSignatureSha256 / checks）。
#    枚举的 name 来自构造参数的字符串字面量，不受字段重命名影响，事件 JSON 输出不变。

# 3) 检测实现类不需要保留名字（R8 可正常混淆）
