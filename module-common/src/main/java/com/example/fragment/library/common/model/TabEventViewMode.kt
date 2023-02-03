package com.example.fragment.library.common.model

import com.example.fragment.library.base.model.BaseViewModel

class TabEventViewMode : BaseViewModel() {

    private var qaTabIndex = 0

    fun qaTabIndex(): Int {
        return qaTabIndex
    }

    fun setQATabIndex(index: Int) {
        qaTabIndex = index
    }

    private var projectTabIndex = 0

    fun projectTabIndex(): Int {
        return projectTabIndex
    }

    fun setProjectTabIndex(index: Int) {
        projectTabIndex = index
    }

}