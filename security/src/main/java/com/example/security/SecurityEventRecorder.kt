package com.example.security

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 命中事件的记录与分发。
 *
 * ============================ 为什么单独一个类 ============================
 * 引擎原本同时负责「调度 / 判定 / 记录 / 分发 / 终止」，文件超过 500 行后
 * 架构守卫会拦下（本项目对单文件有上限）。把「记录与分发」拆出来之后：
 * - 引擎只管时序与阻断；
 * - 环形缓冲、去重、回调异常隔离集中在一处，便于单测与替换存储（如落盘）。
 *
 * 线程安全：所有状态由 [lock] 保护；回调在锁外触发（避免宿主的回调里再调用
 * [events] 造成死锁）。
 */
internal class SecurityEventRecorder(private val config: SecurityConfig) {

    private val lock = Any()
    private val recent = ArrayDeque<SecurityEvent>()

    /** 最近事件（结构化，时间正序）。 */
    fun events(): List<SecurityEvent> = synchronized(lock) { recent.toList() }

    /** 最近事件（字符串，供展示/导出）。 */
    fun recentEvents(): List<String> = synchronized(lock) {
        val format = SimpleDateFormat("HH:mm:ss", Locale.US)
        recent.map { "${format.format(Date(it.timestampMillis))} ${it.severity} ${it.reason}" }
    }

    /**
     * 记录命中：写环形缓冲 + 依次通知三个出口。
     *
     * 出口异常一律吞掉并降级为日志：**上报失败不能影响检测与阻断**，
     * 否则宿主一个写错的 sink 就能让安全模块自己失效。
     */
    fun record(hit: SecurityHit) {
        val event = SecurityEvent(
            code = hit.code,
            reason = hit.reason,
            severity = hit.severity,
            timestampMillis = System.currentTimeMillis(),
        )
        synchronized(lock) {
            if (recent.size >= MAX_EVENTS) {
                recent.removeFirst()
            }
            recent.addLast(event)
        }

        config.sink?.let { sink ->
            runCatching { sink.emit(event) }
                .onFailure { Log.w(SecurityGuard.TAG, "event sink failed: ${it.javaClass.simpleName}") }
        }
        config.onEvent?.let { callback ->
            runCatching { callback(event) }
                .onFailure { Log.w(SecurityGuard.TAG, "onEvent failed: ${it.javaClass.simpleName}") }
        }
        config.onHit?.let { callback ->
            runCatching { callback(hit) }
                .onFailure { Log.w(SecurityGuard.TAG, "onHit failed: ${it.javaClass.simpleName}") }
        }
    }

    private companion object {
        /** 环形缓冲上限：够现场排障，也不会长期占内存。 */
        const val MAX_EVENTS = 20
    }
}

/**
 * 检测链路的健康度与自动降级。
 *
 * 规则：连续失败达到阈值 → 降级为「只上报、不阻断」。
 * 设计取向是**宁可漏杀，不可误杀**：检测自身故障时继续按命中杀人，风险高于收益。
 *
 * 抽成独立类的原因：[SecurityEngine] 依赖 Handler/Looper，无法在 JVM 单测里构造；
 * 而「什么时候降级」是安全语义的关键，必须可测（见 `SecurityPolicyTest`）。
 */
internal class DetectionHealth(private val limit: Int) {

    @Volatile
    var degraded: Boolean = false
        private set

    private var consecutiveFailures = 0

    /** @return true 表示本次失败**刚刚**触发降级（用于只发一次事件）。 */
    fun onFailure(): Boolean {
        consecutiveFailures++
        if (!shouldDegrade(consecutiveFailures, limit, degraded)) {
            return false
        }
        degraded = true
        return true
    }

    /** 成功一次即清零：偶发异常不应累积成降级。 */
    fun onSuccess() {
        if (consecutiveFailures != 0) {
            consecutiveFailures = 0
        }
    }

    /** 当前连续失败次数（诊断用）。 */
    val failureCount: Int get() = consecutiveFailures
}
