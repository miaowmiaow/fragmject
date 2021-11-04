package com.example.fragment.project

import android.app.Application
import com.example.fragment.library.base.http.SimpleHttp

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initSimpleHttp()
    }

    private fun initSimpleHttp() {
        SimpleHttp.setBaseUrl("https://www.wanandroid.com/")
    }

}