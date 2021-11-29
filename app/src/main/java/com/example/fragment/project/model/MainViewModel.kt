package com.example.fragment.project.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.HotKeyListBean
import com.example.fragment.library.common.bean.ProjectTreeListBean
import kotlinx.coroutines.launch

class MainViewModel : BaseViewModel() {

    val hotKeyResult = MutableLiveData<HotKeyListBean>()
    val projectListResult = MutableLiveData<ProjectTreeListBean>()

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
            hotKeyResult.postValue(response)
        }
    }

    /**
     * 获取项目分类
     */
    fun getProjectTree() {
        viewModelScope.launch {
            val request = HttpRequest("project/tree/json")
            val response = get<ProjectTreeListBean>(request)
            projectListResult.postValue(response)
        }
    }
}