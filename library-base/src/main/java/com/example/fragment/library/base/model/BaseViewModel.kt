package com.example.fragment.library.base.model

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    companion object{
        /**
         * 在转场动画结束后加载数据，
         * 用于解决过度动画卡顿问题，
         * 建议大于等于转场动画时间。
         */
        const val LOAD_DELAY_MILLIS = 250L
        const val DEFAULT_KEY = "null"
        const val DEFAULT_PAGE = 0
        const val PAGE_CONT = 1
    }

    /**
     *  通过 Map 来存储数据，方便 activityViewModels 使用
     */
    private var homePage: MutableMap<String, Int> = HashMap()
    private var currPage: MutableMap<String, Int> = HashMap()
    private var pageCont: MutableMap<String, Int> = HashMap()

    private val progress = MutableLiveData<Double>()
    /**
     * 获取首页
     */
    fun getHomePage(page: Int = DEFAULT_PAGE, key: String = DEFAULT_KEY): Int {
        this.homePage[key] = page
        this.currPage[key] = page
        this.pageCont[key] = page + 1
        return page
    }

    fun isHomePage(key: String = DEFAULT_KEY): Boolean {
        val homePage = this.homePage[key] ?: DEFAULT_PAGE
        val currPage = this.currPage[key] ?: DEFAULT_PAGE
        return homePage == currPage
    }

    /**
     * 获取下一页
     */
    fun getNextPage(key: String = DEFAULT_KEY): Int {
        val currPage = this.currPage[key] ?: DEFAULT_PAGE
        val nextPage = currPage + 1
        this.currPage[key] = nextPage
        return nextPage
    }

    fun hasNextPage(key: String = DEFAULT_KEY): Boolean {
        val currPage = this.currPage[key] ?: DEFAULT_PAGE
        val pageCont = this.pageCont[key] ?: PAGE_CONT
        return currPage < pageCont
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
        if (homePage == currPage) progress.postValue(num)
    }

}