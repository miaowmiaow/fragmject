package com.example.fragmject.core.navigation.contract

import androidx.navigation3.runtime.NavKey

/**
 * 标记接口：属于**认证流程**（登录 / 注册）的路由 key。
 *
 * 与 [RequiresAuth] / [DetailPaneNavKey] / [FlowScopedNavKey] 同属「供组合根做分发判定的标记」。
 * 组合根据此在「离开认证流程」时清理待回跳目标（pendingRedirect）：用户从登录页按返回、
 * 或从登录页进入注册页，都意味着放弃登录前暂存的回跳意图，避免下次登录成功后被迫跳转。
 *
 * 依赖提示：本接口为纯契约标记，定义于 :core:navigation-contract；feature:*:api 声明实现
 * 本接口的 NavKey 时仅需依赖 contract（与 [RequiresAuth] 一致）。
 */
interface AuthFlowNavKey : NavKey
