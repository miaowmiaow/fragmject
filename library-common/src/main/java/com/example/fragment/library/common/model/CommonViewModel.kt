package com.example.fragment.library.common.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.*
import kotlinx.coroutines.launch

class CommonViewModel : BaseViewModel() {

    val hotKeyResult = MutableLiveData<List<HotKeyBean>>()
    val navigationResult = MutableLiveData<List<NavigationBean>>()
    val projectTreeResult = MutableLiveData<List<ProjectTreeBean>>()
    val systemTreeResult = MutableLiveData<List<SystemTreeBean>>()

    /**
     * 获取搜索热词
     */
    fun getHotKey() {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("hotkey/json")
            //以get方式发起网络请求
            val response = get<HotKeyListBean>(request)
            //通过LiveData通知界面更新
            response.data?.let {
                hotKeyResult.postValue(it)
            }
        }
    }

    /**
     * 获取导航数据
     */
    fun getNavigation(show: Boolean = false) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("navi/json")
            //以get方式发起网络请求
            val response = get<NavigationListBean>(request) { if (show) progress(it) }
            //通过LiveData通知界面更新
            response.data?.let {
                navigationResult.postValue(it)
            }
        }
    }

    /**
     * 获取项目分类
     */
    fun getProjectTree(show: Boolean = false) {
        viewModelScope.launch {
            val request = HttpRequest("project/tree/json")
            val response = get<ProjectTreeListBean>(request) { if (show) progress(it) }
            response.data?.let {
                projectTreeResult.postValue(it)
            }
        }
    }

    /**
     * 获取项目分类
     */
    fun getSystemTree(show: Boolean = false) {
        viewModelScope.launch {
            val request = HttpRequest("tree/json")
            val response = get<SystemTreeListBean>(request) { if (show) progress(it) }
            response.data?.let {
                systemTreeResult.postValue(it)
            }
        }
    }

}