package com.example.fragment.module.user.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.library.common.utils.WanHelper

class SettingViewModel : BaseViewModel() {

    private val uiModeResult: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            getUiMode()
        }
    }

    fun uiModeResult(): LiveData<String> {
        return uiModeResult
    }

    fun updateUiMode(status: String) {
        WanHelper.setUiMode(status)
        uiModeResult.postValue(status)
    }

    private fun getUiMode() {
        WanHelper.getUiMode {
            uiModeResult.postValue(it)
        }
    }

}