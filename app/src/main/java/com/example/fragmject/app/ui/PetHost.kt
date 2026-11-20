package com.example.fragmject.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.fragmject.core.domain.pet.PetSettings
import com.example.fragmject.core.pet.fishPointerObserver

/**
 * 漂浮宠物的宿主：**触摸观察 + 装饰层**一起挂到包裹内容的容器上。
 *
 * 职责有两个，都只在组合根做：把页面容器的触摸转成宠物输入事件、把装饰层画在内容之上。
 * 「观察器挂哪一层」是有讲究的，写在这里比散在 Activity 里更难被误改。
 *
 * ============================ 为什么观察器挂在包裹层 ============================
 * 只有**包裹内容**的容器才在命中链里、且不拦截子节点：父节点收到事件后仍会向下分发，
 * 页面自己的点击/滚动照常工作（观察器用 `PointerEventPass.Initial` 只看不消费）。
 *
 * 反例（踩过）：把观察器挂到与内容**平级**、铺满全屏的兄弟节点上，那个节点会把点击
 * 全部吃掉——Compose 的命中测试不会把事件"共享"给下层兄弟，整页就点不动了。
 *
 * ============================ 为什么 FloatingPet 放最后 ============================
 * 它是纯绘制层（自身不持有 `pointerInput`），放在内容之后即"画在最上层"，
 * 不会参与命中，也不影响内容的命中优先级。
 *
 * ============================ 事件清理为什么不在这里 ============================
 * hub 的寿命长于 `FishPet`（前者随 Activity，后者随开关组合/摘除），关闭时必须清理事件，
 * 否则重新开启的首次组合会把旧事件重放一遍（旧穿越坐标在 `bounds = Size.Zero` 下被夹到左上角）。
 *
 * 但清理**不在本文件**做，而是 `FloatingPet` 的 `onDispose`：不变量是「装饰层离开组合 ⇒ 事件已清空」，
 * 与「因为开关关了」这种具体原因解耦。本组件只负责把触摸接到 hub 上、把装饰层画在最上层。
 */
@Composable
fun PetHost(
    settings: PetSettings,
    input: PetInputHub,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .fishPointerObserver(
                enabled = settings.enabled,
                onTap = input::emitTap,
                onSwipe = input::emitSwipe,
            ),
    ) {
        content()
        FloatingPet(settings = settings, input = input)
    }
}
