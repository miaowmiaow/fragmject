package com.example.fragment.library.common.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.model.BaseViewModel

class TabEventViewMode : BaseViewModel() {

    private val homeTab: MutableLiveData<Int> by lazy { MutableLiveData<Int>(0) }

    fun homeTab(): LiveData<Int> {
        return homeTab
    }

    fun setHomeTab(isTop: Int){
        homeTab.value = isTop
    }

    private val navigationTab: MutableLiveData<Int> by lazy { MutableLiveData<Int>(0) }

    fun navigationTab(): LiveData<Int> {
        return navigationTab
    }

    fun setNavigationTab(isTop: Int){
        navigationTab.value = isTop
    }

    private var qaTabIndex= 0

    fun qaTabIndex(): Int {
        return qaTabIndex
    }

    fun setQATabIndex(index: Int){
        qaTabIndex = index
    }

    private var qaQuizFirstVisibleItemIndex = 0

    fun qaQuizFirstVisibleItemIndex(): Int {
        return qaQuizFirstVisibleItemIndex
    }

    fun setQAQuizFirstVisibleItemIndex(index: Int){
        qaQuizFirstVisibleItemIndex = index
    }

    private var qaQuizFirstVisibleItemScrollOffset = 0

    fun qaQuizFirstVisibleItemScrollOffset(): Int {
        return qaQuizFirstVisibleItemScrollOffset
    }

    fun setQAQuizFirstVisibleItemScrollOffset(offset: Int){
        qaQuizFirstVisibleItemScrollOffset = offset
    }

    private var qaSquareFirstVisibleItemIndex = 0

    fun qaSquareFirstVisibleItemIndex(): Int {
        return qaSquareFirstVisibleItemIndex
    }

    fun setQASquareFirstVisibleItemIndex(index: Int){
        qaSquareFirstVisibleItemIndex = index
    }

    private var qaSquareFirstVisibleItemScrollOffset = 0

    fun qaSquareFirstVisibleItemScrollOffset(): Int {
        return qaSquareFirstVisibleItemScrollOffset
    }

    fun setQASquareFirstVisibleItemScrollOffset(offset: Int){
        qaSquareFirstVisibleItemScrollOffset = offset
    }

    private val projectTab: MutableLiveData<Int> by lazy { MutableLiveData<Int>(0) }

    fun projectTab(): LiveData<Int> {
        return projectTab
    }

    fun setProjectTab(isTop: Int){
        projectTab.value = isTop
    }

    private val userTab: MutableLiveData<Int> by lazy { MutableLiveData<Int>(0) }

    fun userTab(): LiveData<Int> {
        return userTab
    }

    fun setUserTab(isTop: Int){
        userTab.value = isTop
    }

}