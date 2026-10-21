package com.example.fragmject.core.webview

import android.webkit.PermissionRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 网页权限请求的受理状态。
 *
 * 规则：仅受信来源（https）且仅受支持资源才进入确认流程；同一时刻只处理一个请求，
 * 其余直接拒绝。每个 [PermissionRequest] 只能应答一次（grant/deny），故统一经 answer* 收口。
 *
 * 从 `WebViewContainer` 抽出，使容器组合函数只保留「装配」职责。
 */
@Stable
internal class WebPermissionHost {

    /**
     * 权限请求入口。
     *
     * 用 SharedFlow 而不是 mutableState 承接请求，避免相同实例引用导致 LaunchedEffect 不再触发。
     */
    internal val requests = MutableSharedFlow<PermissionRequest>(extraBufferCapacity = 1)

    /** 待用户确认 / 正在申请系统权限的请求；同一时刻只处理一个。 */
    internal var pending by mutableStateOf<PermissionRequest?>(null)
        private set

    /** 用户已点击「允许」，系统权限弹窗进行中。 */
    internal var confirmed by mutableStateOf(false)
        private set

    /** 由 [rememberWebPermissionHost] 注入的系统权限申请入口。 */
    internal var launchSystemPermissions: (Array<String>) -> Unit = {}

    /** 受理一条请求；不可受理（已有待处理 / 来源不可信 / 无受支持资源）时直接拒绝。 */
    fun accept(request: PermissionRequest) {
        val acceptable = pending == null &&
            request.hasTrustedOrigin() &&
            request.supportedWebResources().isNotEmpty()
        if (!acceptable) {
            request.answerDeny()
            return
        }
        confirmed = false
        pending = request
    }

    /** 页面在用户作答前取消请求：丢弃待处理状态，不再应答（已失效的请求不能 grant/deny）。 */
    fun cancel(request: PermissionRequest?) {
        if (request != null && pending === request) {
            pending = null
            confirmed = false
        }
    }

    /** 弹窗「允许」：标记已确认，随后由弹窗发起系统权限申请。 */
    fun markConfirmed() {
        confirmed = true
    }

    /** 弹窗关闭 / 拒绝：拒绝并清空。 */
    fun deny() {
        pending?.answerDeny()
        pending = null
        confirmed = false
    }

    /** 系统权限结果回执：全部授予才 grant，否则拒绝。 */
    fun onSystemPermissionResult(result: Map<String, Boolean>) {
        val request = pending
        pending = null
        confirmed = false
        if (request == null) return
        val allowed = request.supportedWebResources()
        if (allowed.isNotEmpty() && result.values.all { it }) {
            request.answerGrant(allowed)
        } else {
            request.answerDeny()
        }
    }
}

/** 创建权限受理器，并接管系统权限申请与页面侧取消事件。 */
@Composable
internal fun rememberWebPermissionHost(): WebPermissionHost {
    val host = remember { WebPermissionHost() }
    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        host.onSystemPermissionResult(result)
    }
    host.launchSystemPermissions = { permissions -> requestPermissions.launch(permissions) }
    LaunchedEffect(host) {
        host.requests.collect { host.accept(it) }
    }
    DisposableEffect(host) {
        onDispose { host.deny() }
    }
    return host
}

/** 权限确认弹窗：展示来源与资源，用户确认后才申请系统权限。 */
@Composable
internal fun WebPermissionRequestDialog(host: WebPermissionHost) {
    val request = host.pending ?: return
    if (host.confirmed) return
    val resourceNames = request.supportedWebResources()
        .joinToString("、") { WEB_PERMISSION_LABELS.getValue(it) }
    AlertDialog(
        onDismissRequest = { host.deny() },
        title = { Text("网页权限请求") },
        text = { Text("${request.origin?.host.orEmpty()} 请求使用$resourceNames，是否允许？") },
        confirmButton = {
            TextButton(onClick = {
                host.markConfirmed()
                val permissions = request.supportedWebResources()
                    .map { WEB_PERMISSIONS.getValue(it) }
                host.launchSystemPermissions(permissions.toTypedArray())
            }) { Text("允许") }
        },
        dismissButton = {
            TextButton(onClick = { host.deny() }) { Text("拒绝") }
        },
    )
}
