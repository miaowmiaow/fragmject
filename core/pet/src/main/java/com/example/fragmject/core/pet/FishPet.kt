/**
 * 漂浮宠物组件。
 *
 * 原项目：https://github.com/Jichensheng/Fish_2
 * 原项目是两个类：`FishDrawable`（画）+ `FishDrawableView`（触摸并让鱼沿路径移动）。
 * 这里重写为纯 Compose：绘制走 `Canvas`，位置走 `graphicsLayer` 位移（只更新图层，不触发重绘）。
 */
package com.example.fragmject.core.pet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.designsystem.AppColors
import kotlinx.coroutines.isActive
import kotlin.random.Random

/** 默认头半径：设置页以它为 1.0 倍基准，改这里所有消费方一起变。 */
val FishPetDefaultHeadRadius = 14.dp

/** 鱼节点边长 = 8.38R（与原实现 `getIntrinsicWidth/Height` 一致，保证旋转后不裁切）。 */
internal const val NODE_RATIO = 8.38f

/** 涟漪最大半径（与原实现 `DEFAULT_RADIUS = 150` 的 px 值对应）。 */
internal val RIPPLE_MAX_RADIUS = 150.dp

private const val NANOS_PER_SECOND = 1_000_000_000f

/** 单帧上限：卡顿/后台回来后不要让鱼「瞬移」一大段。 */
private const val MAX_FRAME_SECONDS = 0.05f

/**
 * 一次轻点。[id] 由调用方自增，用来识别「同一个点的重复点击」
 * （只比较 [position] 的话，连点同一处不会触发第二次涟漪与冲刺）。
 */
data class FishPetTap(
    val position: Offset,
    val id: Int,
    /**
     * 事件时间（`SystemClock.uptimeMillis()`）。
     *
     * **必填**：给它默认值曾导致"某个构造点忘了传 → 时间戳为 0 → 页面切换时被
     * 新鲜度判断整条丢弃 → 穿越静默失效"。没有默认值，编译器会替我们兜住。
     */
    val atMillis: Long,
)

/**
 * 一次「真正的换页」（导航返回栈变化：跳详情、返回、深链）。
 *
 * 与 [FishPetTap] 的区别是**语义**：普通点击是"过来看看"（游过去），
 * 页面切换是"页面都换了"，鱼应该直接穿越到新位置，而不是横穿新页面。
 *
 * 底部 tab **不**产生本事件：它只换同页内容，鱼维持"游过去"。
 */
data class FishPetTeleport(val position: Offset, val id: Int)

/**
 * 一次快速划过（手指速度超过 `FishPetInput.kt` 中 `SWIPE_SPEED_DP_PER_SECOND` 且离鱼较近）。
 *
 * 与 [FishPetTap] 分开是因为语义完全不同：轻点是"过来看看"，快速划过是"被吓到"。
 */
data class FishPetSwipe(val position: Offset, val id: Int)

/** 与密度相关的尺寸与游动参数一次性算好，避免每个私有 composable 都重复 `toPx()`。 */
internal class FishMetrics(
    val density: Density,
    val headRadiusPx: Float,
    val nodeSize: Dp,
    val nodeSizePx: Float,
    val rippleRadiusPx: Float,
    speedScale: Float,
) {
    val rippleStrokePx: Float = with(density) { 2.dp.toPx() }
    /**
     * 随机目标点的内缩量。
     *
     * 以前用节点的一半（≈4.2R），鱼永远在内圈打转、碰不到边缘；
     * 改成一个头半径：目标可以贴着边，身体自然压到屏幕外一点点。
     */
    private val marginPx: Float = headRadiusPx * 0.9f

    /**
     * 游动参数按头半径成比例：这样改 `headRadius` 时「每秒几个身位」保持不变，
     * 小屏大屏的观感一致（写死 px 会在平板上看起来像慢动作）。
     *
     * [speedScale] 只缩放线速度与加/减速度，**不缩放** `maxTurnRate`：
     * 转弯角速度跟着放大会让鱼在低速档显得「打滑」。
     */
    val tuning: FishSwimTuning = FishSwimTuning(
        maxSpeed = headRadiusPx * 7.5f * speedScale,
        maxTurnRate = 170f,
        acceleration = headRadiusPx * 14f * speedScale,
        deceleration = headRadiusPx * 10f * speedScale,
        slowRadius = headRadiusPx * 5f,
        arriveRadius = headRadiusPx * 0.6f,
        minSpeed = headRadiusPx * 1.2f * speedScale,
    )

    /** 随机目标点：落在 [marginPx] 内切的安全区里，鱼不会被画到容器外。 */
    fun randomTarget(bounds: Size): Offset {
        val usableWidth = (bounds.width - marginPx * 2f).coerceAtLeast(1f)
        val usableHeight = (bounds.height - marginPx * 2f).coerceAtLeast(1f)
        return Offset(
            x = marginPx + Random.nextFloat() * usableWidth,
            y = marginPx + Random.nextFloat() * usableHeight,
        )
    }
}

