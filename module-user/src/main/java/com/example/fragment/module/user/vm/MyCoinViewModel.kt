package com.example.fragment.module.user.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fragment.library.base.http.HttpRequest
import com.example.fragment.library.base.http.get
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.library.common.bean.MyCoinListBean
import com.example.fragment.library.common.bean.UserCoinBean
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MyCoinViewModel : BaseViewModel() {

    private val userCoinResult = MutableLiveData<UserCoinBean>()

    fun userCoinResult(): LiveData<UserCoinBean> {
        return userCoinResult
    }

    private val myCoinResult: MutableLiveData<MyCoinListBean> by lazy {
        MutableLiveData<MyCoinListBean>().also {
            getMyCoinHome()
        }
    }

    fun myCoinResult(): LiveData<MyCoinListBean> {
        return myCoinResult
    }

    fun clearMyCoinResult() {
        myCoinResult.value = null
    }

    fun getMyCoinHome() {
        //通过viewModelScope创建一个协程
        viewModelScope.launch {
            //通过async获取需要展示的数据
            val userCoin = async { userCoin() }
            val myCoin = async { getMyCoin(getHomePage(1)) }
            //通过LiveData通知界面更新
            userCoinResult.postValue(userCoin.await())
            myCoinResult.postValue(myCoin.await())
        }
    }

    fun getMyCoinNext() {
        viewModelScope.launch {
            myCoinResult.postValue(getMyCoin(getNextPage()))
        }
    }

    /**
     * 获取个人积分
     */
    private suspend fun userCoin(): UserCoinBean {
        val request = HttpRequest("lg/coin/userinfo/json")
        return coroutineScope { get(request) }
    }

    /**
     * 获取个人积分获取列表
     * page 1开始
     */
    private suspend fun getMyCoin(page: Int): MyCoinListBean {
        //构建请求体，传入请求参数
        val request = HttpRequest("lg/coin/list/{page}/json").putPath("page", page.toString())
        //以get方式发起网络请求
        val response = coroutineScope { get<MyCoinListBean>(request) { updateProgress(it) } }
        //如果LiveData.value == null，则在转场动画结束后加载数据，用于解决过度动画卡顿问题
        if (myCoinResult.value == null) {
            transitionAnimationEnd(request, response)
        }
        //根据接口返回更新总页码
        response.data?.pageCount?.let { updatePageCont(it.toInt()) }
        return response
    }

}