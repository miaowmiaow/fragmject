package com.example.fragmject.feature.picture.state

import com.example.fragmject.core.domain.repository.PictureEditRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 编辑后 uri 解析契约。
 *
 * 首要回归点是「进程重建」场景：内存里的替换关系随进程丢失，只剩持久记录，
 * 而预览返回栈仍带着编辑前的原始 uri——解析必须仍落到新图，否则预览会去解码
 * 一张已被删除的原图。其次是连续编辑（A→B→C）必须一路走到 C。
 */
class EditedUriResolverTest {

    @Test
    fun `没有替换记录时原样返回`() = runTest {
        val resolver = EditedUriResolver(FakeRepository())
        assertEquals(OLD, resolver.resolve(OLD))
    }

    @Test
    fun `进程重建后仅凭持久记录也能解析到新图`() = runTest {
        // 新实例 = 内存缓存为空，等价于进程重建
        val resolver = EditedUriResolver(FakeRepository(mutableMapOf(OLD to NEW)))
        assertEquals(NEW, resolver.resolve(OLD))
    }

    @Test
    fun `同一张图被连续编辑时沿链走到最终 uri`() = runTest {
        val repository = FakeRepository(mutableMapOf(OLD to MID, MID to NEW))
        assertEquals(NEW, EditedUriResolver(repository).resolve(OLD))
    }

    @Test
    fun `重复解析不再查库`() = runTest {
        val repository = FakeRepository(mutableMapOf(OLD to NEW))
        val resolver = EditedUriResolver(repository)

        resolver.resolve(OLD)
        // 首次要查两次：一次命中 OLD，一次确认链尾 NEW 没有后续替换
        assertEquals(2, repository.lookupCount)

        resolver.resolve(OLD)
        assertEquals("第二次解析应全部命中缓存", 2, repository.lookupCount)
    }

    @Test
    fun `已确认无替换的 uri 后续被本进程编辑后仍能解析到新图`() = runTest {
        val resolver = EditedUriResolver(FakeRepository())
        assertEquals(OLD, resolver.resolve(OLD))

        resolver.remember(OLD, NEW)

        assertEquals(NEW, resolver.resolve(OLD))
    }

    @Test
    fun `本进程内的替换优先于持久记录`() = runTest {
        val repository = FakeRepository(mutableMapOf(OLD to NEW))
        val resolver = EditedUriResolver(repository)
        resolver.remember(OLD, "content://media/newer")

        assertEquals("content://media/newer", resolver.resolve(OLD))
    }

    @Test
    fun `持久化写入落到仓库`() = runTest {
        val repository = FakeRepository()
        EditedUriResolver(repository).persist(OLD, NEW)

        assertEquals(NEW, repository.stored[OLD])
    }

    @Test
    fun `异常成环的数据不会死循环`() = runTest {
        val repository = FakeRepository(mutableMapOf(OLD to NEW, NEW to OLD))
        val resolved = EditedUriResolver(repository).resolve(OLD)
        assertTrue("应终止并停在环内某个 uri，实际=$resolved", resolved == OLD || resolved == NEW)
    }

    private class FakeRepository(
        val stored: MutableMap<String, String> = mutableMapOf(),
    ) : PictureEditRepository {
        var lookupCount = 0

        override suspend fun recordReplacement(oldUri: String, newUri: String) {
            stored[oldUri] = newUri
        }

        override suspend fun findReplacement(oldUri: String): String? {
            lookupCount++
            return stored[oldUri]
        }
    }

    private companion object {
        const val OLD = "content://media/1"
        const val MID = "content://media/2"
        const val NEW = "content://media/3"
    }
}
