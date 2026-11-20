package com.example.security

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 检测引擎：负责「何时检测、命中后怎么办」，不负责「怎么判定」。
 *
 * 判定逻辑在 [detectAllHits] 及两个 Checks 文件；入口与生命周期在 [SecurityGuard]。
 *
 * ============================ 状态作用域 ============================
 * 所有可变状态都是进程级 @Volatile 字段，没有持久化：
 * - 配置变更（切换前后台、退避）用运行时字段即可；
 * - 进程死亡后登录态与检测状态都会重新初始化，暂存没有意义。
 *
 * 用 HandlerThread 而不是裸 Thread：需要「延迟执行 + 可取消重排」的轮询语义
 * （`postDelayed` / `removeCallbacks`），`onBackground` 时要能立即取消下一轮。
 */
internal class SecurityEngine(
    private val config: SecurityConfig,
    private val facts: SecurityFacts,
    private val native: NativeChecks,
) {

    /** 是否已经启动，防止重复注册轮询与生命周期回调。 */
    @Volatile
    private var started = false

    /**
     * 轮询是否处于「活跃」状态（前台且未被 stop）。
     *
     * 单独一个标志而不是只依赖 `removeCallbacks`：任务可能正在执行，
     * 此时 `removeCallbacks` 对它无效，执行完仍会 `postDelayed` → 后台继续轮询（耗电）。
     */
    @Volatile
    private var active = false

    /**
     * 是否已命中阻断。
     *
     * 用 [AtomicBoolean] 而不是 `@Volatile Boolean` + 先判断后赋值：
     * 该方法会被轮询线程与完整性校验线程同时调用，二者可能同时通过检查，
     * 导致 [SecurityConfig.onHit] 回调两次、重复调度终止。
     */
    private val blockedFlag = AtomicBoolean(false)

    /** 是否已命中阻断。置位后不再重复处理，直到进程被终止。 */
    val blocked: Boolean get() = blockedFlag.get()

    /** 命中原因，用于 UI 提示与日志排查。 */
    @Volatile
    var blockReason: String? = null
        private set

    /** 当前轮询间隔，随退避机制动态调整。 */
    @Volatile
    private var currentIntervalMs = config.initialIntervalMs

    /** 距离上次退避已连续干净的轮数。 */
    @Volatile
    private var consecutiveCleanRounds = 0

    /** 上次端口扫描时间戳，用于按时间降频。 */
    @Volatile
    private var lastPortScanMs = 0L

    /** 上次 CRC 校验时间戳，用于按时间降频。 */
    @Volatile
    private var lastCrcCheckMs = 0L

    /** 上次「重量级注入扫描」时间戳，用于按时间降频。 */
    @Volatile
    private var lastInjectionScanMs = 0L

    /** 事件记录与分发（环形缓冲 / sink / 回调异常隔离）。 */
    private val recorder = SecurityEventRecorder(config)

    /** 健康度与自动降级（连续失败达阈值后只上报、不阻断）。 */
    private val health = DetectionHealth(config.maxConsecutiveFailures)

    /** 是否已降级为「只上报、不阻断」；见 [DetectionHealth]。 */
    val degraded: Boolean get() = health.degraded

    /**
     * 已记录过的 WARN 去重键集合（见 [SecurityHit.dedupeKey]）；同一键不重复记录。
     * 用集合而非单值：同一轮可能同时有多个环境类 WARN（如 Root 与 Hook 框架并存），
     * 单值会互相覆盖，导致每轮重复记录。
     */
    private val lastWarnKeys: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /**
     * 后台检测线程：仅在真正启动后才创建，未启动的引擎不产生任何开销。
     *
     * 用 lazy 持有而不是直接 `by lazy`：`stop()` 需要判断「线程是否真的创建过」，
     * 避免为一个从未启动的引擎创建线程再退出。
     */
    private val handlerHolder = lazy {
        val thread = HandlerThread("AppSec-Check")
        thread.start()
        Handler(thread.looper)
    }

    private val handler: Handler get() = handlerHolder.value

    /**
     * 启动检测。应在 `Application.attachBaseContext` 中（经 [SecurityGuard.install]）调用。
     *
     * 执行顺序及原因：
     * 1. **native 加载**：签名校验等 native 检测的前提；
     * 2. **验签**（见 [signatureHit]）：库内没有任何跳过分支，验签是启动后的第一道检查；
     * 3. 生产包却带 `debuggable` 标记 → 判为改包并阻断（见 [isDebuggableApp]）；
     * 4. native 占坑 ptrace：必须早于任何可能被附加的时机；
     * 5. 首轮**同步**轻量检测：在进入应用主流程前判断「启动前是否已被附加/注入」；
     * 6. 异步完整性校验：so `.text` / `.rodata` 与资源基线只需一次；
     * 7. 注册生命周期回调 + 启动轮询：覆盖运行期新附加的情况。
     *
     * 首轮有意**不**执行 CRC 与端口扫描：端口扫描最坏 11 × 50ms 超时会拖慢冷启动，
     * CRC 需要读数 MB 数据。两者由后台轮询第一次执行立即补上（`lastXxxMs` 初值为 0，
     * 首轮必然 due），所以不会漏掉「启动前已被 inline hook」的场景。
     */
    fun start(context: Context) {
        if (started) return
        started = true
        active = true

        // 1) native 先加载：签名校验优先走 native（期望摘要编译在 so 中，见 signatureHit）。
        //    加载失败不阻断启动，engine 依据 available 自动走 JVM 降级路径。
        native.load()
        reportNativeAvailability()
        if (blocked) return

        // 2) 验签：启动后的第一道检查，库内没有任何跳过分支
        signatureHit(native, facts, config)?.let {
            block(it)
            return
        }

        // 3) 生产包被标记为可调试：manifest 被改过（apktool 常干的事），直接阻断
        if (isDebuggableApp(context)) {
            block(SecurityHit("生产包被标记为可调试（疑似改包）", Severity.CRITICAL, code = "tamper.debuggable"))
            return
        }

        // 反 dump（可做部分）：尽早执行——越早把 /proc 权限收紧，暴露窗口越小
        applyAntiDump(config, native)

        // 诊断模式：把「启用了什么、native 是否可用、签名是否已配置」打出来，
        // 这是接入方排查「为什么没生效」最快的一条线索。
        logDiagnostics(config, native)

        // start() 的契约是「主线程调用」：
        // - 唯一调用点 Application.attachBaseContext 由 ActivityThread 在主线程执行；
        // - registerActivityLifecycleCallbacks 的回调派发依赖主线程 Looper，
        //   从子线程注册属于未定义行为。
        // 这里做防御性检查：非主线程调用只记日志并跳过生命周期注册，轮询仍会启动。
        val onMainThread = Looper.myLooper() == Looper.getMainLooper()
        if (!onMainThread) {
            Log.w(SecurityGuard.TAG, "install() should be called on the main thread, lifecycle link skipped")
        }

        // 最早时机占坑 ptrace，杜绝后续调试器附加。
        // 失败不直接判定为「被调试」：部分 ROM 的 ptrace 策略会导致正常进程也失败，
        // 这里只记录日志，真正判定仍交给 TracerPid 检测。
        if (native.available && native.installAntiPtrace() != 0) {
            Log.w(SecurityGuard.TAG, "ptrace self-trace failed (already attached?)")
        }

        val hits = detectAllHits(facts, native, config, includePortScan = false, includeCrc = false)
        // 只有 CRITICAL 才终止；WARN（Root / 模拟器 / Hook 框架，默认）逐条记录后继续正常启动流程，
        // 否则一次环境特征就会让应用在启动阶段闪退。
        hits.firstOrNull { it.severity == Severity.CRITICAL }?.let {
            block(it)
            return
        }
        hits.forEach { warn(it) }

        // 一次性完整性校验（签名已在启动早期同步完成，这里只剩 so 与资源）
        startOneShotIntegrityChecks()

        // 7) 只有拿到 Application 且在主线程时，才能安全注册 Activity 生命周期回调；
        //    其它 Context 或子线程调用时跳过前后台联动，轮询仍会常驻。
        if (context is Application && onMainThread) {
            AppSecurityLifecycle.register(context, ::onForeground, ::onBackground)
        }

        handler.removeCallbacks(checkTask)
        handler.post(checkTask)
    }

    /**
     * 检测过程抛异常：累计次数，达到阈值后降级为「只上报」。
     *
     * 为什么不用「异常即放行」了事：单次异常可能只是偶发（如 native 读取被打断），
     * 连续异常才说明检测链路真的坏了。降级时发一条 WARN 事件，宿主上报后能发现。
     */
    private fun onDetectionFailure(cause: Throwable) {
        val justDegraded = health.onFailure()
        Log.e(
            SecurityGuard.TAG,
            "detection failed (${health.failureCount}/${config.maxConsecutiveFailures}): " +
                cause.javaClass.simpleName,
        )
        if (justDegraded) {
            Log.e(SecurityGuard.TAG, "detection degraded: env checks report-only, core checks still block")
            recorder.record(
                SecurityHit(
                    reason = "安全检测异常，环境类检测已降级为只上报（核心防护仍生效）",
                    severity = Severity.WARN,
                    code = "internal.degraded",
                ),
            )
        }
    }

    /**
     * 按 [nativeUnavailableHit] 的结果处理 native 可用性（阻断或记录）。
     *
     * 拆成独立方法：`start()` 已接近 detekt 的圈复杂度阈值，分支放在这里。
     */
    private fun reportNativeAvailability() {
        val hit = nativeUnavailableHit(native.available, config.requireNative) ?: return
        if (hit.severity == Severity.CRITICAL) {
            block(hit)
        } else {
            recorder.record(hit)
        }
    }

    /** 一次成功检测会清零连续失败计数（偶发异常不应累积成降级）。 */
    private fun onDetectionSuccess() = health.onSuccess()




    /**
     * 停止检测并释放检测线程（供 [SecurityGuard.uninstall] 使用）。
     *
     * 注意：已注册的 Activity 生命周期回调无法反注册（Application 不提供移除 API），
     * 但回调只会转到「已置空」的引擎，表现为空操作。
     */
    fun stop() {
        active = false
        started = false
        lastWarnKeys.clear()
        if (handlerHolder.isInitialized()) {
            handler.removeCallbacksAndMessages(null)
            handler.looper.quitSafely()
        }
    }

    /**
     * 进入前台：立即全量检测一次，并把退避重置为最短间隔。
     *
     * 为什么需要：用户切回前台是「调试器/Frida 新附加」的高发窗口，
     * 如果沿用后台的长间隔（可能已退避到上限），会错过这个窗口。
     * 这里同时把 lastPortScanMs 归零，强制下一轮重做端口扫描。
     */
    fun onForeground() {
        if (!started) return
        active = true
        consecutiveCleanRounds = 0
        currentIntervalMs = config.initialIntervalMs
        lastPortScanMs = 0L
        handler.removeCallbacks(checkTask)
        handler.post(checkTask)
    }

    /** 最近命中的事件（字符串，时间正序；展示/导出用）。 */
    fun recentEvents(): List<String> = recorder.recentEvents()

    /**
     * 最近的安全事件（结构化，时间正序）。
     *
     * 与 [recentEvents] 的区别：这里给的是带稳定 `code` 的对象，适合接入上报；
     * 上面那个是给人看的字符串。
     */
    fun events(): List<SecurityEvent> = recorder.events()

    /**
     * 进入后台：暂停轮询。
     *
     * 进程在后台时用户不可见，继续高频检测只会耗电/发热；
     * 回到前台会通过 [onForeground] 立即恢复。
     */
    fun onBackground() {
        if (!started) return
        // 只置标志 + 取消排队：正在执行的这一轮可能在 `active` 置 false 之后才结束，
        // 由 [checkTask] 自己判断是否继续排队（见该方法末尾）。
        active = false
        // 离开前台时清掉 WARN 去重：用户切回来若环境仍异常，会再记录一条，
        // 便于复现「只有切回前台才出现」的问题；放在这里而不是 onForeground，
        // 是为了避免启动阶段 start() 与首次 onForeground 各记一次造成重复。
        lastWarnKeys.clear()
        handler.removeCallbacks(checkTask)
    }

    /**
     * 命中后置标记并延迟终止进程。
     *
     * 为什么延迟：给 UI 一个展示 [SecurityGuard.blockingMessage] 的窗口，避免「莫名闪退」。
     * 为什么延迟不削弱安全性：
     * - 终止动作优先由 native `SIGKILL` 完成，Java 层 hook 无法取消；
     * - 延迟固定且很短（默认 1.2s），攻击者拿不到有意义的操作窗口；
     * - 命中后不再排下一轮检测，避免在「已被控制的环境」里继续跑逻辑。
     *
     * 并发安全：用 [blockedFlag] 的 CAS 保证「只处理一次」——
     * 该方法可能同时被轮询线程与完整性校验线程调用。
     */
    fun block(hit: SecurityHit) {
        // 降级只放松「环境类」判定。完整性 / 注入 / 调试器 / 篡改类命中始终阻断：
        // 降级由检测异常触发，而异常可以被攻击者人为制造（hook 检测函数让它抛错），
        // 不能借此关掉核心防护。
        if (degraded && !hit.isCoreCheck()) {
            warn(hit)
            return
        }
        if (!blockedFlag.compareAndSet(false, true)) return
        blockReason = hit.reason
        // 用 Log.e 而不是 Log.w：release 的 proguard 规则把 Log.v/d/i/w 全部当作无副作用裁剪掉了，
        // 用 w 会导致「线上误杀时没有任何日志」，排查无从下手。e 不在裁剪列表内。
        // 日志只带稳定 code：release 中原因文案会暴露检测细节（同机攻击者可据此定向绕过）。
        // 文案通过 events() / onEvent 交给宿主，宿主自行决定是否展示或上报。
        Log.e(SecurityGuard.TAG, "security check hit: [${hit.code}]")
        record(hit)

        handler.postDelayed({
            // 先 native SIGKILL（不可捕获/不可 hook），无论它成功与否都必须执行 Java 兜底：
            // 之前缺 try/finally，一旦 JNI 抛异常（so 被替换、方法被裁剪），进程就杀不掉。
            try {
                if (native.available) {
                    native.killSelf()
                }
            } catch (t: Throwable) {
                Log.e(SecurityGuard.TAG, "native kill failed, fallback to Process.killProcess", t)
            } finally {
                Process.killProcess(Process.myPid())
            }
        }, config.blockGraceMs)
    }

    /**
     * WARN 级命中：只记录、不终止。
     *
     * 去重：同一问题只在「首次出现」和「回前台」时记录一次，
     * 否则按 1s 轮询会产生每秒一条的噪音（也掩盖了真正攻击特征的日志）。
     * 去重键用 [SecurityHit.dedupeKey]（已去掉运行期数字），避免段数变化导致重复刷屏。
     */
    fun warn(hit: SecurityHit) {
        if (!lastWarnKeys.add(hit.dedupeKey)) return
        Log.e(SecurityGuard.TAG, "security warn: [${hit.code}]")
        record(hit)
    }

    /**
     * 记录命中：写环形缓冲 + 触发回调。
     *
     * 回调在这里统一触发（[block] 与 [warn] 都走这条路径），保证宿主无论从哪个入口
     * 收到事件，拿到的东西都与 [events] 里的一致。
     */
    private fun record(hit: SecurityHit) = recorder.record(hit)

    /**
     * 轮询任务。
     *
     * 每轮：
     * 1. 按时间决定本轮是否执行「重量级检测」（端口扫描 / CRC）；
     * 2. 执行轻量检测；
     * 3. CRITICAL 命中则 [block] 并停止轮询（进程即将被终止，不必再排下一轮）；
     *    WARN 命中只 [warn] 记录，继续按退避排下一轮；
     * 4. 未命中则按退避策略排下一轮；`active == false`（已切后台/已 stop）时不再排。
     */
    private val checkTask = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()

            // 用「时间差」而不是「轮数」控制重量级检测频率：
            // 轮询间隔会随退避变化，按时间判断才能保证「最多每 N 秒一次」。
            val portScanDue = (now - lastPortScanMs >= config.portScanIntervalMs).also {
                if (it) lastPortScanMs = now
            }
            val crcCheckDue = (now - lastCrcCheckMs >= config.crcCheckIntervalMs).also {
                if (it) lastCrcCheckMs = now
            }
            // maps 解析（Frida 特征 / 匿名段）单独降频：稳态 CPU 的主要来源
            val injectionScanDue = (now - lastInjectionScanMs >= config.injectionScanIntervalMs).also {
                if (it) lastInjectionScanMs = now
            }

            val startedNs = if (config.diagnosticMode) System.nanoTime() else 0L
            val hits = runCatching {
                detectAllHits(
                    facts = facts,
                    native = native,
                    config = config,
                    includePortScan = portScanDue,
                    includeCrc = crcCheckDue,
                    includeInjectionScan = injectionScanDue,
                )
            }.onFailure { onDetectionFailure(it) }
                .onSuccess { onDetectionSuccess() }
                .getOrNull().orEmpty()
            if (config.diagnosticMode) {
                // 性能基线：诊断模式下打印单轮耗时（含 port/crc 的那一轮会明显更久）
                val costMs = (System.nanoTime() - startedNs) / 1_000_000.0
                Log.e(
                    SecurityGuard.TAG,
                    "security diag: round cost=${"%.2f".format(costMs)}ms " +
                        "portScan=$portScanDue crc=$crcCheckDue hit=${hits.firstOrNull()?.code ?: "none"}",
                )
            }
            hits.firstOrNull { it.severity == Severity.CRITICAL }?.let {
                block(it)
                return
            }
            hits.forEach { warn(it) }

            // 指数退避：连续干净后逐步拉长间隔（1s → 2s → 4s … → 上限封顶）。
            consecutiveCleanRounds++
            currentIntervalMs = nextIntervalMs(currentIntervalMs, consecutiveCleanRounds, config)
            if (consecutiveCleanRounds >= config.cleanRoundsToBackoff) {
                consecutiveCleanRounds = 0
            }

            // 关键：本轮执行期间可能已切到后台（或 stop），此时不能再排下一轮，
            // 否则 removeCallbacks 之后又会被自己重新排上，后台持续耗电。
            if (active) {
                handler.postDelayed(this, currentIntervalMs)
            }
        }
    }

    /**
     * 一次性完整性校验：so `.text` + so `.rodata` + 资源基线。
     *
     * 签名校验不在这里——它已前移到 [start] 的同步路径（启动第一步）。
     *
     * 拆成独立方法的两个原因：
     * - `start()` 已经承担 native 加载、首轮检测、生命周期注册，
     *   再塞多个分支会超过 detekt 的圈复杂度阈值；
     * - 这几项都是「进程生命周期内只做一次」的同类工作，放一起便于按需增删。
     */
    private fun startOneShotIntegrityChecks() {
        Thread {
            val hit = runCatching { firstIntegrityFailure(facts, native, config) }
                .onFailure { onDetectionFailure(it) }
                .onSuccess { onDetectionSuccess() }
                .getOrNull()
            if (hit != null) {
                block(hit)
            }
        }.start()
    }


    /**
     * 是否是可调试应用（读 `ApplicationInfo.FLAG_DEBUGGABLE`）。
     *
     * 注意：这个标志由 manifest 的 `android:debuggable` 决定，**攻击者可以改**。
     * 它只用于「生产包被改成 debuggable」的**篡改判定**。库内没有任何依据它跳过检测的分支。
     */
    private fun isDebuggableApp(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
