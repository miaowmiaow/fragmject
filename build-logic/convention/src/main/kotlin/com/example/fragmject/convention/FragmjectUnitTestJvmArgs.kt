package com.example.fragmject.convention

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

/**
 * 给 JVM 单测补 `--add-opens`（JDK 强封装）。
 *
 * ============================ 为什么必须加 ============================
 * Robolectric 需要反射读写 JDK 内部字段：
 * - `java.io.FileDescriptor.fd`（Android 14+ 的 `ApplicationSharedMemory` 会走这条路径）；
 * - 以及 `android.os`/`sun.nio.ch` 的一些内部类。
 *
 * JDK 16 起默认拒绝「跨模块深度反射」，不放开就会在
 * `AndroidTestEnvironment.setUpApplicationState` 阶段直接失败：
 *
 * ```
 * java.lang.RuntimeException: Failed to interact with raw FileDescriptor internals; perhaps JRE has changed?
 *   at org.robolectric.interceptors.AndroidInterceptors$FileDescriptorInterceptor.setInt
 * ```
 *
 * 表现是「所有 Robolectric 用例全红、且堆栈完全不指向业务代码」——
 * 一度被误判为测试写错。加在约定插件里，所有模块统一生效，避免有人新加模块时再踩一次。
 *
 * 注：`configureEach` 是配置缓存友好的写法（不要在 `afterEvaluate` 里遍历 tasks）。
 */
private val JVM_OPENS = listOf(
    "java.base/java.lang",
    "java.base/java.lang.reflect",
    "java.base/java.lang.invoke",
    "java.base/java.io",
    "java.base/java.net",
    "java.base/java.nio",
    "java.base/java.text",
    "java.base/java.time",
    "java.base/java.util",
    "java.base/java.util.concurrent",
    "java.base/sun.nio.ch",
)

/**
 * 需要 **export** 的 JDK 内部包（不是 opens）。
 *
 * 关键一条：Robolectric 的 `FileDescriptorInterceptor` 直接引用
 * `jdk.internal.access.SharedSecrets`，只加 `--add-opens` 会得到
 * ```
 * IllegalAccessException: ... cannot access class jdk.internal.access.SharedSecrets
 * (in module java.base) because module java.base does not export jdk.internal.access
 * ```
 * 实测：加上 opens 仍失败，补 exports 才通。
 */
private val JVM_EXPORTS = listOf(
    "java.base/jdk.internal.access",
    "java.base/jdk.internal.misc",
    "java.base/sun.nio.ch",
    "java.base/sun.security.action",
)

/** 给本模块所有 JVM 测试任务补 `--add-opens` 与 `--add-exports`。 */
internal fun Project.configureUnitTestJvmArgs() {
    tasks.withType(Test::class.java).configureEach {
        jvmArgs(JVM_OPENS.map { module -> "--add-opens=$module=ALL-UNNAMED" })
        jvmArgs(JVM_EXPORTS.map { module -> "--add-exports=$module=ALL-UNNAMED" })
    }
}
