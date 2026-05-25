package com.example.miaow.base.vm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

//转场动画时间
const val TRANSITION_TIME = 350

abstract class BaseViewModel : ViewModel() {

    companion object {
        const val DEFAULT_KEY = "null"
        const val DEFAULT_VALUE = 0
    }

    /**
     *  通过 ConcurrentHashMap 来存储分页状态，避免多协程并发更新时的不一致问题
     */
    private val homePage: MutableMap<String, Int> = ConcurrentHashMap()
    private val currPage: MutableMap<String, Int> = ConcurrentHashMap()
    private val pageCont: MutableMap<String, Int> = ConcurrentHashMap()

    /**
     * 初始化分页：将 home/curr 置为 page，pageCont 暂置为 page+1
     * 命名上 "getHomePage" 略有歧义，但为避免破坏既有调用方暂保留。
     * page：首页初始值
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
        val pageCont = this.pageCont[key] ?: DEFAULT_VALUE
        return currPage < pageCont
    }

    /**
     * 更新总页码
     */
    fun updatePageCont(pageCont: Int?, key: String = DEFAULT_KEY) {
        this.pageCont[key] = pageCont ?: DEFAULT_VALUE
    }

    /**
     * 用于解决过度动画卡顿问题
     */
    suspend fun transitionAnimationEnd(time: Long) {
        withContext(Dispatchers.Main) {
            //如果请求结束时间小于转场动画时间则等待转场动画结束后返回数据
            val delayMillis = TRANSITION_TIME - time
            if (delayMillis > 0) {
                delay(delayMillis)
            }
        }
    }
}