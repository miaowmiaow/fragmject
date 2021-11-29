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

    /**
     * 获取导航数据
     */
    fun getNavigation() {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("navi/json")
            //以get方式发起网络请求
            val response = get<NavigationListBean>(request) { progress(it) }
            //通过LiveData通知界面更新
            navigationResult.postValue(response)
        }
    }

    /**
     * 获取项目分类
     */
    fun getSystemTree() {
        viewModelScope.launch {
            val request = HttpRequest("tree/json")
            val response = get<TreeListBean>(request) { progress(it) }
            systemTreeResult.postValue(response)
        }
    }

}