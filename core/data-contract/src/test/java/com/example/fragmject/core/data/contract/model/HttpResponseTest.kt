package com.example.fragmject.core.data.contract.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpResponseTest {

    @Test
    fun `业务码严格等于 SUCCESS 时判定为成功`() {
        assertTrue(HttpResponse(errorCode = ApiCode.SUCCESS).isSuccess)
    }

    @Test
    fun `空串与缺省的业务码一律视为失败`() {
        assertFalse(HttpResponse().isSuccess)
        assertFalse(HttpResponse(errorCode = "").isSuccess)
    }

    @Test
    fun `客户端失败码与登录失效码都不是成功`() {
        assertFalse(HttpResponse(errorCode = ApiCode.CLIENT_FAILURE).isSuccess)
        assertFalse(HttpResponse(errorCode = ApiCode.LOGIN_REQUIRED).isSuccess)
    }

    @Test
    fun `近似成功码不被误判为成功`() {
        // 只做严格相等：前后空白、前导零、大小写相近的写法都不能蒙混过关
        assertFalse(HttpResponse(errorCode = " 0").isSuccess)
        assertFalse(HttpResponse(errorCode = "00").isSuccess)
        assertFalse(HttpResponse(errorCode = "0 ").isSuccess)
    }

    @Test
    fun `DataResponse 保留基类的业务码与消息，并携带载荷`() {
        val response = DataResponse(data = "payload", errorCode = ApiCode.SUCCESS, errorMsg = "ok")

        assertTrue(response.isSuccess)
        assertEquals("ok", response.errorMsg)
        assertEquals("payload", response.data)
    }

    @Test
    fun `DataResponse 失败时载荷可为空且 isSuccess 为假`() {
        val response = DataResponse<String>(errorCode = ApiCode.CLIENT_FAILURE, errorMsg = "network")

        assertFalse(response.isSuccess)
        assertEquals(null, response.data)
    }
}
