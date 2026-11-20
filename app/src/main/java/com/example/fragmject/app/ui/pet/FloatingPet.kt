package com.example.fragmject.app.ui.pet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.example.fragmject.core.domain.pet.PetSettings
import com.example.fragmject.core.pet.FishPet
import com.example.fragmject.core.pet.FishPetDefaultHeadRadius

/**
 * 全应用常驻的漂浮宠物：把用户配置翻译成 [FishPet] 的参数。
 *
 * 放在 `:app` 而不是某个 feature：它是**跨所有页面**的装饰层，
 * 挂在组合根才能保证首页/详情/WebView 之上都有；同时 `:app` 可以读 domain 端口，
 * 而 feature 之间不允许互相依赖（规则 2/3）。
 *
 * ============================ 页面切换 → 穿越 ============================
 * 普通点击让鱼游过去；但**真正换页**的点击（点条目跳详情、返回）不该如此——
 * 页面本身正在转场，鱼慢慢游会横穿新页面。
 *
 * 唯一的信号是导航返回栈变化，它由 [PetInputHub.notifyRouteChanged] 收下并转成
 * `teleport` 事件（底部 tab **不**触发穿越：它切换的是同一页面内的内容，鱼瞬移反而突兀，
 * 那里维持"游过去"）。本组件因此只做一件事：把事件读出来喂给 [FishPet]，**不写任何状态**。
 *
 * ============================ 关闭时为什么在 onDispose 清理 ============================
 * [PetInputHub] 的寿命长于本组件（前者随 Activity，后者随开关组合/摘除）。若关闭时不清理，
 * 重新开启的首次组合会把旧事件重放一遍，旧穿越坐标在 `bounds = Size.Zero` 下被夹到左上角。
 *
 * 清理挂在这里的 `onDispose`，而不是 `PetHost` 里监听 `settings.enabled`：不变量是
 * 「本组件离开组合 ⇒ 事件已清空」，与**为什么**离开无关。以后若给 `FishPet` 加别的显示条件
 * （例如按页面类型隐藏），监听 `enabled` 的写法会漏掉那些路径，`onDispose` 不会。
 *
 * `onDispose` 只调用 `reset()`，不读回 hub 状态；Activity 销毁时也会执行一次，
 * 此时 hub 即将被回收，没有副作用。
 */
@Composable
fun FloatingPet(
    settings: PetSettings,
    input: PetInputHub,
) {
    // 用户主动关闭才摘掉组件：此时本来就不该有帧循环
    if (!settings.enabled) return

    DisposableEffect(Unit) {
        onDispose { input.reset() }
    }

    FishPet(
        headRadius = FishPetDefaultHeadRadius * settings.sizeScale,
        opacity = settings.opacity,
        speedScale = settings.speedScale,
        tap = input.tap,
        swipe = input.swipe,
        teleport = input.teleport,
    )
}
