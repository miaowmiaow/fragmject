package com.example.fragment.project.ui.main.home

import com.example.fragmject.core.model.Article
import com.example.fragmject.core.model.Banner
import com.example.fragmject.core.data.repository.HomeRepository
import com.example.fragmject.core.data.repository.PageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(mainDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `init observes Room and triggers refresh`() = runTest {
        val repo = FakeHomeRepository()
        val vm = HomeViewModel(homeRepo = repo)
        advanceUntilIdle()
        val state = vm.uiState.first()
        assertFalse("refreshing cleared after load", state.isRefreshing)
        assertEquals(1, repo.refreshHomeCount.get())
    }

    @Test
    fun `getHome triggers refresh again`() = runTest {
        val repo = FakeHomeRepository()
        val vm = HomeViewModel(homeRepo = repo)
        advanceUntilIdle()
        assertEquals(1, repo.refreshHomeCount.get())
        vm.getHome()
        advanceUntilIdle()
        assertEquals(2, repo.refreshHomeCount.get())
    }

    @Test
    fun `getNext triggers loadNextPage`() = runTest {
        val repo = FakeHomeRepository()
        val vm = HomeViewModel(homeRepo = repo)
        advanceUntilIdle()
        vm.getNext()
        advanceUntilIdle()
        assertEquals(1, repo.loadNextPageCount.get())
        assertEquals(1, repo.lastRequestedPage)
    }

    private class FakeHomeRepository : HomeRepository {
        val refreshHomeCount = AtomicInteger(0)
        val loadNextPageCount = AtomicInteger(0)
        var lastRequestedPage = -1

        override fun observeHomeArticles(): Flow<List<Article>> = emptyFlow()
        override fun observeHomeBanners(): Flow<List<Banner>> = emptyFlow()
        override suspend fun refreshHome(): Int? {
            refreshHomeCount.incrementAndGet()
            return 838
        }
        override suspend fun loadNextPage(page: Int): PageData? {
            loadNextPageCount.incrementAndGet()
            lastRequestedPage = page
            return PageData(articles = emptyList(), pageCount = 838)
        }
    }
}