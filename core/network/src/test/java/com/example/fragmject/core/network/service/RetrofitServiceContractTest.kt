package com.example.fragmject.core.network.service

import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.InvocationTargetException

/**
 * Retrofit Service 契约防线的单元测试。
 *
 * Retrofit 的 ServiceMethod 采用懒解析：`retrofit.create(Service)` 只返回动态代理，
 * 直到第一次调用方法时才解析参数注解（@Path/@Query/@Field 等）。因此，类似
 * 「@Path 声明在 @Query 之后」「@Field 缺 @FormUrlEncoded」这类错误，在运行时
 * 才会抛 [IllegalArgumentException]，且首个症状是「接口没请求」。
 *
 * 本测试用反射逐一触发每个 Service 方法，让注解错误在测试阶段提前暴露。
 */
class RetrofitServiceContractTest {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val serviceClasses = listOf(
        ArticleService::class.java,
        ProjectService::class.java,
        CommonService::class.java,
        MyService::class.java,
        UserService::class.java,
    )

    @Test
    fun `all service methods parse without annotation error`() {
        serviceClasses.forEach { serviceClass ->
            val proxy = retrofit.create(serviceClass)
            serviceClass.declaredMethods
                .filter { !it.isSynthetic && !it.isBridge }
                .forEach { method ->
                    val args = method.parameterTypes.map { defaultFor(it) }.toTypedArray()
                    try {
                        method.invoke(proxy, *args)
                    } catch (e: InvocationTargetException) {
                        // 只有注解配置错误才会在解析阶段抛 IllegalArgumentException；
                        // 其余异常（如网络连接失败、参数为 null 的 NPE）说明注解本身正确。
                        if (e.cause is IllegalArgumentException) {
                            fail(
                                "${serviceClass.simpleName}.${method.name} 注解配置错误: " +
                                    e.cause?.message
                            )
                        }
                    } catch (e: IllegalArgumentException) {
                        fail(
                            "${serviceClass.simpleName}.${method.name} 注解配置错误: " +
                                e.message
                        )
                    }
                }
        }
    }

    /**
     * 为原始类型参数提供非 null 默认值，避免反射调用时 null 无法转换为
     * 原始类型而提前抛 NPE（这发生在 Retrofit 代理解析注解之前）。
     */
    private fun defaultFor(type: Class<*>): Any? = when (type) {
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Boolean.TYPE -> false
        java.lang.Double.TYPE -> 0.0
        java.lang.Float.TYPE -> 0f
        java.lang.Character.TYPE -> '\u0000'
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Short.TYPE -> 0.toShort()
        else -> null
    }
}
