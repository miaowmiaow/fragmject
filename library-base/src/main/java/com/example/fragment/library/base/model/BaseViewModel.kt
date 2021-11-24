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

    fun getHomePage(homePage: Int = 0): Int {
        this.homePage = homePage
        this.curPage = homePage
        return homePage
    }

    fun getNextPage(): Int {
        this.curPage = curPage + 1
        return curPage
    }

    fun updatePageCont(pageCont: Int = 1) {
        this.pageCont = pageCont
    }

    fun isHomePage(): Boolean {
        return homePage == curPage
    }

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
        viewModelScope.launch {
            if(homePage == curPage){
                progress.postValue(num)
            }
        }
    }

}