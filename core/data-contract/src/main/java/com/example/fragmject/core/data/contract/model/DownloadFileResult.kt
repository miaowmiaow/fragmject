package com.example.fragmject.core.data.contract.model

/**
 * 一次文件下载的返回：业务结果 + 缓存决策所需的响应头。
 *
 * 为什么只回传头子集：下载器（OkHttp 流式写文件）拿得到完整响应头，但上层只需要
 * 少数几个决定「能否落盘 / 能否复用」的字段，回传全集会把传输层细节漏给缓存实现。
 */
data class DownloadFileResult(
    val response: HttpResponse,
    /**
     * 仅含 `Content-Type` / `Cache-Control` / `Vary` 等缓存相关字段；
     * 下载失败或实现无法提供时为空。
     */
    val headers: Map<String, String> = emptyMap(),
)
