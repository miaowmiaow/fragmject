package com.example.fragment.library.common.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.http.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {

    fun collect(title: String, author: String, link: String): MutableLiveData<HttpResponse> {
        val result = MutableLiveData<HttpResponse>()
        viewModelScope.launch(Dispatchers.Main) {
            val request = HttpRequest("lg/collect/add/json")
            request.putParam("title", title)
            request.putParam("author", author)
            request.putParam("link", link)
            result.postValue(post(request))
        }
        return result
    }

    fun collect(id: String): MutableLiveData<HttpResponse> {
        val result = MutableLiveData<HttpResponse>()
        viewModelScope.launch(Dispatchers.Main) {
            result.postValue(post(HttpRequest("lg/collect/{id}/json").putPath("id", id)))
        }
        return result
    }

    fun unCollect(id: String): MutableLiveData<HttpResponse> {
        val result = MutableLiveData<HttpResponse>()
        viewModelScope.launch(Dispatchers.Main) {
            val request = HttpRequest("lg/uncollect_originId/{id}/json")
            request.putPath("id", id)
            result.postValue(post(request))
        }
        return result
    }

}