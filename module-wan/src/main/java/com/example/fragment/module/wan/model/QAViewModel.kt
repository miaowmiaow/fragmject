package com.example.fragment.module.wan.model

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean
import com.example.fragment.library.common.bean.ArticleListBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QAState(
    val refreshingMap: MutableMap<String, Boolean> = HashMap(),
    val loadingMap: MutableMap<String, Boolean> = HashMap(),
    val resultMap: MutableMap<String, ArrayList<ArticleBean>> = HashMap(),
    var time: Long = 0
) {
    fun getRefreshing(tab: String): Boolean {
        return refreshingMap[tab] ?: false
    }

    fun getLoading(tab: String): Boolean {
        return loadingMap[tab] ?: false
    }

    fun getResult(tab: String): ArrayList<ArticleBean>? {
        return resultMap[tab]
    }

}

class QAViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(QAState(time = 0))

    val uiState: StateFlow<QAState> = _uiState.asStateFlow()

    fun init(tab: String) {
        if (!uiState.value.resultMap.containsKey(tab)) {
            getHome(tab)
        }
    }

    fun getHome(tab: String) {
        _uiState.update {
            it.refreshingMap[tab] = true
            it.copy(time = System.currentTimeMillis())
        }
        getList(tab, getHomePage(1, tab))
    }

    fun getNext(tab: String) {
        _uiState.update {
            it.loadingMap[tab] = false
            it.copy(time = System.currentTimeMillis())
        }
        getList(tab, getNextPage(tab))
    }

    /**
     * tab 分类
     * page 1开始
     */
    private fun getList(tab: String, page: Int) {
        viewModelScope.launch {
            val request = HttpRequest()
            if (tab == "问答") {
                request.setUrl("wenda/list/{page}/json")
            } else if (tab == "广场") {
                request.setUrl("user_article/list/{page}/json")
            }
            request.putPath("page", page.toString())
            val response = get<ArticleListBean>(request) { updateProgress(it) }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt(), tab) }
            _uiState.update {
                response.data?.datas?.let { datas ->
                    if (isHomePage(tab)) {
                        it.resultMap[tab] = arrayListOf()
                    }
                    it.resultMap[tab]?.addAll(datas)
                }
                //设置下拉刷新状态
                it.refreshingMap[tab] = false
                //设置加载更多状态
                it.loadingMap[tab] = hasNextPage()
                it.copy(time = System.currentTimeMillis())
            }
        }
    }

}