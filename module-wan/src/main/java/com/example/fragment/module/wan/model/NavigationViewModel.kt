package com.example.fragment.module.wan.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.NavigationBean
import com.example.fragment.library.common.bean.NavigationListBean
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NavigationViewModel : BaseViewModel() {

    private val navigationResult: MutableLiveData<List<NavigationBean>> by lazy {
        MutableLiveData<List<NavigationBean>>().also {
            getNavigation()
        }
    }

    fun navigationResult(): LiveData<List<NavigationBean>> {
        return navigationResult
    }

    /**
     * 获取导航数据
     */
    private fun getNavigation() {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (navigationResult.value == null) {
                delay(LOAD_DELAY_MILLIS)
            }
            //构建请求体，传入请求参数
            val request = HttpRequest("navi/json")
            //以get方式发起网络请求
            val response = get<NavigationListBean>(request) { updateProgress(it) }
            //通过LiveData通知界面更新
            response.data?.let {
                navigationResult.postValue(it)
            }
        }
    }

}