/**
 * 鱼的瞬时状态。
 *
 * 只在**绘制阶段**读取：
 * - [middle] / [bobR] / [jitter] / [leapLiftR] / [leapScale] 由 `graphicsLayer` 与变换消费；
 * - [angle] / [phase] / [vigor] / [opacity] / [stretchLong] 等由绘制 lambda 消费。
 *
 * [target] / [speed] / [urge] 只有运行时读，用普通字段即可，不必进快照系统。
 */
@Stable
internal class FishMotion {
    var middle by mutableStateOf(Offset.Zero)
    var angle by mutableFloatStateOf(0f)
    var phase by mutableFloatStateOf(0f)
    var vigor by mutableFloatStateOf(0.3f)

    /** 0..1：情绪唤醒度。受惊拉满、约 5s 衰减回平静 */
    var arousal by mutableFloatStateOf(0f)

    var sleeping by mutableStateOf(false)

    /** 打盹浮动（R 的倍数，正值向下） */
    var bobR by mutableFloatStateOf(0f)

    /** 跃起：放大倍率、屏幕上抛（R）、影子偏移（R）、影子透明度与缩放 */
    var leapScale by mutableFloatStateOf(1f)
    var leapLiftR by mutableFloatStateOf(0f)
    var shadowOffsetR by mutableFloatStateOf(0f)
    var shadowAlpha by mutableFloatStateOf(0f)
    var shadowScale by mutableFloatStateOf(1f)

    /** 影子的水面锚点（世界坐标，跃起时 = 起跳点）；Zero 表示就在鱼脚下 */
    var shadowCenter by mutableStateOf(Offset.Zero)

    /** 受惊：整体不透明度、沿运动方向的挤压拉伸 */
    var opacity by mutableFloatStateOf(0.88f)
    var stretchLong by mutableFloatStateOf(1f)
    var stretchShort by mutableFloatStateOf(1f)

    /** 受惊逃逸时的位置抖动（px） */
    var jitter by mutableStateOf(Offset.Zero)

    /** 穿越已进行的时间（秒）；负数表示不在穿越中 */
    var teleportElapsed by mutableFloatStateOf(-1f)

    /** 穿越的目标点（淡出期间先记着，完全不可见时才瞬移过去） */
    var teleportTo by mutableStateOf(Offset.Zero)

    /** 水面：可同时存在多圈涟漪 */
    var ripples by mutableStateOf<List<FishRipple>>(emptyList())

    /** 落水水花（只在落水后 0.35s 内非空） */
    var splash by mutableStateOf<FishSplash?>(null)

    /** 身后的残影（0~2 个） */
    var ghosts by mutableStateOf<List<FishGhost>>(emptyList())

    var target: Offset = Offset.Zero
    var speed: Float = 0f
    var urge: Float = 0f
}

/**
 * [FishMotion] 的保存策略：只存决定「鱼看起来在哪」的三个量。
 *
 * ============================ 为什么需要 ============================
 * `FishMotion` 随组合 `remember`，Activity 重建（切深色模式、分屏、折叠屏展开）会丢弃整个组合，
 * 于是 `FishSwimRuntime` 从容器中心重新起步，还会因为 `middle == Offset.Zero` 再补一圈「落水」涟漪——
 * 表现为「鱼先消失一帧，再从中心带涟漪出现」。应用级 `PetSettingsState` 只能消除前半段，
 * 位置延续必须由状态自己带上。
 *
 * ============================ 为什么只存这三个 ============================
 * - `FloatArray` 是 SavedState 原生支持的类型，不需要自定义 Parcelable；
 * - `middle` / `angle` / `speed` 决定重建后第一帧的样子，其余（打盹、跃起、受惊、涟漪、残影）
 *   都是几秒内自收敛的瞬时表现，重建后从头开始看不出异常。
 *
 * 已知限制：存的是 px，若进程重建时密度变了，位置会按旧密度还原（`configChanges` 已含 `density`，
 * 常规场景不会触发）。
 */
private val FishMotionSaver: Saver<FishMotion, FloatArray> = Saver(
    save = { motion -> floatArrayOf(motion.middle.x, motion.middle.y, motion.angle, motion.speed) },
    restore = { saved ->
        FishMotion().apply {
            middle = Offset(saved[0], saved[1])
            angle = saved[2]
            speed = saved[3]
        }
    },
)

