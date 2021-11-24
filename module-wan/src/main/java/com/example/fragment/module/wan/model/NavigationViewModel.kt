package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.TreeListBean
import com.example.fragment.module.wan.bean.NavigationListBean
import kotlinx.coroutines.launch

class NavigationViewModel : BaseViewModel() {

    val navigationResult = MutableLiveData<NavigationListBean>()
    val systemTreeResult = MutableLiveData<TreeListBean>()

    fun getNavigation() {
        viewModelScope.launch {
            val request = HttpRequest("navi/json")
            val response = get<NavigationListBean>(request) { progress(it) }
            navigationResult.postValue(response)
        }
    }

    fun getSystemTree() {
        viewModelScope.launch {
            val request = HttpRequest("tree/json")
            val response = get<TreeListBean>(request) { progress(it) }
            systemTreeResult.postValue(response)
        }
    }

}