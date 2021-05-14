package com.example.fragment.project.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.HotKeyListBean
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.system.bean.TreeListBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : BaseViewModel() {

    val hotKeyResult = MutableLiveData<HotKeyListBean>()

    fun getHotKey() {
        viewModelScope.launch(Dispatchers.Main) {
            hotKeyResult.postValue(get(HttpRequest("hotkey/json")))
        }
    }

    fun getTree() {
        viewModelScope.launch(Dispatchers.Main) {
            val request = HttpRequest("tree/json")
            val response = get<TreeListBean>(request)
            response.data?.apply {
                WanHelper.setTreeList(this)
            }
        }
    }

}