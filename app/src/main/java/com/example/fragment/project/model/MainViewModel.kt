package com.example.fragment.project.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.project.bean.HotKeyBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : BaseViewModel() {

    val hotKeyResult = MutableLiveData<HotKeyBean>()

    fun getHotKey() {
        viewModelScope.launch(Dispatchers.Main) {
            hotKeyResult.postValue(get(HttpRequest("hotkey/json")))
        }
    }

}