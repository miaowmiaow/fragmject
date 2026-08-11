@file:SuppressLint("JavascriptInterface")

package com.example.fragmject.feature.wan.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.core.net.toUri
import com.example.fragmject.core.ui.components.StandardDialog
import com.example.fragmject.core.network.http.download
import com.example.fragmject.core.network.utils.CacheUtils
import com.example.fragmject.core.network.utils.saveImagesToAlbum
import android.os.Environment
import android.media.MediaScannerConnection
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.fragmject.core.ui.utils.JsInjectCache
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * JS → Native 视频保存桥接。
 *
 * 注册到 [WebView.addJavascriptInterface] 中供 H5 的 `VideoSaveBridge.onVideoLongPress(src)` 调用。
 * 所有业务逻辑（空 URL / 单视频 / 多视频分发）由 [onResult] lambda 委托给 Composable 层处理，
 * 本类只负责解析 `|` 分隔的 URL 列表并回调。
 */
@SuppressLint("JavascriptInterface")
private class VideoSaveBridge(
    private val onResult: (urls: List<String>) -> Unit,
) {
    @android.webkit.JavascriptInterface
    fun onVideoLongPress(videoUrl: String) {
        if (videoUrl.isEmpty()) {
            onResult(emptyList())
            return
        }
        onResult(videoUrl.split("|").filter { it.isNotBlank() })
    }
}

/**
 * WebView 内部用到的回调集合。WebChromeClient/WebViewClient 提为顶层类，避免在 Composable 工厂里
 * 用匿名内部类闭包当前 Activity / Composable 状态——配合 [WebViewManager] 的复用机制时，
 * 旧的匿名类会跟随 WebView 实例长久存活而泄漏。
 */
private class WebViewCallbacks(
    var url: String,
    var onProgress: (Float) -> Unit = {},
    var onTitle: (String?) -> Unit = {},
    var onCustomView: (View?) -> Unit = {},
    var onPermissionRequest: (PermissionRequest?) -> Unit = {},
    var shouldOverrideUrl: (String) -> Unit = {},
    var injectScript: (String) -> Unit = {},
    var injectVConsole: () -> Boolean = { false },
    var onReceivedError: (errorCode: Int, description: String, failingUrl: String?) -> Unit = { _, _, _ -> },
)

private class PooledWebChromeClient(
    private val callbacks: WebViewCallbacks,
) : WebChromeClient() {

    /** 每个 url 仅注入一次脚本，避免 onProgress 反复回调时重复注入。 */
    private var injectedForUrl: String? = null

    fun resetInjection() {
        injectedForUrl = null
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        callbacks.onProgress((newProgress / 100f).coerceIn(0f, 1f))
        if (newProgress > 80 && injectedForUrl != view.url) {
            if (callbacks.injectVConsole()) {
                callbacks.injectScript("vconsole")
            }
            callbacks.injectScript("quickVideo")
            callbacks.injectScript("videoSave")
            injectedForUrl = view.url
        }
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        callbacks.onTitle(title)
        view?.tag = title
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        callbacks.onCustomView(view)
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        callbacks.onCustomView(null)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        callbacks.onPermissionRequest(request)
    }
}

