package com.example.fragmject.app.navigation

import android.util.Log
import androidx.navigation3.runtime.NavKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导航执行桥（app 组合根专属）。
 *
 * backStack 是 [androidx.navigation3.runtime.NavBackStack]（Composable 局部状态），
 * 无法直接注入到 Hilt 单例的语义 Navigator 实现中。本类作为可变持有者，
 * 由 [com.example.fragmject.app.navigation.AppNavGraph] 在组合期绑定当前导航能力，
 * 语义 Navigator 实现（如 AppArticleNavigator）通过本类把「语义动作」落到具体 NavKey。
 */
@Singleton
class NavigationDispatcher @Inject constructor() {

    private sealed interface Command {
        data class Navigate(val key: NavKey) : Command
        data object NavigateUp : Command
        data class PopBackStack(val key: NavKey) : Command
        data object AuthSuccess : Command
    }

    private class Binding(
        val owner: Any,
        val navigate: (NavKey) -> Unit,
        val navigateUp: () -> Unit,
        val popBackStack: (NavKey) -> Unit,
        val onAuthSuccess: () -> Unit,
    ) {
        fun execute(command: Command) {
            when (command) {
                is Command.Navigate -> navigate(command.key)
                Command.NavigateUp -> navigateUp()
                is Command.PopBackStack -> popBackStack(command.key)
                Command.AuthSuccess -> onAuthSuccess()
            }
        }
    }

    private var binding: Binding? = null
    private val pendingCommands = ArrayDeque<Command>()
    private var replaying = false

    fun navigate(key: NavKey) = dispatch(Command.Navigate(key))

    fun navigateUp() = dispatch(Command.NavigateUp)

    fun popBackStack(key: NavKey) = dispatch(Command.PopBackStack(key))

    fun onAuthSuccess() = dispatch(Command.AuthSuccess)

    private fun dispatch(command: Command) {
        val current = binding
        if (current != null && !replaying) {
            current.execute(command)
        } else {
            // 导航图尚未绑定时命令只能积压，这本身是「图还没准备好」的预期状态，
            // 不能因达到上限就让调用方崩溃（生产上会变成 IllegalStateException）。
            // 超限丢弃最旧的一条，保留最近的导航意图。
            if (pendingCommands.size >= MAX_PENDING_COMMANDS) {
                pendingCommands.removeFirst()
                Log.w(TAG, "导航命令积压超过 $MAX_PENDING_COMMANDS 条，已丢弃最旧的一条")
            }
            pendingCommands.addLast(command)
        }
    }

    /** 在主线程绑定导航图，返回仅能释放本次绑定的清理函数。 */
    fun bind(
        navigate: (NavKey) -> Unit,
        navigateUp: () -> Unit,
        popBackStack: (NavKey) -> Unit,
        onAuthSuccess: () -> Unit,
    ): () -> Unit {
        val owner = Any()
        binding = Binding(owner, navigate, navigateUp, popBackStack, onAuthSuccess)
        if (!replaying) {
            replaying = true
            try {
                while (pendingCommands.isNotEmpty()) {
                    val current = binding ?: break
                    current.execute(pendingCommands.removeFirst())
                }
            } finally {
                replaying = false
            }
        }
        return {
            if (binding?.owner === owner) binding = null
        }
    }

    private companion object {
        const val TAG = "NavigationDispatcher"
        const val MAX_PENDING_COMMANDS = 32
    }
}
