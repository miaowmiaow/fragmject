package com.example.fragment.library.base.model

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    companion object{
        const val DEFAULT_KEY = "null"
        const val DEFAULT_PAGE = 0
        const val PAGE_CONT = 2
    }

    /**
     *  通过 Map 来存储数据，方便 activityViewModels 使用
     */
    private var homePage: MutableMap<String, Int> = HashMap()
    private var curPage: MutableMap<String, Int> = HashMap()
    private var pageCont: MutableMap<String, Int> = HashMap()

    private val progress = MutableLiveData<Double>()
    /**
     * 获取首页
     */
    fun getHomePage(page: Int = DEFAULT_PAGE, key: String = DEFAULT_KEY): Int {
        this.homePage[key] = page
        this.curPage[key] = page
        return page
    }

    fun isHomePage(key: String = DEFAULT_KEY): Boolean {
        val homePage = this.homePage[key] ?: DEFAULT_PAGE
        val curPage = this.curPage[key] ?: DEFAULT_PAGE
        return homePage == curPage
    }

    /**
     * 获取下一页
     */
    fun getNextPage(key: String = DEFAULT_KEY): Int {
        val curPage = this.curPage[key] ?: DEFAULT_PAGE
        val nextPage = curPage + 1
        this.curPage[key] = nextPage
        return nextPage
    }

    fun hasNextPage(key: String = DEFAULT_KEY): Boolean {
        val curPage = this.curPage[key] ?: DEFAULT_PAGE
        val pageCont = this.pageCont[key] ?: PAGE_CONT
        return curPage < pageCont
    }

    /**
     * 更新总页码
     */
    fun updatePageCont(pageCont: Int = PAGE_CONT, key: String = DEFAULT_KEY) {
        this.pageCont[key] = pageCont
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
     * @param num: 0.0 请求开始, 1.0 请求结束
     */
    fun updateProgress(num: Double) {
        if (homePage == curPage) progress.postValue(num)
    }

}