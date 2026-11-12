package com.example.security

/**
 * 检测项开关（按「检测维度」而不是「具体信号」划分）。
 *
 * ============================ 为什么需要 ============================
 * 库对外交付后，不同宿主的风控尺度不一样：
 * - 金融类希望全开；
 * - 内容类可能想关掉 `PORT_SCAN`（部分企业网络会误报）或 `ROOT`（定制 ROM 用户多）；
 * - 内部测试包只想留 `DEBUGGER` 观察。
 *
 * 用一个集合表达「启用哪些维度」，比给每项加一个 Boolean 更好扩展：
 * 新增检测项时只需加一个枚举值，宿主不传就默认全开，不会破坏既有调用。
 *
 * ============================ 与 Action 的关系 ============================
 * [SecurityCheck] 决定「**检不检**」，[Action] 决定「检到之后**怎么办**」（仅环境类项）。
 * 两者都关闭时以「不检测」为准：不检测就不会产生事件。
 */
enum class SecurityCheck {

    /** 调试器：native TracerPid + Java 层 Debug 接口。 */
    DEBUGGER,

    /** 注入类：Frida maps/线程/落地文件、匿名可执行段。 */
    INJECTION,

    /** libc `.text` CRC（inline hook 检测），本身已按时间降频。 */
    LIB_CRC,

    /** Frida Server 端口扫描（27042-27052），本身已按时间降频。 */
    PORT_SCAN,

    /** Root 环境（处置级别由 [SecurityConfig.rootAction] 决定）。 */
    ROOT,

    /** 模拟器环境（处置级别由 [SecurityConfig.emulatorAction] 决定）。 */
    EMULATOR,
    ;

    companion object {
        /** 默认全开：不传 `checks` 的老调用点行为不变。 */
        @JvmField
        val ALL: Set<SecurityCheck> = entries.toSet()
    }
}
