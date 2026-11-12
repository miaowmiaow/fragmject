package com.example.security

/**
 * 安全事件出口（宿主实现）。
 *
 * ============================ 与 onEvent 的区别 ============================
 * - `SecurityConfig.onEvent` 是**函数回调**，适合「就地处理」（打日志、弹提示）；
 * - `SecurityEventSink` 是**对象**，适合「接入既有上报通道」——宿主可以把它实现成
 *   队列 / 批量上报 / 落盘，库只负责把事件交出去，不关心线程与重试。
 *
 * 调用线程：由检测线程触发（非主线程），实现方**必须自己保证线程安全**，
 * 且不要在 [emit] 里做阻塞操作（会拖慢检测轮询）。
 */
interface SecurityEventSink {
    fun emit(event: SecurityEvent)
}

/**
 * 限流出口：包一层别的出口，按「滑动窗口内最多 N 条」丢弃多余事件。
 *
 * 为什么需要：WARN 类事件（Root / 模拟器）在异常环境下可能持续产生；
 * 宿主若直接接 HTTP 上报，很容易把自家接口打挂。
 *
 * 线程安全；被限流丢弃的事件不会进入后续出口，但**仍会进入引擎的环形缓冲**
 * （`SecurityGuard.events()` 仍能看到），便于现场排障。
 */
class ThrottledEventSink(
    private val delegate: SecurityEventSink,
    /** 窗口内允许通过的最大条数。 */
    private val maxEventsPerWindow: Int = 20,
    /** 窗口长度（毫秒）。 */
    private val windowMs: Long = 60_000L,
) : SecurityEventSink {

    private val lock = Any()
    private val timestamps = ArrayDeque<Long>()

    override fun emit(event: SecurityEvent) {
        val now = event.timestampMillis
        val allowed = synchronized(lock) {
            while (timestamps.isNotEmpty() && now - timestamps.first() > windowMs) {
                timestamps.removeFirst()
            }
            if (timestamps.size >= maxEventsPerWindow) {
                false
            } else {
                timestamps.addLast(now)
                true
            }
        }
        if (allowed) {
            delegate.emit(event)
        }
    }
}

/**
 * 只转发某几个 `code` 的出口（例如只关心篡改类，不关心环境类）。
 *
 * @param codes 允许通过的 code 集合；空集合表示全部放行
 */
class FilteredEventSink(
    private val delegate: SecurityEventSink,
    private val codes: Set<String>,
) : SecurityEventSink {
    override fun emit(event: SecurityEvent) {
        if (codes.isEmpty() || event.code in codes) {
            delegate.emit(event)
        }
    }
}
