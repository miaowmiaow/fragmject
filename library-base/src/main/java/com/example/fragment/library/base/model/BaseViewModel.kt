package com.example.fragment.library.base.model

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val progress = MutableLiveData<Double>()

    private var homePage = 0
    private var curPage = 0
    private var pageCont = 1

    /**
     * 获取首页
     * 默认从0开始
     */
    fun getHomePage(homePage: Int = 0): Int {
        this.homePage = homePage
        this.curPage = homePage
        return homePage
    }

    /**
     * 获取下页
     */
    fun getNextPage(): Int {
        this.curPage = curPage + 1
        return curPage
    }

    /**
     * 更新总页码
     */
    fun updatePageCont(pageCont: Int = 1) {
        this.pageCont = pageCont
    }

    /**
     * 是否首页
     */
    fun isHomePage(): Boolean {
        return homePage == curPage
    }

    /**
     * 还有下页
     */
    fun hasNextPage(): Boolean {
        return curPage < pageCont
    }

    /**
     * @param start 请求开始
     * @param end   请求结束
     */
    fun progressState(owner: LifecycleOwner, start: () -> Unit, end: () -> Unit) {
        progress.observe(owner) {
            when (it) {
                0.0 -> start.invoke()
                1.0 -> end.invoke()
            }
        }
    }

    fun progress(num: Double) {
        if (homePage == curPage) progress.postValue(num)
    }

}