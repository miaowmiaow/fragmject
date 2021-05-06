package com.example.fragment.module.home.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.get
import com.example.fragment.library.common.model.BaseViewModel
import com.example.fragment.module.home.bean.ArticleBean
import com.example.fragment.module.home.bean.BannerBean
import com.example.fragment.module.home.bean.ConfigBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : BaseViewModel() {

    val configResult = MutableLiveData<ConfigBean>()
    val bannerResult = MutableLiveData<BannerBean>()
    val articleResult = MutableLiveData<ArticleBean>()
    var page = 0

    fun getConfig(){
        viewModelScope.launch(Dispatchers.Main) {
            val httpRequest = HttpRequest("https://gitee.com/goweii/WanAndroidServer/raw/master/config/config.json")
            configResult.postValue(get(httpRequest))
        }
    }

    fun getBanner() {
        viewModelScope.launch(Dispatchers.Main) {
            val httpRequest = HttpRequest("banner/json")
            bannerResult.postValue(get(httpRequest))
        }
    }

    fun getArticleList(){
        viewModelScope.launch(Dispatchers.Main) {
            val httpRequest = HttpRequest("article/list/{page}/json")
            httpRequest.putPath("page", page.toString())
            articleResult.postValue(get(httpRequest))
        }
    }

}