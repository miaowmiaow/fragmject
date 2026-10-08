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
 * 为什么是同步接口：
 * 主要消费方 okhttp3.CookieJar 的 loadForRequest/saveFromResponse 是同步回调，
 * 若端口声明为 suspend，调用方只能 runBlocking（有 ANR 风险）或异步化（读写不保证时序）。
 * CookieManager 自身线程安全且开销极小，故端口与 OkHttp 保持同一形态（同步）。
 * 实现若需切线程，应在实现内部完成。
 *
 * 为什么不在端口里暴露 android.webkit.WebView：
 * domain 禁止 android.*（架构测试规则 8）。第三方 Cookie 开关属于 WebView 实例配置，
 * 由 core:webview 在池内创建 WebView 时自行处理，不进入领域契约。
 *
 * 实现归属（必须遵守，违反会被架构测试规则 15 拦截）：
 * - **唯一实现**：`core:webview` 的 `WebViewCookieStore`（基于 `android.webkit.CookieManager`），
 *   由 `core:webview/di/CookieStoreModule` 经 `@Binds` 绑定到本契约，
 *   再由 `:app` 组合根聚合 Hilt 模块完成最终的图组装。
 * - **消费方只允许注入使用**：`core:network` 的 `CookieJar` 构造注入本端口并读写 Cookie。
 *   network **不得自行实现本接口**——network 不允许依赖 webview
 *   （见 Gradle 守卫 `CORE_ALLOWED_DEPENDENCIES`），自行实现等于绕过该约束，
 *   会形成「第二个会话真相源」，导致 WebView 与 OkHttp 的 Cookie 不一致。
 * - 如需新增实现（例如纯 OkHttp 的 Cookie 容器），应先调整依赖矩阵与实现归属，
 *   而不是在 network 内部就地 new 一个。
 */
interface CookieStore {

    /** 读取指定 URL 的 Cookie（期望完整 URL 含 scheme）。 */
    fun getCookie(url: String): String?

    /** 写入 Cookie。 */
    fun setCookie(url: String, value: String)

    /** 将内存中的 Cookie 立即持久化。 */
    fun flush()
}
