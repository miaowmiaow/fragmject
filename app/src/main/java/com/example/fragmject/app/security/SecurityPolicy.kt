package com.example.fragmject.app.security

/**
 * 运行时安全防护的**策略常量**：只放「可调参数与开关」，不放任何执行逻辑。
 *
 * 为什么从 [AppSecurityGuard] 里分出：
 * - 原文件 498 行贴着「单文件 500 行」的守卫上限（规则 28），任何一次小改动都会撞线；
 * - 策略值与调度逻辑的变更频率完全不同：调阈值（如误杀后放宽 [ANON_EXEC_THRESHOLD]）
 *   不应该和改轮询编排放在同一个 diff 里评审；
 * - 这些常量被 `SecurityDetectors.kt` 的检测项共同引用，独立后两者的依赖关系变成
 *   「守卫 → 策略」「检测器 → 策略」，而不是「检测器 → 守卫的成员」。
 *
 * 注意：[AppSecurityGuard] 里的 native 声明**不能**迁出——它们在 security.cpp 中按
 * 「静态注册」实现，JNI 符号名由类名决定，换类即链接失败。
 */
object SecurityPolicy {

    /**
     * Baseline Profile 采集专用**构建类型**名。
     *
     * 由 `androidx.baselineprofile` 插件创建（`nonMinifiedRelease`），不对外发布。
     * 该变体下跳过运行时检测：模拟器 ART/JIT 会产生匿名可执行段，命中
     * [ANON_EXEC_THRESHOLD] 后进程在 1.2s 内被 SIGKILL，采集必然失败。
     *
     * 已核对生成的 BuildConfig：`nonMinifiedRelease` 变体的 `BUILD_TYPE` 取值就是
     * `"nonMinifiedRelease"`（而非 `"release"`），所以比较是对的。
     *
     * 刻意用「带 getter 的 val」而不是 `const val`：IDE 会用**当前所选变体**的 BuildConfig
     * 做常量折叠（debug 下 `BUILD_TYPE = "debug"`），从而误报「Condition is always false」；
     * 改成运行期读取后不再折叠，语义不变。
     */
    internal val collectionBuildType: String get() = "nonMinifiedRelease"

    /**
     * 首轮检测与「回前台」后的轮询间隔。
     *
     * 取 1s 的原因：调试器/Frida 往往在应用刚启动或切回前台时附加，
     * 这个窗口需要高频率覆盖；随后由退避机制逐步拉长，避免长期耗电。
     */
    internal const val INITIAL_INTERVAL_MS = 1_000L

    /**
     * 轮询间隔上限。
     *
     * 60s 是「及时性」与「耗电」的折中：即使攻击者在应用长时间驻留后才附加，
     * 最迟也能在一分钟内发现，同时避免后台长期高频唤醒 CPU。
     */
    internal const val MAX_INTERVAL_MS = 60_000L

    /**
     * 连续多少轮「未命中」后把间隔翻倍。
     *
     * 取 3 的原因：连续几轮干净通常说明当前环境稳定，可以开始退避；
     * 太小（如 1）会让间隔增长过慢，漏掉「刚附加」的瞬间。
     */
    internal const val CLEAN_ROUNDS_TO_BACKOFF = 3

    /**
     * 端口扫描的独立降频间隔。
     *
     * 端口扫描是重量级操作（每个端口都要建一次 socket，最坏 11 次连接），
     * 因此不能跟着 1s 的轮询节奏跑，单独限制为最多每 60s 一次。
     */
    internal const val PORT_SCAN_INTERVAL_MS = 60_000L

    /**
     * libc `.text` CRC 校验的降频间隔。
     *
     * CRC 需要把 libc 的数 MB 代码段读两遍并计算，单次耗时虽在毫秒级，
     * 但没必要每轮都做；30s 一次足以发现「运行中才挂上的 inline hook」。
     */
    internal const val CRC_CHECK_INTERVAL_MS = 30_000L

    /**
     * 命中后到终止进程的宽限时间。
     *
     * 仅用于让 UI 有机会展示 [AppSecurityGuard.blockingMessage]；终止动作本身不可取消。
     * 取值不宜过长：窗口越长，攻击者可利用的时间越多。
     */
    const val BLOCK_GRACE_MS = 1_200L

    /**
     * 匿名可执行段容忍阈值。
     *
     * 正常环境下，除 ART JIT、vdso 等白名单段外，不应存在「无对应文件却可执行」的内存，
     * 因此默认 0（发现即判定）。但不同 ROM/ART 实现可能产生少量合法匿名可执行段，
     * 若真机出现误杀，可放宽到 1~2；不要设得过宽，否则会漏掉「少量匿名段」的注入。
     */
    const val ANON_EXEC_THRESHOLD = 0

    /**
     * 预期 APK 签名 SHA-256（大写十六进制）。
     *
     * 默认留空表示**不启用**签名校验：
     * - 未配置时若强行校验，会导致所有安装包（包括自己的 Release）被误杀；
     * - 项目里同时存在 debug/release 签名，硬编码单一值也容易出错。
     *
     * 启用方式（二选一）：
     * 1. 用 `keytool -list -v -keystore <your.jks>` 取 SHA256，去掉冒号后填入；
     * 2. 由构建脚本在 release 构建时通过 BuildConfig 注入（更适合多环境）。
     *
     * 注意：只校验「当前包名的签名证书」，不能防御「用同一证书签名的改包」；
     * 多签名 APK 需要改为逐一比对，本项目未启用该场景。
     */
    const val EXPECTED_SIGNATURE_SHA256 = ""

    /**
     * 是否把 Root 作为阻断项。
     *
     * 误报来源：部分定制 ROM / 工程机自带 su，但用户并无恶意。
     * 若业务对 Root 不敏感（如普通内容类应用），可置为 false，仅保留日志。
     *
     * 刻意用「带 getter 的 val」而不是 `const val`：
     * `const` 会在编译期折叠，`enforceRootDetection && hasRootSignals()`
     * 会被折叠成 `hasRootSignals()`，IDE 会提示「Boolean expression can be simplified」，
     * 折叠后这个开关也失去了「改一处即生效」的意义。
     * （写成带 getter 的 val 同样不会被 detekt 的 MayBeConst 判定为可改为 const。）
     */
    val enforceRootDetection: Boolean get() = true

    /**
     * 是否把模拟器作为阻断项。
     *
     * 误报来源：云手机、投屏、自动化测试环境。
     * 若业务需要支持云游戏/自动化，可置为 false。
     *
     * 同 [enforceRootDetection]：保持运行期读取，避免被编译期折叠。
     */
    val enforceEmulatorDetection: Boolean get() = true
}
