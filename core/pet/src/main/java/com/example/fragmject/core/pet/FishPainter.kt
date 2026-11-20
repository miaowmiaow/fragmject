/**
 * 鱼的绘制，移植自原项目 https://github.com/Jichensheng/Fish_2 的 `FishDrawable`。
 *
 * 头部圆、两节躯干、双鳍、开合鱼尾的尺寸比例与 `sin(currentValue * k)` 摆动公式都和原实现一一对应；
 * 差别只在入口：原实现是 `Drawable.onDraw(Canvas)`，这里是 `DrawScope` 扩展函数。
 */
package com.example.fragmject.core.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.fragmject.core.designsystem.AppColors
import kotlin.math.sin

/** 鱼的默认主色（原实现的朱红），色值唯一落点是 [AppColors.fishBody]。 */

/** 头/躯干/尾：原实现 `OTHER_ALPHA = 160`。 */
private const val OTHER_ALPHA = 160f / 255f

/** 最大那片躯干：原实现 `BODY_ALPHA + 5 = 225`。 */
private const val BODY_ALPHA = 225f / 255f

/** 鱼鳍：原实现 `FINS_ALPHA = 100`。 */
private const val FINS_ALPHA = 100f / 255f

/** 躯干长度 = 3.2R（原实现 `BODY_LENGHT`）。 */
private const val BODY_LENGTH_RATIO = 3.2f

/**
 * 复用的路径缓冲。
 *
 * 每帧要重建 4 段轮廓（鳍 / 躯干 / 节肢梯形 / 尾三角）；若每帧 `Path()` 新建，
 * 60fps 下约 240 个对象/秒，会在页面转场或低端机上叠加成 GC 尖峰。
 * `rewind()` 保留已分配的点数组、只清空指令，配合复用可以把这部分分配降到 0。
 *
 * 一个实例就够：所有 `drawPath` 都是"构形 → 立刻绘制"，不存在嵌套复用。
 */
internal class FishPaths {
    val path: Path = Path()
}

/**
 * 一次绘制所需的动态参数：相位、活力与头半径。
 *
 * 收敛成一个参数是因为尾/节肢的私有函数本就需要这三者，逐个摊开会让参数列表超过 detekt 的 8 个上限。
 */
private data class FishFrame(
    val phaseDegrees: Float,
    val vigor: Float,
    val headRadius: Float,
)

/**
 * 行波相位滞后（度）：从头到尾逐节增大，构成 `y(x,t) = a(x)·sin(kx − ωt)` 里的 `kx` 项。
 *
 * 这是鲹科（carangiform）游动的关键：振幅从头到尾递增、相位从头到尾**传递**。
 * 原实现给各节用了不同频率（1.2 / 1.5 / 1.7），得到的是「各节各自抖」的拍频观感，
 * 而不是一道波从身体上流过；改成同频 + 固定滞后后，扭动才连贯。
 */
private const val LAG_SEGMENT1 = 30f
private const val LAG_SEGMENT2 = 65f
private const val LAG_TAIL = 100f
private const val LAG_FIN = -90f

/** 鱼鳍长度 = 1.3R（原实现 `FINS_LENGTH`）。 */
private const val FINS_LENGTH_RATIO = 1.3f

/**
 * 画一条鱼：头 + 双鳍 + 两节躯干 + 开合鱼尾，全部是 Canvas 图元，无图片资源。
 *
 * 移植自「灵动的红鲤鱼」的 `FishDrawable`，几何比例与绘制顺序逐项对照原实现：
 * 头 → 鳍 → 躯干节肢 → 尾 → 最大那片躯干（最后画，压住前面的接缝）。
 *
 * 与原实现的两处有意偏差：
 * 1. 原实现用 `saveLayerAlpha(240)` 把整条鱼压到 94% 不透明；这里不做离屏图层，
 *    改为各部件自身的 alpha，省掉每帧一次 `saveLayer`（小尺寸下视觉差异不可见）。
 * 2. 原实现的鱼鳍角度由 `ObjectAnimator` 随机次数抖动；这里用相位驱动连续摆动。
 *
 * @param middle 鱼身重心（也即节点的中心），所有部件的坐标都以它为原点
 * @param headRadius 头半径，其余尺寸全部按它成比例，因此这是唯一的缩放入口
 * @param mainAngle 身体主轴方向（度），口径见 [FishGeometry]
 * @param phaseDegrees 行波相位（度），由调用方按「速度越快推进越快」积分
 * @param vigor 活力 0..1.5：同时放大主轴摆角、节肢摆角、尾张合幅度与鱼鳍幅度。
 *   慢速滑行时接近 0（像在水里漂），冲刺/急转时接近 1.5（甩得最狠）
 * @param opacity 整体不透明度 0..1，乘进各部件的 alpha（不做离屏图层，省一次全屏 saveLayer）
 *
 * 仅模块内可见：它只服务 `FishPetRenderer` 与渲染侧的特效；暴露到模块外等于承诺
 * 一个我们不打算维护的绘制 API（实验台已删除，没有第二个消费者）。
 */