/**
 * 应用内漂浮宠物鱼：自主游动 + 点击接近 + 一整套习性动作。
 *
 * ============================ 用法（**必须**照此接线）============================
 * ```
 * val hub = rememberPetInputHub()                    // 组合根持有事件与 id/时间戳
 * Box(Modifier.fillMaxSize().fishPointerObserver(enabled, hub::emitTap, hub::emitSwipe)) {
 *     AppContent()                                   // 子节点，事件照常送达
 *     FishPet(tap = hub.tap, swipe = hub.swipe, teleport = hub.teleport)
 * }
 * ```
 * 观察器为什么必须在父容器上，见 `fishPointerObserver`；事件为什么由外部组装，见
 * `app/ui/PetInputHub.kt`（本库只上报位置，不持有可写状态）。
 *
 * ============================ 行为 ============================
 * 打盹 / 惊醒 / 受惊（僵直→逃逸→回头→恢复）/ 跃起 / 好奇绕圈 / 原地打转 / 三段冲刺，
 * 调度见 [FishSwimRuntime]，表现见 `FishPoseFrames` 与 `FishLeap`。
 *
 * ============================ 灵动性来自哪 ============================
 * 1. **转向**：Reynolds steering（seek + arrive + 角速度限幅）；
 * 2. **行波**：鲹科游动方程，相位从头到尾滞后、振幅递增；
 * 3. **随速调频**：摆尾频率与振幅随速度/角速度上升；
 * 4. **习性动作**：冲刺、悬停、打盹、受惊、跃起……让"看 10 秒"和"看 1 分钟"不一样。
 *
 * @param headRadius 鱼头半径，也是唯一的尺寸入口
 * @param color 鱼身主色
 * @param opacity 整体不透明度 0..1（乘进各部件 alpha，不做离屏图层）
 * @param speedScale 游动速度倍率
 * @param enabled 关掉后鱼停在原地且不再响应点击（用于省电/设置开关）
 * @param tap 外部观察到的轻点；null 表示不响应
 * @param swipe 外部观察到的快速划过；null 表示不响应
 * @param teleport 外部观察到的换页；null 表示不响应
 */
@Composable
fun FishPet(
    modifier: Modifier = Modifier,
    headRadius: Dp = FishPetDefaultHeadRadius,
    color: Color = AppColors.fishBody,
    opacity: Float = 1f,
    speedScale: Float = 1f,
    enabled: Boolean = true,
    tap: FishPetTap? = null,
    swipe: FishPetSwipe? = null,
    teleport: FishPetTeleport? = null,
) {
    val density = LocalDensity.current
    val metrics = remember(density, headRadius, speedScale) {
        val headRadiusPx = with(density) { headRadius.toPx() }
        FishMetrics(
            density = density,
            headRadiusPx = headRadiusPx,
            nodeSize = with(density) { (headRadiusPx * NODE_RATIO).toDp() },
            nodeSizePx = headRadiusPx * NODE_RATIO,
            rippleRadiusPx = with(density) { RIPPLE_MAX_RADIUS.toPx() },
            speedScale = speedScale,
        )
    }
    // 位置/朝向/速度进 SavedState：Activity 重建后从原处续上，而不是回中心重来（见 FishMotionSaver）。
    // 仍用 rememberSaveable 而不是外部传入，是为了让「保留」成为组件自身的能力，:app 无需知情。
    val motion = rememberSaveable(saver = FishMotionSaver) { FishMotion() }
    // 用 Box + onSizeChanged 而不是 BoxWithConstraints：
    // 后者是 SubcomposeLayout，覆盖层挂在页面之上时，宿主每次重组都可能触发一次重新
    // subcompose（转场期间宿主重组频繁）。这里只需要容器尺寸，普通布局 + 回调就够，
    // 且尺寸稳定后内容可以被跳过。
    var bounds by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { size -> bounds = Size(size.width.toFloat(), size.height.toFloat()) },
    ) {
        FishSwimDriver(motion, enabled, bounds, metrics, tap, swipe, teleport)
        FishRippleLayer(motion, metrics, opacity)
        FishBody(motion, metrics, color, opacity)
    }
}

/**
 * 驱动：一个帧循环推进运行时，另有两个 effect 把外部输入喂给运行时。
 *
 * 运行时用 `remember(bounds, metrics)` 持有：改尺寸/改鱼的大小时重建一次，
 * 其余重组（主题、开关）不会打断运动状态。
 */
@Composable
private fun FishSwimDriver(
    motion: FishMotion,
    enabled: Boolean,
    bounds: Size,
    metrics: FishMetrics,
    tap: FishPetTap?,
    swipe: FishPetSwipe?,
    teleport: FishPetTeleport?,
) {
    val runtime = remember(bounds, metrics) { FishSwimRuntime(motion, metrics, bounds) }
    LaunchedEffect(tap?.id) {
        tap?.let { runtime.onTap(it.position) }
    }
    LaunchedEffect(swipe?.id) {
        swipe?.let { runtime.onFastSwipe(it.position) }
    }
    LaunchedEffect(teleport?.id) {
        teleport?.let { runtime.onTeleport(it.position) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(enabled, runtime, bounds, lifecycle) {
        if (!enabled || bounds.minDimension <= 0f) return@LaunchedEffect
        // 只在 STARTED 之后跑：`LaunchedEffect` 的寿命等于**组合**，退到后台后组合还在，
        // 而 Choreographer 仍会驱动 `withFrameNanos`——等于在后台持续 60fps 写快照、
        // 重绘不可见的画面（耗电，也会和真正的动画抢帧）。
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastNanos = 0L
            while (isActive) {
                val nanos = withFrameNanos { it }
                val dt = if (lastNanos == 0L) 0f else (nanos - lastNanos) / NANOS_PER_SECOND
                lastNanos = nanos
                // 不再做「静止降频」：打盹时 20fps 的尾巴会读成掉帧
                if (dt > 0f) runtime.advance(dt.coerceAtMost(MAX_FRAME_SECONDS))
            }
        }
    }
}
