package com.example.fragment.module.wan.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.HotKeyBean
import com.example.fragment.library.common.bean.HotKeyListBean
import kotlinx.coroutines.launch

class HotKeyViewModel : BaseViewModel() {

    private val hotKeyResult: MutableLiveData<List<HotKeyBean>> by lazy {
        MutableLiveData<List<HotKeyBean>>().also {
            getHotKey()
        }
    }

    fun hotKeyResult(): LiveData<List<HotKeyBean>> {
        return hotKeyResult
    }

    /**
     * 获取搜索热词
     */
    private fun getHotKey() {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //构建请求体，传入请求参数
            val request = HttpRequest("hotkey/json")
            //以get方式发起网络请求
            val response = get<HotKeyListBean>(request)
            //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
            if (hotKeyResult.value == null) {
                transitionAnimationEnd(request, response)
            }
            //通过LiveData通知界面更新
            response.data?.let {
                hotKeyResult.postValue(it)
            }
        }
    }

}