internal fun DrawScope.drawFish(
    middle: Offset,
    headRadius: Float,
    mainAngle: Float,
    phaseDegrees: Float,
    vigor: Float,
    paths: FishPaths,
    color: Color = AppColors.fishBody,
    opacity: Float = 1f,
) {
    val frame = FishFrame(phaseDegrees, vigor, headRadius)
    val fade = opacity.coerceIn(0f, 1f)
    val other = color.copy(alpha = OTHER_ALPHA * fade)
    val body = color.copy(alpha = BODY_ALPHA * fade)
    val fins = color.copy(alpha = FINS_ALPHA * fade)
    val bodyLength = BODY_LENGTH_RATIO * headRadius

    // 主轴摆角：慢速 1.2°，冲刺 ~3.9°（原实现固定 2°）
    val bodyAngle = mainAngle + sinDeg(phaseDegrees) * (1.2f + 2.6f * vigor)
    val head = FishGeometry.point(middle, bodyLength / 2f, mainAngle)

    drawCircle(other, radius = headRadius, center = head)

    // 鱼鳍与身体反相摆动（原实现是随机次数的一次性抖动）
    val finsAngle = (20f + 35f * vigor) * (0.5f + 0.5f * sinDeg(phaseDegrees + LAG_FIN))
    drawFin(FishGeometry.point(head, headRadius * 0.9f, bodyAngle - 110f), true, finsAngle, bodyAngle, fins,
        headRadius, paths
    )
    drawFin(FishGeometry.point(head, headRadius * 0.9f, bodyAngle + 110f), false, finsAngle, bodyAngle, fins,
        headRadius, paths
    )

    // 两节躯干 + 尾先画，最大那片躯干最后画（覆盖接缝，与原实现顺序一致）
    val bodyEnd = FishGeometry.point(head, bodyLength, bodyAngle - 180f)
    drawTailSegments(bodyEnd, headRadius * 0.7f, 0.6f, bodyAngle, other, frame, paths)
    drawBody(head, bodyEnd, bodyAngle, bodyLength, headRadius, body, paths)
}

/**
 * 两节躯干：第一节半径 0.7R、上下比例 0.6，第二节缩到 0.6 倍并接上鱼尾。
 *
 * 每一节都由「两个圆 + 一个梯形（quad）」拼成，节与节之间的角度各自滞后，
 * 这正是原实现里鱼身「逐节扭动」的来源。
 */
private fun DrawScope.drawTailSegments(
    mainPoint: Offset,
    segmentRadius: Float,
    ratio: Float,
    fatherAngle: Float,
    color: Color,
    frame: FishFrame,
    paths: FishPaths,
) {
    // 第一节：慢速 8°、冲刺 ~35° 的摆动，相位比主轴滞后 LAG_SEGMENT1
    val angle = fatherAngle + sinDeg(frame.phaseDegrees - LAG_SEGMENT1) * (8f + 18f * frame.vigor)
    val length = segmentRadius * (ratio + 1f)
    val endPoint = FishGeometry.point(mainPoint, length, angle - 180f)
    drawCircle(color, segmentRadius, mainPoint)
    drawCircle(color, segmentRadius * ratio, endPoint)
    drawQuad(mainPoint, endPoint, segmentRadius, segmentRadius * ratio, angle, color, paths)

    // 第二节：振幅更大、滞后更多（行波越靠尾部摆幅越大）
    val nextRadius = segmentRadius * 0.6f
    val nextRatio = 0.4f
    val nextAngle = angle + sinDeg(frame.phaseDegrees - LAG_SEGMENT2) * (18f + 34f * frame.vigor)
    val nextLength = nextRadius * (nextRatio + 2.7f)
    val nextEnd = FishGeometry.point(endPoint, nextLength, nextAngle - 180f)
    drawTail(endPoint, nextLength, nextRadius, nextAngle, color, frame, paths)
    drawCircle(color, nextRadius * nextRatio, nextEnd)
    drawQuad(endPoint, nextEnd, nextRadius, nextRadius * nextRatio, nextAngle, color, paths)
}

