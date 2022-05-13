package com.example.fragment.module.user.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.utils.WanHelper

class SettingViewModel : BaseViewModel() {

    private val screenRecordResult: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            getScreenRecord()
        }
    }

    fun screenRecordResult(): LiveData<String> {
        return screenRecordResult
    }

    fun updateScreenRecord(status: String) {
        WanHelper.setScreenRecord(status)
        screenRecordResult.postValue(status)
    }

    private fun getScreenRecord() {
        WanHelper.getScreenRecord {
            screenRecordResult.postValue(it)
        }
    }

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