# 安全防护 native 层构建脚本。
#
# 只产出一个很小的 so（libfragmject_security.so），职责是：
#   - 读取 /proc 做 TracerPid、匿名可执行段、Frida maps 特征检测；
#   - 计算 libc .text 的磁盘/内存 CRC32 并比对；
#   - 提供 native SIGKILL 入口。
#
# 版本说明：cmake_minimum_required 与 app/build.gradle.kts 中声明的
# externalNativeBuild.cmake.version 保持一致（本机安装的是 3.22.1）。

cmake_minimum_required(VERSION 3.22.1)

project(fragmject_security LANGUAGES CXX)

# SHARED：JNI 需要以动态库形式被 System.loadLibrary 加载。
# 库名决定 Java 侧加载名：add_library(fragmject_security ...) -> libfragmject_security.so
# -> System.loadLibrary("fragmject_security")
add_library(fragmject_security SHARED security.cpp)

# C++17 是硬性要求：security.cpp 里的 XorString 依赖类模板参数推导（CTAD），
# 即 `XorString("literal")` 能自动推导出 N。低于 C++17 会编译失败。
target_compile_features(fragmject_security PRIVATE cxx_std_17)

# -O2：CRC32 与 maps 扫描在启动路径上执行，需要足够优化；
# -Wall：保留常用告警，便于尽早发现未使用变量/类型转换等问题。
target_compile_options(fragmject_security PRIVATE -O2 -Wall)

# log：__android_log_print 依赖 liblog。
# dl：dladdr 依赖 libdl（bionic 中单独成库）。
find_library(log-lib log)
target_link_libraries(fragmject_security ${log-lib} dl)
