package com.example.fragment.library.common.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.model.BaseViewModel

class TabEventViewMode : BaseViewModel() {

    private val mainTabReselected: MutableLiveData<Int> by lazy { MutableLiveData(-1) }

    fun mainTabReselected(): LiveData<Int> {
        return mainTabReselected
    }

    fun setMainTabReselected(index: Int) {
        mainTabReselected.value = index
    }

    fun resetMainTabReselected() {
        mainTabReselected.value = -1
    }

    private var qaTabIndex = 0

    fun qaTabIndex(): Int {
        return qaTabIndex
    }

    fun setQATabIndex(index: Int) {
        qaTabIndex = index
    }

}