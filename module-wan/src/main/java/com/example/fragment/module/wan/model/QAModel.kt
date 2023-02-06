package com.example.fragment.module.wan.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.ArticleBean

abstract class QAModel : BaseViewModel() {

    val result = ArrayList<ArticleBean>().toMutableStateList()

    var refreshing by mutableStateOf(false)
    var loading by mutableStateOf(true)

    var pagerItemIndex = 0
    var pagerItemScrollOffset = 0

    abstract fun getHome()

    abstract fun getNext()

    abstract fun geList(page: Int)
}