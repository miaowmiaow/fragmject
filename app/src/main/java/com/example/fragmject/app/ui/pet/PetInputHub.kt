package com.example.fragmject.app.ui.pet

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.fragmject.core.pet.FishPetSwipe
import com.example.fragmject.core.pet.FishPetTap
import com.example.fragmject.core.pet.FishPetTeleport

/**
 * 宠物的输入中枢：收拢「谁在什么时候让鱼做什么」的三类事件。
 *
 * | 事件 | 来源 | 宠物反应 |
 * |---|---|---|
 * | 普通点击 | `fishPointerObserver` → [emitTap] | 游过去（原有逻辑） |
 * | 快速划过 | 同上 → [emitSwipe] | 受惊 |
 * | **页面切换** | 导航返回栈变化 → [notifyRouteChanged] | **穿越**到最后一次点击的位置 |
 *
 * 底部 tab 不在此列：它只换同页内容，鱼维持"游过去"，避免突兀的瞬移。
 *
 * ============================ 为什么写权限全部收回本类 ============================
 * 事件 id 自增、时间戳、穿越判定三件事只在 [emitTap] / [emitSwipe] / [notifyRouteChanged]
 * 里发生：状态 `private set`，外部只能读。此前的写法把 `MutableState` 直接交给
 * `core:pet` 的观察器和 `FloatingPet`，id 在三处各写一份，`SystemClock` 还落在 UI 库，
 * 结果是「谁都能造一个事件」。时钟走构造参数注入，[PetInputHubTest] 才能断言时间戳。
 *
 * ============================ 为什么导航用「自增 id」而不是布尔 ============================
 * 布尔量只有 true/false 两个状态：`true → true` 不产生变化，Compose 的
 * `LaunchedEffect(key)` 不会重跑。若 300ms 内连续两次跳转（点条目 → 返回 → 再点），
 * 第二次就拿不到穿越机会。每次通知都给 [teleport] 换一个新 id，订阅方必然重跑。
 */
@Stable
class PetInputHub internal constructor(
    private val clock: () -> Long = SystemClock::uptimeMillis,
) {
    /** 最近一次轻点；null 表示本轮还没有。 */
    var tap: FishPetTap? by mutableStateOf(null)
        private set

    /** 最近一次快速划过；null 表示本轮还没有。 */
    var swipe: FishPetSwipe? by mutableStateOf(null)
        private set

    /** 最近一次穿越请求；null 表示本轮还没有。 */
    var teleport: FishPetTeleport? by mutableStateOf(null)
        private set

    /** 记录一次轻点：id 自增 + 打时间戳（穿越判定要用）。 */
    fun emitTap(position: Offset) {
        tap = FishPetTap(
            position = position,
            id = (tap?.id ?: 0) + 1,
            atMillis = clock(),
        )
    }

    /** 记录一次快速划过：只有位置与自增 id，不参与穿越判定。 */
    fun emitSwipe(position: Offset) {
        swipe = FishPetSwipe(position = position, id = (swipe?.id ?: 0) + 1)
    }

    /**
     * 导航返回栈变化（跳详情、返回、深链）。
     *
     * 只有 1s 内的点击才算"引发这次切换"：返回手势、深链、代码内跳转没有新点击，
     * 用旧坐标穿越会像随机瞬移（判定见 [TeleportGate]）。
     */
    fun notifyRouteChanged() {
        val lastTap = tap ?: return
        if (!TeleportGate.shouldTeleport(clock(), lastTap.atMillis)) return
        teleport = FishPetTeleport(
            position = lastTap.position,
            id = (teleport?.id ?: 0) + 1,
        )
    }

    /**
     * 清空三类事件。
     *
     * 由 `FloatingPet` 的 `onDispose` 调用（不是由开关状态触发）：本类的寿命长于装饰层
     * （前者随 Activity，后者随开关组合/摘除），若不清空，装饰层重新进入组合时
     * `FishSwimDriver` 首次组合会立刻把**旧事件**重放一遍——旧穿越坐标在 `bounds = Size.Zero`
     * 下会被夹到左上角（见 P1）。
     *
     * 之所以由 `onDispose` 驱动：不变量是「装饰层离开组合 ⇒ 事件已清空」，与离开的原因无关；
     * 若改成监听「宠物已关闭」，以后任何新的显示条件都会漏掉清理。
     */
    fun reset() {
        tap = null
        swipe = null
        teleport = null
    }
}

/** 创建输入中枢（纯状态，不含计时器：触发时机由消费方按 key 变化判断）。 */
@Composable
fun rememberPetInputHub(clock: () -> Long = SystemClock::uptimeMillis): PetInputHub = remember {
    PetInputHub(clock)
}
