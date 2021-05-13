package com.example.fragment.project.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.bean.HotKeyListBean
import com.example.fragment.library.common.model.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : BaseViewModel() {

    val hotKeyResult = MutableLiveData<HotKeyListBean>()

    fun getHotKey() {
        viewModelScope.launch(Dispatchers.Main) {
            hotKeyResult.postValue(get(HttpRequest("hotkey/json")))
        }
    }

}