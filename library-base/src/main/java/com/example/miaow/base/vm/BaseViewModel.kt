package com.example.miaow.base.vm

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.miaow.base.http.HttpRequest
import com.example.miaow.base.http.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

abstract class BaseViewModel : ViewModel() {

    companion object {
        //动画时间见WanNavGraph
        const val TRANSITION_ANIMATION_TIME = 350L
        const val DEFAULT_KEY = "null"
        const val DEFAULT_VALUE = 0
        const val PAGE_CONT = 1
    }

    /**
     *  通过 Map 来存储数据，方便 ViewModel 使用
     */
    private val homePage: MutableMap<String, Int> = HashMap()
    private val currPage: MutableMap<String, Int> = HashMap()
    private val pageCont: MutableMap<String, Int> = HashMap()

    private val progress = MutableLiveData<Double>()

    /**
     * 获取首页
     */
    fun getHomePage(page: Int = DEFAULT_VALUE, key: String = DEFAULT_KEY): Int {
        this.homePage[key] = page
        this.currPage[key] = page
        this.pageCont[key] = page + 1
        return page
    }

    fun isHomePage(key: String = DEFAULT_KEY): Boolean {
        val homePage = this.homePage[key] ?: DEFAULT_VALUE
        val currPage = this.currPage[key] ?: DEFAULT_VALUE
        return homePage == currPage
    }

    /**
     * 获取下一页
     */
    fun getNextPage(key: String = DEFAULT_KEY): Int {
        val currPage = this.currPage[key] ?: DEFAULT_VALUE
        val nextPage = if (hasNextPage(key)) currPage + 1 else currPage
        this.currPage[key] = nextPage
        return nextPage
    }

    fun hasNextPage(key: String = DEFAULT_KEY): Boolean {
        val currPage = this.currPage[key] ?: DEFAULT_VALUE
        val pageCont = this.pageCont[key] ?: PAGE_CONT
        return currPage < pageCont
    }

    /**
     * 更新总页码
     */
    fun updatePageCont(pageCont: Int? = PAGE_CONT, key: String = DEFAULT_KEY) {
        pageCont?.let {
            this.pageCont[key] = it
        }
    }

    /**
     * 请求进度，0.0 请求开始, 1.0 请求结束
     */
    fun progress(owner: LifecycleOwner, start: () -> Unit, end: () -> Unit) {
        progress.observe(owner) {
            when (it) {
                0.0 -> start.invoke()
                1.0 -> end.invoke()
            }
        }
    }

    /**
     * 更新进度
     * @param num: 0.0 请求开始, 1.0 请求结束
     */
    fun updateProgress(num: Double) {
        if (homePage == currPage) progress.postValue(num)
    }

    /**
     * 用于解决过度动画卡顿问题
     */
    suspend fun transitionAnimationEnd(time: Long) {
        withContext(Dispatchers.Main) {
            //如果请求结束时间小于转场动画时间则等待转场动画结束后返回数据
            val delayMillis = TRANSITION_ANIMATION_TIME - time
            if (delayMillis > 0) {
                delay(delayMillis)
            }
        }
    }
}