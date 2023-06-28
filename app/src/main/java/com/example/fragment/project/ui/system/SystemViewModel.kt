package com.example.fragment.project.ui.system

import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.project.bean.ArticleBean
import com.example.fragment.project.bean.ArticleListBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemState(
    val refreshing: MutableMap<String, Boolean> = HashMap(),
    val loading: MutableMap<String, Boolean> = HashMap(),
    val result: MutableMap<String, ArrayList<ArticleBean>> = HashMap(),
    var updateTime: Long = 0
) {
    fun getRefreshing(cid: String): Boolean {
        return refreshing[cid] ?: true
    }

    fun getLoading(cid: String): Boolean {
        return loading[cid] ?: false
    }

    fun getResult(cid: String): ArrayList<ArticleBean>? {
        return result[cid]
    }

}

class SystemViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SystemState())

    val uiState: StateFlow<SystemState> = _uiState.asStateFlow()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            getHome(cid)
        }
    }

    fun getHome(cid: String) {
        _uiState.update {
            it.refreshing[cid] = true
            it.copy(updateTime = System.nanoTime())
        }
        getList(cid, getHomePage(key = cid))
    }

    fun getNext(cid: String) {
        _uiState.update {
            it.loading[cid] = false
            it.copy(updateTime = System.nanoTime())
        }
        getList(cid, getNextPage(cid))
    }

    /**
     * 获取知识体系下的文章
     * 	cid 分类id
     * 	page 0开始
     */
    private fun getList(cid: String, page: Int) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("article/list/{page}/json").putPath("page", page.toString()).putQuery("cid", cid)
            //以get方式发起网络请求
            val response = get<ArticleListBean>(request)
            //根据接口返回更新总页码
            updatePageCont(response.data?.pageCount?.toInt(), cid)
            _uiState.update { state ->
                //如果result.isNullOrEmpty()，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
                if (state.result[cid].isNullOrEmpty()) {
                    transitionAnimationEnd(request, response)
                }
                response.data?.datas?.let { datas ->
                    if (isHomePage(cid)) {
                        state.result[cid] = arrayListOf()
                    }
                    datas.forEach {
                        state.result[cid]?.add(it.build())
                    }
                }
                //设置下拉刷新状态
                state.refreshing[cid] = false
                //设置加载更多状态
                state.loading[cid] = hasNextPage(cid)
                state.copy(updateTime = System.nanoTime())
            }
        }
    }

}