private class PooledWebViewClient(
    private val callbacks: WebViewCallbacks,
    private val onReset: () -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (view != null && request != null) {
            val context = view.context
            when {
                WebViewManager.isAssetsResource(request) ->
                    return WebViewManager.assetsResourceRequest(context, request)

                WebViewManager.isCacheResource(request) ->
                    return WebViewManager.cacheResourceRequest(context, request)
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        if (view == null || request == null) return false
        val requestUrl = request.url.toString()
        if (request.hasGesture()
            && !request.isRedirect
            && URLUtil.isNetworkUrl(requestUrl)
            && requestUrl != callbacks.url
        ) {
            callbacks.shouldOverrideUrl(requestUrl)
            return true
        }
        if (!URLUtil.isValidUrl(requestUrl)) {
            try {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
            } catch (e: Exception) {
                Log.e("WebView", "shouldOverrideUrlLoading failed: ${request.url}", e)
            }
            return true
        }
        return false
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onReset()
    }

    @Suppress("DEPRECATION")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?,
    ) {
        callbacks.onReceivedError(errorCode, description ?: "未知错误", failingUrl)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        if (request?.isForMainFrame == true && error != null) {
            callbacks.onReceivedError(
                error.errorCode,
                error.description?.toString() ?: "未知错误",
                request.url?.toString(),
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun WebView(
    modifier: Modifier = Modifier,
    url: String,
    control: WebViewControl,
    title: String? = null,
    onReceivedTitle: (title: String?) -> Unit = {},
    onCustomView: (view: View?) -> Unit = {},
    shouldOverrideUrl: (url: String) -> Unit = {},
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var extra by remember { mutableStateOf<String?>(null) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var videoSaveUrl by remember { mutableStateOf<String?>(null) }
    var noVideoFound by remember { mutableStateOf(false) }
    var videoUrlList by remember { mutableStateOf<List<String>>(emptyList()) }
    var showVideoSelectDialog by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // JS 桥接：与 WebView 生命周期绑定的命名对象，比匿名内部类更清晰
    val videoBridge = remember {
        VideoSaveBridge { urls ->
            when {
                urls.isEmpty() -> noVideoFound = true
                urls.size == 1 -> {
                    videoSaveUrl = urls[0]
                    showVideoDialog = true
                }
                else -> {
                    videoUrlList = urls
                    showVideoSelectDialog = true
                }
            }
        }
    }

    // 用 SharedFlow 而不是 mutableState 承接权限请求，避免相同实例引用导致 LaunchedEffect 不再触发
    val permissionRequests =
        remember { MutableSharedFlow<PermissionRequest>(extraBufferCapacity = 1) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val resourceToPermissionMap = remember {
        mapOf(
            "android.webkit.resource.VIDEO_CAPTURE" to Manifest.permission.CAMERA,
            "android.webkit.resource.AUDIO_CAPTURE" to Manifest.permission.RECORD_AUDIO,
        )
    }
    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        pendingPermissionRequest?.apply {
            if (result.values.all { it }) grant(resources)
        }
        pendingPermissionRequest = null
    }
    LaunchedEffect(permissionRequests) {
        permissionRequests.collectLatest { request ->
            pendingPermissionRequest = request
            val permissions = request.resources.mapNotNull { resourceToPermissionMap[it] }
            if (permissions.isNotEmpty()) {
                requestPermissions.launch(permissions.toTypedArray())
            }
        }
    }

    // 一份与 Composable 状态绑定的回调容器；WebView 复用时只需替换里面的字段，不会替换 client 本体
    val callbacks = remember {
        WebViewCallbacks(url = url)
    }
    callbacks.url = url
    callbacks.onProgress = { control.progress = it; if (it > 0f) webViewError = null }
    callbacks.onTitle = onReceivedTitle
    callbacks.onCustomView = onCustomView
    callbacks.onPermissionRequest = { req -> req?.let { permissionRequests.tryEmit(it) } }
    callbacks.shouldOverrideUrl = shouldOverrideUrl
    callbacks.injectVConsole = { control.injectState }
    callbacks.injectScript = { tag ->
        webView?.let { wv ->
            val script = when (tag) {
                "vconsole" -> JsInjectCache.vConsoleJs(wv.context)
                "quickVideo" -> JsInjectCache.quickVideoJs(wv.context)
                "videoSave" -> JsInjectCache.videoSaveJs()
                else -> return@let
            }
            wv.evaluateJavascript(script) {}
        }
    }
    callbacks.onReceivedError = { errorCode, description, failingUrl ->
        webViewError = when (errorCode) {
            WebViewClient.ERROR_HOST_LOOKUP, WebViewClient.ERROR_CONNECT,
            WebViewClient.ERROR_TIMEOUT -> "网络连接失败，请检查网络后重试"
            else -> "加载失败（$errorCode）：$description"
        }
    }

    LaunchedEffect(webView, control) {
        webView?.let {
            with(control) {
                handleControlEvents(
                    reload = { it.reload() },
                    evaluateJavascript = { script, resultCallback ->
                        it.evaluateJavascript(script) { value ->
                            resultCallback?.onReceiveValue(value ?: "")
                        }
                    }
                )
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            WebViewManager.obtain(ctx, url).apply {
                this.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setDownloadListener { downloadUrl, _, _, _, _ ->
                    handleDownload(ctx, downloadUrl)
                }
                addJavascriptInterface(videoBridge, "VideoSaveBridge")
                setOnLongClickListener {
                    val result = hitTestResult
                    when (result.type) {
                        WebView.HitTestResult.IMAGE_TYPE,
                        WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                            extra = result.extra
                            showDialog = true
                            true
                        }

                        else -> false
                    }
                }
                val chromeClient = PooledWebChromeClient(callbacks)
                webChromeClient = chromeClient
                webViewClient = PooledWebViewClient(callbacks) { chromeClient.resetInjection() }
                if (URLUtil.isValidUrl(url) && this.url != url) {
                    WebViewManager.prefetchDns(url)
                    this.loadUrl(url)
                }
                tag?.let { title -> onReceivedTitle(title.toString()) }
                webView = this
            }
        },
        update = { wv ->
            // url 变化时主动 loadUrl，避免复用同一个 WebView 时新地址不生效
            if (URLUtil.isValidUrl(url) && wv.url != url) {
                WebViewManager.prefetchDns(url)
                wv.loadUrl(url)
            }
        },
        modifier = modifier,
        onRelease = { WebViewManager.recycle(it) }
    )

    // 网络错误覆盖层：ERR_CONNECTION_REFUSED 等 WebView 不可达错误在此展示
    if (webViewError != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = webViewError!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = {
                    webViewError = null
                    webView?.reload()
                }) {
                    Text("点击重试")
                }
            }
        }
    }

    StandardDialog(
        show = showDialog,
        title = "提示",
        text = "你希望保存该图片吗？",
        onConfirm = {
            extra?.let {
                if (URLUtil.isValidUrl(it)) {
                    context.saveImagesToAlbum(it) { _, _ ->
                        Toast.makeText(context, "保存图片成功", Toast.LENGTH_SHORT).show()
                        showDialog = false
                    }
                } else {
                    var str = it
                    if (str.contains(",")) {
                        str = str.split(",")[1]
                    }
                    val array = Base64.decode(str, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(array, 0, array.size)
                    context.saveImagesToAlbum(bitmap) { _, _ ->
                        Toast.makeText(context, "保存图片成功", Toast.LENGTH_SHORT).show()
                        showDialog = false
                    }
                }
            }
        },
        onDismiss = { showDialog = false },
    )

    // 视频保存确认对话框
    StandardDialog(
        show = showVideoDialog,
        title = "提示",
        text = videoSaveUrl?.let { url ->
            val ext = url.substringAfterLast(".").substringBefore("?").lowercase()
            if (ext == "m3u8") "检测到 m3u8 流媒体视频，下载后合并为 .ts 文件（可在大部分播放器播放）。是否继续？"
            else "你希望保存该视频吗？"
        } ?: "你希望保存该视频吗？",
        onConfirm = {
            val url = videoSaveUrl ?: return@StandardDialog
            showVideoDialog = false
            VideoDownloadManager.register(
                title = title ?: url.substringAfterLast("/").substringBefore("?"),
                url = url,
            ) { taskId ->
                val saved = downloadVideo(context, url, title, taskId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (saved) "保存视频成功" else "保存视频失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onDismiss = { showVideoDialog = false },
    )

    // 未检测到视频 Toast 提示
    LaunchedEffect(noVideoFound) {
        if (noVideoFound) {
            Toast.makeText(context, "页面中未检测到视频", Toast.LENGTH_SHORT).show()
            noVideoFound = false
        }
    }

    // 多视频选择对话框
    if (showVideoSelectDialog) {
        AlertDialog(
            onDismissRequest = { showVideoSelectDialog = false },
            title = { Text("选择要下载的视频") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    videoUrlList.forEachIndexed { index, url ->
                        TextButton(
                            onClick = {
                                videoSaveUrl = url
                                showVideoSelectDialog = false
                                showVideoDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${index + 1}. ${url.substringAfterLast("/").substringBefore("?")}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVideoSelectDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            textContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private fun handleDownload(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("WebView", "setOnDownloadListener: open url failed: $url", e)
    }
}

/**
 * 下载视频到 Movies 目录并通知 MediaStore 扫描。
 *
 * 策略：
 * 1. 先从 URL path 末段提取合法扩展名（仅 2-5 位字母数字）；
 * 2. 下载到临时文件，检测前 20 字节是否含 #EXTM3U（m3u8 特征码）；
 * 3. 是 m3u8 → 删除临时文件，委托 M3u8Downloader 重新下载+合并；
 * 4. 不是 m3u8 → 重命名为正式文件名。
 *
 * @return true 表示下载成功
 */
internal suspend fun downloadVideo(
    context: Context, videoUrl: String, title: String?, taskId: String,
): Boolean {
    try {
        val baseDir = CacheUtils.getDirPath(context, Environment.DIRECTORY_MOVIES)
        val safeTitle = title?.take(40)?.replace(Regex("[/\\\\:*?\"<>|]"), "_") ?: "video"
        val saveDir = File(baseDir, safeTitle).also { it.mkdirs() }.absolutePath

        val pathExt = videoUrl
            .substringBefore("?")
            .substringAfterLast("/")
            .substringAfterLast(".", "")
            .lowercase()
        val ext = if (pathExt.length in 2..5 && pathExt.all { it in 'a'..'z' || it in '0'..'9' })
            pathExt else ""

        // ── m3u8 断点续传检测 ──
        val task = VideoDownloadManager.tasks.value.find { it.id == taskId }
        val resumePlaylist = task?.playlistPath?.let { File(it) }?.takeIf { it.exists() }
        val resumeSegDir = task?.segTmpDir?.let { File(it) }?.takeIf { it.isDirectory }

        if (ext == "m3u8" || resumePlaylist != null) {
            val merged: File? = if (resumePlaylist != null && resumeSegDir != null) {
                // 断点续传：跳过已下载的分片
                M3u8Downloader.downloadResumable(videoUrl, saveDir, resumePlaylist, resumeSegDir) { p ->
                    VideoDownloadManager.onProgress(taskId, p * 0.95f)
                }
            } else {
                // 首次下载 m3u8
                val mergedFile = M3u8Downloader.download(videoUrl, saveDir) { p ->
                    VideoDownloadManager.onProgress(taskId, 0.05f + p * 0.9f)
                }
                // 下载过程中保存 playlist 和 seg 目录路径到 Task，供断点续传
                if (mergedFile != null) {
                    saveM3u8ResumePaths(videoUrl, taskId, saveDir)
                }
                mergedFile
            }
            if (merged != null) {
                // Successful resume also cleans up paths
                VideoDownloadManager.onM3u8Cleanup(taskId)
                MediaScannerConnection.scanFile(
                    context, arrayOf(merged.absolutePath), arrayOf("video/mp4")
                ) { _, _ -> }
                VideoDownloadManager.onComplete(taskId, merged.absolutePath)
                return true
            }
            VideoDownloadManager.onFailed(taskId)
            return false
        }

        VideoDownloadManager.onProgress(taskId, 0.1f)
        val tmpName = "video_tmp_${System.currentTimeMillis()}"

        // 全量下载（兜底）：每次尝试带 2 分钟超时，最多重试 3 次
        // 重试间隔向 UI 反馈进度（10%~15%），避免用户以为卡死
        val mp4RetryDelays = longArrayOf(500, 1000, 2000)
        val mp4AttemptTimeout = 120L
        var result: com.example.fragmject.core.network.http.HttpResponse? = null
        for (attempt in 0..mp4RetryDelays.size) {
            VideoDownloadManager.onProgress(taskId, 0.10f + 0.01f * attempt)
            val tmpFileAttempt = File(saveDir, tmpName + "_" + attempt)
            result = try {
                withTimeoutOrNull(mp4AttemptTimeout.seconds) {
                    withContext(Dispatchers.IO) {
                        download(saveDir, tmpFileAttempt.name) { setUrl(videoUrl) }
                    }
                }
            } catch (_: Exception) {
                null
            }
            if (result != null && result.errorCode == "0") {
                tmpFileAttempt.renameTo(File(saveDir, tmpName))
                break
            }
            if (tmpFileAttempt.exists()) tmpFileAttempt.delete()
            if (attempt < mp4RetryDelays.size) {
                Log.w("WebView", "MP4 download attempt ${attempt + 1} failed, retrying in ${mp4RetryDelays[attempt]}ms: $videoUrl")
                delay(mp4RetryDelays[attempt])
            } else {
                Log.e("WebView", "MP4 download failed after ${mp4RetryDelays.size + 1} attempts: $videoUrl")
            }
        }
        if (result == null || result.errorCode != "0") {
            VideoDownloadManager.onFailed(taskId); return false
        }

        val tmpFile = File(saveDir, tmpName)
        if (!tmpFile.exists() || tmpFile.length() == 0L) {
            tmpFile.delete(); VideoDownloadManager.onFailed(taskId); return false
        }
        VideoDownloadManager.onProgress(taskId, 0.8f)

        val isM3u8Content = tmpFile.length() < 2 * 1024 * 1024 && try {
            val header = ByteArray(20).also { tmpFile.inputStream().use { s -> s.read(it) } }
            String(header, Charsets.UTF_8).contains("#EXTM3U")
        } catch (_: Exception) { false }

        if (isM3u8Content) {
            tmpFile.delete()
            val merged = M3u8Downloader.download(videoUrl, saveDir) { p ->
                VideoDownloadManager.onProgress(taskId, 0.8f + p * 0.15f)
            }
            if (merged != null) {
                VideoDownloadManager.onM3u8Cleanup(taskId)
                MediaScannerConnection.scanFile(
                    context, arrayOf(merged.absolutePath), arrayOf("video/mp4")
                ) { _, _ -> }
                VideoDownloadManager.onComplete(taskId, merged.absolutePath)
                return true
            }
            VideoDownloadManager.onFailed(taskId)
            return false
        }

        val finalExt = ext.ifBlank { "mp4" }
        val outputFile = File(saveDir, "video_${System.currentTimeMillis()}.$finalExt")
        tmpFile.renameTo(outputFile)

        MediaScannerConnection.scanFile(
            context, arrayOf(outputFile.absolutePath), arrayOf("video/$finalExt")
        ) { _, _ -> }
        VideoDownloadManager.onComplete(taskId, outputFile.absolutePath)
        return true
    } catch (e: Exception) {
        Log.e("WebView", "downloadVideo failed: $videoUrl", e)
        VideoDownloadManager.onFailed(taskId)
        return false
    }
}

/** 扫描 saveDir 中最新生成的 playlist 和 seg_tmp 目录，保存到 Task 供断点续传。 */
private fun saveM3u8ResumePaths(m3u8Url: String, taskId: String, saveDir: String) {
    try {
        val dir = File(saveDir)
        val playlist = dir.listFiles()?.filter {
            it.name.startsWith("playlist_") && it.name.endsWith(".m3u8")
        }?.maxByOrNull { it.lastModified() }
        val segDir = dir.listFiles()?.filter {
            it.isDirectory && it.name.startsWith("seg_tmp_")
        }?.maxByOrNull { it.lastModified() }
        if (playlist != null && segDir != null) {
            VideoDownloadManager.onM3u8Progress(
                taskId, saveDir,
                playlist.absolutePath, segDir.absolutePath
            )
        }
    } catch (_: Exception) { }
}

@Stable
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
        val resultCallback: ValueCallback<String>?,
    )

    var injectState: Boolean by mutableStateOf(false)
        internal set
    var progress: Float by mutableFloatStateOf(0f)
        internal set

    @OptIn(FlowPreview::class)
    internal suspend fun handleControlEvents(
        reload: () -> Unit = {},
        evaluateJavascript: (script: String, resultCallback: ValueCallback<String>?) -> Unit = { _, _ -> },
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

    fun inject(): Boolean {
        injectState = !injectState
        reload()
        return injectState
    }

    fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>? = null) {
        scope.launch { evalEvents.emit(EvalEvent(script, resultCallback)) }
    }
}

@Composable
fun rememberWebViewControl(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewControl = remember(coroutineScope) { WebViewControl(coroutineScope) }
