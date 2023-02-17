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
    var time: Long = 0
) {
    fun getRefreshing(cid: String): Boolean {
        return refreshing[cid] ?: false
    }

    fun getLoading(cid: String): Boolean {
        return loading[cid] ?: false
    }

    fun getResult(cid: String): ArrayList<ArticleBean>? {
        return result[cid]
    }

}

class SystemViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(SystemState(time = 0))

    val uiState: StateFlow<SystemState> = _uiState.asStateFlow()

    fun init(cid: String) {
        if (!uiState.value.result.containsKey(cid)) {
            getHome(cid)
        }
    }

    fun getHome(cid: String) {
        _uiState.update {
            it.refreshing[cid] = true
            it.copy(time = System.currentTimeMillis())
        }
        getList(cid, getHomePage(key = cid))
    }

    fun getNext(cid: String) {
        _uiState.update {
            it.loading[cid] = false
            it.copy(time = System.currentTimeMillis())
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
            val request = HttpRequest("article/list/{page}/json")
                .putPath("page", page.toString())
                .putQuery("cid", cid)
            //以get方式发起网络请求
            val response = get<ArticleListBean>(request) { updateProgress(it) }
            //根据接口返回更新总页码
            response.data?.pageCount?.let { updatePageCont(it.toInt(), cid) }
            _uiState.update {
                response.data?.datas?.let { datas ->
                    if (isHomePage(cid)) {
                        it.result[cid] = arrayListOf()
                    }
                    it.result[cid]?.addAll(datas)
                }
                //设置下拉刷新状态
                it.refreshing[cid] = false
                //设置加载更多状态
                it.loading[cid] = hasNextPage()
                it.copy(time = System.currentTimeMillis())
            }
        }
    }

}