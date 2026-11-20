/**
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 触摸观察。
 *
 * 原实现在 `FishDrawableView.onTouchEvent` 里**消费**整屏事件，鱼跟着手指游。
 * 这里反过来了：只观察不消费（挂在父容器 + `PointerEventPass.Initial`），
 * 页面本身的点击、滚动、长按全部照常工作。
 */
package com.example.fragmject.core.pet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * 快速划过的速度阈值（**dp/s**，不是 px/s）：超过它才算"被吓到"，否则只是普通拖动。
 *
 * 用 dp/s 而不是 px/s：px/s 随屏幕密度缩放，同一物理手势在 xxhdpi 上数值是 mdpi 的 3 倍，
 * 阈值写死 px 会让高密度屏频繁误判、低密度屏几乎触发不了。800dp/s 取的是原实现在
 * 3 倍密度屏（xxhdpi）上的等效值，手势手感与原实现一致。
 */
private const val SWIPE_SPEED_DP_PER_SECOND = 800f

/**
 * 指针观察器：在 `PointerEventPass.Initial` 阶段读取轻点与快速划过，**不消费**事件。
 *
 * 只上报位置，不持有事件状态：id 自增、时间戳、字段语义都由调用方的输入中枢组装
 * （组合根见 `app/ui/pet/PetInputHub.kt`）。这样本库不需要 `SystemClock`，也不会把
 * 可写状态交给调用方。
 *
 * ============================ 必须挂在父容器上 ============================
 * 正确用法是包住**页面内容**：
 * ```
 * Box(Modifier.fillMaxSize().fishPointerObserver(enabled, onTap = ::emitTap, onSwipe = ::emitSwipe)) {
 *     AppContent()                                   // 子节点，事件照常送达
 *     FishPet(tap = tap, swipe = swipe)
 * }
 * ```
 * **不要**挂到与内容平级、铺满全屏的兄弟节点上：Compose 命中测试在命中一个指针节点后
 * 默认不再向下共享（`sharePointerInputWithSiblings()` 默认 false），整页都会点不动。
 *
 * ============================ 为什么是 `@Composable` ============================
 * 回调用 `rememberUpdatedState` 承接，`pointerInput` 的 key 只有 `enabled`：
 * 调用方传方法引用（`hub::emitTap`）还是 lambda，手势协程都不会因重组重启，且总能调到最新的回调。
 *
 * **不要改成把回调放进 `pointerInput` 的 key**（`pointerInput(enabled, onTap, onSwipe)`）：
 * 那样等于要求每个调用方都传「`equals` 稳定」的引用，一旦有人写成 lambda，每次重组都会重启
 * 手势协程——症状是拖动中丢手势、点一下没反应，且**静默**无报错。
 *
 * ============================ 为什么忽略"是否被消费" ============================
 * 父容器的 Initial 阶段一定先于子节点收到同一次事件，因此这里**不看** `isConsumed`：
 * 点按钮、点列表项时鱼也要有反应，否则宠物会显得"只在空白处活"。
 * 代价是滚动页面时也会触发轻点——用 `touchSlop` 区分：位移超过阈值就当拖动，丢弃。
 */
@Composable
fun Modifier.fishPointerObserver(
    enabled: Boolean,
    onTap: (Offset) -> Unit,
    onSwipe: (Offset) -> Unit,
): Modifier {
    // pointerInput 的 lambda 只在 key 变化时重建，直接捕获回调会锁死第一次组合的 lambda
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        // PointerInputScope 自带 Density：阈值在这里换算成 px/s，只算一次
        val swipeSpeedPxPerSecond = SWIPE_SPEED_DP_PER_SECOND.dp.toPx()
        awaitPointerEventScope {
            var downPosition: Offset? = null
            var lastPosition = Offset.Zero
            var lastMillis = 0L
            while (true) {
                // 多指按下不当作轻点/划过
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.singleOrNull()
                if (change == null) {
                    downPosition = null
                    continue
                }
                val result = handlePointerChange(
                    change = change,
                    downPosition = downPosition,
                    touchSlop = viewConfiguration.touchSlop,
                    swipeSpeedPxPerSecond = swipeSpeedPxPerSecond,
                    lastPosition = lastPosition,
                    lastMillis = lastMillis,
                )
                downPosition = result.downPosition
                lastPosition = result.lastPosition
                lastMillis = result.lastMillis
                result.swipeAt?.let { currentOnSwipe(it) }
                result.tapAt?.let { currentOnTap(it) }
            }
        }
    }
}

/** 一次事件处理的结果：更新后的跟踪状态，以及本帧是否产生轻点/划过。 */
private data class PointerResult(
    val downPosition: Offset?,
    val lastPosition: Offset,
    val lastMillis: Long,
    val tapAt: Offset? = null,
    val swipeAt: Offset? = null,
)

/**
 * 单指事件的状态机。
 *
 * 单独抽出来是为了让 [fishPointerObserver] 保持可读（内联写会超过 detekt 的复杂度阈值）。
 */
private fun handlePointerChange(
    change: PointerInputChange,
    downPosition: Offset?,
    touchSlop: Float,
    swipeSpeedPxPerSecond: Float,
    lastPosition: Offset,
    lastMillis: Long,
): PointerResult = when {
    change.pressed && !change.previousPressed -> PointerResult(
        downPosition = change.position,
        lastPosition = change.position,
        lastMillis = change.uptimeMillis,
    )

    change.pressed -> {
        val seconds = (change.uptimeMillis - lastMillis) / 1000f
        val speed = if (seconds > 0f) (change.position - lastPosition).getDistance() / seconds else 0f
        PointerResult(
            downPosition = downPosition,
            lastPosition = change.position,
            lastMillis = change.uptimeMillis,
            swipeAt = change.position.takeIf { speed > swipeSpeedPxPerSecond },
        )
    }

    change.previousPressed -> {
        val isTap = downPosition != null && (change.position - downPosition).getDistance() <= touchSlop
        PointerResult(
            downPosition = null,
            lastPosition = lastPosition,
            lastMillis = lastMillis,
            tapAt = change.position.takeIf { isTap },
        )
    }

    else -> PointerResult(downPosition, lastPosition, lastMillis)
}
