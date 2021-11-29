package com.example.fragment.module.wan.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.RegisterBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareArticleModel : BaseViewModel() {

    val shareArticleResult = MutableLiveData<HttpResponse>()

    fun getShareArticle(title: String, link: String) {
        //通过viewModelScope创建一个协程
        viewModelScope.launch(Dispatchers.Main) {
            //构建请求体，传入请求参数
            val request = HttpRequest("lg/user_article/add/json")
                .putParam("title", title)
                .putParam("link", link)
            //以get方式发起网络请求
            val response = post<RegisterBean>(request) { progress(it) }
            //通过LiveData通知界面更新
            shareArticleResult.postValue(response)
        }
    }

}