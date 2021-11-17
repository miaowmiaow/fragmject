package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.module.wan.bean.NavigationListBean
import kotlinx.coroutines.launch

class NavigationViewModel : ViewModel() {

    val navigationResult = MutableLiveData<NavigationListBean>()

    fun getNavigation() {
        viewModelScope.launch {
            val request = HttpRequest("navi/json")
            val response = get<NavigationListBean>(request)
            navigationResult.postValue(response)
        }
    }

}