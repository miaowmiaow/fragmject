package com.example.security

/**
 * 运行时安全防护配置。
 *
 * ============================ 为什么从 object 常量改成数据类 ============================
 * 原实现把阈值与开关写成 `SecurityPolicy` 的 `const val` / 带 getter 的 `val`，有两个问题：
 * - `const` 会在编译期折叠，`enforceRootDetection && hasRootSignals()` 被折叠成
 *   `hasRootSignals()`，IDE 提示可化简，且开关失去「改一处即生效」的意义；
 * - 常量无法由接入方配置。本类改为由宿主在 [SecurityGuard.install] 时注入，
 *   **默认值与原常量完全一致**，行为不变。
 *
 * ============================ 字段约定 ============================
 * - [buildType]：由宿主注入（一般是 `BuildConfig.BUILD_TYPE`）。库自身**不读取**宿主的
 *   BuildConfig——库模块的 BuildConfig 是库自己的，读不到宿主的变体；
 * - [skipBuildTypes]：默认跳过 `nonMinifiedRelease`（`androidx.baselineprofile` 插件创建的
 *   采集变体）。该变体不参与发布，且模拟器 ART/JIT 会产生匿名可执行段，命中
 *   [anonExecThreshold] 后进程会在 [blockGraceMs] 内被 SIGKILL，导致采集失败；
 * - [expectedSignatureSha256]：空集合 = 不校验（默认）。未配置时强行校验会把所有正常安装包
 *   误杀；配置方式与多签名处理见 [verifySignature]；
 * - [onHit]：命中回调，供宿主上报或展示提示；默认 null（只写日志）。
 *
 * @see SecurityGuard 入口
 */
data class SecurityConfig(
    /** 总开关：false 时 [SecurityGuard.install] 直接返回，不建线程、不注册回调。 */
    val enabled: Boolean = true,
    /** 宿主构建类型，仅用于与 [skipBuildTypes] 比对。 */
    val buildType: String = "",
    /** 需要跳过防护的构建类型。 */
    val skipBuildTypes: Set<String> = setOf("nonMinifiedRelease"),
    /** 期望签名证书 SHA-256（十六进制，忽略大小写与冒号）；空 = 不校验。 */
    val expectedSignatureSha256: Set<String> = emptySet(),
    /** 关键资源 SHA-256 基线（APK entry 路径 -> 摘要）；空 = 不校验。 */
    val expectedResourceSha256: Map<String, String> = emptyMap(),
    /** 匿名可执行段容忍阈值；0 = 发现即判定。 */
    val anonExecThreshold: Int = 0,
    /**
     * Root 检测的处置方式。
     *
     * 默认 [Action.WARN]：定制 ROM / 工程机自带 su 很常见，内容类应用的主链路不因此阻断；
     * 高价值链路（支付、授权）可改成 [Action.BLOCK] 或改为服务端风控。
     */
    val rootAction: Action = Action.WARN,
    /**
     * 模拟器检测的处置方式。
     *
     * 默认 [Action.WARN]：云手机、投屏、自动化测试同样会命中；
     * 若业务要求「只能在真机使用」，改成 [Action.BLOCK]。
     */
    val emulatorAction: Action = Action.WARN,
    /** 首轮检测与「回前台」后的轮询间隔。 */
    val initialIntervalMs: Long = 1_000L,
    /** 轮询间隔上限。 */
    val maxIntervalMs: Long = 60_000L,
    /** 连续多少轮干净后把间隔翻倍。 */
    val cleanRoundsToBackoff: Int = 3,
    /** 端口扫描独立降频间隔。 */
    val portScanIntervalMs: Long = 60_000L,
    /** CRC 校验独立降频间隔。 */
    val crcCheckIntervalMs: Long = 30_000L,
    /** 命中后到终止进程的宽限时间（仅用于让 UI 展示提示）。 */
    val blockGraceMs: Long = 1_200L,
    /** 命中回调（含 WARN 与 CRITICAL）；可能由检测线程触发，实现需自行保证线程安全。 */
    val onHit: ((SecurityHit) -> Unit)? = null,
)
