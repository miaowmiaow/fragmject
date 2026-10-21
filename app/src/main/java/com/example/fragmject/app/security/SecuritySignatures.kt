package com.example.fragmject.app.security

// 检测用的静态特征表：顶层 internal，同包内守卫与检测扩展函数都可直接引用。

/**
 * Frida Server 常见监听端口。
 *
 * 27042 是 Frida Server 的默认端口；攻击者常用 `-l 0.0.0.0:27050` 改到附近端口，
 * 因此这里扫描 27042~27052 一段。注意：`frida-gadget` 以 so 形式注入时**不监听端口**，
 * 端口扫描只覆盖 frida-server 形态，不能作为唯一判据。
 */
internal val FRIDA_PORTS = intArrayOf(
    27042, 27043, 27044, 27045, 27046, 27047,
    27048, 27049, 27050, 27051, 27052,
)

/**
 * Frida 特征线程名。
 *
 * Frida 注入后会创建 GumJS/GLib 相关线程；即使注入的 so 被删除或改名，
 * 线程名通常仍会残留，因此这是 maps 特征之外的有效补充。
 *
 * 注意 `gmain`/`gdbus` 也可能被其它使用 GLib 的库命中，单独命中不作为唯一判据，
 * 实际判定时会与其它检测项一起看（当前实现是任一命中即报，若误报可改为计数阈值）。
 */
internal val FRIDA_THREAD_SIGNATURES = listOf(
    "gum-js-loop",
    "gmain",
    "gdbus",
    "pool-frida",
    "linjector",
)

/**
 * Frida 落地文件名特征。
 *
 * 覆盖 frida-server 二进制、注入器 linjector、以及 re.frida.server 等常见命名。
 * 检查范围包括本进程 fd 目录与 /data/local/tmp（frida-server 常用落地路径）。
 */
internal val FRIDA_FILE_SIGNATURES = listOf(
    "frida-server",
    "frida-agent",
    "linjector",
    "re.frida.server",
)

/**
 * Root 相关文件路径（强信号）。
 *
 * 覆盖常见 su 位置与 Superuser.apk；Magisk 会隐藏部分路径，
 * 因此还需结合 maps 注入特征（native）与已安装 root 管理应用综合判断。
 */
internal val ROOT_FILE_PATHS = listOf(
    "/system/bin/su",
    "/system/xbin/su",
    "/sbin/su",
    "/su/bin/su",
    "/system/sd/xbin/su",
    "/system/bin/failsafe/su",
    "/data/local/su",
    "/system/app/Superuser.apk",
)

/** Magisk / Zygisk / Riru 的典型目录与文件。 */
internal val MAGISK_PATHS = listOf(
    "/sbin/.magisk",
    "/data/adb/magisk",
    "/data/adb/modules",
    "/data/adb/zygisk",
    "/data/adb/riru",
    "/dev/.magisk.unblock",
)

/** 常见 root 管理应用包名。 */
internal val ROOT_APP_PACKAGES = listOf(
    "com.topjohnwu.magisk",
    "io.github.vvb2060.magisk",
    "eu.chainfire.supersu",
    "com.koushikdutta.superuser",
    "com.thirdparty.superuser",
    "com.noshufou.android.su",
    "com.kingroot.kinguser",
    "com.qihoo.root",
)

/**
 * 可选的「关键资源」SHA-256 基线（APK 内条目路径 -> 大写十六进制）。
 *
 * 默认空 = **不启用**。原因：
 * - APK 的 v2/v3 签名已覆盖整个包（含所有资源与 so），签名通过即说明未被篡改；
 * - 单独维护资源哈希基线需要构建期生成，且随版本变化，维护成本高；
 * - 该能力主要用于「签名校验被绕过」时的二次兜底。
 *
 * 启用示例：
 *   "resources.arsc" to "<SHA256>",
 *   "assets/config.json" to "<SHA256>"
 */
internal val EXPECTED_RESOURCE_SHA256: Map<String, String> = emptyMap()