/** 鱼尾：内外两个三角形，宽度按 `sin(currentValue * 1.7 * wave)` 张合。 */
private fun DrawScope.drawTail(
    mainPoint: Offset,
    length: Float,
    maxWidth: Float,
    angle: Float,
    color: Color,
    frame: FishFrame,
    paths: FishPaths,
) {
    // 原实现里的 10 / 20 是按 HEAD_RADIUS = 30 写死的像素，这里按 1/3、2/3 半径换算
    val shrink = frame.headRadius / 3f
    // 张合幅度随活力变化：慢速时轻轻摆，冲刺时几乎完全张开。
    // 这里用 (1 + sin)/2 而不是 abs(sin)：后者每周期开合两次，视觉上等于频率翻倍（实测「太快」）。
    val swing = (0.55f + 1.1f * frame.vigor) * (0.5f + 0.5f * sinDeg(frame.phaseDegrees - LAG_TAIL))
    val width = swing * maxWidth + frame.headRadius * 0.6f
    val endPoint = FishGeometry.point(mainPoint, length, angle - 180f)
    val innerEnd = FishGeometry.point(mainPoint, length - shrink, angle - 180f)
    val outerLeft = FishGeometry.point(endPoint, width, angle - 90f)
    val outerRight = FishGeometry.point(endPoint, width, angle + 90f)
    val innerLeft = FishGeometry.point(innerEnd, width - shrink * 2f, angle - 90f)
    val innerRight = FishGeometry.point(innerEnd, width - shrink * 2f, angle + 90f)

    drawTriangle(mainPoint, innerLeft, innerRight, color, paths)
    drawTriangle(mainPoint, outerLeft, outerRight, color, paths)
}

/** 鱼鳍：以起点为顶点、二次贝塞尔一侧鼓起的三角，[right] 决定镜像方向。 */
private fun DrawScope.drawFin(
    start: Offset,
    right: Boolean,
    finsAngle: Float,
    fatherAngle: Float,
    color: Color,
    headRadius: Float,
    paths: FishPaths,
) {
    val length = FINS_LENGTH_RATIO * headRadius
    val endAngle = if (right) fatherAngle - finsAngle - 180f else fatherAngle + finsAngle + 180f
    val controlAngle = if (right) fatherAngle - 115f - finsAngle else fatherAngle + 115f + finsAngle
    val end = FishGeometry.point(start, length, endAngle)
    val control = FishGeometry.point(start, length * 1.8f, controlAngle)
    drawPath(
        paths.path.also { it.rewind() }.apply {
            moveTo(start.x, start.y)
            quadraticTo(control.x, control.y, end.x, end.y)
            close()
        },
        color,
    )
}

/** 最大那片躯干：四条二次贝塞尔围成的纺锤形，控制点决定胖瘦。 */
private fun DrawScope.drawBody(
    head: Offset,
    bodyEnd: Offset,
    angle: Float,
    bodyLength: Float,
    headRadius: Float,
    color: Color,
    paths: FishPaths,
) {
    val topLeft = FishGeometry.point(head, headRadius, angle - 80f)
    val topRight = FishGeometry.point(bodyEnd, headRadius * 0.7f, angle - 90f)
    val bottomRight = FishGeometry.point(bodyEnd, headRadius * 0.7f, angle + 90f)
    val bottomLeft = FishGeometry.point(head, headRadius, angle + 80f)
    val controlLeft = FishGeometry.point(head, bodyLength * 0.56f, angle - 130f)
    val controlRight = FishGeometry.point(head, bodyLength * 0.56f, angle + 130f)
    drawPath(
        paths.path.also { it.rewind() }.apply {
            moveTo(topLeft.x, topLeft.y)
            quadraticTo(controlLeft.x, controlLeft.y, topRight.x, topRight.y)
            lineTo(bottomRight.x, bottomRight.y)
            quadraticTo(controlRight.x, controlRight.y, bottomLeft.x, bottomLeft.y)
            close()
        },
        color,
    )
}

/** 节肢之间的梯形：垂直于主轴的两条边，宽度分别为 [startWidth] 与 [endWidth]。 */
private fun DrawScope.drawQuad(
    start: Offset,
    end: Offset,
    startWidth: Float,
    endWidth: Float,
    angle: Float,
    color: Color,
    paths: FishPaths,
) {
    val startLeft = FishGeometry.point(start, startWidth, angle - 90f)
    val startRight = FishGeometry.point(start, startWidth, angle + 90f)
    val endLeft = FishGeometry.point(end, endWidth, angle - 90f)
    val endRight = FishGeometry.point(end, endWidth, angle + 90f)
    drawPath(
        paths.path.also { it.rewind() }.apply {
            moveTo(startLeft.x, startLeft.y)
            lineTo(endLeft.x, endLeft.y)
            lineTo(endRight.x, endRight.y)
            lineTo(startRight.x, startRight.y)
            close()
        },
        color,
    )
}

private fun DrawScope.drawTriangle(a: Offset, b: Offset, c: Offset, color: Color, paths: FishPaths) {
    drawPath(
        paths.path.also { it.rewind() }.apply {
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            close()
        },
        color,
    )
}

private fun sinDeg(degrees: Float): Float = sin(Math.toRadians(degrees.toDouble())).toFloat()
