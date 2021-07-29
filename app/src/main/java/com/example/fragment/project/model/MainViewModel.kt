package com.example.fragment.project.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.HotKeyListBean
import com.example.fragment.library.common.bean.TreeListBean
import com.example.fragment.library.common.utils.WanHelper
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    val hotKeyResult = MutableLiveData<HotKeyListBean>()

    // 获取热词接口
    fun getHotKey() {
        // 通过viewModelScope创建一个协程
        viewModelScope.launch {
            // 构建请求体，传入请求参数
            val request = HttpRequest("hotkey/json")
            // 以get方式发起网络请求
            val response = get<HotKeyListBean>(request)
            // 通过LiveData通知界面更新
            hotKeyResult.postValue(response)
        }
    }

    fun getTree() {
        viewModelScope.launch {
            val request = HttpRequest("tree/json")
            val response = get<TreeListBean>(request)
            response.data?.apply {
                WanHelper.setTreeList(this)
            }
        }
    }

}