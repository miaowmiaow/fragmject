package com.example.fragmject.app.security

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * 前后台联动的注册逻辑：把 Activity 生命周期翻译成 [AppSecurityGuard.onForeground] / [AppSecurityGuard.onBackground] 回调。
 *
 * 为什么从 [AppSecurityGuard] 里分出：
 * 它只依赖「Application + 两个回调」，与检测策略、native 能力、轮询调度都无耦合，
 * 是最干净的一块；分出后 [AppSecurityGuard] 只负责「何时检测、命中后怎么办」。
 *
 * 为什么不用 ProcessLifecycleOwner：需要额外引入 lifecycle-process 依赖；
 * 本项目是单 Activity 架构，用 ActivityLifecycleCallbacks 统计 started 数量
 * 已经足够，且不增加依赖。
 *
 * 判定规则：第一个 Activity onStart 视为进入前台；最后一个 onStop 视为进入后台。
 * 用计数器而不是布尔值，是为了正确处理 Activity 切换（A.stop 先于 B.start 的情况）。
 */
internal object AppSecurityLifecycle {

    fun register(
        app: Application,
        onForeground: () -> Unit,
        onBackground: () -> Unit,
    ) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: Activity) {
                if (startedActivities == 0) {
                    onForeground()
                }
                startedActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) {
                    startedActivities = 0
                    onBackground()
                }
            }

            // 其余回调与前后台判定无关，保持空实现。
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
