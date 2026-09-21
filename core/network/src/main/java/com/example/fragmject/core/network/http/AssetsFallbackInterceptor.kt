package com.example.fragmject.core.network.http

import android.content.Context
import com.example.fragmject.core.network.BuildConfig
import com.example.fragmject.core.android.platform.FileUtil
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * Debug 包离线兜底拦截器。
 *
 * 网络请求发生 [IOException]（连接失败 / DNS 失败 / 超时等）时，尝试读取
 * `assets/json/` 下的预置数据并伪装成一次成功的 HTTP 200 响应返回，
 * Retrofit 会照常反序列化，上层 Repository 完全无感知。
 *
 * 行为约束：
 * - 仅当 `BuildConfig.DEBUG` 为 true（Debug 构建）时才兜底；
 *   Release 包直接透传异常，避免线上隐式读取本地数据。
 * - assets 中不存在对应 json 时同样透传异常，不吞错。
 *
 * 文件名映射沿用历史约定：将相对 URL 的 `/` 替换为 `-`、`?` 替换为 `_`。
 * 例如 `project/list/1/json?cid=328` → `project-list-1-json_cid=328`。
 */
class AssetsFallbackInterceptor(
    private val context: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (!BuildConfig.DEBUG) throw e

            val jsonName = toJsonName(request.url)
            val json = FileUtil.readAssetString(context, "json/$jsonName.json")
            if (json.isBlank()) throw e

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    /** 从完整 URL 还原相对路径，并按约定生成 assets 文件名。 */
    private fun toJsonName(url: HttpUrl): String {
        val path = url.encodedPath.removePrefix("/")
        val query = url.encodedQuery
        val relative = if (query != null) "$path?$query" else path
        return relative.replace("/", "-").replace("?", "_")
    }
}
