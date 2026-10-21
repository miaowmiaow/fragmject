package com.example.fragmject.feature.collection.ui.share

import androidx.paging.PagingData
import com.example.fragmject.core.domain.repository.MyRepository
import com.example.fragmject.core.domain.result.SimpleResult
import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Coin
import com.example.fragmject.core.model.MyCoin
import com.example.fragmject.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [ShareArticleViewModel] 测试：表单校验前置、提交结果映射与消息复位。
 *
 * 校验失败**不得**发起请求，这是避免空标题打请求的服务端浪费；成功时消息为空要有兜底文案。
 */
class ShareArticleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit with blank title - reports validation error without request`() = runTest {
        val repo = FakeMyRepository()
        val viewModel = ShareArticleViewModel(repo)
        viewModel.updateLink("https://a.com")

        viewModel.submit()

        assertEquals("文章标题不能为空", viewModel.uiState.value.message)
        assertTrue(repo.sharedTitles.isEmpty())
    }

    @Test
    fun `submit with blank link - reports validation error without request`() = runTest {
        val repo = FakeMyRepository()
        val viewModel = ShareArticleViewModel(repo)
        viewModel.updateTitle("标题")

        viewModel.submit()

        assertEquals("文章链接不能为空", viewModel.uiState.value.message)
        assertTrue(repo.sharedTitles.isEmpty())
    }

    @Test
    fun `submit success - marks success and keeps repository message`() = runTest {
        val repo = FakeMyRepository(result = SimpleResult(success = true, message = "已提交"))
        val viewModel = ShareArticleViewModel(repo)
        viewModel.updateTitle("标题")
        viewModel.updateLink("https://a.com")

        viewModel.submit()

        val state = viewModel.uiState.value
        assertTrue(state.success)
        assertFalse(state.isLoading)
        assertEquals("已提交", state.message)
        assertEquals("标题" to "https://a.com", repo.sharedTitles.single())
    }

    @Test
    fun `submit success without message - falls back to default text`() = runTest {
        val repo = FakeMyRepository(result = SimpleResult(success = true, message = ""))
        val viewModel = ShareArticleViewModel(repo)
        viewModel.updateTitle("标题")
        viewModel.updateLink("https://a.com")

        viewModel.submit()

        assertEquals("分享成功", viewModel.uiState.value.message)
    }

    @Test
    fun `submit failure - keeps success false and repository message`() = runTest {
        val repo = FakeMyRepository(result = SimpleResult(success = false, message = "链接不合法"))
        val viewModel = ShareArticleViewModel(repo)
        viewModel.updateTitle("标题")
        viewModel.updateLink("https://a.com")

        viewModel.submit()

        val state = viewModel.uiState.value
        assertFalse(state.success)
        assertFalse(state.isLoading)
        assertEquals("链接不合法", state.message)
    }

    @Test
    fun `reset message - clears message only`() = runTest {
        val repo = FakeMyRepository(result = SimpleResult(success = false, message = "失败"))
        val viewModel = ShareArticleViewModel(repo)
        viewModel.updateTitle("标题")
        viewModel.updateLink("https://a.com")
        viewModel.submit()

        viewModel.resetMessage()

        val state = viewModel.uiState.value
        assertEquals("", state.message)
        assertEquals("标题", state.title)
        assertEquals("https://a.com", state.link)
    }
}

private class FakeMyRepository(
    private val result: SimpleResult = SimpleResult(success = true),
) : MyRepository {

    val sharedTitles = mutableListOf<Pair<String, String>>()

    override suspend fun getUserCoin(): Coin? = null

    override fun getMyCoinPagingData(): Flow<PagingData<MyCoin>> = emptyFlow()

    override fun getMySharePagingData(): Flow<PagingData<Article>> = emptyFlow()

    override suspend fun shareArticle(title: String, link: String): SimpleResult {
        sharedTitles += title to link
        return result
    }

    override suspend fun collectArticle(id: String): SimpleResult = SimpleResult(success = true)

    override suspend fun uncollectArticle(id: String): SimpleResult = SimpleResult(success = true)
}
