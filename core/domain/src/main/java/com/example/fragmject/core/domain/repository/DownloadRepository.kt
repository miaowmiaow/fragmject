package com.example.fragmject.core.domain.repository

import com.example.fragmject.core.domain.result.SimpleResult

/**
 * 文件下载领域端口。
 *
 * 抽象 HTTP 文件下载能力，供 feature 层（WebView 资源缓存、m3u8 下载等）
 * 通过领域接口使用，由 data 层基于网络基础设施实现。
 */
interface DownloadRepository {

    suspend fun download(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String> = emptyMap(),
    ): SimpleResult

    /**
     * 带响应头的下载，供 WebView 资源缓存判断 `Cache-Control` / `Vary` / `Content-Type`。
     *
     * 单独开一个方法而不是改造 [SimpleResult]：缓存策略只有资源缓存需要，
     * 普通调用方（媒体保存等）仍只关心成败。
     *
     * 默认实现退回 [download] 并返回空响应头——缓存侧对「拿不到响应头」一律按**不可缓存**处理
     * （fail-closed），因此测试替身只实现 [download] 也不会让缓存意外落盘带身份的内容。
     */
    suspend fun downloadResource(
        url: String,
        savePath: String,
        fileName: String,
        headers: Map<String, String> = emptyMap(),
    ): DownloadResource = DownloadResource(download(url, savePath, fileName, headers))
}

/**
 * 一次下载的产物：结果 + 缓存决策所需的响应头子集。
 *
 * [responseHeaders] 只应包含 `Content-Type` / `Cache-Control` / `Vary` 等缓存相关字段，
 * 实现无法提供时为空（调用方按不可缓存处理）。
 */
data class DownloadResource(
    val result: SimpleResult,
    val responseHeaders: Map<String, String> = emptyMap(),
)
