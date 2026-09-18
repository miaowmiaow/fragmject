package com.example.fragmject.feature.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 单元测试中用 [TestDispatcher] 接管 [Dispatchers.Main] 的 JUnit Rule。
 *
 * `viewModelScope` 默认运行在 `Dispatchers.Main.immediate` 上，而 JVM 单元测试没有 Android
 * Main Looper，直接实例化 ViewModel 会抛 `MissingMainDispatcherException`。
 * 本 Rule 在测试开始前把 Main 替换为测试调度器，结束后恢复，从而解锁 ViewModel 层测试。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
