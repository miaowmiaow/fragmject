package com.example.fragmject.core.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class WebViewControl(private val scope: CoroutineScope) {

    /**
     * reload 与 evaluateJavascript 拆成两条 Flow：
     * - reload 走 350ms debounce，避免连续点击导致连续刷新；
     * - evaluateJavascript 不防抖，否则连续点击的脚本注入会被丢弃。
     */
    private val reloadEvents: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)
    private val evalEvents: MutableSharedFlow<EvalEvent> =
        MutableSharedFlow(extraBufferCapacity = 8)

    private data class EvalEvent(
        val script: String,
        val resultCallback: ((String) -> Unit)?,
    )

    var progress: Float by mutableFloatStateOf(0f)
        internal set

    @OptIn(FlowPreview::class)
    internal suspend fun handleControlEvents(
        reload: () -> Unit = {},
        evaluateJavascript: (script: String, resultCallback: ((String) -> Unit)?) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.Main) {
        launch {
            reloadEvents.debounce(350.milliseconds).collect { reload() }
        }
        launch {
            evalEvents.collect { event ->
                evaluateJavascript(event.script, event.resultCallback)
            }
        }
    }

    fun reload() {
        scope.launch { reloadEvents.emit(Unit) }
    }

    fun evaluateJavascript(script: String, resultCallback: ((String) -> Unit)? = null) {
        scope.launch { evalEvents.emit(EvalEvent(script, resultCallback)) }
    }
}

@Composable
fun rememberWebViewControl(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewControl = remember(coroutineScope) { WebViewControl(coroutineScope) }
