package com.example.fragmject.core.domain.session

/**
 * Cookie 存储领域端口（共享会话能力）。
 *
 * 与 [com.example.fragmject.core.domain.repository.UserRepository] 等「业务端口」不同，
 * 也与 [com.example.fragmject.core.data.contract.remote.*] 等「数据访问机制」不同：
 * 本端口抽象的是「进程级会话容器」——WebView 登录后写入的 Cookie，需被
 * OkHttp（core:network）复用，因此它是跨网络与 WebView 的共享能力，归入 domain。
 *
 * 为什么不放 core:data-contract：
 * core:webview 已依赖 core:domain（资源缓存复用 DownloadRepository），放 domain
 * 不会新增边；放 data-contract 反而会让 UI 基础设施反向依赖数据访问契约层。
 *
 * 读写保持同步以适配 OkHttp 的 CookieJar 回调：写入返回前须确认完成，
 * flush 返回前须确认此前写入已持久化。同步写入/持久化应在后台线程调用，
 * 实现不得阻塞主线程等待平台回调。业务会话清理使用 suspend，允许等待删除与持久化。
 *
 * 为什么不在端口里暴露 android.webkit.WebView：
 * domain 禁止 android.*（架构测试规则 8）。第三方 Cookie 开关属于 WebView 实例配置，
 * 由 core:webview 在池内创建 WebView 时自行处理，不进入领域契约。
 *
 * 实现归属（必须遵守，违反会被架构测试规则 15 拦截）：
 * - **唯一实现**：`core:webview` 的 `WebViewCookieStore`（基于 `android.webkit.CookieManager`），
 *   由 `core:webview/di/CookieStoreModule` 经 `@Binds` 绑定到本契约，
 *   再由 `:app` 组合根聚合 Hilt 模块完成最终的图组装。
 * - **消费方只允许注入使用**：`core:network` 的 `CookieJar` 读写 Cookie，
 *   `core:data-repository` 的 `UserRepositoryImpl` 清理业务会话，均构造注入本端口。
 *   network **不得自行实现本接口**——network 不允许依赖 webview
 *   （见 Gradle 守卫 `CORE_ALLOWED_DEPENDENCIES`），自行实现等于绕过该约束，
 *   会形成「第二个会话真相源」，导致 WebView 与 OkHttp 的 Cookie 不一致。
 * - 如需新增实现（例如纯 OkHttp 的 Cookie 容器），应先调整依赖矩阵与实现归属，
 *   而不是在 network 内部就地 new 一个。
 */
interface CookieStore {

    /** 读取指定 URL 的 Cookie（期望完整 URL 含 scheme）。 */
    fun getCookie(url: String): String?

    /** 在后台线程写入 Cookie，返回前确认平台已完成写入。 */
    fun setCookie(url: String, value: String)

    /** 在后台线程将此前写入的 Cookie 持久化，失败时抛出异常。 */
    fun flush()

    /** 清理业务站点会话，等待定向删除与持久化完成；不得清除其他浏览站点。 */
    suspend fun clearSession()
}
