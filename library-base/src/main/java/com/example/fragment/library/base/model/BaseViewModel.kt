package com.example.fragment.library.base.model

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    private val progress = MutableLiveData<Double>()

    private var homePage: MutableMap<String, Int> = HashMap()
    private var curPage: MutableMap<String, Int> = HashMap()
    private var pageCont: MutableMap<String, Int> = HashMap()

    companion object{
        const val DEFAULT_PAGE = 0
        const val PAGE_CONT = 2
    }

    /**
     * 获取首页
     * 默认从0开始
     */
    fun getHomePage(page: Int = DEFAULT_PAGE, key: String = "null"): Int {
        this.homePage[key] = page
        this.curPage[key] = page
        return page
    }

    /**
     * 获取下一页
     */
    fun getNextPage(key: String = "null"): Int {
        val curPage = this.curPage[key] ?: DEFAULT_PAGE
        val nextPage = curPage + 1
        this.curPage[key] = nextPage
        return nextPage
    }

    /**
     * 更新总页码
     */
    fun updatePageCont(pageCont: Int = PAGE_CONT, key: String = "null") {
        this.pageCont[key] = pageCont
    }

    /**
     * 是否首页
     */
    fun isHomePage(key: String = "null"): Boolean {
        val homePage = this.homePage[key] ?: DEFAULT_PAGE
        val curPage = this.curPage[key] ?: DEFAULT_PAGE
        return homePage == curPage
    }

    /**
     * 还有下页
     */
    fun hasNextPage(key: String = "null"): Boolean {
        val curPage = this.curPage[key] ?: DEFAULT_PAGE
        val pageCont = this.pageCont[key] ?: PAGE_CONT
        return curPage < pageCont
    }

    fun progress(owner: LifecycleOwner, start: () -> Unit, end: () -> Unit) {
        progress.observe(owner) {
            when (it) {
                0.0 -> start.invoke()
                1.0 -> end.invoke()
            }
        }
    }

    /**
     * @param 0.0 请求开始
     * @param 1.0 请求结束
     */
    fun updateProgress(num: Double) {
        if (homePage == curPage) progress.postValue(num)
    }

}