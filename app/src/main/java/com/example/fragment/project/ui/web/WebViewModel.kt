package com.example.fragment.project.ui.web

import com.example.fragment.project.utils.WanHelper
import com.example.miaow.base.vm.BaseViewModel

class WebViewModel : BaseViewModel() {

    private var history: MutableList<String> = ArrayList()

    init {
        WanHelper.getHistoryWeb {
            history.clear()
            history.addAll(it)
        }
    }

    fun updateHistoryWeb(key: String) {
        if (history.contains(key)) {
            history.remove(key)
        }
        history.add(0, key)
        WanHelper.setHistoryWeb(history)
    }

}