package com.example.fragment.module.navigation.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.navigation.bean.NavigationListBean
import kotlinx.coroutines.launch

class NavigationViewModel : BaseViewModel() {

    val navigationResult = MutableLiveData<NavigationListBean>()

    fun getNavigation() {
        viewModelScope.launch {
            val request = HttpRequest("navi/json")
            navigationResult.postValue(get(request))
        }
    }

}