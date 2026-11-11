package com.example.security

// Kotlin 侧仅保留「必须由 Java 使用」的两张表；其余特征表已下沉 native（H3）。
//
// 为什么下沉：Frida 线程名/落地文件名、su 与 Magisk 路径是检测逻辑的指纹，
// 留在 DEX 里 `strings` 一搜就暴露判定规则。native 侧用 XorString 编译期混淆，
// 且 Kotlin 只拿到「命中数」，看不到具体特征。
//
// 为什么这两张没下沉：
// - 端口表：端口扫描在 Java 侧建 socket，需要具体端口号（下沉还得回传数组，无收益）；
// - root 应用包名表：查询依赖 PackageManager，包名是公开的应用标识，泄露价值低。

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

// 注：「关键资源 SHA-256 基线」已迁到 SecurityConfig.expectedResourceSha256，由宿主注入。
