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