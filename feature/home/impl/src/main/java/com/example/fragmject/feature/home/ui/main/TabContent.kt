package com.example.fragmject.feature.home.ui.main

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.example.fragmject.feature.home.ui.home.HomeScreen
import com.example.fragmject.feature.home.ui.my.MyScreen
import com.example.fragmject.feature.home.ui.nav.NavScreen
import com.example.fragmject.feature.home.ui.project.ProjectScreen

/**
 * 单个底部 tab 的内容。
 *
 * 抽成函数只是为了让 `MainContent` 的 `when` 保持可读；**不要**在这里做任何
 * "提前组合/预热"——见下。
 *
 * ============================ 为什么不做 tab 预热 ============================
 * 曾经试过「首页稳定后逐个组合其余 tab 两帧再丢弃」，指望把 VM 创建与首屏请求
 * 提前点火，从而消除首次切换的掉帧。已移除，原因：
 *
 * 1. **代价是确定的，收益是未验证的**：会让启动后 2~3 秒内额外发出 3 个页面的
 *    首屏请求（项目页还一次建 2 个 VM），而用户可能永远不点那些 tab；
 * 2. **时机与用户操作重叠**：预热窗口正好落在用户开始滑动列表的时候，
 *    被预热的页面仍在参与组合/布局，可能把掉帧挪了个位置而不是消除；
 * 3. 首次切换真正的成本在**整页组合/布局**，VM 创建只占一小部分——预热点火的
 *    只是前者没法省的那部分。
 *
 * 如果以后仍要解这个卡顿，先量：用 Macrobenchmark 的 `frameTimingMetric` 测
 * 「点 tab 后 500ms 内的 jank 帧数」，确认基线再谈方案（常驻组合、VM 提前创建、
 * 骨架屏等），不要先加预热点。
 */
@Composable
internal fun TabContent(index: Int, homeListState: LazyListState) {
    when (index) {
        0 -> HomeScreen(listState = homeListState)
        1 -> NavScreen()
        2 -> ProjectScreen()
        else -> MyScreen()
    }
}
