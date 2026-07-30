package com.example.fragmject.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * NiA 风格统一结果封装。
 *
 * ```
 * Success ──── 数据就绪
 * Loading ──── 进行中（由 [Flow.asAppResult] 自动发出）
 * Failure ──── 业务错误或系统异常
 * ```
 *
 * 使用 [Flow.asAppResult] 自动包裹三个阶段：
 * ```
 * repo.getArticles()
 *   .asAppResult()
 *   .stateIn(scope, WhileSubscribed(5000), AppResult.Loading)
 * ```
 */
sealed interface AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>

    data object Loading : AppResult<Nothing>

    data class Failure(
        val error: AppError,
    ) : AppResult<Nothing>

    fun onSuccess(block: (T) -> Unit): AppResult<T> {
        if (this is Success) block(data)
        return this
    }

    fun onFailure(block: (AppError) -> Unit): AppResult<T> {
        if (this is Failure) block(error)
        return this
    }

    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Loading -> this
        is Failure -> this
    }

    val isSuccess: Boolean get() = this is Success
    val isLoading: Boolean get() = this is Loading
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): AppError? = (this as? Failure)?.error
}

sealed interface AppError {
    val code: String
    val message: String

    data class Business(
        override val code: String,
        override val message: String,
    ) : AppError

    data class Network(
        override val message: String,
        override val code: String = ERROR_NETWORK,
    ) : AppError

    data class Unknown(
        override val message: String,
        override val code: String = ERROR_UNKNOWN,
    ) : AppError

    companion object {
        const val SUCCESS = "0"
        const val ERROR_NETWORK = "NETWORK_ERROR"
        const val ERROR_UNKNOWN = "UNKNOWN_ERROR"
    }
}

fun Throwable.toAppError(): AppError {
    val msg = message?.takeIf { it.isNotBlank() } ?: "网络请求异常"
    return AppError.Network(message = msg)
}

/**
 * 将任意的 [Flow] 自动包裹为 [Flow]。
 *
 * ● onStart  → emit(Loading)
 * ● 正常值   → emit(Success(value))
 * ● 异常     → emit(Failure(AppError))
 */
fun <T> Flow<T>.asAppResult(): Flow<AppResult<T>> = this
    .map<T, AppResult<T>> { AppResult.Success(it) }
    .onStart { emit(AppResult.Loading) }
    .catch { emit(AppResult.Failure(it.toAppError())) }

/**
 * suspend 版本的 runCatching 封装——用于非 Flow 的一次性调用。
 */
suspend inline fun <T> runAppResult(crossinline block: suspend () -> T): AppResult<T> {
    return runCatching { block() }
        .fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Failure(it.toAppError()) },
        )
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success<T>)?.data

fun <T> AppResult<T>.errorOrNull(): AppError? = (this as? AppResult.Failure)?.error
