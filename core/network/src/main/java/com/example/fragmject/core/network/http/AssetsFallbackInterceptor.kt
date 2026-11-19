package com.example.fragmject.core.network.http

import android.content.Context
import com.example.fragmject.core.android.platform.file.FileUtils
import com.example.fragmject.core.data.contract.model.ApiCode
import com.example.fragmject.core.network.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * Debug 包离线 / 服务端故障兜底拦截器。
 *
 * 命中以下任一情形时，尝试读取 `assets/json/` 下的预置数据并伪装成一次成功的
 * HTTP 200 响应返回，Retrofit 会照常反序列化，上层 Repository 完全无感知：
 *
 * 1. 请求抛 [IOException]（连接失败 / DNS 失败 / 超时等）；
 * 2. 响应非 2xx（如服务端故障返回 500）；
 * 3. 响应为 2xx 但业务码不是 [ApiCode.SUCCESS] —— 例如玩Android 数据库故障时返回的
 *    `{"data":null,"errorCode":-1,"errorMsg":"could not execute query"}`。
 *    HTTP 层是成功的，异常要等到 Repository 解析业务码才抛出，拦截器必须自己判断。
 *
 * 行为约束：
 * - 仅当 `BuildConfig.DEBUG` 为 true（Debug 构建）时才兜底；
 *   Release 包直接透传，避免线上隐式读取本地数据。
 * - assets 中不存在对应 json 时同样透传原始响应 / 异常，不吞错。
 * - 情形 2、3 需要读一次响应体判断业务码（[Response.peekBody]，原 body 仍可读）；
 *   业务码缺失或无法解析时一律视为正常，交回下游按原逻辑处理，不猜测语义。
 *
 * 文件名映射沿用历史约定：将相对 URL 的 `/` 替换为 `-`、`?` 替换为 `_`。
 * 例如 `project/list/1/json?cid=328` → `project-list-1-json_cid=328`。
 */
class AssetsFallbackInterceptor(
    private val context: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (!BuildConfig.DEBUG) throw e
            // 网络层就失败了，没有原始响应可透传
            return fallbackResponse(request) ?: throw e
        }

        if (!BuildConfig.DEBUG || !response.needsAssetsFallback()) return response

        // 没有对应预置数据：原样透传，让下游照常报错
        val fallback = fallbackResponse(request) ?: return response
        // 替换响应前关闭原响应，避免连接 / 响应体泄漏
        response.close()
        return fallback
    }

    /** 读取 `assets/json/` 下与请求 URL 对应的预置响应；没有则返回 null。 */
    private fun fallbackResponse(request: Request): Response? {
        val jsonName = toJsonName(request.url)
        val json = FileUtils.readAssetString(context, "json/$jsonName.json")
        if (json.isBlank()) return null

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }

    /** 从完整 URL 还原相对路径，并按约定生成 assets 文件名。 */
    private fun toJsonName(url: HttpUrl): String {
        val path = url.encodedPath.removePrefix("/")
        val query = url.encodedQuery
        val relative = if (query != null) "$path?$query" else path
        return relative.replace("/", "-").replace("?", "_")
    }
}

/**
 * 是否需要用预置数据替换该响应：非 2xx，或 2xx 但业务码明确失败。
 *
 * 业务码缺失 / 无法解析（非 JSON、字段不存在、响应体被截断）时一律返回 false，
 * 把响应交回下游按原逻辑报错，不猜测语义。
 *
 * 提为顶层 internal 而非私有成员：它只依赖 [Response]，便于 JVM 单测覆盖。
 */
internal fun Response.needsAssetsFallback(): Boolean {
    if (!isSuccessful) return true
    val errorCode = runCatching {
        (JsonParser.parseString(peekBody(PEEK_LIMIT).string()) as? JsonObject)
            ?.get("errorCode")
            ?.takeUnless { it.isJsonNull }
            ?.asString
    }.getOrNull() ?: return false
    return errorCode != ApiCode.SUCCESS
}

/**
 * 判断业务码时最多读取的响应体字节数。
 *
 * 截断会导致 JSON 解析失败进而被判为「正常」，因此取一个远大于现有接口
 * 响应体积（最大约 200KB）的上限，避免误伤成功响应。
 */
private const val PEEK_LIMIT: Long = 512L * 1024
