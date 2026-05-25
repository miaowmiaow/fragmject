package com.example.miaow.base.vm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [BaseViewModel] 分页状态机的纯 JVM 单测。
 *
 * 无 Android Framework / 协程依赖，运行成本最低。覆盖：
 * 1. 初始化分页（getHomePage）
 * 2. 是否首页（isHomePage）
 * 3. 是否还有下一页（hasNextPage）
 * 4. 翻页（getNextPage）+ 边界（达到末页后停止递增）
 * 5. 多 key 并存互不干扰
 */
class BaseViewModelTest {

    /** 简单的可实例化 BaseViewModel（其本身是 abstract） */
    private class TestViewModel : BaseViewModel()

    @Test
    fun `getHomePage initializes home and curr to given page`() {
        val vm = TestViewModel()
        val page = vm.getHomePage(0)

        assertEquals(0, page)
        assertTrue("just-initialized state should be at home page", vm.isHomePage())
    }

    @Test
    fun `hasNextPage returns true when curr is less than pageCont`() {
        val vm = TestViewModel()
        vm.getHomePage(0)
        vm.updatePageCont(5)

        assertTrue(vm.hasNextPage())
    }

    @Test
    fun `getNextPage advances curr and stops at pageCont`() {
        val vm = TestViewModel()
        vm.getHomePage(0)
        vm.updatePageCont(3)

        assertEquals(1, vm.getNextPage())
        assertEquals(2, vm.getNextPage())
        assertEquals(3, vm.getNextPage())

        // 已到末页，再调用 getNextPage 不应再继续递增
        assertFalse(vm.hasNextPage())
        assertEquals(3, vm.getNextPage())
        assertEquals(3, vm.getNextPage())
    }

    @Test
    fun `isHomePage becomes false after first getNextPage`() {
        val vm = TestViewModel()
        vm.getHomePage(0)
        vm.updatePageCont(5)

        assertTrue(vm.isHomePage())
        vm.getNextPage()
        assertFalse(vm.isHomePage())
    }

    @Test
    fun `multiple keys are isolated`() {
        val vm = TestViewModel()
        vm.getHomePage(page = 0, key = "tabA")
        vm.getHomePage(page = 1, key = "tabB")
        vm.updatePageCont(pageCont = 5, key = "tabA")
        vm.updatePageCont(pageCont = 10, key = "tabB")

        // 推进 tabA 一页，tabB 不应受影响
        vm.getNextPage("tabA")
        assertFalse(vm.isHomePage("tabA"))
        assertTrue(vm.isHomePage("tabB"))
        assertEquals(2, vm.getNextPage("tabB"))
    }

    @Test
    fun `updatePageCont with null falls back to default`() {
        val vm = TestViewModel()
        vm.getHomePage(0)
        vm.updatePageCont(null)

        // 默认值是 0，curr=0 也是 0，因此不应再有下一页
        assertFalse(vm.hasNextPage())
    }
}
