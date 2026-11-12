# 安全防护 native 层构建脚本。
#
# 只产出一个很小的 so（libappsecurity.so），职责是：
#   - 读取 /proc 做 TracerPid、匿名可执行段、Frida maps 特征检测；
#   - 计算 libc .text 的磁盘/内存 CRC32 并比对；
#   - 提供 native SIGKILL 入口。
#
# 版本说明：cmake_minimum_required 与 app/build.gradle.kts 中声明的
# externalNativeBuild.cmake.version 保持一致（本机安装的是 3.22.1）。

cmake_minimum_required(VERSION 3.22.1)

project(appsecurity LANGUAGES CXX)

# SHARED：JNI 需要以动态库形式被 System.loadLibrary 加载。
# 库名决定 Java 侧加载名：add_library(appsecurity ...) -> libappsecurity.so
# -> System.loadLibrary("appsecurity")
add_library(appsecurity SHARED security.cpp)

# C++17 是硬性要求：security.cpp 里的 XorString 依赖类模板参数推导（CTAD），
# 即 `XorString("literal")` 能自动推导出 N。低于 C++17 会编译失败。
target_compile_features(appsecurity PRIVATE cxx_std_17)

# -O2：CRC32 与 maps 扫描在启动路径上执行，需要足够优化；
# -Wall：保留常用告警，便于尽早发现未使用变量/类型转换等问题；
# -fvisibility=hidden：动态注册后，jniXxx 实现函数不必出现在 .dynsym 里，
#   只保留 JNI_OnLoad 一个对外符号（它带 JNIEXPORT）。这是动态注册的主要收益之一。
target_compile_options(appsecurity PRIVATE -O2 -Wall -fvisibility=hidden)


# log：__android_log_print 依赖 liblog。
# dl：dladdr 依赖 libdl（bionic 中单独成库）。
find_library(log-lib log)
target_link_libraries(appsecurity ${log-lib} dl)

# 测试用：编译进「自篡改探针」（模拟一次 inline hook），仅 -PselfTamperProbe=true 时开启。
# 生产构建不定义该宏，探针代码不进 so。
if(SELF_TAMPER_PROBE)
    target_compile_definitions(appsecurity PRIVATE SELF_TAMPER_PROBE)
endif()

# 测试用：把 .text 首页置为 RWX（不改内容），验证 rwx 映射检测分支
if(SELF_RWX_PROBE)
    target_compile_definitions(appsecurity PRIVATE SELF_RWX_PROBE)
endif()

# 测试用：改写一个 GOT 表项（模拟 GOT 劫持），验证 PLT 检测分支
if(SELF_GOT_PROBE)
    target_compile_definitions(appsecurity PRIVATE SELF_GOT_PROBE)
endif()
